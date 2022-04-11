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

import essttp.journey.JourneyConnector
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.EPayeLandingPage2
import _root_.actions.Actions
import essttp.journey.model.Origin.Epaye.GovUk
import models.TaxRegimeFE.EpayeRegime
import models.{ TaxOrigin, TaxRegimeFE }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton()
class EpayeGovUkController @Inject() (
  cc: MessagesControllerComponents,
  jc: JourneyConnector, as: Actions)(implicit ec: ExecutionContext) extends TaxOriginController[EpayeRegime.type](cc, jc, as) {

  val originator = TaxOrigin.EpayeGovUk

  override def landingPage: Action[AnyContent] = start
}
