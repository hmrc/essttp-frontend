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
import controllers.UpfrontPaymentController._
import models.MoneyUtil.amountOfMoneyFormatter
import moveittocor.corcommon.model.AmountInPence
import play.api.data.{ Form, Forms }
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.{ UpfrontPayment, UpfrontPaymentAmount, UpfrontSummary }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class UpfrontPaymentController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  upfrontPaymentPage: UpfrontPayment,
  upfrontPaymentAmountPage: UpfrontPaymentAmount,
  upfrontSummaryPage: UpfrontSummary)(implicit ec: ExecutionContext)
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
          case _ => Redirect(routes.MonthlyPaymentAmountController.monthlyPaymentAmount())
        })

  }

  val upfrontPaymentAmount: Action[AnyContent] = as.default { implicit request =>
    val form: Form[BigDecimal] = answers.upfrontAmount match {
      case Some(a: AmountInPence) => upfrontPaymentAmountForm().fill(a.inPounds)
      case None => upfrontPaymentAmountForm()
    }
    Ok(upfrontPaymentAmountPage(form, maximumPaymentAmount, minimumPaymentAmount))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.default { implicit request =>
    upfrontPaymentAmountForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Ok(upfrontPaymentAmountPage(formWithErrors, maximumPaymentAmount, minimumPaymentAmount)),
        (s: BigDecimal) => {
          answers = FakeSession(
            originalDebt = AmountInPence(originalDebt),
            upfrontAmount = Some(AmountInPence((s * 100).longValue())))
          Redirect(routes.UpfrontPaymentController.upfrontSummary())
        })
  }

  val upfrontSummary: Action[AnyContent] = as.default { implicit request =>
    val remaining: AmountInPence = getRemainingBalance
    Ok(upfrontSummaryPage(answers, remaining))
  }
}

object UpfrontPaymentController {
  // this value to come from session data/api data from ETMP...
  val originalDebt: Long = 175050L
  val maximumPaymentAmount: AmountInPence = AmountInPence(originalDebt)
  val minimumPaymentAmount: AmountInPence = AmountInPence(100)
  val key: String = "UpfrontPaymentAmount"

  // temp fake session that should not live in here!
  var answers: FakeSession = FakeSession(originalDebt = AmountInPence(originalDebt), upfrontAmount = None)

  case class FakeSession(
    originalDebt: AmountInPence,
    upfrontAmount: Option[AmountInPence])

  def getRemainingBalance: AmountInPence = {
    answers.upfrontAmount match {
      case Some(s: AmountInPence) => AmountInPence(originalDebt - s.value)
      case _ => AmountInPence(originalDebt)
    }
  }

  def upfrontPaymentForm(): Form[String] = Form(
    mapping(
      "UpfrontPayment" -> nonEmptyText)(identity)(Some(_)))

  // this should be AmountInPence and validate using business rules about
  // min and max amount
  def upfrontPaymentAmountForm(): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(minimumPaymentAmount.inPounds > _, maximumPaymentAmount.inPounds < _)))(identity)(Some(_)))
}