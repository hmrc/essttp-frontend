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
import essttp.crypto.CryptoFormat
import essttp.journey.JourneyConnector
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DebugJourneyController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  implicit val cryptoFormat: CryptoFormat = CryptoFormat.NoOpCryptoFormat

  val showJourney: Action[AnyContent] = as.default.async { implicit request =>
    if (hc.sessionId.isEmpty) {
      Future.successful(Ok("Missing session id"))
    } else {
      journeyConnector.findLatestJourneyBySessionId().map {
        case Some(j) => Ok(Json.prettyPrint(Json.toJson(j)))
        case None    => NotFound(s"There is no journey with such sessionId ${hc.sessionId.toString}")
      }
    }

  }
}
