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
import config.AppConfig
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import essttp.journey.model.Journey
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.Inject

class EmailController @Inject() (
    as:        Actions,
    appConfig: AppConfig,
    mcc:       MessagesControllerComponents
) extends FrontendController(mcc) with Logging {

  val dummy: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeAgreedTermsAndConditions =>
        logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterAgreedTermsAndConditions =>
        if (!j.isEmailAddressRequired) logErrorAndRouteToDefaultPage(j) else Ok("dummy email page")

    }
  }

}
