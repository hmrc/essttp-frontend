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
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.model.Journey
import essttp.rootmodel.IsEmailAddressRequired
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TermsAndConditionsController @Inject() (
    appConfig:      AppConfig,
    as:             Actions,
    views:          Views,
    mcc:            MessagesControllerComponents,
    journeyService: JourneyService
)(
    implicit
    executionContext: ExecutionContext
) extends FrontendController(mcc) with Logging {

  val termsAndConditions: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeConfirmedDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterConfirmedDirectDebitDetails  => finalStateCheck(j, Ok(views.termsAndConditions(j.taxRegime)))
    }
  }

  val termsAndConditionsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeConfirmedDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j)
      case _: Journey.AfterConfirmedDirectDebitDetails =>
        val isEmailAddressRequired = appConfig.emailJourneyEnabled

        journeyService
          .updateAgreedTermsAndConditions(request.journeyId, IsEmailAddressRequired(isEmailAddressRequired))
          .map { updatedJourney => Redirect(Routing.next(updatedJourney)) }
    }
  }

}
