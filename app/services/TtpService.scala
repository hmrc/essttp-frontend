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
import essttp.journey.model.Journey.Stages.ComputedTaxId
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.ttp._
import essttp.rootmodel.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.rootmodel.ttp.affordablequotes._
import essttp.rootmodel.ttp.arrangement._
import essttp.rootmodel.{AmountInPence, EmpRef, TaxRegime, UpfrontPaymentAmount}
import essttp.utils.Errors
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
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
    datesService: DatesService,
    auditService: AuditService
)(implicit executionContext: ExecutionContext) {

  def determineEligibility(journey: ComputedTaxId)(implicit request: RequestHeader): Future[EligibilityCheckResult] = {
    val eligibilityRequest: CallEligibilityApiRequest = journey match {
      case j: Journey.Epaye =>
        CallEligibilityApiRequest(
          channelIdentifier         = EligibilityRequestDefaults.essttpChannelIdentifier,
          idType                    = EligibilityRequestDefaults.Epaye.idType,
          idValue                   = j.taxId match {
            case empRef: EmpRef => empRef.value //Hmm, will it compile, theoretically it can't be Vrn ...
            case other          => sys.error(s"Expected EmpRef but found ${other.getClass.getSimpleName}")
          },
          regimeType                = EligibilityRequestDefaults.Epaye.regimeType,
          returnFinancialAssessment = true
        )
    }
    JourneyLogger.debug("EligibilityRequest: " + Json.prettyPrint(Json.toJson(eligibilityRequest)))
    ttpConnector.callEligibilityApi(eligibilityRequest, journey.correlationId)
  }

  def determineAffordability(journey: Journey.AfterUpfrontPaymentAnswers)(implicit requestHeader: RequestHeader): Future[InstalmentAmounts] = {
    val j = journey match {
      case j1: Journey.Stages.RetrievedExtremeDates        => j1
      case j1: Journey.Stages.RetrievedAffordabilityResult => j1
      case j1: Journey.Stages.EnteredMonthlyPaymentAmount  => j1
      case j1: Journey.Stages.EnteredDayOfMonth            => j1
      case j1: Journey.Stages.RetrievedStartDates          => j1
      case j1: Journey.Stages.RetrievedAffordableQuotes    => j1
      case j1: Journey.Stages.ChosenPaymentPlan            => j1
      case j1: Journey.Stages.CheckedPaymentPlan           => j1
      case j1: Journey.Stages.ChosenTypeOfBankAccount      => j1
      case j1: Journey.Stages.EnteredDirectDebitDetails    => j1
      case j1: Journey.Stages.ConfirmedDirectDebitDetails  => j1
      case j1: Journey.Stages.AgreedTermsAndConditions     => j1
      case j1: Journey.Stages.SubmittedArrangement         => j1
    }
    val upfrontPaymentAmount: Option[UpfrontPaymentAmount] = TtpService.deriveUpfrontPaymentAmount(j.upfrontPaymentAnswers)
    val eligibilityCheckResult: EligibilityCheckResult = j.eligibilityCheckResult
    val instalmentAmountRequest: InstalmentAmountRequest = TtpService.buildInstalmentRequest(upfrontPaymentAmount, eligibilityCheckResult, j.extremeDatesResponse)

    ttpConnector.callAffordabilityApi(instalmentAmountRequest, journey.correlationId)
  }

  def determineAffordableQuotes(journey: Journey.AfterStartDatesResponse)(implicit requestHeader: RequestHeader): Future[AffordableQuotesResponse] = {
    val j = journey match {
      case j1: Journey.Stages.RetrievedStartDates         => j1
      case j1: Journey.Stages.RetrievedAffordableQuotes   => j1
      case j1: Journey.Stages.ChosenPaymentPlan           => j1
      case j1: Journey.Stages.CheckedPaymentPlan          => j1
      case j1: Journey.Stages.ChosenTypeOfBankAccount     => j1
      case j1: Journey.Stages.EnteredDirectDebitDetails   => j1
      case j1: Journey.Stages.ConfirmedDirectDebitDetails => j1
      case j1: Journey.Stages.AgreedTermsAndConditions    => j1
      case j1: Journey.Stages.SubmittedArrangement        => j1
    }
    val initialPaymentAmount: Option[UpfrontPaymentAmount] = TtpService.deriveUpfrontPaymentAmount(j.upfrontPaymentAnswers)
    val debtItemCharges = j.eligibilityCheckResult.chargeTypeAssessment
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
      paymentPlanAffordableAmount = PaymentPlanAffordableAmount(j.monthlyPaymentAmount.value),
      paymentPlanFrequency        = PaymentPlanFrequencies.Monthly,
      paymentPlanMaxLength        = TtpService.paymentPlanMaxLength,
      paymentPlanMinLength        = TtpService.paymentPlanMinLength,
      accruedDebtInterest         = AccruedDebtInterest(TtpService.calculateCumulativeInterest(j.eligibilityCheckResult)),
      paymentPlanStartDate        = j.startDatesResponse.instalmentStartDate,
      initialPaymentDate          = j.startDatesResponse.initialPaymentDate,
      initialPaymentAmount        = initialPaymentAmount,
      debtItemCharges             = debtItemCharges,
      customerPostcodes           = j.eligibilityCheckResult.customerPostcodes
    )

    ttpConnector.callAffordableQuotesApi(affordableQuotesRequest, journey.correlationId)
  }

  def submitArrangement(
      journey: Journey.Stages.AgreedTermsAndConditions
  )(
      implicit
      authenticatedJourneyRequest: AuthenticatedJourneyRequest[_]
  ): Future[ArrangementResponse] = {

    val regimeType: RegimeType = journey.taxRegime match {
      case TaxRegime.Epaye => RegimeType.`PAYE`
      case TaxRegime.Vat   => RegimeType.`VAT`
    }
    val arrangementRequest: ArrangementRequest = ArrangementRequest(
      channelIdentifier      = ChannelIdentifiers.eSSTTP,
      regimeType             = regimeType,
      regimePaymentFrequency = PaymentPlanFrequencies.Monthly,
      arrangementAgreedDate  = ArrangementAgreedDate(LocalDate.now(ZoneOffset.of("Z")).toString),
      identification         = journey.eligibilityCheckResult.identification,
      directDebitInstruction = DirectDebitInstruction(
        sortCode        = journey.directDebitDetails.bankDetails.sortCode,
        accountNumber   = journey.directDebitDetails.bankDetails.accountNumber,
        accountName     = journey.directDebitDetails.bankDetails.name,
        paperAuddisFlag = PaperAuddisFlag(false)
      ),
      paymentPlan            = EnactPaymentPlan(
        planDuration         = journey.selectedPaymentPlan.planDuration,
        paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
        numberOfInstalments  = journey.selectedPaymentPlan.numberOfInstalments,
        totalDebt            = journey.selectedPaymentPlan.totalDebt,
        totalDebtIncInt      = journey.selectedPaymentPlan.totalDebtIncInt,
        planInterest         = journey.selectedPaymentPlan.planInterest,
        collections          = journey.selectedPaymentPlan.collections,
        instalments          = journey.selectedPaymentPlan.instalments
      )
    )

    ttpConnector.callArrangementApi(arrangementRequest, journey.correlationId)
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
}
