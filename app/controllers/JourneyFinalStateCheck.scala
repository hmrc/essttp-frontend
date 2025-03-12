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

import essttp.journey.model.{Journey, JourneyStage}
import essttp.rootmodel.TaxRegime
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, RequestHeader, Result}
import util.JourneyLogger

import scala.concurrent.Future

object JourneyFinalStateCheck {

  private val endStateConditional: Journey => Boolean = {
    case _: JourneyStage.AfterArrangementSubmitted => true
    case _                                         => false
  }

  private def logMessage(using RequestHeader): Unit =
    JourneyLogger.info(
      "User tried to force browser to a page in the journey, but they have finished their journey. Redirecting to confirmation page"
    )

  private def confirmationRedirect(taxRegime: TaxRegime): Result = Redirect(
    SubmitArrangementController.whichPaymentPlanSetupPage(taxRegime)
  )

  def finalStateCheckF(journey: Journey, result: => Future[Result])(using Request[?]): Future[Result] =
    if (endStateConditional(journey)) {
      logMessage
      confirmationRedirect(journey.taxRegime)
    } else {
      result
    }

  def finalStateCheck(journey: Journey, result: => Result)(using Request[?]): Result =
    if (endStateConditional(journey)) {
      logMessage
      confirmationRedirect(journey.taxRegime)
    } else {
      result
    }
}
