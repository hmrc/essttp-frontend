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
import essttp.journey.model.{SjRequest, SjResponse}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class StartJourneyController @Inject() (
    cc:               MessagesControllerComponents,
    journeyConnector: JourneyConnector,
    as:               Actions,
    appConfig:        AppConfig
)(implicit ec: ExecutionContext) extends FrontendController(cc) with Logging {

  def startGovukEpayeJourney: Action[AnyContent] = as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
    journeyConnector.Epaye.startJourneyGovUk(SjRequest.Epaye.Empty())
      .map(_ => Redirect(routes.DetermineTaxIdController.determineTaxId.url))
  }

  def startGovukVatJourney: Action[AnyContent] =
    as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
      journeyConnector.Vat.startJourneyGovUk(SjRequest.Vat.Empty())
        .map(_ => Redirect(routes.DetermineTaxIdController.determineTaxId.url))
    }

  def startGovukSaJourney: Action[AnyContent] =
    if (appConfig.saEnabled) {
      as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
        journeyConnector.Sa.startJourneyGovUk(SjRequest.Sa.Empty())
          .map(_ => Redirect(routes.DetermineTaxIdController.determineTaxId.url))
      }
    } else {
      as.default(_ => Redirect(appConfig.Urls.saSuppUrl))
    }

  def startGovukSimpJourney: Action[AnyContent] =
    if (appConfig.simpEnabled) {
      as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
        journeyConnector.Simp.startJourneyGovUk(SjRequest.Simp.Empty())
          .map(_ => Redirect(routes.DetermineTaxIdController.determineTaxId.url))
      }
    } else {
      throw new RuntimeException("Simple Assessment is not available")
    }

  def startDetachedEpayeJourney: Action[AnyContent] = as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
    journeyConnector.Epaye.startJourneyDetachedUrl(SjRequest.Epaye.Empty())
      .map(redirectFromDetachedJourneyStarted)
  }

  def startDetachedVatJourney: Action[AnyContent] =
    as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
      journeyConnector.Vat.startJourneyDetachedUrl(SjRequest.Vat.Empty())
        .map(redirectFromDetachedJourneyStarted)
    }

  def startDetachedSaJourney: Action[AnyContent] =
    if (appConfig.saEnabled) {
      as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
        journeyConnector.Sa.startJourneyDetachedUrl(SjRequest.Sa.Empty())
          .map(redirectFromDetachedJourneyStarted)
      }
    } else {
      as.default(_ => Redirect(appConfig.Urls.saSuppUrl))
    }

  def startDetachedSimpJourney: Action[AnyContent] =
    if (appConfig.simpEnabled) {
      as.continueToSameEndpointAuthenticatedAction.async { implicit request =>
        journeyConnector.Simp.startJourneyDetachedUrl(SjRequest.Simp.Empty())
          .map(redirectFromDetachedJourneyStarted)
      }
    } else {
      throw new RuntimeException("Simple Assessment is not available")
    }

  private def redirectFromDetachedJourneyStarted(sjResponse: SjResponse)(implicit r: Request[_]): Result = {
    val next = if (r.session.get(LandingController.hasSeenLandingPageSessionKey).isEmpty) {
      sjResponse.nextUrl.value
    } else {
      routes.DetermineTaxIdController.determineTaxId.url
    }

    Redirect(next).withSession(r.session - LandingController.hasSeenLandingPageSessionKey)
  }

}
