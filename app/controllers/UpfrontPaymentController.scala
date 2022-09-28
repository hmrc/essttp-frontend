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
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.journey.model.Journey.AfterUpfrontPaymentAnswers
import essttp.journey.model.UpfrontPaymentAnswers.DeclaredUpfrontPayment
import essttp.rootmodel.ttp.{DebtTotalAmount, EligibilityCheckResult}
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
    finalStateCheck(request.journey, displayCanYouPayUpfrontPage(request.journey))
  }

  private def displayCanYouPayUpfrontPage(journey: Journey)(implicit request: Request[_]): Result = {
    val maybePrePoppedForm: Form[CanPayUpfrontFormValue] = journey match {
      case _: Journey.BeforeAnsweredCanPayUpfront => CanPayUpfrontForm.form
      case j: Journey.AfterAnsweredCanPayUpfront =>
        CanPayUpfrontForm.form.fill(CanPayUpfrontFormValue.canPayUpfrontToFormValue(j.canPayUpfront))
      case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers match {
        case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment =>
          CanPayUpfrontForm.form.fill(CanPayUpfrontFormValue.canPayUpfrontToFormValue(CanPayUpfront(true)))
        case UpfrontPaymentAnswers.NoUpfrontPayment =>
          CanPayUpfrontForm.form.fill(CanPayUpfrontFormValue.canPayUpfrontToFormValue(CanPayUpfront(false)))
      }
    }
    Ok(views.canYouMakeAnUpFrontPayment(maybePrePoppedForm))
  }

  val canYouMakeAnUpfrontPaymentSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    CanPayUpfrontForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(views.canYouMakeAnUpFrontPayment(formWithErrors))),
        (canPayUpfrontForm: CanPayUpfrontFormValue) => {
          val canPayUpfront: CanPayUpfront = canPayUpfrontForm.asCanPayUpfront
          val pageToRedirectTo: Call =
            if (canPayUpfront.value) {
              UpfrontPaymentController.upfrontPaymentAmountCall
            } else {
              routes.DatesApiController.retrieveExtremeDates
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
      case j: Journey.AfterAnsweredCanPayUpfront if j.canPayUpfront.userCanPayUpfront  => finalStateCheck(j, displayUpfrontPageAmountPage(Left(j)))
      case j: Journey.AfterUpfrontPaymentAnswers                                       => finalStateCheck(j, displayUpfrontPageAmountPage(Right(j)))
    }
  }

  private val minimumUpfrontPaymentAmount: AmountInPence = appConfig.PolicyParameters.minimumUpfrontPaymentAmountInPence

  private def displayUpfrontPageAmountPage(
      journey: Either[Journey.AfterAnsweredCanPayUpfront, Journey.AfterUpfrontPaymentAnswers]
  )(implicit request: Request[_]): Result = {
    val debtTotalAmount: DebtTotalAmount = UpfrontPaymentController.determineMaxDebt(journey) match {
      case Some(value) => value
      case None        => Errors.throwBadRequestException("Could not determine max debt")
    }
    val maximumUpfrontPaymentAmountInPence: AmountInPence = debtTotalAmount.value.-(minimumUpfrontPaymentAmount)
    val maybePrePoppedForm: Form[BigDecimal] = journey.merge match {
      case _: Journey.BeforeEnteredUpfrontPaymentAmount => UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount)
      case j: Journey.AfterEnteredUpfrontPaymentAmount  => UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount).fill(j.upfrontPaymentAmount.value.inPounds)
      case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers match {
        case j1: UpfrontPaymentAnswers.DeclaredUpfrontPayment => UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount).fill(j1.amount.value.inPounds)
        case UpfrontPaymentAnswers.NoUpfrontPayment           => UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount)
      }
    }
    Ok(views.upfrontPaymentAmountPage(
      form           = maybePrePoppedForm,
      maximumPayment = maximumUpfrontPaymentAmountInPence,
      minimumPayment = minimumUpfrontPaymentAmount
    ))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val journey: Either[Journey.AfterAnsweredCanPayUpfront, AfterUpfrontPaymentAnswers] = request.journey match {
      case _: Journey.BeforeAnsweredCanPayUpfront => Errors.throwBadRequestException("User has submitted upfront payment amount before answering CanPayUpfront")
      case j: Journey.AfterAnsweredCanPayUpfront  => Left(j)
      case j: Journey.AfterUpfrontPaymentAnswers  => Right(j)
    }
    /**
     * TODO change the totalDebtAmount calculated here
     * I think ttp are changing their api to have total debt on the top level, not in the chargeTypeAssessment array
     */
    val debtTotalAmount: DebtTotalAmount = UpfrontPaymentController.determineMaxDebt(journey) match {
      case Some(value) => value
      case None        => Errors.throwBadRequestException("Could not determine max debt")
    }
    val maximumUpfrontPaymentAmountInPence: AmountInPence = debtTotalAmount.value.-(minimumUpfrontPaymentAmount)

    UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[BigDecimal]) =>
          Future.successful(Ok(
            views.upfrontPaymentAmountPage(
              form           = formWithErrors,
              maximumPayment = maximumUpfrontPaymentAmountInPence,
              // todo, find out what the minimum upfront payment amount can be, for now £1
              minimumPayment = minimumUpfrontPaymentAmount
            )
          )),
        (validForm: BigDecimal) => {
          //amount in pence case class apply method converts big decimal to pennies
          val amountInPence: AmountInPence = AmountInPence(validForm)
          journeyService.updateUpfrontPaymentAmount(request.journeyId, UpfrontPaymentAmount(amountInPence))
            .map(_ => Redirect(routes.UpfrontPaymentController.upfrontPaymentSummary.url))
        }
      )
  }

  val upfrontPaymentSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case _: Journey.BeforeEnteredUpfrontPaymentAmount =>
        MissingInfoController.redirectToMissingInfoPage()

      case j: Journey.AfterEnteredUpfrontPaymentAmount =>
        val declaredUpfrontPayment = UpfrontPaymentAnswers.DeclaredUpfrontPayment(j.upfrontPaymentAmount)
        finalStateCheck(request.journey, displayUpfrontPaymentSummaryPage(request.eligibilityCheckResult, declaredUpfrontPayment))

      case j: Journey.AfterUpfrontPaymentAnswers =>
        j.upfrontPaymentAnswers match {
          case UpfrontPaymentAnswers.NoUpfrontPayment =>
            finalStateCheck(request.journey, MissingInfoController.redirectToMissingInfoPage())
          case d: UpfrontPaymentAnswers.DeclaredUpfrontPayment =>
            finalStateCheck(request.journey, displayUpfrontPaymentSummaryPage(request.eligibilityCheckResult, d))
        }
    }
  }

  private def displayUpfrontPaymentSummaryPage(
      eligibilityCheckResult: EligibilityCheckResult,
      declaredUpfrontPayment: DeclaredUpfrontPayment
  )(implicit request: Request[_]): Result = {
    val totalAmountToPay: DebtTotalAmount = DebtTotalAmount(AmountInPence(eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount.value.value).sum))
    val remainingAmountTest: AmountInPence = UpfrontPaymentController.deriveRemainingAmountToPay(totalAmountToPay, declaredUpfrontPayment.amount)

    Ok(views.upfrontSummaryPage(
      upfrontPayment       = declaredUpfrontPayment.amount,
      remainingAmountToPay = remainingAmountTest
    ))
  }
}

