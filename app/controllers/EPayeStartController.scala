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
import essttp.rootmodel.TaxRegime
import models.{ EligibilityData, ttp }
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import services.{ EligibilityDataService, JourneyService }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.{ EPayeLandingPage, EPayeStartPage }
import views.html.ErrorTemplate

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class EPayeStartController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  eligibilityDataService: EligibilityDataService,
  errorTemplate: ErrorTemplate,
  ePayeLandingPage: EPayeLandingPage,
  ePayeStartPage: EPayeStartPage)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging with I18nSupport {

  val ePayeLanding: Action[AnyContent] = as.default { implicit request =>
    Ok(ePayeLandingPage())
  }

  val ePayeLandingSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.EPayeStartController.ePayeStart())
  }

  val ePayeStart: Action[AnyContent] = as.default.async { implicit request =>
    request.session.data.get("JourneyId") match {
      case Some(_: String) => for {
        data <- eligibilityDataService.data("AOR", TaxRegime.Epaye, ttp.DefaultTaxId, true)
      } yield routeResponse(data)
      case _ => throw new IllegalStateException("missing journey")
    }
  }

  val ePayeStartSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.UpfrontPaymentController.upfrontPayment())
  }

  def routeResponse(data: EligibilityData)(implicit R: Request[_]): Result = {
    if (data.hasRejections) {
      val errorMessage = data.rejections.map(_.entryName).mkString(", ")
      Ok(errorTemplate("Error Page", "TTP Rejections", errorMessage)(implicitly[Request[_]], R.messages))
    } else {
      Ok(ePayeStartPage(data.overduePayments, Option(controllers.routes.JourneyCompletionController.abort)))
    }

  }

}
