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
import moveittocor.corcommon.model.AmountInPence
import play.api.data.{ Form, FormError, Forms }
import play.api.data.Forms.mapping
import play.api.data.format.Formatter
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.MonthlyPaymentAmount
import cats.syntax.either._
import cats.syntax.eq._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class MonthlyPaymentAmountController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  monthlyPaymentAmountPage: MonthlyPaymentAmount)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val monthlyPaymentAmount: Action[AnyContent] = as.default { implicit request =>
    Ok(monthlyPaymentAmountPage(monthlyPaymentAmountForm(), maximumPaymentAmount, minimumPaymentAmount))
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
              formWithErrors, maximumPaymentAmount, minimumPaymentAmount)),
        _ => Redirect(routes.UpfrontPaymentController.upfrontPaymentAmount()))
  }
}

object MonthlyPaymentAmountController {
  // this value to come from session data/api data from ETMP...
  val originalDebt: Long = 175050L
  val maximumPaymentAmount: AmountInPence = AmountInPence(originalDebt)
  val minimumPaymentAmount: AmountInPence = AmountInPence(originalDebt / 6)
  val key: String = "MonthlyPaymentAmount"
  private def cleanupAmountOfMoneyString(s: String): String = {
    s.trim().filter(c => c =!= ',' && c =!= '£')
  }
  def formatAmountOfMoneyWithoutPoundSign(d: BigDecimal): String =
    d.toString().replaceAllLiterally("£", "")
  def monthlyPaymentAmountFormatter(
    isTooSmall: BigDecimal => Boolean,
    isTooLarge: BigDecimal => Boolean): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      override def bind(
        key: String,
        data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        validateAmountOfMoney(
          key,
          isTooSmall,
          isTooLarge)(data(key)).leftMap(Seq(_))

      def validateAmountOfMoney(
        key: String,
        isTooSmall: BigDecimal => Boolean,
        isTooLarge: BigDecimal => Boolean)(
        s: String): Either[FormError, BigDecimal] =
        Try(BigDecimal(cleanupAmountOfMoneyString(s))).toEither
          .leftMap(_ => FormError(key, "error.pattern"))
          .flatMap { d: BigDecimal =>
            if (isTooSmall(d)) {
              Left(FormError(key, "error.tooSmall"))
            } else if (isTooLarge(d)) {
              Left(FormError(key, "error.tooLarge"))
            } else {
              Right(d)
            }
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        Map(key -> formatAmountOfMoneyWithoutPoundSign(value))
    }

  def monthlyPaymentAmountForm(): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(monthlyPaymentAmountFormatter(minimumPaymentAmount.inPounds > _, maximumPaymentAmount.inPounds < _))
    )(identity)(Some(_)))
}