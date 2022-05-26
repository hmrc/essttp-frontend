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
import essttp.utils.Errors
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import util.JourneyLogger

import scala.concurrent.Future

object JourneyIncorrectStateRouter {

  def logErrorAndRouteToDefaultPageF(journey: Journey)(implicit request: Request[_]): Future[Result] = Future.successful(logErrorAndRouteToDefaultPage(journey))

  def logErrorAndRouteToDefaultPage(journey: Journey)(implicit request: Request[_]): Result = {
    val defaultEndpoint = journey match {
      case _: Journey.Stages.Started                     => Redirect(routes.LandingController.landingPage())
      case _: Journey.Stages.ComputedTaxId               => Redirect(routes.DetermineEligibilityController.determineEligibility())
      case j: Journey.Stages.EligibilityChecked          => EligibilityRouter.nextPage(j.eligibilityCheckResult)
      case _: Journey.Stages.AnsweredCanPayUpfront       => Redirect(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment())
      case _: Journey.Stages.EnteredUpfrontPaymentAmount => Errors.throwBadRequestException("Page not built yet")
      case _: Journey.Stages.EnteredDayOfMonth           => Errors.throwBadRequestException("Page not built yet")
      case _: Journey.Stages.EnteredInstalmentAmount     => Errors.throwBadRequestException("Page not built yet")
      case _: Journey.Stages.HasSelectedPlan             => Errors.throwBadRequestException("Page not built yet")
    }

    JourneyLogger.error(
      "Journey in incorrect state. " +
        "Please investigate why. " +
        s"Sending user to the first page which supports that journey state ${defaultEndpoint}"
    )
    defaultEndpoint
  }
}
