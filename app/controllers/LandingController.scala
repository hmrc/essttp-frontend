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

import _root_.actions.Actions
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.rootmodel.TaxRegime
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LandingController @Inject() (
    as:               Actions,
    journeyConnector: JourneyConnector,
    mcc:              MessagesControllerComponents,
    views:            Views,
    appConfig:        AppConfig
)(implicit ec: ExecutionContext) extends FrontendController(mcc)
  with Logging {

  val epayeLandingPage: Action[AnyContent] = as.default.async { implicit request =>
    checkNotShuttered(TaxRegime.Epaye){
      Future.successful(Ok(views.epayeLanding()))
    }
  }

  val vatLandingPage: Action[AnyContent] =
    if (appConfig.vatEnabled) {
      as.default.async { implicit request =>
        checkNotShuttered(TaxRegime.Vat) {
          Future.successful(Ok(views.vatLanding()))
        }
      }
    } else {
      as.default(_ => NotImplemented)
    }

  val epayeLandingPageContinue: Action[AnyContent] = as.continueToSameEndpointAuthenticatedJourneyAction.async { implicit request =>
    checkNotShuttered(TaxRegime.Epaye) {
      handleLandingPageContinue(routes.StartJourneyController.startDetachedEpayeJourney)
    }
  }

  val vatLandingPageContinue: Action[AnyContent] = as.continueToSameEndpointAuthenticatedJourneyAction.async { implicit request =>
    checkNotShuttered(TaxRegime.Vat) {
      handleLandingPageContinue(routes.StartJourneyController.startDetachedVatJourney)
    }
  }

  private def handleLandingPageContinue(startDetachedJourney: Call)(implicit r: Request[_]): Future[Result] =
    journeyConnector.findLatestJourneyBySessionId().map{
      case None =>
        Redirect(startDetachedJourney).withSession(
          r.session + (LandingController.hasSeenLandingPageSessionKey -> "true")
        )

      case Some(_) =>
        Redirect(routes.DetermineTaxIdController.determineTaxId)
    }

  private def checkNotShuttered(taxRegime: TaxRegime)(f: => Future[Result])(implicit request: Request[_]): Future[Result] =
    if (appConfig.shutteredTaxRegimes.contains(taxRegime)) Future.successful(Ok(views.shuttered())) else f

}

object LandingController {

  val hasSeenLandingPageSessionKey: String = "ESSTTP_HAS_SEEN_LANDING"

}
