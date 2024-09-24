/*
 * Copyright 2023 HM Revenue & Customs
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

import actions.Actions
import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import controllers.PaymentDayController.{PaymentDayForm, paymentDayForm}
import essttp.journey.model.{Journey, PaymentPlanAnswers}
import essttp.rootmodel.DayOfMonth
import essttp.utils.Errors
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
)(implicit executionContext: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc)
  with Logging {

  val paymentDay: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    withJourneyInCorrectState(request.journey){ j =>
      finalStateCheck(request.journey, displayPaymentDayPage(j))
    }
  }

  private def displayPaymentDayPage(
      journey: Either[Journey.AfterEnteredMonthlyPaymentAmount, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)]
  )(implicit request: Request[_]): Result = {
    val maybePrePopForm: Form[PaymentDayForm] =
      existingPaymentDay(journey).fold(paymentDayForm()){ day =>
        if (day.value === 28) {
          paymentDayForm().fill(PaymentDayForm("28", None))
        } else {
          paymentDayForm().fill(PaymentDayForm("", Some(day.value)))
        }
      }

    Ok(views.paymentDayPage(maybePrePopForm))
  }

  private def existingPaymentDay(
      journey: Either[Journey.AfterEnteredMonthlyPaymentAmount, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)]
  ): Option[DayOfMonth] = {
    journey.fold(
      {
        case j: Journey.AfterEnteredDayOfMonth  => Some(j.dayOfMonth)
        case _: Journey.BeforeEnteredDayOfMonth => None
      },
      _._2.dayOfMonth.some
    )
  }

  val paymentDaySubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    withJourneyInCorrectState(request.journey){ j =>
      paymentDayForm()
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(views.paymentDayPage(form = formWithErrors)),
          (form: PaymentDayForm) => {
            val dayOfMonth: DayOfMonth = form.differentDay match {
              case Some(otherDay) => DayOfMonth(otherDay)
              case None           => DayOfMonth(form.paymentDay.toInt)
            }
            journeyService.updateDayOfMonth(request.journeyId, dayOfMonth)
              .map(updatedJourney =>
                Routing.redirectToNext(
                  routes.PaymentDayController.paymentDay,
                  updatedJourney,
                  existingPaymentDay(j).contains(dayOfMonth)
                ))
          }
        )
    }
  }

  private def withJourneyInCorrectState[A](journey: Journey)(
      f: Either[Journey.AfterEnteredMonthlyPaymentAmount, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)] => Future[Result]
  )(implicit r: Request[_]): Future[Result] =
    journey match {
      case j: Journey.BeforeEnteredMonthlyPaymentAmount =>
        logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterEnteredMonthlyPaymentAmount =>
        f(Left(j))
      case j: Journey.AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case p: PaymentPlanAnswers.PaymentPlanNoAffordability =>
            f(Right(j -> p))
          case _: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
            Errors.throwServerErrorException("Not expecting to select payment plan option when payment plan has been checked on affordability journey")
        }

      case _: Journey.AfterStartedPegaCase =>
        Errors.throwServerErrorException("Not expecting to select payment plan option when started PEGA case")
    }

}

object PaymentDayController {

  import cats.syntax.either._
  import play.api.data.Form
  import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

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
          readValue(key, data, _.replaceAll(" ", "").toInt)
            .flatMap(validateDayOfMonth)
        result.leftMap(Seq(_))
      }

      override def unbind(key: String, value: Int): Map[String, String] =
        Map(key -> value.toString)
    }
  }
}
