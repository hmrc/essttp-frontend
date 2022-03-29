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

import essttp.journey.JourneyConnector
import play.api.mvc.MessagesControllerComponents
import testOnly.controllers.TestOnlyController.AuthRequest
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisedFunctions }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.EPayeLandingPage2

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton()
class NoSourceController @Inject() (mcc: MessagesControllerComponents, epayeLandingPage: EPayeLandingPage2,
  jc: JourneyConnector, val authConnector: AuthConnector)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging with AuthorisedFunctions {

  def payeLandingPage = Action { implicit request =>
    Ok(epayeLandingPage(controllers.routes.NoSourceController.startPaye))
  }

  def startPaye = Action.async { implicit request =>
    authorised() {
      val result = for {
        response <- jc.Epaye.startJourneyDetachedUrl(
          essttp.journey.model.SjRequest.Epaye.Empty())
      } yield Redirect(routes.EPayeStartController.ePayeStart())

      result
    }

  }

  def vatLandingPage = Action { implicit request =>
    Ok("no source vat landing page")
  }

  def beginVat = Action.async(parse.json) { implicit request =>
    withJsonBody[AuthRequest] { auth =>
      Future.successful(Redirect(controllers.routes.LandingController.landingPage()))
    }

  }

}