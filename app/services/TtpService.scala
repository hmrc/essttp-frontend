/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import actionsmodel.AuthenticatedJourneyRequest
import cats.Eq
import config.AppConfig
import connectors.{CallEligibilityApiRequest, TtpConnector}
import requests.RequestSupport.hc
import essttp.crypto.CryptoFormat
import essttp.journey.model.Journey.ComputedTaxId
import essttp.journey.model.{Journey, JourneyStage, PaymentPlanAnswers, UpfrontPaymentAnswers}
import essttp.rootmodel.*
import essttp.rootmodel.bank.AccountNumber
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.ttp.*
import essttp.rootmodel.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.rootmodel.ttp.affordablequotes.*
import essttp.rootmodel.ttp.arrangement.*
import essttp.rootmodel.ttp.eligibility.*
import essttp.utils.Errors
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import services.TtpService.{deriveCustomerDetail, maxPlanLength, padLeftWithZeros, toDebtItemCharge}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.{HttpException, UpstreamErrorResponse}
import util.JourneyLogger

import java.time.{LocalDate, ZoneOffset}
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** Time To Pay (Ttp) Service.
  */
@Singleton
class TtpService @Inject() (
  ttpConnector: TtpConnector,
  auditService: AuditService,
  appConfig:    AppConfig
)(using ExecutionContext) {

  import appConfig.eligibilityReqIdentificationFlag

  given CryptoFormat  = CryptoFormat.NoOpCryptoFormat
  given Eq[TaxRegime] = Eq.fromUniversalEquals

  def determineEligibility(
    journey: ComputedTaxId
  )(using RequestHeader): Future[Option[EligibilityCheckResult]] = {
    val eligibilityRequest: CallEligibilityApiRequest = TtpService.buildEligibilityRequest(journey)
    JourneyLogger.debug("EligibilityRequest: " + Json.prettyPrint(Json.toJson(eligibilityRequest)))
    // below log message used by Kibana dashboard.
    JourneyLogger.info(s"TTP eligibility check being made for ${journey.taxRegime.toString}")

    ttpConnector
      .callEligibilityApi(eligibilityRequest, journey.correlationId)
      .map(Option.apply)
      .recover {
        // 422 is a case where ttp thinks user is deregistered, so they should be sent to ineligible if the tax regime is
        // epaye or vat, otherwise we should error as usual.
        case e: UpstreamErrorResponse
            if e.statusCode == 422 && (
              journey.taxRegime == TaxRegime.Epaye || journey.taxRegime == TaxRegime.Vat
            ) =>
          JourneyLogger.info("422 Error Code from TTP, suggesting de-registered user")
          None
      }
  }

  def determineAffordability(
    journey:                JourneyStage.AfterUpfrontPaymentAnswers & Journey,
    eligibilityCheckResult: EligibilityCheckResult
  )(using RequestHeader): Future[InstalmentAmounts] = {
    val extremeDatesResponse                               = journey match {
      case j: JourneyStage.AfterExtremeDatesResponse => j.extremeDatesResponse
    }
    val upfrontPaymentAmount: Option[UpfrontPaymentAmount] =
      TtpService.deriveUpfrontPaymentAmount(journey.upfrontPaymentAnswers)
    val instalmentAmountRequest: InstalmentAmountRequest   =
      TtpService.buildInstalmentRequest(upfrontPaymentAmount, eligibilityCheckResult, extremeDatesResponse, journey)

    ttpConnector.callAffordabilityApi(instalmentAmountRequest, journey.correlationId)
  }

  def determineAffordableQuotes(
    journey:                Either[
      JourneyStage.AfterStartDatesResponse & Journey,
      (JourneyStage.AfterCheckedPaymentPlan & Journey, PaymentPlanAnswers.PaymentPlanNoAffordability)
    ],
    eligibilityCheckResult: EligibilityCheckResult
  )(using RequestHeader): Future[AffordableQuotesResponse] = {
    val journeyMerged                                      = journey.map[Journey](_._1).merge
    val upfrontPaymentAnswers                              = TtpService.upfrontPaymentAnswersFromJourney(journeyMerged)
    val initialPaymentAmount: Option[UpfrontPaymentAmount] =
      TtpService.deriveUpfrontPaymentAmount(upfrontPaymentAnswers)
    val monthlyPaymentAmount                               = journey.fold(TtpService.monthlyPaymentAmountFromJourney, _._2.monthlyPaymentAmount)
    val startDatesResponse                                 = journey.fold(_.startDatesResponse, _._2.startDatesResponse)
    val debtItemCharges                                    = eligibilityCheckResult.chargeTypeAssessment.flatMap(toDebtItemCharge)

    val affordableQuotesRequest: AffordableQuotesRequest = AffordableQuotesRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      regimeType = RegimeType.fromTaxRegime(journeyMerged.taxRegime),
      paymentPlanAffordableAmount = PaymentPlanAffordableAmount(monthlyPaymentAmount.value),
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanMaxLength = maxPlanLength(eligibilityCheckResult, journeyMerged),
      paymentPlanMinLength = eligibilityCheckResult.paymentPlanMinLength,
      accruedDebtInterest = AccruedDebtInterest(TtpService.calculateCumulativeInterest(eligibilityCheckResult)),
      paymentPlanStartDate = startDatesResponse.instalmentStartDate,
      initialPaymentDate = startDatesResponse.initialPaymentDate,
      initialPaymentAmount = initialPaymentAmount,
      debtItemCharges = debtItemCharges,
      customerPostcodes = eligibilityCheckResult.customerPostcodes
    )

    ttpConnector.callAffordableQuotesApi(affordableQuotesRequest, journeyMerged.correlationId)
  }

  def submitArrangement(
    journey:                     Either[Journey.AgreedTermsAndConditions, Journey.EmailVerificationComplete]
  )(implicit
    authenticatedJourneyRequest: AuthenticatedJourneyRequest[?]
  ): Future[ArrangementResponse] = {
    val taxRegime                   = journey.fold(_.taxRegime, _.taxRegime)
    val eligibilityCheckResult      = journey.fold(_.eligibilityCheckResult, _.eligibilityCheckResult)
    val selectedPaymentPlan         = journey.fold(_.paymentPlanAnswers, _.paymentPlanAnswers) match {
      case p: PaymentPlanAnswers.PaymentPlanNoAffordability    => p.selectedPaymentPlan
      case p: PaymentPlanAnswers.PaymentPlanAfterAffordability => p.selectedPaymentPlan
    }
    val correlationId               = journey.fold(_.correlationId, _.correlationId)
    val regimeDigitalCorrespondence = journey.fold(
      _.eligibilityCheckResult.regimeDigitalCorrespondence,
      _.eligibilityCheckResult.regimeDigitalCorrespondence
    )
    val customerDetail              = journey.fold(_.eligibilityCheckResult.customerDetails, deriveCustomerDetail)
    val individualDetails           =
      journey.fold(_.eligibilityCheckResult.individualDetails, _.eligibilityCheckResult.individualDetails)
    val addresses                   = journey.fold(_.eligibilityCheckResult.addresses, _.eligibilityCheckResult.addresses)

    val directDebitDetails                         = journey.fold(_.directDebitDetails, _.directDebitDetails)
    val accountNumberPaddedWithZero: AccountNumber = directDebitDetails.accountNumber
      .copy(SensitiveString(padLeftWithZeros(directDebitDetails.accountNumber.value.decryptedValue)))

    val identification = taxRegime match {
      case TaxRegime.Sa =>
        val maybeNinoId =
          authenticatedJourneyRequest.nino.map(nino => Identification(IdType("NINO"), IdValue(nino.value)))
        maybeNinoId.fold(eligibilityCheckResult.identification)(_ :: eligibilityCheckResult.identification)

      case TaxRegime.Epaye | TaxRegime.Vat | TaxRegime.Simp =>
        eligibilityCheckResult.identification
    }

    def toDebtItemCharges(chargeTypeAssessment: ChargeTypeAssessment): List[DebtItemCharges] =
      chargeTypeAssessment.charges.map { (charge: Charges) =>
        DebtItemCharges(
          outstandingDebtAmount = OutstandingDebtAmount(charge.outstandingAmount.value),
          debtItemChargeId = chargeTypeAssessment.chargeReference,
          debtItemOriginalDueDate = DebtItemOriginalDueDate(charge.dueDate.value),
          accruedInterest = charge.accruedInterest,
          isInterestBearingCharge = charge.isInterestBearingCharge,
          useChargeReference = charge.useChargeReference,
          mainTrans = Some(charge.mainTrans),
          subTrans = Some(charge.subTrans),
          parentChargeReference = charge.parentChargeReference,
          parentMainTrans = charge.parentMainTrans,
          creationDate = charge.creationDate,
          originalCreationDate = charge.originalCreationDate,
          saTaxYearEnd = charge.saTaxYearEnd,
          tieBreaker = charge.tieBreaker,
          originalTieBreaker = charge.originalTieBreaker,
          chargeType = Some(charge.chargeType),
          originalChargeType = charge.originalChargeType,
          chargeSource = charge.chargeSource,
          interestStartDate = charge.interestStartDate,
          taxPeriodFrom = Some(chargeTypeAssessment.taxPeriodFrom),
          taxPeriodTo = Some(chargeTypeAssessment.taxPeriodTo)
        )
      }

    val debtItemCharges = eligibilityCheckResult.chargeTypeAssessment.flatMap(toDebtItemCharges)

    val hasAffordabilityAssessment = journey.fold(_.paymentPlanAnswers, _.paymentPlanAnswers) match {
      case _: PaymentPlanAnswers.PaymentPlanNoAffordability    => false
      case _: PaymentPlanAnswers.PaymentPlanAfterAffordability => true
    }

    val caseID = journey.fold(_.paymentPlanAnswers, _.paymentPlanAnswers) match {
      case _: PaymentPlanAnswers.PaymentPlanNoAffordability    => None
      case p: PaymentPlanAnswers.PaymentPlanAfterAffordability => Some(p.startCaseResponse.caseId)
    }

    val arrangementRequest: ArrangementRequest = ArrangementRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      hasAffordabilityAssessment = hasAffordabilityAssessment,
      caseID = caseID,
      regimeType = RegimeType.fromTaxRegime(taxRegime),
      regimePaymentFrequency = PaymentPlanFrequencies.Monthly,
      arrangementAgreedDate = ArrangementAgreedDate(LocalDate.now(ZoneOffset.of("Z")).toString),
      identification = identification,
      directDebitInstruction = DirectDebitInstruction(
        sortCode = directDebitDetails.sortCode,
        accountNumber = accountNumberPaddedWithZero,
        accountName = directDebitDetails.name,
        paperAuddisFlag = PaperAuddisFlag(value = false)
      ),
      paymentPlan = EnactPaymentPlan(
        planDuration = selectedPaymentPlan.planDuration,
        paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
        numberOfInstalments = selectedPaymentPlan.numberOfInstalments,
        totalDebt = selectedPaymentPlan.totalDebt,
        totalDebtIncInt = selectedPaymentPlan.totalDebtIncInt,
        planInterest = selectedPaymentPlan.planInterest,
        collections = selectedPaymentPlan.collections,
        instalments = selectedPaymentPlan.instalments,
        debtItemCharges = debtItemCharges
      ),
      customerDetails = customerDetail,
      individualDetails = individualDetails,
      addresses = Some(addresses),
      regimeDigitalCorrespondence = Some(regimeDigitalCorrespondence)
    )

    ttpConnector
      .callArrangementApi(arrangementRequest, correlationId)
      .map { (response: ArrangementResponse) =>
        auditService.auditPaymentPlanSetUp(journey, Right(response))
        response
      }
      .recover { case httpException: HttpException =>
        auditService.auditPaymentPlanSetUp(journey, Left(httpException))
        JourneyLogger.info(
          s"Error calling EnactArrangement API for TaxType: ${journey.fold(_.taxRegime, _.taxRegime).toString}"
        )
        Errors.throwServerErrorException(httpException.message)
      }
  }
}

