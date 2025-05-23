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

import essttp.journey.model.Journey
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import util.JourneyLogger

import scala.concurrent.Future

object JourneyIncorrectStateRouter {

  def logErrorAndRouteToDefaultPageF(journey: Journey)(using Request[?]): Future[Result] =
    Future.successful(logErrorAndRouteToDefaultPage(journey))

  def logErrorAndRouteToDefaultPage(journey: Journey)(using Request[?]): Result = {
    val redirectTo = Routing.latestPossiblePage(journey)

    JourneyLogger.error(
      "Journey in incorrect state. " +
        "Please investigate why. " +
        s"Sending user to the next page the user needs to provide an answer on: ${redirectTo.toString}"
    )
    Redirect(redirectTo)
  }
}
