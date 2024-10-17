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
import essttp.journey.model.UpfrontPaymentAnswers.DeclaredUpfrontPayment
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.ttp.eligibility.{DebtTotalAmount, EligibilityCheckResult}
import essttp.rootmodel.{AmountInPence, CanPayUpfront, UpfrontPaymentAmount}
import models.enumsforforms.CanPayUpfrontFormValue
import models.forms.{CanPayUpfrontForm, UpfrontPaymentAmountForm}
import play.api.data.Form
import play.api.mvc._
import requests.RequestSupport
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import viewmodels.UpfrontPaymentSummaryChangeLink
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UpfrontPaymentController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    views:          Views,
    journeyService: JourneyService,
    requestSupport: RequestSupport
)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val canYouMakeAnUpfrontPayment: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    finalStateCheck(request.journey, displayCanYouPayUpfrontPage(request.journey))
  }

  private def displayCanYouPayUpfrontPage(journey: Journey)(implicit request: Request[_]): Result = {
    val maybePrePoppedForm: Form[CanPayUpfrontFormValue] =
      existingCanYouPayUpfrontAnswer(journey).fold(CanPayUpfrontForm.form){ canPayUpfront =>
        CanPayUpfrontForm.form.fill(CanPayUpfrontFormValue.canPayUpfrontToFormValue(canPayUpfront))
      }

    Ok(views.canYouMakeAnUpFrontPayment(maybePrePoppedForm))
  }

  private def existingCanYouPayUpfrontAnswer(journey: Journey): Option[CanPayUpfront] = journey match {
    case _: Journey.BeforeAnsweredCanPayUpfront => None
    case j: Journey.AfterAnsweredCanPayUpfront  => Some(j.canPayUpfront)
    case j: Journey.AfterUpfrontPaymentAnswers =>
      j.upfrontPaymentAnswers match {
        case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(CanPayUpfront(value = true))
        case UpfrontPaymentAnswers.NoUpfrontPayment          => Some(CanPayUpfront(value = false))
      }
  }

  val canYouMakeAnUpfrontPaymentSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    CanPayUpfrontForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(views.canYouMakeAnUpFrontPayment(formWithErrors)),
        { canPayUpfrontForm: CanPayUpfrontFormValue =>
          val canPayUpfront: CanPayUpfront = canPayUpfrontForm.asCanPayUpfront
          journeyService.updateCanPayUpfront(request.journeyId, canPayUpfront)
            .map(updatedJourney =>
              Routing.redirectToNext(
                routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment,
                updatedJourney,
                existingCanYouPayUpfrontAnswer(request.journey).contains(canPayUpfront)
              ))
        }
      )
  }

  val upfrontPaymentAmount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeAnsweredCanPayUpfront =>
        logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterAnsweredCanPayUpfront =>
        if (j.canPayUpfront.userCanPayUpfront) {
          finalStateCheck(j, displayUpfrontPaymentAmountPage(request.eligibilityCheckResult, Left(j)))
        } else {
          logErrorAndRouteToDefaultPage(j)
        }

      case j: Journey.AfterUpfrontPaymentAnswers =>
        finalStateCheck(j, displayUpfrontPaymentAmountPage(request.eligibilityCheckResult, Right(j)))
    }
  }

  private val minimumUpfrontPaymentAmount: AmountInPence = appConfig.PolicyParameters.minimumUpfrontPaymentAmountInPence

  private def displayUpfrontPaymentAmountPage(
      eligibilityCheckResult: EligibilityCheckResult,
      journey:                Either[Journey.AfterAnsweredCanPayUpfront, Journey.AfterUpfrontPaymentAnswers]
  )(implicit request: Request[_]): Result = {
    val debtTotalAmount: DebtTotalAmount = UpfrontPaymentController.determineTotalAmountToPayWithoutInterest(eligibilityCheckResult)
    val maximumUpfrontPaymentAmountInPence: AmountInPence = debtTotalAmount.value.-(minimumUpfrontPaymentAmount)

    val maybePrePoppedForm: Form[BigDecimal] = {
      val form = UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount)
      existingUpfrontPaymentAmount(journey.merge).fold(form){ amount =>
        form.fill(amount.value.inPounds)
      }
    }

    Ok(views.upfrontPaymentAmountPage(
      form           = maybePrePoppedForm,
      maximumPayment = maximumUpfrontPaymentAmountInPence,
      minimumPayment = minimumUpfrontPaymentAmount
    ))
  }

  private def existingUpfrontPaymentAmount(journey: Journey): Option[UpfrontPaymentAmount] = journey match {
    case _: Journey.BeforeEnteredUpfrontPaymentAmount => None
    case j: Journey.AfterEnteredUpfrontPaymentAmount  => Some(j.upfrontPaymentAmount)
    case j: Journey.AfterUpfrontPaymentAnswers =>
      j.upfrontPaymentAnswers match {
        case j1: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(j1.amount)
        case UpfrontPaymentAnswers.NoUpfrontPayment           => None
      }
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val debtTotalAmount: DebtTotalAmount = UpfrontPaymentController.determineTotalAmountToPayWithoutInterest(request.eligibilityCheckResult)
    val maximumUpfrontPaymentAmountInPence: AmountInPence = debtTotalAmount.value.-(minimumUpfrontPaymentAmount)

    UpfrontPaymentAmountForm.form(DebtTotalAmount(maximumUpfrontPaymentAmountInPence), minimumUpfrontPaymentAmount)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[BigDecimal]) =>
          Ok(
            views.upfrontPaymentAmountPage(
              form           = formWithErrors,
              maximumPayment = maximumUpfrontPaymentAmountInPence,
              minimumPayment = minimumUpfrontPaymentAmount
            )
          ),
        (validForm: BigDecimal) => {
          //amount in pence case class apply method converts big decimal to pennies
          val upfrontPaymentAmount = UpfrontPaymentAmount(AmountInPence(validForm))

          journeyService.updateUpfrontPaymentAmount(request.journeyId, upfrontPaymentAmount)
            .map(updatedJourney =>
              Routing.redirectToNext(
                routes.UpfrontPaymentController.upfrontPaymentAmount,
                updatedJourney,
                existingUpfrontPaymentAmount(request.journey).contains(upfrontPaymentAmount)
              ))
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
    val totalAmountToPay: DebtTotalAmount = UpfrontPaymentController.determineTotalAmountToPayWithInterest(eligibilityCheckResult)
    val remainingAmountTest: AmountInPence = UpfrontPaymentController.deriveRemainingAmountToPay(totalAmountToPay, declaredUpfrontPayment.amount)

    Ok(views.upfrontSummaryPage(
      upfrontPayment       = declaredUpfrontPayment.amount,
      remainingAmountToPay = remainingAmountTest
    ))
  }

  def changeFromUpfrontPaymentSummary(pageId: String): Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    Redirect(UpfrontPaymentSummaryChangeLink.withName(pageId).targetPage)
      .addingToSession(Routing.clickedChangeFromSessionKey -> routes.UpfrontPaymentController.upfrontPaymentSummary.url)
  }

}

object UpfrontPaymentController {

  def determineTotalAmountToPayWithInterest(eligibilityCheckResult: EligibilityCheckResult): DebtTotalAmount =
    DebtTotalAmount(
      eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount.value).fold(AmountInPence.zero)(_ + _)
    )

  def determineTotalAmountToPayWithoutInterest(eligibilityCheckResult: EligibilityCheckResult): DebtTotalAmount =
    DebtTotalAmount(
      eligibilityCheckResult.chargeTypeAssessment.flatMap(_.charges.map(_.charges1.outstandingAmount.value)).fold(AmountInPence.zero)(_ + _)
    )

  def deriveRemainingAmountToPay(totalDebt: DebtTotalAmount, upfrontPaymentAmount: UpfrontPaymentAmount): AmountInPence =
    totalDebt.value.-(upfrontPaymentAmount.value)

}
