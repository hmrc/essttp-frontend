/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.{CallEligibilityApiRequest, TtpConnector}
import controllers.support.RequestSupport.hc
import essttp.crypto.CryptoFormat
import essttp.journey.model.Journey.Stages.ComputedTaxId
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.ttp._
import essttp.rootmodel.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.rootmodel.ttp.affordablequotes._
import essttp.rootmodel.ttp.arrangement._
import essttp.rootmodel.ttp.eligibility.{CustomerDetail, EmailSource}
import essttp.rootmodel.{AmountInPence, EmpRef, TaxRegime, UpfrontPaymentAmount, Vrn}
import essttp.utils.Errors
import io.scalaland.chimney.dsl.TransformerOps
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import services.TtpService.deriveCustomerDetail
import uk.gov.hmrc.http.HttpException
import util.JourneyLogger

import java.time.{LocalDate, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Time To Pay (Ttp) Service.
 */
@Singleton
class TtpService @Inject() (
    ttpConnector: TtpConnector,
    auditService: AuditService
)(implicit executionContext: ExecutionContext) {

  implicit val cryptoFormat: CryptoFormat = CryptoFormat.NoOpCryptoFormat

  def determineEligibility(journey: ComputedTaxId)(implicit request: RequestHeader): Future[EligibilityCheckResult] = {
    val eligibilityRequest: CallEligibilityApiRequest = journey match {
      case j: Journey.Epaye =>
        CallEligibilityApiRequest(
          channelIdentifier         = EligibilityRequestDefaults.essttpChannelIdentifier,
          idType                    = EligibilityRequestDefaults.Epaye.idType,
          idValue                   = j.taxId match {
            case empRef: EmpRef => empRef.value
            case other          => sys.error(s"Expected EmpRef but found ${other.getClass.getSimpleName}")
          },
          regimeType                = EligibilityRequestDefaults.Epaye.regimeType,
          returnFinancialAssessment = true
        )

      case j: Journey.Vat =>
        CallEligibilityApiRequest(
          channelIdentifier         = EligibilityRequestDefaults.essttpChannelIdentifier,
          idType                    = EligibilityRequestDefaults.Vat.idType,
          idValue                   = j.taxId match {
            case vrn: Vrn => vrn.value
            case other    => sys.error(s"Expected Vrn but found ${other.getClass.getSimpleName}")
          },
          regimeType                = EligibilityRequestDefaults.Vat.regimeType,
          returnFinancialAssessment = true
        )
    }
    JourneyLogger.debug("EligibilityRequest: " + Json.prettyPrint(Json.toJson(eligibilityRequest)))
    ttpConnector.callEligibilityApi(eligibilityRequest, journey.correlationId)
  }

  def determineAffordability(journey: Journey.AfterUpfrontPaymentAnswers, eligibilityCheckResult: EligibilityCheckResult)(implicit requestHeader: RequestHeader): Future[InstalmentAmounts] = {
    val extremeDatesResponse = journey.into[Journey.AfterExtremeDatesResponse].transform.extremeDatesResponse
    val upfrontPaymentAmount: Option[UpfrontPaymentAmount] = TtpService.deriveUpfrontPaymentAmount(journey.upfrontPaymentAnswers)
    val instalmentAmountRequest: InstalmentAmountRequest = TtpService.buildInstalmentRequest(upfrontPaymentAmount, eligibilityCheckResult, extremeDatesResponse)

    ttpConnector.callAffordabilityApi(instalmentAmountRequest, journey.correlationId)
  }

  def determineAffordableQuotes(
      journey:                Journey.AfterStartDatesResponse,
      eligibilityCheckResult: EligibilityCheckResult
  )(implicit requestHeader: RequestHeader): Future[AffordableQuotesResponse] = {
    val upfrontPaymentAnswers = journey.into[Journey.AfterUpfrontPaymentAnswers].transform.upfrontPaymentAnswers
    val initialPaymentAmount: Option[UpfrontPaymentAmount] = TtpService.deriveUpfrontPaymentAmount(upfrontPaymentAnswers)
    val monthlyPaymentAmount = journey.into[Journey.AfterEnteredMonthlyPaymentAmount].transform.monthlyPaymentAmount
    val startDatesResponse = journey.into[Journey.AfterStartDatesResponse].transform.startDatesResponse

    val debtItemCharges = eligibilityCheckResult.chargeTypeAssessment
      .flatMap(_.charges)
      .map { charge: Charges =>
        DebtItemCharge(
          outstandingDebtAmount   = OutstandingDebtAmount(charge.outstandingAmount.value),
          mainTrans               = charge.mainTrans,
          subTrans                = charge.subTrans,
          debtItemChargeId        = charge.chargeReference,
          interestStartDate       = charge.interestStartDate,
          debtItemOriginalDueDate = DebtItemOriginalDueDate(charge.dueDate.value)
        )
      }

    val affordableQuotesRequest: AffordableQuotesRequest = AffordableQuotesRequest(
      channelIdentifier           = ChannelIdentifiers.eSSTTP,
      paymentPlanAffordableAmount = PaymentPlanAffordableAmount(monthlyPaymentAmount.value),
      paymentPlanFrequency        = PaymentPlanFrequencies.Monthly,
      paymentPlanMaxLength        = TtpService.paymentPlanMaxLength,
      paymentPlanMinLength        = TtpService.paymentPlanMinLength,
      accruedDebtInterest         = AccruedDebtInterest(TtpService.calculateCumulativeInterest(eligibilityCheckResult)),
      paymentPlanStartDate        = startDatesResponse.instalmentStartDate,
      initialPaymentDate          = startDatesResponse.initialPaymentDate,
      initialPaymentAmount        = initialPaymentAmount,
      debtItemCharges             = debtItemCharges,
      customerPostcodes           = eligibilityCheckResult.customerPostcodes
    )

    ttpConnector.callAffordableQuotesApi(affordableQuotesRequest, journey.correlationId)
  }

  def submitArrangement(
      journey: Either[Journey.Stages.AgreedTermsAndConditions, Journey.Stages.EmailVerificationComplete]
  )(
      implicit
      authenticatedJourneyRequest: AuthenticatedJourneyRequest[_]
  ): Future[ArrangementResponse] = {

    val regimeType: RegimeType = journey.fold(_.taxRegime, _.taxRegime) match {
      case TaxRegime.Epaye => RegimeType.`PAYE`
      case TaxRegime.Vat   => RegimeType.`VAT`
    }
    val eligibilityCheckResult = journey.fold(_.eligibilityCheckResult, _.eligibilityCheckResult)
    val directDebitDetails = journey.fold(_.directDebitDetails, _.directDebitDetails)
    val selectedPaymentPlan = journey.fold(_.selectedPaymentPlan, _.selectedPaymentPlan)
    val correlationId = journey.fold(_.correlationId, _.correlationId)
    val regimeDigitalCorrespondence = journey.fold(_.eligibilityCheckResult.regimeDigitalCorrespondence, _.eligibilityCheckResult.regimeDigitalCorrespondence)
    val customerDetail = journey.fold(_.eligibilityCheckResult.customerDetails, deriveCustomerDetail)

    val arrangementRequest: ArrangementRequest = ArrangementRequest(
      channelIdentifier           = ChannelIdentifiers.eSSTTP,
      regimeType                  = regimeType,
      regimePaymentFrequency      = PaymentPlanFrequencies.Monthly,
      arrangementAgreedDate       = ArrangementAgreedDate(LocalDate.now(ZoneOffset.of("Z")).toString),
      identification              = eligibilityCheckResult.identification,
      directDebitInstruction      = DirectDebitInstruction(
        sortCode        = directDebitDetails.sortCode,
        accountNumber   = directDebitDetails.accountNumber,
        accountName     = directDebitDetails.name,
        paperAuddisFlag = PaperAuddisFlag(false)
      ),
      paymentPlan                 = EnactPaymentPlan(
        planDuration         = selectedPaymentPlan.planDuration,
        paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
        numberOfInstalments  = selectedPaymentPlan.numberOfInstalments,
        totalDebt            = selectedPaymentPlan.totalDebt,
        totalDebtIncInt      = selectedPaymentPlan.totalDebtIncInt,
        planInterest         = selectedPaymentPlan.planInterest,
        collections          = selectedPaymentPlan.collections,
        instalments          = selectedPaymentPlan.instalments
      ),
      customerDetails             = customerDetail,
      regimeDigitalCorrespondence = regimeDigitalCorrespondence
    )

    ttpConnector.callArrangementApi(arrangementRequest, correlationId)
      .map { response: ArrangementResponse =>
        auditService.auditPaymentPlanSetUp(journey, Right(response))
        response
      }.recover {
        case httpException: HttpException =>
          auditService.auditPaymentPlanSetUp(journey, Left(httpException))
          Errors.throwServerErrorException(httpException.message)
      }
  }
}

object TtpService {
  // these are technically hard coded, may change per tax type? I don't want to put in config so I've put them here...
  private val paymentPlanMaxLength: PaymentPlanMaxLength = PaymentPlanMaxLength(6)
  private val paymentPlanMinLength: PaymentPlanMinLength = PaymentPlanMinLength(1)

  private def buildInstalmentRequest(
      upfrontPaymentAmount:   Option[UpfrontPaymentAmount],
      eligibilityCheckResult: EligibilityCheckResult,
      extremeDatesResponse:   ExtremeDatesResponse
  ): InstalmentAmountRequest = {
    val allInterestAccrued: AmountInPence = AmountInPence(
      eligibilityCheckResult.chargeTypeAssessment
        .flatMap(_.charges
          .map(_.accruedInterest.value.value))
        .sum
    )
    val debtChargeItemsFromEligibilityCheck: List[DebtItemCharge] = eligibilityCheckResult.chargeTypeAssessment.flatMap {
      chargeTypeAssessment: ChargeTypeAssessment =>
        chargeTypeAssessment.charges.map { charge: Charges =>
          DebtItemCharge(
            outstandingDebtAmount   = OutstandingDebtAmount(charge.outstandingAmount.value),
            mainTrans               = charge.mainTrans,
            subTrans                = charge.subTrans,
            debtItemChargeId        = charge.chargeReference,
            interestStartDate       = charge.interestStartDate,
            debtItemOriginalDueDate = DebtItemOriginalDueDate(charge.dueDate.value)
          )
        }
    }
    InstalmentAmountRequest(
      channelIdentifier            = ChannelIdentifiers.eSSTTP,
      paymentPlanMinLength         = eligibilityCheckResult.paymentPlanMinLength,
      paymentPlanMaxLength         = eligibilityCheckResult.paymentPlanMaxLength,
      paymentPlanFrequency         = PaymentPlanFrequencies.Monthly,
      earliestPaymentPlanStartDate = extremeDatesResponse.earliestPlanStartDate,
      latestPaymentPlanStartDate   = extremeDatesResponse.latestPlanStartDate,
      initialPaymentDate           = extremeDatesResponse.initialPaymentDate,
      initialPaymentAmount         = upfrontPaymentAmount.map(_.value),
      accruedDebtInterest          = AccruedDebtInterest(allInterestAccrued),
      debtItemCharges              = debtChargeItemsFromEligibilityCheck,
      customerPostcodes            = eligibilityCheckResult.customerPostcodes
    )
  }

  private def calculateCumulativeInterest(eligibilityCheckResult: EligibilityCheckResult): AmountInPence = AmountInPence(
    eligibilityCheckResult.chargeTypeAssessment
      .flatMap(_.charges)
      .map(_.accruedInterest.value.value)
      .sum
  )

  private def deriveUpfrontPaymentAmount(upfrontPaymentAnswers: UpfrontPaymentAnswers): Option[UpfrontPaymentAmount] = upfrontPaymentAnswers match {
    case someAmount: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(someAmount.amount)
    case UpfrontPaymentAnswers.NoUpfrontPayment                   => None
  }

  /**
   * If email matches the one from the eligibility API - ETMP
   * If new email entered on the screen which doesn't match the ETMP one - TEMP
   */
  private def deriveCustomerDetail(journey: Journey.Stages.EmailVerificationComplete): Option[List[CustomerDetail]] = {
    val emailFromEligibilityResponse: Option[List[Option[String]]] =
      journey.eligibilityCheckResult.customerDetails.map(customerDetails => customerDetails.map(_.emailAddress))
    val maybeEmailSource: Option[EmailSource] = emailFromEligibilityResponse.fold[Option[EmailSource]](None){ nonEmptyFromEligibility =>
      if (nonEmptyFromEligibility.contains(Some(journey.emailToBeVerified.value.decryptedValue))) Some(EmailSource.ETMP)
      else Some(EmailSource.TEMP)
    }
    maybeEmailSource.map { derivedSource =>
      List(CustomerDetail(Some(journey.emailToBeVerified.value.decryptedValue), Some(derivedSource)))
    }
  }

}
