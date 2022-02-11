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
import controllers.PaymentScheduleController.computeMonths
import models.Journey
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.Confirmation

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ConfirmationController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  confirmationPage: Confirmation)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val confirmation: Action[AnyContent] = as.default.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        Future.successful(Ok(confirmationPage(j.userAnswers, computeMonths(j.userAnswers.getMonthsToPay), "222PX00222222")))
      case _ => sys.error("no journey found to update")
    }
  }
}