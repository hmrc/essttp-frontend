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

import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import testOnly.controllers.TestOnlyController.AuthRequest
import testOnly.models.Enrolment
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future

@Singleton()
class GovUkController @Inject() (mcc: MessagesControllerComponents) extends FrontendController(mcc) with Logging {

  def payeLandingPage = Action { implicit request =>
    Ok("gov uk paye landing page")
  }

  def beginPaye: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[AuthRequest] { auth =>
      Future.successful(Redirect(controllers.routes.EPayeStartController.ePayeStart()))
    }
  }

  def vatLandingPage = Action { implicit request =>
    Ok("gov uk vat landing page")
  }

  def beginVat = Action {
    Redirect(controllers.routes.LandingController.landingPage())
  }

}
