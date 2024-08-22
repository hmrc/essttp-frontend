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
import config.AppConfig
import essttp.enrolments.EnrolmentDef
import essttp.rootmodel.TaxRegime
import models.forms.TaxRegimeForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import requests.RequestSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}

@Singleton
class WhichTaxRegimeController @Inject() (
    appConfig:      AppConfig,
    as:             Actions,
    mcc:            MessagesControllerComponents,
    requestSupport: RequestSupport,
    views:          Views
)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val whichTaxRegime: Action[AnyContent] =
    as.authenticatedAction { implicit request =>
      val hasEpayeEnrolment = EnrolmentDef.Epaye.findEnrolmentValues(request.enrolments).isSuccess
      val hasVatEnrolment = EnrolmentDef.Vat.findEnrolmentValues(request.enrolments).isSuccess
      val hasSaEnrolment = EnrolmentDef.Sa.findEnrolmentValues(request.enrolments).isSuccess

      (hasEpayeEnrolment, hasVatEnrolment, hasSaEnrolment) match {
        case (true, false, false)                        => Redirect(routes.LandingController.epayeLandingPage)
        case (false, true, false)                        => Redirect(routes.LandingController.vatLandingPage)
        case (false, false, true) if appConfig.saEnabled => Redirect(routes.LandingController.saLandingPage)
        case _ =>
          Ok(views.whichTaxRegime(TaxRegimeForm.form, appConfig.saEnabled))
      }
    }

  val whichTaxRegimeSubmit: Action[AnyContent] =
    as.authenticatedAction { implicit request =>
      TaxRegimeForm.form.bindFromRequest()
        .fold(
          formWithErrors => Ok(views.whichTaxRegime(formWithErrors, appConfig.saEnabled)),
          {
            case TaxRegime.Epaye => Redirect(routes.StartJourneyController.startDetachedEpayeJourney)
            case TaxRegime.Vat   => Redirect(routes.StartJourneyController.startDetachedVatJourney)
            case TaxRegime.Sa    => Redirect(routes.StartJourneyController.startDetachedSaJourney)
          }
        )
    }

}
