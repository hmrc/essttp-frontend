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
import actionsmodel.EligibleJourneyRequest
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.PaymentScheduleController._
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Journey, PaymentPlanAnswers, UpfrontPaymentAnswers, WhyCannotPayInFullAnswers}
import essttp.rootmodel.{DayOfMonth, TaxRegime}
import essttp.utils.Errors
import models.Language
import models.Languages.{English, Welsh}
import play.api.mvc._
import services.{AuditService, JourneyService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}
import viewmodels.CheckPaymentPlanChangeLink
import views.html.checkpaymentchedule.CheckPaymentSchedule

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentScheduleController @Inject() (
    as:                  Actions,
    mcc:                 MessagesControllerComponents,
    paymentSchedulePage: CheckPaymentSchedule,
    journeyService:      JourneyService,
    auditService:        AuditService
)(implicit ec: ExecutionContext, appConfig: AppConfig) extends FrontendController(mcc)
  with Logging {

  implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _

  val checkPaymentSchedule: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request: EligibleJourneyRequest[_] =>
    withJourneyInCorrectState(request.journey){ j =>
      finalStateCheck(request.journey, displayPage(j))
    }
  }

  private def displayPage(
      journey: Either[Journey.AfterSelectedPaymentPlan, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)]
  )(implicit r: EligibleJourneyRequest[_]): Result = {
    val journeyMerged = journey.map(_._1).merge
    val upfrontPaymentAnswers = upfrontPaymentAnswersFromJourney(journeyMerged)
    val dayOfMonth = journey.fold(dayOfMonthFromJourney, _._2.dayOfMonth)
    val selectedPaymentPlan = journey.fold(_.selectedPaymentPlan, _._2.selectedPaymentPlan)
    val monthlyPaymentAmount = journey.fold(
      { case j1: Journey.AfterEnteredMonthlyPaymentAmount => j1.monthlyPaymentAmount },
      _._2.monthlyPaymentAmount
    )

    Ok(paymentSchedulePage(
      upfrontPaymentAnswers,
      dayOfMonth,
      selectedPaymentPlan,
      monthlyPaymentAmount.value,
      whyCannotPayInFullAnswersFromJourney(journeyMerged),
      canPayWithinSixMonthsFromJourney(journeyMerged),
      journeyMerged.taxRegime
    ))
  }

  val checkPaymentScheduleSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    withJourneyInCorrectState(request.journey){

      case Left(j) =>
        j match {
          case j1: Journey.Stages.ChosenPaymentPlan =>
            auditService.auditPaymentPlanBeforeSubmission(j1)

            val paymentPlanAnswers = PaymentPlanAnswers.PaymentPlanNoAffordability(
              j1.monthlyPaymentAmount,
              j1.dayOfMonth,
              j1.startDatesResponse,
              j1.affordableQuotesResponse,
              j.selectedPaymentPlan
            )

            journeyService.updateHasCheckedPaymentPlan(j.journeyId, paymentPlanAnswers)
              .map(updatedJourney =>
                Routing.redirectToNext(routes.PaymentScheduleController.checkPaymentSchedule, updatedJourney, submittedValueUnchanged = false))
        }

      case Right((j, _)) =>
        JourneyLogger.debug(s"Nothing to audit for stage: ${j.stage.toString}")
        Routing.redirectToNext(routes.PaymentScheduleController.checkPaymentSchedule, j, submittedValueUnchanged = false)
    }
  }

  def changeFromCheckPaymentSchedule(pageId: String, regime: TaxRegime, lang: Option[Language]): Action[AnyContent] =
    as.continueToSameEndpointAuthenticatedJourneyAction { implicit request =>
      request.journey match {
        case _: Journey.AfterStartedPegaCase | _: Journey.AfterSelectedPaymentPlan | _: Journey.AfterCheckedPaymentPlan =>
          val result = Redirect(CheckPaymentPlanChangeLink.withName(pageId).targetPage(regime, lang))
            .addingToSession(Routing.clickedChangeFromSessionKey -> "true")
          lang match {
            case Some(Welsh)   => result.withCookies(Cookie("PLAY_LANG", "cy"))
            case Some(English) => result.withCookies(Cookie("PLAY_LANG", "en"))
            case None          => result
          }
        case other =>
          Errors.throwServerErrorException(s"Cannot change answer from check your payment plan page in journey state ${other.name}")
      }
    }

  private def withJourneyInCorrectState[A](journey: Journey)(
      f: Either[Journey.AfterSelectedPaymentPlan, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)] => Future[Result]
  )(implicit r: Request[_]): Future[Result] =
    journey match {
      case _: Journey.BeforeSelectedPaymentPlan =>
        MissingInfoController.redirectToMissingInfoPage()
      case j: Journey.AfterSelectedPaymentPlan =>
        f(Left(j))
      case j: Journey.AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case p: PaymentPlanAnswers.PaymentPlanNoAffordability =>
            f(Right(j -> p))
          case _: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
            Errors.throwServerErrorException("Not expecting to check payment plan here on affordability journey")
        }

      case _: Journey.AfterStartedPegaCase =>
        Errors.throwServerErrorException("Not expecting to check payment plan here when started PEGA case")
    }

}

object PaymentScheduleController {
  private def upfrontPaymentAnswersFromJourney(journey: Journey): UpfrontPaymentAnswers = journey match {
    case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
    case _                                     => Errors.throwServerErrorException("Trying to get upfront payment answers for journey before they exist..")
  }

  private def whyCannotPayInFullAnswersFromJourney(journey: Journey): WhyCannotPayInFullAnswers = journey match {
    case j: Journey.AfterWhyCannotPayInFullAnswers => j.whyCannotPayInFullAnswers
    case _                                         => Errors.throwServerErrorException("Trying to get why cannot pay in full answer for journey before it exists..")
  }

  private def canPayWithinSixMonthsFromJourney(journey: Journey): CanPayWithinSixMonthsAnswers = journey match {
    case j: Journey.AfterCanPayWithinSixMonthsAnswers => j.canPayWithinSixMonthsAnswers
    case _ => Errors.throwServerErrorException("Trying to get can pay within six months answer for journey before it exists...")
  }

  private def dayOfMonthFromJourney(journey: Journey.AfterSelectedPaymentPlan): DayOfMonth = journey match {
    case j: Journey.AfterEnteredDayOfMonth => j.dayOfMonth
    case _                                 => Errors.throwServerErrorException("Trying to get day of month answer for journey before it exists..")
  }
}

