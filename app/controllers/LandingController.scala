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
import essttp.journey.JourneyConnector
import essttp.rootmodel.{BackUrl, TaxRegime}
import play.api.mvc._
import requests.RequestSupport
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
    checkNotShuttered(TaxRegime.Epaye) {
      getBackUrl().map { maybeBackUrl =>
        Ok(views.epayeLanding(maybeBackUrl))
      }
    }
  }

  val vatLandingPage: Action[AnyContent] =
    as.default.async { implicit request =>
      checkNotShuttered(TaxRegime.Vat) {
        getBackUrl().map { maybeBackUrl =>
          Ok(views.vatLanding(maybeBackUrl))
        }
      }
    }

  val saLandingPage: Action[AnyContent] =
    if (appConfig.saEnabled) {
      as.default.async { implicit request =>
        checkNotShuttered(TaxRegime.Sa) {
          getBackUrl().map { maybeBackUrl =>
            Ok(views.saLanding(maybeBackUrl))
          }
        }
      }
    } else {
      as.default(_ => Redirect(appConfig.Urls.saSuppUrl))
    }
  // OPS-12345 sia landing needs to be a def, otherwise tests where it is set to false go to the exception the moment the controller is injected
  def siaLandingPage: Action[AnyContent] = {
    if (appConfig.siaEnabled) {
      as.default.async { implicit request =>
        checkNotShuttered(TaxRegime.Sia) {
          getBackUrl().map { maybeBackUrl =>
            Ok(views.siaLanding(maybeBackUrl))
          }
        }
      }
    } else {
      throw new RuntimeException("Simple Assessment is not available")
    }
  }

  val epayeLandingPageContinue: Action[AnyContent] = as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
    checkNotShuttered(TaxRegime.Epaye) {
      handleLandingPageContinue(routes.StartJourneyController.startDetachedEpayeJourney)
    }
  }

  val vatLandingPageContinue: Action[AnyContent] = as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
    checkNotShuttered(TaxRegime.Vat) {
      handleLandingPageContinue(routes.StartJourneyController.startDetachedVatJourney)
    }
  }

  val saLandingPageContinue: Action[AnyContent] = as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
    checkNotShuttered(TaxRegime.Sa) {
      handleLandingPageContinue(routes.StartJourneyController.startDetachedSaJourney)
    }
  }

  val siaLandingPageContinue: Action[AnyContent] = as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
    checkNotShuttered(TaxRegime.Sia) {
      handleLandingPageContinue(routes.StartJourneyController.startDetachedSiaJourney)
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

  private def getBackUrl()(implicit request: Request[AnyContent]): Future[Option[BackUrl]] =
    if (RequestSupport.isLoggedIn) {
      journeyConnector.findLatestJourneyBySessionId()
        .map(maybeJourney => maybeJourney.flatMap(_.backUrl))
    } else {
      Future.successful[Option[BackUrl]](None)
    }
}

object LandingController {

  val hasSeenLandingPageSessionKey: String = "ESSTTP_HAS_SEEN_LANDING"

}
