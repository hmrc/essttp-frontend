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
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.SjRequest
import play.api.mvc._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class GovUkController @Inject() (
    cc:               MessagesControllerComponents,
    journeyConnector: JourneyConnector,
    as:               Actions,
    appConfig:        AppConfig
)(implicit ec: ExecutionContext) extends FrontendController(cc) {

  def startEpayeJourney: Action[AnyContent] = as.authenticatedAction.async { implicit request =>
    val startJourneyRedirect: Future[Result] = {
      // gov uk needs to skip landing page, we don't want to show guidance again.
      if (isComingFromGovUk(request)) {
        journeyConnector.Epaye.startJourneyGovUk(SjRequest.Epaye.Empty())
          .map(_ => Redirect(routes.DetermineTaxIdController.determineTaxId.url))
      } else {
        journeyConnector.Epaye.startJourneyDetachedUrl(SjRequest.Epaye.Empty())
          .map(r => Redirect(r.nextUrl.value))
      }
    }
    startJourneyRedirect
  }

  def startVatJourney: Action[AnyContent] =
    if (appConfig.vatEnabled) {
      as.authenticatedAction.async { implicit request =>
        val startJourneyRedirect: Future[Result] = {
          // gov uk needs to skip landing page, we don't want to show guidance again.
          if (isComingFromGovUk(request)) {
            journeyConnector.Vat.startJourneyGovUk(SjRequest.Vat.Empty())
              .map(_ => Redirect(routes.DetermineTaxIdController.determineTaxId.url))
          } else {
            journeyConnector.Vat.startJourneyDetachedUrl(SjRequest.Vat.Empty())
              .map(r => Redirect(r.nextUrl.value))
          }
        }
        startJourneyRedirect
      }
    } else {
      as.default(_ => NotImplemented)
    }

  /**
   * Based on the "Referrer" http header it determines
   * if the incoming request originated on the gov-uk pages.
   */
  def isComingFromGovUk(request: Request[_]): Boolean =
    request.headers.get(HeaderNames.REFERER).exists(_.contains(appConfig.refererForGovUk))

}
