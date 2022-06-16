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
import cats.implicits.catsSyntaxEq
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import controllers.PaymentDayController.{PaymentDayForm, paymentDayForm}
import essttp.journey.model.Journey
import essttp.rootmodel.DayOfMonth
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms}
import play.api.mvc._
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class PaymentDayController @Inject() (
    as:             Actions,
    views:          Views,
    journeyService: JourneyService,
    mcc:            MessagesControllerComponents
)(implicit executionContext: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val paymentDay: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredMonthlyPaymentAmount => logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterEnteredMonthlyPaymentAmount  => displayPaymentDayPage(j)
    }
  }

  private def displayPaymentDayPage(journey: Journey.AfterEnteredMonthlyPaymentAmount)(implicit request: Request[_]): Result = {
    val maybePrePopForm: Form[PaymentDayForm] = journey match {
      case j: Journey.AfterEnteredDayOfMonth =>
        if (j.dayOfMonth.value === 28) {
          paymentDayForm().fill(PaymentDayForm("28", None))
        } else {
          paymentDayForm().fill(PaymentDayForm("", Some(j.dayOfMonth.value)))
        }
      case _: Journey.BeforeEnteredDayOfMonth => paymentDayForm()
    }
    Ok(views.paymentDayPage(
      form    = maybePrePopForm,
      backUrl = PaymentDayController.backUrl
    ))
  }

  val paymentDaySubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    paymentDayForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(
          Ok(views.paymentDayPage(form    = formWithErrors, backUrl = PaymentDayController.backUrl))
        ),
        (form: PaymentDayForm) => {
          val dayOfMonth: DayOfMonth = form.differentDay match {
            case Some(otherDay) => DayOfMonth(otherDay)
            case None           => DayOfMonth(form.paymentDay.toInt)
          }
          journeyService.updateDayOfMonth(request.journeyId, dayOfMonth)
            .map(_ => Redirect(routes.InstalmentsController.instalmentOptions()))
        }
      )
  }
}

object PaymentDayController {

  import cats.syntax.either._
  import play.api.data.Form
  import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

  val backUrl: Option[String] = Some(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount().url)

  final case class PaymentDayForm(paymentDay: String, differentDay: Option[Int])

  def paymentDayForm(): Form[PaymentDayForm] = Form(
    mapping(
      "PaymentDay" -> nonEmptyText,
      "DifferentDay" -> mandatoryIfEqual("PaymentDay", "other", Forms.of(dayOfMonthFormatter))
    )(PaymentDayForm.apply)(PaymentDayForm.unapply)
  )

  def readValue[T](
      key:  String,
      data: Map[String, String],
      f:    String => T
  ): Either[FormError, T] =
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
          key:  String,
          data: Map[String, String]
      ): Either[Seq[FormError], Int] = {
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
