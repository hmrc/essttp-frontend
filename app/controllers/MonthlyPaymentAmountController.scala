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

package controllers

import actions.Actions
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import controllers.MonthlyPaymentAmountController.{monthlyPaymentAmountForm, upfrontPaymentAnswersFromJourney}
import essttp.journey.model.{Journey, JourneyStage, PaymentPlanAnswers, UpfrontPaymentAnswers}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import essttp.rootmodel.{AmountInPence, MonthlyPaymentAmount}
import essttp.utils.Errors
import models.MoneyUtil.*
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms}
import play.api.mvc.*
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MonthlyPaymentAmountController @Inject() (
  as:             Actions,
  mcc:            MessagesControllerComponents,
  views:          Views,
  journeyService: JourneyService
)(using ExecutionContext, AppConfig)
    extends FrontendController(mcc),
      Logging {

  val displayMonthlyPaymentAmount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeRetrievedAffordabilityResult => logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterRetrievedAffordabilityResult  =>
        finalStateCheck(j, displayMonthlyPaymentAmountPage(j, request.eligibilityCheckResult))
    }
  }

  private def displayMonthlyPaymentAmountPage(
    journey:                JourneyStage.AfterRetrievedAffordabilityResult & Journey,
    eligibilityCheckResult: EligibilityCheckResult
  )(using Request[?]): Result = {

    val totalDebt                                                = AmountInPence(eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount.value.value).sum)
    val upfrontPaymentAmount                                     = upfrontPaymentAnswersFromJourney(journey) match {
      case j1: UpfrontPaymentAnswers.DeclaredUpfrontPayment => j1.amount.value
      case UpfrontPaymentAnswers.NoUpfrontPayment           => AmountInPence.zero
    }
    val instalmentAmounts                                        = journey.instalmentAmounts
    val (roundedMin, roundedMax): (AmountInPence, AmountInPence) = MonthlyPaymentAmountController.roundingForMinMax(
      amountLeft = totalDebt.-(upfrontPaymentAmount),
      minimumAmount = instalmentAmounts.minimumInstalmentAmount,
      maximumAmount = instalmentAmounts.maximumInstalmentAmount
    )
    val maybePrePoppedForm: Form[BigDecimal]                     = {
      val form = monthlyPaymentAmountForm(roundedMin, roundedMax)
      existingMonthlyPaymentAmount(journey).fold(form)(amount => form.fill(amount.value.inPounds))
    }

    Ok(
      views.monthlyPaymentAmountPage(
        form = maybePrePoppedForm,
        maximumPayment = roundedMax,
        minimumPayment = roundedMin
      )
    )
  }

  private def existingMonthlyPaymentAmount(journey: Journey): Option[MonthlyPaymentAmount] = journey match {
    case _: JourneyStage.BeforeEnteredMonthlyPaymentAmount =>
      None
    case j: JourneyStage.AfterEnteredMonthlyPaymentAmount  =>
      Some(j.monthlyPaymentAmount)
    case j: JourneyStage.AfterCheckedPaymentPlan           =>
      j.paymentPlanAnswers match {
        case p: PaymentPlanAnswers.PaymentPlanNoAffordability    =>
          Some(p.monthlyPaymentAmount)
        case _: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
          Errors.throwServerErrorException("Trying to find monthly payment amount on affordability journey")
      }
    case _: JourneyStage.AfterStartedPegaCase              =>
      Errors.throwServerErrorException("Trying to find monthly payment amount after stating PEGA case")
  }

  val monthlyPaymentAmountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val (minMaxResponse, amountLeft)                             = request.journey match {
      case _: JourneyStage.BeforeRetrievedAffordabilityResult =>
        Errors.throwServerErrorException("We don't have the affordability api response...")
      case j: JourneyStage.AfterRetrievedAffordabilityResult  =>
        val totalDebt: AmountInPence            =
          AmountInPence(request.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount.value.value).sum)
        val upfrontPaymentAmount: AmountInPence = upfrontPaymentAnswersFromJourney(j) match {
          case UpfrontPaymentAnswers.DeclaredUpfrontPayment(amount) => amount.value
          case UpfrontPaymentAnswers.NoUpfrontPayment               => AmountInPence.zero
        }
        (j.instalmentAmounts, totalDebt.-(upfrontPaymentAmount))
    }
    val (roundedMin, roundedMax): (AmountInPence, AmountInPence) =
      MonthlyPaymentAmountController.roundingForMinMax(
        amountLeft,
        minMaxResponse.minimumInstalmentAmount,
        minMaxResponse.maximumInstalmentAmount
      )

    MonthlyPaymentAmountController
      .monthlyPaymentAmountForm(roundedMin, roundedMax)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Ok(
            views.monthlyPaymentAmountPage(
              form = formWithErrors,
              maximumPayment = roundedMax,
              minimumPayment = roundedMin
            )
          ),
        (submittedValue: BigDecimal) => {
          val monthlyPaymentAmount: MonthlyPaymentAmount = MonthlyPaymentAmount(AmountInPence(submittedValue))
          journeyService
            .updateMonthlyPaymentAmount(request.journeyId, monthlyPaymentAmount)
            .map(updatedJourney =>
              Routing.redirectToNext(
                routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount,
                updatedJourney,
                existingMonthlyPaymentAmount(request.journey).map(_.value.inPounds).contains(submittedValue)
              )
            )
        }
      )
  }
}

