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
import controllers.MonthlyPaymentAmountController._
import controllers.UpfrontPaymentController.getRemainingBalance
import models.MoneyUtil._
import moveittocor.corcommon.model.AmountInPence
import play.api.data.{ Form, Forms }
import play.api.data.Forms.mapping
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.MonthlyPaymentAmount

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
@Singleton
class MonthlyPaymentAmountController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  monthlyPaymentAmountPage: MonthlyPaymentAmount)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val monthlyPaymentAmount: Action[AnyContent] = as.default { implicit request =>
    Ok(monthlyPaymentAmountPage(monthlyPaymentAmountForm(), maximumMonthlyPaymentAmount, minimumMonthlyPaymentAmount))
  }

  val monthlyPaymentAmountSubmit: Action[AnyContent] = as.default { implicit request =>
    // this is an example to test using play forms and errors
    // normally answers would be uplifted to session storage instead of just
    // redirecting to next page..
    monthlyPaymentAmountForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Ok(
            monthlyPaymentAmountPage(
              formWithErrors, maximumMonthlyPaymentAmount, minimumMonthlyPaymentAmount)),
        _ => Redirect(routes.PaymentDayController.paymentDay()))
  }
}

object MonthlyPaymentAmountController {
  // this value to come from session data/api data from ETMP...
  val remaining: AmountInPence = getRemainingBalance
  val maximumMonthlyPaymentAmount: AmountInPence = remaining
  val minimumMonthlyPaymentAmount: AmountInPence = AmountInPence(remaining.value / 6)
  val key: String = "MonthlyPaymentAmount"

  def monthlyPaymentAmountForm(): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(minimumMonthlyPaymentAmount.inPounds > _, maximumMonthlyPaymentAmount.inPounds < _)))(identity)(Some(_)))
}
