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

package testOnly.controllers

import _root_.actions.Actions
import _root_.essttp.journey.model.ttp._
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.{Origins, SjRequest}
import essttp.rootmodel.{BackUrl, ReturnUrl}
import models.EligibilityErrors._
import models.{EligibilityError, EligibilityErrors}
import play.api.libs.json.Json
import play.api.mvc._
import testOnly.AuthLoginApiService
import testOnly.connectors.EssttpStubConnector
import testOnly.controllers.StartJourneyController._
import testOnly.formsmodel.StartJourneyForm
import testOnly.testusermodel.TestUser
import testOnly.views.html.TestOnlyStartPage
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DebugJourneyController @Inject() (
    as:                  Actions,
    appConfig:           AppConfig,
    essttpStubConnector: EssttpStubConnector,
    mcc:                 MessagesControllerComponents,
    testOnlyStartPage:   TestOnlyStartPage,
    journeyConnector:    JourneyConnector,
    loginService:        AuthLoginApiService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val showJourney: Action[AnyContent] = as.default.async { implicit request =>
    if (hc.sessionId.isEmpty) {
      Future.successful(Ok("Missing session id"))
    } else {
      journeyConnector.findLatestJourneyBySessionId().map {
        case Some(j) => Ok(Json.prettyPrint(Json.toJson(j)))
        case None    => NotFound(s"There is no journey with such sessionId ${hc.sessionId}")
      }
    }

  }
}
