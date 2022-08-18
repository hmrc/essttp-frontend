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
import cats.syntax.eq._
import controllers.InstalmentsController.instalmentsForm
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import essttp.journey.model.Journey
import essttp.rootmodel.ttp.affordablequotes.PaymentPlan
import essttp.utils.Errors
import models.InstalmentOption
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InstalmentsController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    journeyService: JourneyService,
    views:          Views
)(implicit executionContext: ExecutionContext) extends FrontendController(mcc)
  with Logging {

  val instalmentOptions: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeAffordableQuotesResponse => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterAffordableQuotesResponse  => finalStateCheckF(j, displayInstalmentOptionsPage(j))
    }
  }

  private def displayInstalmentOptionsPage(journey: Journey.AfterAffordableQuotesResponse)(implicit request: Request[_]): Future[Result] = {
    val maybePrePopForm: Form[String] = journey match {
      case j: Journey.AfterSelectedPaymentPlan =>
        InstalmentsController.instalmentsForm().fill(j.selectedPaymentPlan.numberOfInstalments.value.toString)
      case _ => InstalmentsController.instalmentsForm()
    }
    val instalmentOptions = InstalmentsController.retrieveInstalmentOptions(journey.affordableQuotesResponse.paymentPlans)
    Future.successful(
      Ok(views.instalmentOptionsPage(
        form    = maybePrePopForm,
        options = instalmentOptions,
        backUrl = InstalmentsController.backUrl
      ))
    )
  }

  val instalmentOptionsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val journey: Journey.AfterAffordableQuotesResponse =
      request.journey match {
        case j: Journey.BeforeAffordableQuotesResponse =>
          Errors.throwServerErrorException(s"Cannot submit the instalment form if we don't have affordable quotes... stage: [${j.stage}]")
        case j: Journey.AfterAffordableQuotesResponse => j
      }
    val instalmentOptions = InstalmentsController.retrieveInstalmentOptions(journey.affordableQuotesResponse.paymentPlans)

    instalmentsForm()
      .bindFromRequest()
      .fold({
        formWithErrors =>
          Future.successful(Ok(views.instalmentOptionsPage(
            form    = formWithErrors,
            options = instalmentOptions,
            backUrl = InstalmentsController.backUrl
          )))
      }, {
        (option: String) =>
          val maybePaymentPlan: Option[PaymentPlan] =
            journey.affordableQuotesResponse.paymentPlans.find(_.numberOfInstalments.value === option.toInt)

          maybePaymentPlan.fold[Future[Result]](Errors.throwBadRequestExceptionF("There was no payment plan"))(plan =>
            journeyService.updateChosenPaymentPlan(request.journeyId, plan)
              .map(_ => Redirect(routes.PaymentScheduleController.checkPaymentSchedule())))
      })
  }

}

object InstalmentsController {

  val key: String = "Instalments"

  def instalmentsForm(): Form[String] = Form(
    mapping(
      key -> nonEmptyText
    )(identity)(Some(_))
  )

  val backUrl: Option[String] = Some(routes.PaymentDayController.paymentDay.url)

  def retrieveInstalmentOptions(paymentPlans: List[PaymentPlan]): List[InstalmentOption] = paymentPlans.map { plan =>
    InstalmentOption(
      numberOfMonths       = plan.collections.regularCollections.size,
      amountToPayEachMonth = plan.collections.regularCollections
        .headOption.getOrElse(throw new RuntimeException("There were no regular collections")).amountDue.value,
      interestPayment      = plan.planInterest.value
    )
  }
}
