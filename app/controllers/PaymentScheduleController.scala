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
import essttp.journey.model.Journey
import io.scalaland.chimney.dsl.TransformerOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import requests.RequestSupport
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
    requestSupport:      RequestSupport,
    paymentSchedulePage: CheckPaymentSchedule,
    journeyService:      JourneyService,
    auditService:        AuditService
)(implicit ec: ExecutionContext) extends FrontendController(mcc)
  with Logging {

  implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _

  val checkPaymentSchedule: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case _: Journey.BeforeSelectedPaymentPlan =>
        JourneyLogger.warn("Not enough information in session to show check payment plan page. " +
          "Redirecting to missing info page")
        Redirect(routes.MissingInfoController.missingInfo)

      case j: Journey.AfterSelectedPaymentPlan =>
        val upfrontPaymentAnswers =
          j.into[Journey.AfterUpfrontPaymentAnswers].transform.upfrontPaymentAnswers
        val paymentDay =
          j.into[Journey.AfterEnteredDayOfMonth].transform.dayOfMonth

        finalStateCheck(j, Ok(paymentSchedulePage(upfrontPaymentAnswers, paymentDay, j.selectedPaymentPlan)))
    }
  }

  val checkPaymentScheduleSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case _: Journey.BeforeSelectedPaymentPlan =>
        JourneyLogger.warn("Not enough information in session to show the check payment schedule page. " +
          "Redirecting to missing info page")
        Future.successful(Redirect(routes.MissingInfoController.missingInfo))

      case j: Journey.AfterSelectedPaymentPlan =>
        j match {
          case j1: Journey.Stages.ChosenPaymentPlan => auditService.auditPaymentPlanBeforeSubmission(j1)
          case _                                    => JourneyLogger.debug(s"Nothing to audit for stage: ${j.stage}")
        }
        journeyService.updateHasCheckedPaymentPlan(j.journeyId)
          .map(_ => Redirect(routes.BankDetailsController.typeOfAccount))
    }
  }

}

