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

import cats.syntax.eq._
import _root_.actions.Actions
import controllers.UpfrontPaymentController.{ upfrontPaymentAmountForm, upfrontPaymentForm }
import models.Journey
import models.MoneyUtil.amountOfMoneyFormatter
import moveittocor.corcommon.model.AmountInPence
import play.api.data.{ Form, Forms }
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import requests.RequestSupport
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.{ UpfrontPayment, UpfrontPaymentAmount, UpfrontSummary }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class UpfrontPaymentController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  requestSupport: RequestSupport,
  upfrontPaymentPage: UpfrontPayment,
  upfrontPaymentAmountPage: UpfrontPaymentAmount,
  upfrontSummaryPage: UpfrontSummary)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val upfrontPayment: Action[AnyContent] = as.default { implicit request =>
    Ok(upfrontPaymentPage(upfrontPaymentForm()))
  }

  val upfrontPaymentSubmit: Action[AnyContent] = as.getJourney.async { implicit request =>
    upfrontPaymentForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(upfrontPaymentPage(formWithErrors))),
        (answer: String) => {
          journeyService.upsert(
            request.journey.copy(
              userAnswers = request.journey.userAnswers.copy(
                hasUpfrontPayment = Some(answer === "Yes"))))
          answer match {
            case "Yes" =>
              Future.successful(Redirect(routes.UpfrontPaymentController.upfrontPaymentAmount()))
            case _ => Future.successful(Redirect(routes.MonthlyPaymentAmountController.monthlyPaymentAmount()))
          }
        })

  }

  val upfrontPaymentAmount: Action[AnyContent] = as.getJourney.async { implicit request =>
    val form: Form[BigDecimal] = request.journey.userAnswers.upfrontAmount match {
      case Some(a: AmountInPence) => upfrontPaymentAmountForm(request.journey).fill(a.inPounds)
      case None => upfrontPaymentAmountForm(request.journey)
    }
    Future.successful(Ok(upfrontPaymentAmountPage(form, request.journey.qualifyingDebt, AmountInPence(100L))))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.getJourney.async { implicit request =>
    upfrontPaymentAmountForm(request.journey)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(upfrontPaymentAmountPage(formWithErrors, request.journey.qualifyingDebt, AmountInPence(100L)))),
        (s: BigDecimal) => {
          journeyService.upsert(
            request.journey.copy(
              remainingToPay = AmountInPence(request.journey.qualifyingDebt.value - (s.longValue() * 100)),
              userAnswers = request.journey.userAnswers.copy(
                upfrontAmount = Some(AmountInPence((s * 100).longValue())))))
          Future(Redirect(routes.UpfrontPaymentController.upfrontSummary()))
        })
  }

  val upfrontSummary: Action[AnyContent] = as.getJourney.async { implicit request =>
    Future.successful(Ok(upfrontSummaryPage(request.journey.userAnswers, request.journey.remainingToPay)))
  }
}

object UpfrontPaymentController {
  val key: String = "UpfrontPaymentAmount"

  def upfrontPaymentForm(): Form[String] = Form(
    mapping(
      "UpfrontPayment" -> nonEmptyText)(identity)(Some(_)))

  def upfrontPaymentAmountForm(journey: Journey): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(AmountInPence(100L).inPounds > _, journey.qualifyingDebt.inPounds < _)))(identity)(Some(_)))
}