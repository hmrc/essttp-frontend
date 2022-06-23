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

import connectors.{CallEligibilityApiRequest, TtpConnector}
import essttp.journey.model.Journey.Stages.ComputedTaxId
import essttp.journey.model.ttp.affordability.{DebtItemCharge, InstalmentAmountRequest, InstalmentAmounts}
import essttp.journey.model.ttp.affordablequotes._
import essttp.journey.model.ttp.{ChargeTypeAssessment, DisallowedChargeLocks, EligibilityCheckResult, PaymentPlanFrequencies}
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.{AmountInPence, EmpRef, UpfrontPaymentAmount}
import play.api.mvc.RequestHeader

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/**
 * Time To Pay (Ttp) Service.
 */
@Singleton
class TtpService @Inject() (ttpConnector: TtpConnector, datesService: DatesService) {

  def determineEligibility(journey: ComputedTaxId)(implicit request: RequestHeader): Future[EligibilityCheckResult] = {

    val eligibilityRequest: CallEligibilityApiRequest = journey match {
      case j: Journey.Epaye =>
        CallEligibilityApiRequest(
          idType           = "SSTTP",
          idNumber         = j.taxId match {
            case empRef: EmpRef => empRef.value //Hmm, will it compile, theoretically it can't be Vrn ...
            case other          => sys.error(s"Expected EmpRef but found ${other.getClass.getSimpleName}")
          },
          regimeType       = "PAYE",
          returnFinancials = true
        )
    }
    ttpConnector.callEligibilityApi(eligibilityRequest)
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
    }
    val upfrontPaymentAmount: Option[UpfrontPaymentAmount] = TtpService.deriveUpfrontPaymentAmount(j.upfrontPaymentAnswers)
    val eligibilityCheckResult: EligibilityCheckResult = j.eligibilityCheckResult
    val request = TtpService.buildInstalmentRequest(upfrontPaymentAmount, eligibilityCheckResult, j.extremeDatesResponse)

    ttpConnector.callAffordabilityApi(request)
  }

  def determineAffordableQuotes(journey: Journey.AfterStartDatesResponse)(implicit requestHeader: RequestHeader): Future[AffordableQuotesResponse] = {
    val j = journey match {
      case j1: Journey.Stages.RetrievedStartDates       => j1
      case j1: Journey.Stages.RetrievedAffordableQuotes => j1
      case j1: Journey.Stages.ChosenPaymentPlan         => j1
    }
    val initialPaymentAmount: Option[UpfrontPaymentAmount] = TtpService.deriveUpfrontPaymentAmount(j.upfrontPaymentAnswers)
    val debtItemCharges = j.eligibilityCheckResult.chargeTypeAssessment
      .flatMap(_.disallowedChargeLocks)
      .map { chargeLocks: DisallowedChargeLocks =>
        DebtItemCharges(
          outstandingDebtAmount = chargeLocks.outstandingDebtAmount.value,
          mainTrans             = chargeLocks.mainTrans,
          subTrans              = chargeLocks.subTrans,
          debtItemChargeId      = chargeLocks.chargeId,
          interestStartDate     = chargeLocks.interestStartDate,
          // todo this is wrong, but we need to update the eligibility api models to obtain this... That's another ticket.
          debtItemOriginalDueDate = DebtItemOriginalDueDate(LocalDate.now().minusMonths(6))
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

    ttpConnector.callAffordableQuotesApi(affordableQuotesRequest)
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
        .flatMap(_.disallowedChargeLocks
          .map(_.accruedInterestToDate.value.value))
        .sum
    )
    val debtChargeItemsFromEligibilityCheck: List[DebtItemCharge] = eligibilityCheckResult.chargeTypeAssessment.flatMap {
      chargeTypeAssessment: ChargeTypeAssessment =>
        chargeTypeAssessment.disallowedChargeLocks.map { dcl: DisallowedChargeLocks =>
          DebtItemCharge(
            outstandingDebtAmount = dcl.outstandingDebtAmount.value,
            mainTrans             = dcl.mainTrans,
            subTrans              = dcl.subTrans,
            debtItemChargeId      = dcl.chargeId,
            interestStartDate     = dcl.interestStartDate
          )
        }
    }
    InstalmentAmountRequest(
      minPlanLength         = eligibilityCheckResult.minPlanLengthMonths,
      maxPlanLength         = eligibilityCheckResult.maxPlanLengthMonths,
      interestAccrued       = allInterestAccrued,
      frequency             = PaymentPlanFrequencies.Monthly,
      earliestPlanStartDate = extremeDatesResponse.earliestPlanStartDate,
      latestPlanStartDate   = extremeDatesResponse.latestPlanStartDate,
      initialPaymentDate    = extremeDatesResponse.initialPaymentDate,
      initialPaymentAmount  = upfrontPaymentAmount.map(_.value),
      debtItemCharges       = debtChargeItemsFromEligibilityCheck,
      customerPostcodes     = eligibilityCheckResult.customerPostcodes
    )
  }

  private def calculateCumulativeInterest(eligibilityCheckResult: EligibilityCheckResult): AmountInPence = AmountInPence(
    eligibilityCheckResult.chargeTypeAssessment
      .flatMap(_.disallowedChargeLocks)
      .map(_.accruedInterestToDate.value.value)
      .sum
  )

  private def deriveUpfrontPaymentAmount(upfrontPaymentAnswers: UpfrontPaymentAnswers): Option[UpfrontPaymentAmount] = upfrontPaymentAnswers match {
    case someAmount: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(someAmount.amount)
    case UpfrontPaymentAnswers.NoUpfrontPayment                   => None
  }
}