object TtpService {

  private def buildEligibilityRequest(journey: ComputedTaxId): CallEligibilityApiRequest = journey.taxRegime match {
    case TaxRegime.Epaye =>
      val idValue = journey.taxId match {
        case empRef: EmpRef => IdValue(empRef.value)
        case other          => sys.error(s"Expected EmpRef but found ${other.getClass.getSimpleName}")
      }
      CallEligibilityApiRequest(
        channelIdentifier = EligibilityRequestDefaults.essttpChannelIdentifier,
        identification = List(Identification(IdType(EligibilityRequestDefaults.Epaye.idType), idValue)),
        regimeType = RegimeType.EPAYE,
        returnFinancialAssessment = true
      )

    case TaxRegime.Vat =>
      val idValue = journey.taxId match {
        case vrn: Vrn => IdValue(vrn.value)
        case other    => sys.error(s"Expected Vrn but found ${other.getClass.getSimpleName}")
      }
      CallEligibilityApiRequest(
        channelIdentifier = EligibilityRequestDefaults.essttpChannelIdentifier,
        identification = List(Identification(IdType(EligibilityRequestDefaults.Vat.idType), idValue)),
        regimeType = RegimeType.VAT,
        returnFinancialAssessment = true
      )

    case TaxRegime.Sa =>
      CallEligibilityApiRequest(
        channelIdentifier = EligibilityRequestDefaults.essttpChannelIdentifier,
        identification =
          List(Identification(IdType(EligibilityRequestDefaults.Sa.idType), IdValue(journey.taxId.value))),
        regimeType = RegimeType.SA,
        returnFinancialAssessment = true
      )

    case TaxRegime.Simp =>
      CallEligibilityApiRequest(
        channelIdentifier = EligibilityRequestDefaults.essttpChannelIdentifier,
        identification =
          List(Identification(IdType(EligibilityRequestDefaults.Simp.idType), IdValue(journey.taxId.value))),
        regimeType = RegimeType.SIMP,
        returnFinancialAssessment = true
      )

  }

