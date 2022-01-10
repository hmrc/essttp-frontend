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
import moveittocor.corcommon.model.AmountInPence
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Request }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.EPayeStartPage

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EPayeStartController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  ePayeStartPage: EPayeStartPage)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val ePayeStart: Action[AnyContent] = as.default { implicit request =>

    val amountInPence = AmountInPence(175050)
    Ok(ePayeStartPage(amountInPence))
  }

  val ePayeStartSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.UpfrontPaymentController.upfrontPayment())
  }

}