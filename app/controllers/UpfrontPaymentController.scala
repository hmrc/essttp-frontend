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
import models.{ MockJourney, UserAnswers }
import models.MoneyUtil.amountOfMoneyFormatter
import moveittocor.corcommon.model.AmountInPence
import play.api.data.{Form, Forms}
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.{UpfrontPayment, UpfrontPaymentAmount, UpfrontSummary}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpfrontPaymentController @Inject() (
    as:                       Actions,
    mcc:                      MessagesControllerComponents,
    journeyService:           JourneyService,
    upfrontPaymentPage:       UpfrontPayment,
    upfrontPaymentAmountPage: UpfrontPaymentAmount,
    upfrontSummaryPage:       UpfrontSummary
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val upfrontPayment: Action[AnyContent] = as.default.async { implicit request =>
    Future.successful(Ok(upfrontPaymentPage(upfrontPaymentForm())))
  }

  val upfrontPaymentSubmit: Action[AnyContent] = as.default.async { implicit request =>
    upfrontPaymentForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(upfrontPaymentPage(formWithErrors))),
        {
          case "Yes" =>
            Future.successful(Redirect(routes.UpfrontPaymentController.upfrontPaymentAmount()))
          case _ => Future.successful(Redirect(routes.MonthlyPaymentAmountController.monthlyPaymentAmount()))
        })

  }

  val upfrontPaymentAmount: Action[AnyContent] = as.default.async { implicit request =>
    val mockJourney = MockJourney(userAnswers = UserAnswers.empty.copy(hasUpfrontPayment = Some(true)))
    Future.successful(Ok(upfrontPaymentAmountPage(upfrontPaymentAmountForm(mockJourney), mockJourney.qualifyingDebt, AmountInPence(100L))))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.default.async { implicit request =>
    val mockJourney = MockJourney(userAnswers = UserAnswers.empty)
    upfrontPaymentAmountForm(mockJourney)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(upfrontPaymentAmountPage(formWithErrors, mockJourney.qualifyingDebt, AmountInPence(100L)))),
        (s: BigDecimal) => {
          /* TODO: compute what is remaining to pay by subtracting "s" from the initial qualifying debt amount
             and write to session store
           */
          Future(Redirect(routes.UpfrontPaymentController.upfrontSummary()))
        }
      )
  }

  val upfrontSummary: Action[AnyContent] = as.default.async { implicit request =>
    val mockUserAnswers = UserAnswers.empty.copy(
      hasUpfrontPayment = Some(true),
      upfrontAmount = Some(AmountInPence(10000L)))
    Future.successful(Ok(upfrontSummaryPage(mockUserAnswers, AmountInPence(200000L))))
  }
}

object UpfrontPaymentController {
  val key: String = "UpfrontPaymentAmount"

  def upfrontPaymentForm(): Form[String] = Form(
    mapping(
      "UpfrontPayment" -> nonEmptyText
    )(identity)(Some(_))
  )

  def upfrontPaymentAmountForm(journey: MockJourney): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(AmountInPence(100L).inPounds > _, journey.qualifyingDebt.inPounds < _)))(identity)(Some(_)))

}
