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
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.PaymentScheduleController.{dayOfMonthFromJourney, upfrontPaymentAnswersFromJourney}
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.DayOfMonth
import essttp.utils.Errors
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, JourneyService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}
import views.html.CheckPaymentSchedule

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
)(implicit ec: ExecutionContext) extends FrontendController(mcc)
  with Logging {

  implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _

  val checkPaymentSchedule: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case _: Journey.BeforeSelectedPaymentPlan =>
        MissingInfoController.redirectToMissingInfoPage()

      case j: Journey.AfterSelectedPaymentPlan =>
        finalStateCheck(
          journey = j,
          result  = Ok(paymentSchedulePage(
            upfrontPaymentAnswers = upfrontPaymentAnswersFromJourney(j),
            paymentDay            = dayOfMonthFromJourney(j),
            paymentPlan           = j.selectedPaymentPlan
          ))
        )
    }
  }

  val checkPaymentScheduleSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case _: Journey.BeforeSelectedPaymentPlan =>
        Future.successful(MissingInfoController.redirectToMissingInfoPage())

      case j: Journey.AfterSelectedPaymentPlan =>
        j match {
          case j1: Journey.Stages.ChosenPaymentPlan => auditService.auditPaymentPlanBeforeSubmission(j1)
          case _                                    => JourneyLogger.debug(s"Nothing to audit for stage: ${j.stage.toString}")
        }
        journeyService.updateHasCheckedPaymentPlan(j.journeyId)
          .map(updatedJourney => Redirect(Routing.next(updatedJourney)))
    }
  }

}

object PaymentScheduleController {
  private def upfrontPaymentAnswersFromJourney(journey: Journey.AfterSelectedPaymentPlan): UpfrontPaymentAnswers = journey match {
    case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
    case _                                     => Errors.throwServerErrorException("Trying to get upfront payment answers for journey before they exist..")
  }

  private def dayOfMonthFromJourney(journey: Journey.AfterSelectedPaymentPlan): DayOfMonth = journey match {
    case j: Journey.AfterEnteredDayOfMonth => j.dayOfMonth
    case _                                 => Errors.throwServerErrorException("Trying to get day of month answer for journey before it exists..")
  }
}

