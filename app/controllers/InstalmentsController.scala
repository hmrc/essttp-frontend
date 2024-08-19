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
import cats.syntax.eq._
import cats.syntax.option._
import controllers.InstalmentsController.instalmentsForm
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import essttp.journey.model.{Journey, PaymentPlanAnswers}
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
    withJourneyInCorrectState(request.journey)(j =>
      finalStateCheckF(request.journey, displayInstalmentOptionsPage(j)))
  }

  private def displayInstalmentOptionsPage(journey: Either[Journey.AfterAffordableQuotesResponse, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)])(implicit request: Request[_]): Future[Result] = {
    val maybePrePopForm: Form[String] = {
      val existingValue = journey.fold(existingSelectedPaymentPlan, _._2.selectedPaymentPlan.some)

      existingValue.fold(InstalmentsController.instalmentsForm()){ plan =>
        InstalmentsController.instalmentsForm().fill(plan.numberOfInstalments.value.toString)
      }
    }

    val affordableQuotesResponse = journey.fold(_.affordableQuotesResponse, _._2.affordableQuotesResponse)
    val instalmentOptions = InstalmentsController.retrieveInstalmentOptions(affordableQuotesResponse.paymentPlans)
    Ok(views.instalmentOptionsPage(maybePrePopForm, instalmentOptions))
  }

  private def existingSelectedPaymentPlan(journey: Journey.AfterAffordableQuotesResponse): Option[PaymentPlan] = journey match {
    case j: Journey.AfterSelectedPaymentPlan => Some(j.selectedPaymentPlan)
    case _                                   => None
  }

  val instalmentOptionsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    withJourneyInCorrectState(request.journey){ j =>
      val affordableQuotesResponse = j.fold(_.affordableQuotesResponse, _._2.affordableQuotesResponse)

      instalmentsForm()
        .bindFromRequest()
        .fold(
          { formWithErrors =>
            val instalmentOptions = InstalmentsController.retrieveInstalmentOptions(affordableQuotesResponse.paymentPlans)
            Ok(views.instalmentOptionsPage(formWithErrors, instalmentOptions))
          },
          {
            (option: String) =>
              val maybePaymentPlan: Option[PaymentPlan] =
                affordableQuotesResponse.paymentPlans.find(_.collections.regularCollections.length === option.toInt)

              maybePaymentPlan.fold[Future[Result]](Errors.throwBadRequestExceptionF("There was no payment plan"))(plan =>
                journeyService.updateChosenPaymentPlan(request.journeyId, plan)
                  .map(updatedJourney =>
                    Routing.redirectToNext(
                      routes.InstalmentsController.instalmentOptions,
                      updatedJourney,
                      j.fold(existingSelectedPaymentPlan, _._2.selectedPaymentPlan.some).contains(plan)
                    )))
          }
        )
    }
  }

  private def withJourneyInCorrectState[A](journey: Journey)(
      f: Either[Journey.AfterAffordableQuotesResponse, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)] => Future[Result]
  )(implicit r: Request[_]): Future[Result] =
    journey match {
      case j: Journey.BeforeAffordableQuotesResponse =>
        logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterAffordableQuotesResponse =>
        f(Left(j))
      case j: Journey.AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case p: PaymentPlanAnswers.PaymentPlanNoAffordability =>
            f(Right(j -> p))
          case _: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
            Errors.throwServerErrorException("Not expecting to select payment plan option when payment plan has been checked on affordability journey")
        }

      case _: Journey.AfterStartedPegaCase =>
        Errors.throwServerErrorException("Not expecting to select payment plan option when started PEGA case")
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

  /**
   * We should not show the user any plan that has a monthly payment of less than £1.00
   * We filter here and return instalment options that have a regular collection greater than £1.
   */
  def retrieveInstalmentOptions(paymentPlans: List[PaymentPlan]): List[InstalmentOption] = paymentPlans.map { plan =>
    InstalmentOption(
      numberOfMonths       = plan.collections.regularCollections.length,
      amountToPayEachMonth = plan.collections.regularCollections
        .headOption.getOrElse(throw new RuntimeException("There were no regular collections")).amountDue.value,
      interestPayment      = plan.planInterest.value
    )
  }.filter(_.amountToPayEachMonth.value >= 100)
}
