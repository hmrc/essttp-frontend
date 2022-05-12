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

import essttp.journey.model.Journey
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.Redirect
import util.JourneyLogger

object JourneyIncorrectStateRouter {

  def logErrorAndRouteToDefaultPage(journey: Journey)(implicit request: RequestHeader): Result = {

    val defaultEndpoint = journey match {
      case j: Journey.Stages.AfterStarted          => routes.LandingController.landingPage()
      case j: Journey.Stages.AfterComputedTaxId    => routes.DetermineEligibilityController.determineEligibility()
      case j: Journey.Stages.AfterEligibilityCheck => routes.YourBillController.yourBill()
    }

    JourneyLogger.error(
      "Journey in incorrect state. " +
        "Please investigate why. " +
        s"Sending user to the first page which supports that journey state: [${defaultEndpoint.url}]"
    )
    Redirect(defaultEndpoint)
  }
}
