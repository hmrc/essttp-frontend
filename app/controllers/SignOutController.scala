/*
 * Copyright 2023 HM Revenue & Customs
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

import actions.Actions
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import essttp.rootmodel.TaxRegime
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

@Singleton
class SignOutController @Inject() (
  as:        Actions,
  mcc:       MessagesControllerComponents,
  views:     Views,
  appConfig: AppConfig
) extends FrontendController(mcc),
      I18nSupport,
      Logging {

  def signOutFromTimeout: Action[AnyContent] = Action { implicit request =>
    // N.B. the implicit request being passed into the page here may still have the auth
    // token in it so take care to ensure that the sign out link is not shown by mistake
    Ok(views.timedOutPage()).withNewSession
  }

  def signOut: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Redirect {
      request.journey.taxRegime match {
        case TaxRegime.Epaye => routes.SignOutController.exitSurveyPaye
        case TaxRegime.Vat   => routes.SignOutController.exitSurveyVat
        case TaxRegime.Sa    => routes.SignOutController.exitSurveySa
        case TaxRegime.Simp  => routes.SignOutController.exitSurveySimp
      }
    }.withNewSession
  }

  val exitSurveyPaye: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.ExitSurvey.payeExitSurveyUrl).withNewSession
  }

  val exitSurveyVat: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.ExitSurvey.vatExitSurveyUrl).withNewSession
  }

  val exitSurveySa: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.ExitSurvey.saExitSurveyUrl).withNewSession
  }

  val exitSurveySimp: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.ExitSurvey.simpExitSurveyUrl).withNewSession
  }

}