  private def buildInstalmentRequest(
    upfrontPaymentAmount:   Option[UpfrontPaymentAmount],
    eligibilityCheckResult: EligibilityCheckResult,
    extremeDatesResponse:   ExtremeDatesResponse,
    journey:                Journey
  ): InstalmentAmountRequest = {
    val allInterestAccrued: AmountInPence                         = AmountInPence(
      eligibilityCheckResult.chargeTypeAssessment
        .flatMap(
          _.charges
            .map(_.accruedInterest.value.value)
        )
        .sum
    )
    val debtChargeItemsFromEligibilityCheck: List[DebtItemCharge] =
      eligibilityCheckResult.chargeTypeAssessment.flatMap { (chargeTypeAssessment: ChargeTypeAssessment) =>
        toDebtItemCharge(chargeTypeAssessment)
      }

    InstalmentAmountRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      regimeType = RegimeType.fromTaxRegime(journey.taxRegime),
      paymentPlanMinLength = eligibilityCheckResult.paymentPlanMinLength,
      paymentPlanMaxLength = maxPlanLength(eligibilityCheckResult, journey),
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      earliestPaymentPlanStartDate = extremeDatesResponse.earliestPlanStartDate,
      latestPaymentPlanStartDate = extremeDatesResponse.latestPlanStartDate,
      initialPaymentDate = extremeDatesResponse.initialPaymentDate,
      initialPaymentAmount = upfrontPaymentAmount.map(_.value),
      accruedDebtInterest = AccruedDebtInterest(allInterestAccrued),
      debtItemCharges = debtChargeItemsFromEligibilityCheck,
      customerPostcodes = Some(eligibilityCheckResult.customerPostcodes)
    )
  }

  def toDebtItemCharge(chargeTypeAssessment: ChargeTypeAssessment): List[DebtItemCharge] =
    chargeTypeAssessment.charges.map { (charge: Charges) =>
      DebtItemCharge(
        outstandingDebtAmount = OutstandingDebtAmount(charge.outstandingAmount.value),
        mainTrans = charge.mainTrans,
        subTrans = charge.subTrans,
        isInterestBearingCharge = charge.isInterestBearingCharge,
        useChargeReference = charge.useChargeReference,
        debtItemChargeId = chargeTypeAssessment.chargeReference,
        interestStartDate = charge.interestStartDate,
        debtItemOriginalDueDate = DebtItemOriginalDueDate(charge.dueDate.value)
      )
    }

  def calculateCumulativeInterest(eligibilityCheckResult: EligibilityCheckResult): AmountInPence = AmountInPence(
    eligibilityCheckResult.chargeTypeAssessment
      .flatMap(_.charges)
      .map(_.accruedInterest.value.value)
      .sum
  )

  private def deriveUpfrontPaymentAmount(upfrontPaymentAnswers: UpfrontPaymentAnswers): Option[UpfrontPaymentAmount] =
    upfrontPaymentAnswers match {
      case someAmount: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(someAmount.amount)
      case UpfrontPaymentAnswers.NoUpfrontPayment                   => None
    }

  /** If email matches the one from the eligibility API - ETMP If new email entered on the screen which doesn't match
    * the ETMP one - TEMP
    */
  private def deriveCustomerDetail(journey: Journey.EmailVerificationComplete): Option[List[CustomerDetail]] = {
    val emailThatsBeenVerified: Email = journey.emailToBeVerified
    val customerDetail                = journey.eligibilityCheckResult.email match {
      case None =>
        CustomerDetail(Some(emailThatsBeenVerified), Some(EmailSource.TEMP))

      case Some(etmpEmail) =>
        if (
          etmpEmail.value.decryptedValue.toLowerCase(Locale.UK) == emailThatsBeenVerified.value.decryptedValue
            .toLowerCase(Locale.UK)
        ) {
          CustomerDetail(Some(etmpEmail), Some(EmailSource.ETMP))
        } else {
          CustomerDetail(Some(emailThatsBeenVerified), Some(EmailSource.TEMP))
        }
    }
    Some(List(customerDetail))
  }

  private def upfrontPaymentAnswersFromJourney(journey: Journey): UpfrontPaymentAnswers = journey match {
    case j: JourneyStage.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
    case _                                          => Errors.throwServerErrorException("Trying to get upfront payment answers for journey before they exist..")
  }

  private def monthlyPaymentAmountFromJourney(
    journey: JourneyStage.AfterStartDatesResponse & Journey
  ): MonthlyPaymentAmount =
    journey match {
      case j: JourneyStage.AfterEnteredMonthlyPaymentAmount => j.monthlyPaymentAmount
    }

  // account number needs to be length 8 when sent to TTP, we pad with zeros if it's less
  private def padLeftWithZeros(str: String): String = str.reverse
    .padTo(8, '0')
    .reverse

  def maxPlanLength(
    eligibilityCheckResult: EligibilityCheckResult,
    journey:                Journey
  ): PaymentPlanMaxLength =
    if (journey.affordabilityEnabled.contains(true)) PaymentPlanMaxLength(6)
    else eligibilityCheckResult.paymentPlanMaxLength

}
