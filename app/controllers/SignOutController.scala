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

import actions.Actions
import com.google.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import config.AppConfig
import essttp.rootmodel.TaxRegime
import models.enumsforforms.GiveFeedbackFormValue
import models.forms.GiveFeedbackForm
import requests.RequestSupport

@Singleton
class SignOutController @Inject() (
    as:                          Actions,
    mcc:                         MessagesControllerComponents,
    requestSupport:              RequestSupport,
    timedOutPage:                views.html.TimedOut,
    doYouWantToGiveFeedbackPage: views.html.DoYouWantToGiveFeedback,
    appConfig:                   AppConfig
) extends FrontendController(mcc)
  with I18nSupport
  with Logging {

  import requestSupport._

  def signOutFromTimeout: Action[AnyContent] = Action { implicit request =>
    // N.B. the implicit request being passed into the page here may still have the auth
    // token in it so take care to ensure that the sign out link is not shown by mistake
    Ok(timedOutPage()).withNewSession
  }

  def signOut: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Redirect(routes.SignOutController.doYouWantToGiveFeedback)
      .withSession(SignOutController.feedbackRegimeKey -> request.journey.taxRegime.entryName)
  }

  def doYouWantToGiveFeedback: Action[AnyContent] = Action { implicit request =>
    Ok(doYouWantToGiveFeedbackPage(GiveFeedbackForm.form))
  }

  def doYouWantToGiveFeedbackSubmit: Action[AnyContent] = Action { implicit request =>
    GiveFeedbackForm.form.bindFromRequest()
      .fold(
        formWithErrors => Ok(doYouWantToGiveFeedbackPage(formWithErrors)), {
          case GiveFeedbackFormValue.Yes =>
            val taxRegimeString = request.session.get(SignOutController.feedbackRegimeKey).getOrElse(
              sys.error("Could not find tax regime in cookie session")
            )
            TaxRegime.withNameInsensitive(taxRegimeString) match {
              case TaxRegime.Epaye => Redirect(routes.SignOutController.exitSurveyPaye).withNewSession
              case TaxRegime.Vat   => sys.error("Sign out survey not implemented for VAT yet")
            }

          case GiveFeedbackFormValue.No =>
            Redirect(appConfig.Urls.govUkUrl).withNewSession
        }
      )
  }

  val exitSurveyPaye: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.ExitSurvey.payeExitSurveyUrl).withNewSession
  }
}

object SignOutController {

  val feedbackRegimeKey: String = "essttpTaxRegime"

}
