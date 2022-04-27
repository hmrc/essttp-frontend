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
import essttp.rootmodel.TaxRegime
import models.TaxOrigin.EpayeBTA
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.EPaye.EPayeLandingPage2

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class EpayeBTAController @Inject() (cc: MessagesControllerComponents, epayeLandingPage: EPayeLandingPage2,
                                    jc: JourneyConnector, as: Actions)(implicit ec: ExecutionContext)
  extends TaxOriginController[TaxRegime.Epaye.type](cc, jc, as) {

  val originator = EpayeBTA

  override def landingPage: Action[AnyContent] = Action { implicit request =>
    Ok(epayeLandingPage(controllers.routes.EpayeBTAController.start, Option(abortCall)))
  }

}
