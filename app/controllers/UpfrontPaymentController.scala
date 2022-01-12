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
import controllers.UpfrontPaymentController.{ upfrontPaymentAmountForm, upfrontPaymentForm }
import moveittocor.corcommon.model.AmountInPence
import play.api.data.Form
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Result }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.{ UpfrontPayment, UpfrontPaymentAmount }

import java.util.Locale
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class UpfrontPaymentController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  upfrontPaymentPage: UpfrontPayment,
  upfrontPaymentAmountPage: UpfrontPaymentAmount)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val upfrontPayment: Action[AnyContent] = as.default { implicit request =>
    Ok(upfrontPaymentPage(upfrontPaymentForm()))
  }

  val upfrontPaymentSubmit: Action[AnyContent] = as.default { implicit request =>
    // this is an example to test using play forms and errors
    // normally answers would be uplifted to session storage instead of just
    // redirecting to next page..
    upfrontPaymentForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Ok(
            upfrontPaymentPage(
              formWithErrors)),
        {
          case "Yes" => Redirect(routes.UpfrontPaymentController.upfrontPaymentAmount())
          case _ => Redirect(routes.UpfrontPaymentController.upfrontPayment())
        })

  }

  val upfrontPaymentAmount: Action[AnyContent] = as.default { implicit request =>
    Ok(upfrontPaymentAmountPage(upfrontPaymentAmountForm()))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.default { implicit request =>
    upfrontPaymentAmountForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Ok(upfrontPaymentAmountPage(formWithErrors)),
        _ => Redirect(routes.UpfrontPaymentController.upfrontPayment()))
  }
}

object UpfrontPaymentController {
  def upfrontPaymentForm(): Form[String] = Form(
    mapping(
      "UpfrontPayment" -> nonEmptyText)(identity)(Some(_)))

  // this should be AmountInPence and validate using business rules about
  // min and max amount
  def upfrontPaymentAmountForm(): Form[String] = Form(
    mapping(
      "UpfrontPaymentAmount" -> nonEmptyText)(identity)(Some(_)))
}