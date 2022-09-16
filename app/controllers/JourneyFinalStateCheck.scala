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

import cats.Eq
import cats.implicits.catsSyntaxEq
import essttp.journey.model.Stage.AfterSubmittedArrangement
import essttp.journey.model.{Journey, Stage}
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import play.api.mvc.RequestHeader
import util.JourneyLogger

import scala.concurrent.Future

object JourneyFinalStateCheck {
  implicit val eq: Eq[Stage] = Eq.fromUniversalEquals

  private val endStateConditional: Journey => Boolean = journey => journey.stage === AfterSubmittedArrangement.Submitted

  private def logMessage(implicit requestHeader: RequestHeader): Unit =
    JourneyLogger.info("User tried to force browser to a page in the journey, but they have finished their journey. Redirecting to confirmation page")

  private val confirmationRedirect: Result = Redirect(routes.PaymentPlanSetUpController.paymentPlanSetUp())

  def finalStateCheckF(journey: Journey, result: => Future[Result])(implicit request: Request[_]): Future[Result] = {
    if (endStateConditional(journey)) {
      logMessage
      Future.successful(confirmationRedirect)
    } else {
      result
    }
  }

  def finalStateCheck(journey: Journey, result: => Result)(implicit request: Request[_]): Result = {
    if (endStateConditional(journey)) {
      logMessage
      confirmationRedirect
    } else {
      result
    }
  }
}
