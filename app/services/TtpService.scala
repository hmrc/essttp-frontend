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
import essttp.journey.model.ttp.{ChargeTypeAssessment, DisallowedChargeLocks, EligibilityCheckResult}
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.{AmountInPence, EmpRef, UpfrontPaymentAmount}
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Time To Pay (Ttp) Service.
 */
@Singleton
class TtpService @Inject() (ttpConnector: TtpConnector, datesService: DatesService)(implicit executionContext: ExecutionContext) {

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
    }

    val upfrontPaymentAmount: Option[UpfrontPaymentAmount] = j.upfrontPaymentAnswers match {
      case someAmount: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(someAmount.amount)
      case UpfrontPaymentAnswers.NoUpfrontPayment                   => None
    }
    val eligibilityCheckResult: EligibilityCheckResult = j.eligibilityCheckResult
    val request = buildInstalmentRequest(upfrontPaymentAmount, eligibilityCheckResult, j.extremeDatesResponse)

    for {
      //      extremeDates: ExtremeDatesResponse <- datesService.extremeDates(journey)
      instalmentAmounts <- ttpConnector.callAffordabilityApi(request)
    } yield instalmentAmounts
  }

  private def buildInstalmentRequest(
      upfrontPaymentAmount:   Option[UpfrontPaymentAmount],
      eligibilityCheckResult: EligibilityCheckResult,
      extremeDatesResponse:   ExtremeDatesResponse
  ): InstalmentAmountRequest = {
    val allInterestAccrued: AmountInPence = AmountInPence(
      eligibilityCheckResult.chargeTypeAssessment
        .flatMap(_.disallowedChargeLocks
          .map(_.accruedInterestToDate.value.toLong))
        .sum
    )
    val debtChargeItemsFromEligibilityCheck: List[DebtItemCharge] = eligibilityCheckResult.chargeTypeAssessment.flatMap {
      chargeTypeAssessment: ChargeTypeAssessment =>
        chargeTypeAssessment.disallowedChargeLocks.map { dcl: DisallowedChargeLocks =>
          DebtItemCharge(
            outstandingDebtAmount = AmountInPence(dcl.outstandingDebtAmount.value),
            mainTrans             = dcl.mainTrans,
            subTrans              = dcl.subTrans,
            debtItemChargeId      = dcl.chargeId,
            interestStartDate     = dcl.interestStartDate.value
          )
        }
    }
    InstalmentAmountRequest(
      minPlanLength         = eligibilityCheckResult.minPlanLengthMonths,
      maxPlanLength         = eligibilityCheckResult.maxPlanLengthMonths,
      interestAccrued       = allInterestAccrued,
      frequency             = "monthly",
      earliestPlanStartDate = extremeDatesResponse.earliestPlanStartDate,
      latestPlanStartDate   = extremeDatesResponse.latestPlanStartDate,
      initialPaymentDate    = extremeDatesResponse.initialPaymentDate,
      initialPaymentAmount  = upfrontPaymentAmount.map(_.value),
      debtItemCharges       = debtChargeItemsFromEligibilityCheck,
      customerPostcodes     = eligibilityCheckResult.customerPostcodes
    )
  }
}

