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

  val upfrontPaymentSubmit: Action[AnyContent] = as.default { implicit request =>
    upfrontPaymentForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Ok(
            upfrontPaymentPage(
              formWithErrors)),
        {
          case "Yes" =>
            Redirect(routes.UpfrontPaymentController.upfrontPaymentAmount())
          case _ => Redirect(routes.MonthlyPaymentAmountController.monthlyPaymentAmount())
        })

  }

  val upfrontPaymentAmount: Action[AnyContent] = as.default.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        val form: Form[BigDecimal] = j.userAnswers.upfrontAmount match {
          case Some(a: AmountInPence) => upfrontPaymentAmountForm(j).fill(a.inPounds)
          case None => upfrontPaymentAmountForm(j)
        }
        Future.successful(Ok(upfrontPaymentAmountPage(form, j.qualifyingDebt, AmountInPence(100L))))
      case _ => sys.error("no journey found to use")
    }
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.getJourney.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        upfrontPaymentAmountForm(j)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(Ok(upfrontPaymentAmountPage(formWithErrors, j.qualifyingDebt, AmountInPence(100L)))),
            (s: BigDecimal) => {
              journeyService.upsert(
                j.copy(
                  remainingToPay = AmountInPence(j.qualifyingDebt.value - (s.longValue() * 100)),
                  userAnswers = j.userAnswers.copy(
                    upfrontAmount = Some(AmountInPence((s * 100).longValue())))))
              Future(Redirect(routes.UpfrontPaymentController.upfrontSummary()))
            })
      case _ => sys.error("no journey found to update")
    }

  }

  val upfrontSummary: Action[AnyContent] = as.getJourney.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        Future.successful(Ok(upfrontSummaryPage(j.userAnswers, j.remainingToPay)))
      case _ => sys.error("no journey to update")
    }
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