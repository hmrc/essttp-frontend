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

package controllers

import _root_.actions.Actions
import config.AppConfig
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import essttp.journey.model.Journey
import essttp.journey.model.ttp.DebtTotalAmount
import essttp.rootmodel.{AmountInPence, CanPayUpfront, UpfrontPaymentAmount}
import essttp.utils.Errors
import models.enumsforforms.CanPayUpfrontFormValue
import models.forms.{CanPayUpfrontForm, UpfrontPaymentAmountForm}
import play.api.data.Form
import play.api.mvc._
import requests.RequestSupport
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpfrontPaymentController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    views:          Views,
    journeyService: JourneyService,
    requestSupport: RequestSupport,
    appConfig:      AppConfig
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val canYouMakeAnUpfrontPayment: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEligibilityChecked => logErrorAndRouteToDefaultPage(j)
      case _: Journey.AfterEligibilityChecked  => displayCanYouPayUpfrontPage()
    }
  }

  private def displayCanYouPayUpfrontPage()(implicit request: Request[_]): Result = {
    val backUrl: Option[String] = Some(routes.YourBillController.yourBill().url)
    Ok(views.canYouMakeAnUpFrontPayment(CanPayUpfrontForm.form, backUrl))
  }

  val canYouMakeAnUpfrontPaymentSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    CanPayUpfrontForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(views.canYouMakeAnUpFrontPayment(
            form    = formWithErrors,
            backUrl = Some(routes.YourBillController.yourBill().url)
          ))),
        (canPayUpfrontForm: CanPayUpfrontFormValue) => {
          val canPayUpfront: CanPayUpfront = canPayUpfrontForm.asCanPayUpfront
          val pageToRedirectTo: Call =
            if (canPayUpfront.value) {
              UpfrontPaymentController.upfrontPaymentAmountCall
            } else {
              routes.MonthlyPaymentAmountController.monthlyPaymentAmount()
            }
          journeyService.updateCanPayUpfront(request.journeyId, canPayUpfront)
            .map(_ => Redirect(pageToRedirectTo.url))
        }
      )
  }

  val upfrontPaymentAmount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeAnsweredCanPayUpfront                                      => logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterAnsweredCanPayUpfront if !j.canPayUpfront.userCanPayUpfront => logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterAnsweredCanPayUpfront if j.canPayUpfront.userCanPayUpfront  => displayUpfrontPageAmountPage(j)
    }
  }

  private val minimumUpfrontPaymentAmount: AmountInPence = appConfig.JourneyVariables.minimumUpfrontPaymentAmountInPence

  private def displayUpfrontPageAmountPage(journey: Journey.AfterAnsweredCanPayUpfront)(implicit request: Request[_]): Result = {
    val backUrl: Option[String] = Some(UpfrontPaymentController.canYouMakeAnUpfrontPaymentCall.url)
    val debtTotalAmount: DebtTotalAmount = UpfrontPaymentController.determineMaxDebt(journey) match {
      case Some(value) => value
      case None        => Errors.throwBadRequestException("Could not determine max debt")
    }
    val maximumUpfrontPaymentAmountInPence: AmountInPence = AmountInPence(debtTotalAmount.value) - minimumUpfrontPaymentAmount
    Ok(views.upfrontPaymentAmountPage(
      form           = UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence.value.intValue()), minimumUpfrontPaymentAmount),
      maximumPayment = maximumUpfrontPaymentAmountInPence,
      minimumPayment = minimumUpfrontPaymentAmount,
      backUrl        = backUrl
    ))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val journey = request.journey match {
      case _: Journey.BeforeAnsweredCanPayUpfront => Errors.throwBadRequestException("User has submitted upfront payment amount before answering CanPayUpfront")
      case j: Journey.AfterAnsweredCanPayUpfront  => j
    }
    /**
     * TODO change the totalDebtAmount calculated here
     * I think ttp are changing their api to have total debt on the top level, not in the chargeTypeAssessment array
     */
    val debtTotalAmount: DebtTotalAmount = UpfrontPaymentController.determineMaxDebt(journey) match {
      case Some(value) => value
      case None        => Errors.throwBadRequestException("Could not determine max debt")
    }
    val maximumUpfrontPaymentAmountInPence: AmountInPence = AmountInPence(debtTotalAmount.value) - minimumUpfrontPaymentAmount

    UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence.value.intValue()), minimumUpfrontPaymentAmount)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[BigDecimal]) =>
          Future.successful(Ok(
            views.upfrontPaymentAmountPage(
              form           = formWithErrors,
              maximumPayment = maximumUpfrontPaymentAmountInPence,
              // todo, find out what the minimum upfront payment amount can be, for now £1
              minimumPayment = minimumUpfrontPaymentAmount,
              backUrl        = Some(UpfrontPaymentController.canYouMakeAnUpfrontPaymentCall.url)
            )
          )),
        (validForm: BigDecimal) => {
          //amount in pence case class apply method converts big decimal to pennies
          val amountInPence: AmountInPence = AmountInPence(validForm)
          journeyService.updateUpfrontPaymentAmount(request.journeyId, UpfrontPaymentAmount(amountInPence))
            .map(_ => Redirect(routes.UpfrontPaymentController.upfrontPaymentSummary().url))
        }
      )
  }

  val upfrontPaymentSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredUpfrontPaymentAmount => logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterEnteredUpfrontPaymentAmount  => displayUpfrontPaymentSummaryPage(j)
    }
  }

  private def displayUpfrontPaymentSummaryPage(journey: Journey)(implicit request: Request[_]): Result = {
    val journeyWithUpfrontPayment = journey match {
      case j: Journey.BeforeEnteredUpfrontPaymentAmount =>
        Errors.throwBadRequestException(s"We should not be on the upfront payment summary page without an upfront payment... [$j]")
      case j: Journey.Stages.EnteredUpfrontPaymentAmount => j
      case j: Journey.Stages.EnteredDayOfMonth           => Errors.throwBadRequestException("Not built yet, update me when you've built the page")
      case j: Journey.Stages.EnteredInstalmentAmount     => Errors.throwBadRequestException("Not built yet, update me when you've built the page")
      case j: Journey.Stages.HasSelectedPlan             => Errors.throwBadRequestException("Not built yet, update me when you've built the page")
    }
    val remainingAmountToPay: AmountInPence =
      journeyWithUpfrontPayment.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount).headOption
        .fold(Errors.throwBadRequestException("some error"))(
          (amount: DebtTotalAmount) => AmountInPence(amount.value) - journeyWithUpfrontPayment.upfrontPaymentAmount.value
        )
    Ok(views.upfrontSummaryPage(
      upfrontPayment       = journeyWithUpfrontPayment.upfrontPaymentAmount,
      remainingAmountToPay = remainingAmountToPay,
      backUrl              = Some(UpfrontPaymentController.upfrontPaymentAmountCall.url)
    ))
  }
}

object UpfrontPaymentController {
  def determineMaxDebt(journey: Journey.AfterAnsweredCanPayUpfront): Option[DebtTotalAmount] = journey match {
    case j: Journey.AfterEligibilityChecked => j.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount).headOption
  }

  val canYouMakeAnUpfrontPaymentCall: Call = routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment()
  val upfrontPaymentAmountCall: Call = routes.UpfrontPaymentController.upfrontPaymentAmount()
}