object UpfrontPaymentController {
  def determineMaxDebt(journey: Either[Journey.AfterAnsweredCanPayUpfront, Journey.AfterUpfrontPaymentAnswers]): Option[DebtTotalAmount] = {
    val eligibilityCheckResult = journey match {
      case Left(j: Journey.AfterEligibilityChecked) => j.eligibilityCheckResult
      case Right(j: Journey.AfterUpfrontPaymentAnswers) => j match {
        case j1: Journey.Stages.RetrievedExtremeDates          => j1.eligibilityCheckResult
        case j1: Journey.Stages.RetrievedAffordabilityResult   => j1.eligibilityCheckResult
        case j1: Journey.Stages.EnteredMonthlyPaymentAmount    => j1.eligibilityCheckResult
        case j1: Journey.Stages.EnteredDayOfMonth              => j1.eligibilityCheckResult
        case j1: Journey.Stages.RetrievedStartDates            => j1.eligibilityCheckResult
        case j1: Journey.Stages.RetrievedAffordableQuotes      => j1.eligibilityCheckResult
        case j1: Journey.Stages.ChosenPaymentPlan              => j1.eligibilityCheckResult
        case j1: Journey.Stages.CheckedPaymentPlan             => j1.eligibilityCheckResult
        case j1: Journey.Stages.EnteredDetailsAboutBankAccount => j1.eligibilityCheckResult
        case j1: Journey.Stages.EnteredDirectDebitDetails      => j1.eligibilityCheckResult
        case j1: Journey.Stages.ConfirmedDirectDebitDetails    => j1.eligibilityCheckResult
        case j1: Journey.Stages.AgreedTermsAndConditions       => j1.eligibilityCheckResult
        case j1: Journey.Stages.SubmittedArrangement           => j1.eligibilityCheckResult
      }
    }
    eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount).headOption
  }

  def deriveRemainingAmountToPay(maxDebt: DebtTotalAmount, upfrontPaymentAmount: UpfrontPaymentAmount): AmountInPence =
    maxDebt.value.-(upfrontPaymentAmount.value)

  val canYouMakeAnUpfrontPaymentCall: Call = routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment
  val upfrontPaymentAmountCall: Call = routes.UpfrontPaymentController.upfrontPaymentAmount
}
