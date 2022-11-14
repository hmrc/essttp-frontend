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
import config.AppConfig
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}

@Singleton
class LandingController @Inject() (
    as:        Actions,
    mcc:       MessagesControllerComponents,
    views:     Views,
    appConfig: AppConfig
) extends FrontendController(mcc)
  with Logging {

  val epayeLandingPage: Action[AnyContent] = as.default { implicit request =>
    Ok(views.epayeLanding())
  }

  val vatLandingPage: Action[AnyContent] =
    if (appConfig.vatEnabled) {
      as.default { implicit request =>
        Ok(views.vatLanding())
      }
    } else {
      as.default(_ => NotImplemented)
    }

  val epayeLandingPageContinue: Action[AnyContent] = as.default { implicit request =>
    redirectWithHasSeenLandingPage(routes.StartJourneyController.startDetachedEpayeJourney)
  }

  val vatLandingPageContinue: Action[AnyContent] = as.default { implicit request =>
    redirectWithHasSeenLandingPage(routes.StartJourneyController.startDetachedVatJourney)
  }

  private def redirectWithHasSeenLandingPage(redirectTo: Call)(implicit r: Request[_]): Result =
    Redirect(redirectTo).withSession(
      r.session + (LandingController.hasSeenLandingPageSessionKey -> "true")
    )

}

object LandingController {

  val hasSeenLandingPageSessionKey: String = "ESSTTP_HAS_SEEN_LANDING"

}
