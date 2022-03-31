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
import essttp.journey.JourneyConnector
import essttp.rootmodel.{ BackUrl, ReturnUrl }
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, MessagesControllerComponents }
import testOnly.controllers.TestOnlyController.AuthRequest
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.EPayeLandingPage2

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton()
class BTAController @Inject() (mcc: MessagesControllerComponents, epayeLandingPage: EPayeLandingPage2,
  jc: JourneyConnector, as: Actions)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging {

  def payeLandingPage = Action { implicit request =>
    Ok(epayeLandingPage(controllers.routes.BTAController.startPaye))
  }

  def startPaye = as.auth.async { implicit request =>
    for {
      response <- jc.Epaye.startJourneyBta(
        essttp.journey.model.SjRequest.Epaye.Simple(ReturnUrl("http://localhost:9125/return"), BackUrl("http://localhost:9125/back")))
    } yield Redirect(routes.EPayeStartController.ePayeStart()).withSession("JourneyId" -> response.journeyId.value)
  }

  def vatLandingPage = Action { implicit request =>
    Ok("bta vat landing page")
  }

  def beginVat: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[AuthRequest] { auth =>
      Future.successful(Redirect(controllers.routes.LandingController.landingPage()))
    }

  }

}