/*
 * Copyright 2025 HM Revenue & Customs
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

package testOnly

import javax.inject.{Inject, Singleton}
import _root_.connectors.{EssttpBackendConnector, TtpConnector}
import essttp.journey.model.{Journey, JourneyStage, PaymentPlanAnswers, UpfrontPaymentAnswers}
import essttp.rootmodel.AmountInPence
import essttp.rootmodel.dates.InitialPayment
import essttp.rootmodel.dates.startdates.{PreferredDayOfMonth, StartDatesRequest, StartDatesResponse}
import essttp.rootmodel.ttp.affordablequotes.*
import essttp.rootmodel.ttp.{PaymentPlanFrequencies, RegimeType}
import play.api.libs.json.{JsBoolean, JsObject, JsValue, Json}
import play.api.mvc.RequestHeader
import services.TtpService
import services.TtpService.{maxPlanLength, toDebtItemCharge}
import testOnly.connectors.EssttpStubConnector
import testOnly.models.TestOnlyJourney
import testOnly.models.formsmodel.IncomeAndExpenditure
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PegaPlanService @Inject() (
  datesApiConnector:   EssttpBackendConnector,
  ttpConnector:        TtpConnector,
  essttpStubConnector: EssttpStubConnector
)(using ExecutionContext) {

  def getPlans(journey: Journey, incomeAndExpenditure: IncomeAndExpenditure)(using
    RequestHeader
  ): Future[List[PaymentPlan]] =
    for {
      startDatesResponse <- callStartDatesApi(journey)
      plan               <- callPlansApi(journey, incomeAndExpenditure, startDatesResponse)
    } yield plan.paymentPlans

  def storePegaGetCaseResponse(
    journey:         Journey,
    testOnlyJourney: TestOnlyJourney
  )(using HeaderCarrier): Future[Unit] = {
    val caseId = journey match {
      case j: JourneyStage.AfterStartedPegaCase    => j.startCaseResponse.caseId.value
      case j: JourneyStage.AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case p: PaymentPlanAnswers.PaymentPlanAfterAffordability => p.startCaseResponse.caseId.value
          case _: PaymentPlanAnswers.PaymentPlanNoAffordability    =>
            sys.error("Trying to find case ID on non-affordability journey")
        }
      case other                                   => sys.error(s"Could not find PEGA case id for journey in stage ${other.stage}")
    }

    val incomeAndExpenditure =
      testOnlyJourney.incomeAndExpenditure.getOrElse(sys.error("Could not find income and expenditure answers"))
    val paymentPlan          = testOnlyJourney.paymentPlan.getOrElse(sys.error("Could not find paymentPlan"))
    val getCaseResponse      = constructPegaGetCaseResponse(incomeAndExpenditure, paymentPlan)

    essttpStubConnector.storePegaGetCaseResponse(caseId, getCaseResponse)
  }

  private def upfrontPaymentAnswersFromJourney(journey: Journey): UpfrontPaymentAnswers = journey match {
    case j: JourneyStage.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
    case other                                      => sys.error(s"Could not find upfront payment answers for journey in stage ${other.stage}")
  }

  private def callStartDatesApi(journey: Journey)(using RequestHeader): Future[StartDatesResponse] = {
    val dayOfMonth: PreferredDayOfMonth      = PreferredDayOfMonth(28)
    val upfrontPaymentAnswers                = upfrontPaymentAnswersFromJourney(journey)
    val initialPayment                       = upfrontPaymentAnswers match {
      case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => InitialPayment(value = true)
      case UpfrontPaymentAnswers.NoUpfrontPayment          => InitialPayment(value = false)
    }
    val startDatesRequest: StartDatesRequest = StartDatesRequest(initialPayment, dayOfMonth)
    datesApiConnector.startDates(startDatesRequest)
  }

  private def callPlansApi(
    journey:              Journey,
    incomeAndExpenditure: IncomeAndExpenditure,
    startDatesResponse:   StartDatesResponse
  )(using RequestHeader): Future[AffordableQuotesResponse] = {
    val initialPaymentAmount = upfrontPaymentAnswersFromJourney(journey) match {
      case UpfrontPaymentAnswers.NoUpfrontPayment               => None
      case UpfrontPaymentAnswers.DeclaredUpfrontPayment(amount) => Some(amount)
    }

    val monthlyPaymentAmount = {
      val totalIncome = incomeAndExpenditure.income.mainIncome + incomeAndExpenditure.income.otherIncome

      val totalExpenditure =
        incomeAndExpenditure.expenditure.wagesAndSalaries +
          incomeAndExpenditure.expenditure.mortgageAndRent +
          incomeAndExpenditure.expenditure.bills +
          incomeAndExpenditure.expenditure.materialAndStockCosts +
          incomeAndExpenditure.expenditure.businessTravel +
          incomeAndExpenditure.expenditure.employeeBenefits +
          incomeAndExpenditure.expenditure.other

      AmountInPence((totalIncome - totalExpenditure).value / 2)
    }

    val eligibilityCheckResult = journey match {
      case j: JourneyStage.AfterEligibilityChecked => j.eligibilityCheckResult
      case other                                   => sys.error(s"Could not find eligibility check result in journey with stage ${other.stage}")
    }

    val debtItemCharges = eligibilityCheckResult.chargeTypeAssessment.flatMap(toDebtItemCharge)

    val affordableQuotesRequest: AffordableQuotesRequest = AffordableQuotesRequest(
      channelIdentifier = ChannelIdentifiers.eSSTTP,
      regimeType = RegimeType.fromTaxRegime(journey.taxRegime),
      paymentPlanAffordableAmount = PaymentPlanAffordableAmount(monthlyPaymentAmount),
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanMaxLength = maxPlanLength(eligibilityCheckResult, journey),
      paymentPlanMinLength = eligibilityCheckResult.paymentPlanMinLength,
      accruedDebtInterest = AccruedDebtInterest(TtpService.calculateCumulativeInterest(eligibilityCheckResult)),
      paymentPlanStartDate = startDatesResponse.instalmentStartDate,
      initialPaymentDate = startDatesResponse.initialPaymentDate,
      initialPaymentAmount = initialPaymentAmount,
      debtItemCharges = debtItemCharges,
      customerPostcodes = eligibilityCheckResult.customerPostcodes
    )

    ttpConnector.callAffordableQuotesApi(affordableQuotesRequest, journey.correlationId)
  }

  private def constructPegaGetCaseResponse(
    incomeAndExpenditure: IncomeAndExpenditure,
    paymentPlan:          PaymentPlan
  ): JsValue = {
    val paymentPlanJsonString                                    = {
      val json = Json.toJson(paymentPlan).as[JsObject] + ("planSelected", JsBoolean(true))
      json.toString
    }
    def toAmountOfMoneyItem(value: AmountInPence, label: String) =
      s"""{
         |  "amountValue": "${value.formatInPounds}",
         |  "pyLabel": "$label"
         |}
         |""".stripMargin

    val incomeArray = {
      val items: List[String] = List(
        incomeAndExpenditure.income.mainIncome  -> "Main income",
        incomeAndExpenditure.income.otherIncome -> "Other income"
      ).map((toAmountOfMoneyItem _).tupled)

      s"""[ ${items.mkString(",")} ]"""
    }

    val expenditureArray = {
      val items: List[String] =
        List(
          incomeAndExpenditure.expenditure.wagesAndSalaries      -> "Wages and salaries",
          incomeAndExpenditure.expenditure.mortgageAndRent       -> "Mortgage and rental payments on business premises",
          incomeAndExpenditure.expenditure.bills                 -> "Bills for business premises",
          incomeAndExpenditure.expenditure.materialAndStockCosts -> "Material and stock costs",
          incomeAndExpenditure.expenditure.businessTravel        -> "Business travel",
          incomeAndExpenditure.expenditure.employeeBenefits      -> "Employee benefits",
          incomeAndExpenditure.expenditure.other                 -> "Other",
          AmountInPence.zero                                     -> "My company or partnership does not have any expenditure"
        ).map((toAmountOfMoneyItem _).tupled)

      s"""[ ${items.mkString(",")} ]"""
    }

    Json.parse(
      s"""{
         |  "AA": {
         |    "paymentDay": "28",
         |    "paymentPlan": [ $paymentPlanJsonString ],
         |    "expenditure": $expenditureArray,
         |    "income": $incomeArray
         |  }
         |}
         |""".stripMargin
    )
  }

}
