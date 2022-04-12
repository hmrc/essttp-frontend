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
import essttp.rootmodel.{ Aor, TaxRegime }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import services.{ EligibilityDataService, JourneyService }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.{ EPayeLandingPage, EPayeStartPage }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class EPayeStartController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  eligibilityDataService: EligibilityDataService,
  ePayeLandingPage: EPayeLandingPage,
  ePayeStartPage: EPayeStartPage)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val ePayeLanding: Action[AnyContent] = as.default { implicit request =>
    Ok(ePayeLandingPage())
  }

  val ePayeLandingSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.EPayeStartController.ePayeStart())
  }

  val ePayeStart: Action[AnyContent] = as.default.async { implicit request =>
    request.session.data.get("JourneyId") match {
      case Some(_: String) => for {
        data <- eligibilityDataService.data("AOR", TaxRegime.Epaye, Aor("123AAAABBBBCC"), false)
      } yield Ok(ePayeStartPage(data, Option(controllers.routes.JourneyCompletionController.abort)))
      case _ => throw new IllegalStateException("missing journey")
    }
  }

  val ePayeStartSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.UpfrontPaymentController.upfrontPayment())
  }

}
