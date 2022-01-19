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
import controllers.PaymentDayController.paymentDayForm

import play.api.data.Forms.{ mapping, nonEmptyText }

import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
import views.html.PaymentDay

@Singleton
class PaymentDayController @Inject() (
  as: Actions,
  paymentDayPage: PaymentDay,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val paymentDay: Action[AnyContent] = as.default { implicit request =>
    Ok(paymentDayPage(paymentDayForm()))
  }

  val paymentDaySubmit: Action[AnyContent] = as.default { implicit request =>
    paymentDayForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(paymentDayPage(formWithErrors)),
        _ => Ok("this is as far as we go for now..."))
  }
}

object PaymentDayController {
  import play.api.data.{ Form, Mapping }
  import play.api.data.validation.{ Constraint, Invalid, Valid }
  import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

  case class PaymentDayForm(
    paymentDay: String,
    differentDay: Option[Int])

  def paymentDayForm(): Form[PaymentDayForm] = Form(
    mapping(
      "PaymentDay" -> nonEmptyText,
      "DifferentDay" -> mandatoryIfEqual("PaymentDay", "other", differentDayMapping))(PaymentDayForm.apply)(PaymentDayForm.unapply))

  val differentDayMapping: Mapping[Int] = nonEmptyText
    .transform[Int](
      day => day.toInt,
      _.toString)
    .verifying(
      Constraint[Int]((day: Int) =>
        if (day < 1 || day > 28) {
          Invalid("error.outOfRange")
        } else {
          Valid
        }))
}