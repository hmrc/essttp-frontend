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
import controllers.PaymentDayController.{ PaymentDayForm, paymentDayForm }
import models.Journey
import play.api.data.{ Form, FormError, Forms }
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.data.format.Formatter
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import views.html.PaymentDay

import scala.util.Try

@Singleton
class PaymentDayController @Inject() (
  as: Actions,
  paymentDayPage: PaymentDay,
  journeyService: JourneyService,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val paymentDay: Action[AnyContent] = as.getJourney.async { implicit request =>
    val form: Form[PaymentDayForm] = request.journey.userAnswers.paymentDay match {
      case Some(a: String) => paymentDayForm().fill(PaymentDayForm(a, request.journey.userAnswers.differentDay))
      case _ => paymentDayForm()
    }
    Future.successful(Ok(paymentDayPage(form)))
  }

  val paymentDaySubmit: Action[AnyContent] = as.getJourney.async { implicit request =>
    val j: Journey = request.journey
    paymentDayForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(paymentDayPage(formWithErrors))),
        (p: PaymentDayForm) => {
          journeyService.upsert(j.copy(userAnswers = j.userAnswers.copy(
            differentDay = p.differentDay,
            paymentDay = Some(p.paymentDay))))
          Future.successful(Redirect(routes.InstalmentsController.instalmentOptions()))
        })

  }
}

object PaymentDayController {
  import play.api.data.Form
  import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual
  import cats.syntax.either._

  case class PaymentDayForm(
    paymentDay: String,
    differentDay: Option[Int])

  def paymentDayForm(): Form[PaymentDayForm] = Form(
    mapping(
      "PaymentDay" -> nonEmptyText,
      "DifferentDay" -> mandatoryIfEqual("PaymentDay", "other", Forms.of(dayOfMonthFormatter)))(PaymentDayForm.apply)(PaymentDayForm.unapply))

  def readValue[T](
    key: String,
    data: Map[String, String],
    f: String => T): Either[FormError, T] =
    data
      .get(key)
      .map(_.trim())
      .filter(_.nonEmpty)
      .fold[Either[FormError, T]](Left(FormError(key, "error.required"))) { stringValue =>
        Either
          .fromTry(Try(f(stringValue)))
          .leftMap(_ => FormError(key, "error.invalid"))
      }

  val dayOfMonthFormatter: Formatter[Int] = {
    val key = "DifferentDay"
    def validateDayOfMonth(day: Int): Either[FormError, Int] =
      if (day < 1 || day > 28) Left(FormError(key, "error.outOfRange"))
      else Right(day)

    new Formatter[Int] {
      override def bind(
        key: String,
        data: Map[String, String]): Either[Seq[FormError], Int] = {
        val result =
          readValue(key, data, _.toInt)
            .flatMap(validateDayOfMonth)
        result.leftMap(Seq(_))
      }
      override def unbind(key: String, value: Int): Map[String, String] =
        Map(key -> value.toString)
    }
  }
}
