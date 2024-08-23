/*
 * Copyright 2024 HM Revenue & Customs
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
import essttp.rootmodel.TaxRegime
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PegaService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PegaController @Inject() (
    as:          Actions,
    mcc:         MessagesControllerComponents,
    pegaService: PegaService,
    appConfig:   AppConfig
)(implicit ex: ExecutionContext) extends FrontendController(mcc) {

  val startPegaJourney: Action[AnyContent] = as.authenticatedJourneyAction.async{ implicit request =>
    pegaService.startCase(request.journey).map{ _ =>
      SeeOther(appConfig.pegaRedirectUrl(request.journey.taxRegime))
    }
  }

  def callback(regime: TaxRegime): Action[AnyContent] = as.continueToSameEndpointAuthenticatedJourneyAction.async{ implicit request =>
    pegaService.getCase(request.journey).map{ _ =>
      Routing.redirectToNext(
        routes.PegaController.callback(regime),
        request.journey,
        submittedValueUnchanged = true
      )
    }
  }

}