object MonthlyPaymentAmountController {

  private val onePound: AmountInPence = AmountInPence(100)

  // if amount left of the balance is < £10, round to nearest £1, else round to nearest £10
  // extra: if minimum amount is < £1, return £1
  def roundingForMinMax(
    amountLeft:    AmountInPence,
    minimumAmount: AmountInPence,
    maximumAmount: AmountInPence
  ): (AmountInPence, AmountInPence) = {
    val (min, max) = if (amountLeft.value < 1000) {
      round(minimumAmount.inPounds, maximumAmount.inPounds, (1.0, 0.5))
    } else {
      round(minimumAmount.inPounds, maximumAmount.inPounds, (10.0, 5.0))
    }
    if (min.value < 100 & max.value <= 100) {
      (onePound, onePound)
    } else if (min.value < 100) {
      (onePound, max)
    } else {
      (min, max)
    }
  }

  private def round(a: BigDecimal, b: BigDecimal, roundingFactors: (Double, Double)): (AmountInPence, AmountInPence) = {
    val remainderA: BigDecimal = a % roundingFactors._1
    val remainderB: BigDecimal = b % roundingFactors._1
    val minRounded             = if (remainderA > roundingFactors._2) a + (roundingFactors._1 - remainderA) else a - remainderA
    val maxRounded             = if (remainderB > roundingFactors._2) b + (roundingFactors._1 - remainderB) else b - remainderB
    (AmountInPence(minRounded), AmountInPence(maxRounded))
  }

  def backUrl(journey: Journey): Option[String] =
    journey match {
      case _: JourneyStage.BeforeUpfrontPaymentAnswers =>
        Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
      case j: JourneyStage.AfterUpfrontPaymentAnswers  =>
        j.upfrontPaymentAnswers match {
          case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment =>
            Some(routes.UpfrontPaymentController.upfrontPaymentSummary.url)
          case UpfrontPaymentAnswers.NoUpfrontPayment          =>
            Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
        }
    }

  private val key: String = "MonthlyPaymentAmount"

  def monthlyPaymentAmountForm(minimumAmount: AmountInPence, maximumAmount: AmountInPence): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(minimumAmount.inPounds > _, maximumAmount.inPounds < _))
    )(identity)(Some(_))
  )

  private def upfrontPaymentAnswersFromJourney(
    journey: JourneyStage.AfterRetrievedAffordabilityResult & Journey
  ): UpfrontPaymentAnswers =
    journey match {
      case j: JourneyStage.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
    }
}
