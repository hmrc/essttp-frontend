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
import essttp.journey.JourneyConnector
import essttp.journey.model.SjResponse
import models.{ TaxOrigin, TaxRegime }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Request }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

abstract class TaxOriginController[T <: TaxRegime] @Inject() (
  mcc: MessagesControllerComponents,
  jc: JourneyConnector, as: Actions)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging {

  def originator: TaxOrigin { type R = T }

  def createJourney(connector: JourneyConnector)(implicit hc: HeaderCarrier, request: Request[_]): Future[SjResponse] = originator.createJourney(connector)

  def landingPage: Action[AnyContent]

  def start: Action[AnyContent] = as.verifyRole(originator).async { implicit request =>
    for {
      response <- createJourney(jc)
    } yield Redirect(originator.journeyEntryPoint).withSession("JourneyId" -> response.journeyId.value)
  }

}
