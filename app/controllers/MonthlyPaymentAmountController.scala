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
import models.{ MockJourney }
import models.MoneyUtil._
import moveittocor.corcommon.model.AmountInPence
import play.api.data.{Form, Forms}
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.MonthlyPaymentAmount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class MonthlyPaymentAmountController @Inject() (
    as:                       Actions,
    mcc:                      MessagesControllerComponents,
    journeyService:           JourneyService,
    monthlyPaymentAmountPage: MonthlyPaymentAmount
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val monthlyPaymentAmount: Action[AnyContent] = as.default.async { implicit request =>
    val mockJourney = MockJourney()
    Future.successful(Ok(monthlyPaymentAmountPage(
      monthlyPaymentAmountForm(mockJourney),
      mockJourney.remainingToPay,
      AmountInPence(mockJourney.remainingToPay.value / 6))))
  }

  val monthlyPaymentAmountSubmit: Action[AnyContent] = as.default.async { implicit request =>
    val mockJourney = MockJourney()
    monthlyPaymentAmountForm(MockJourney())
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(
            monthlyPaymentAmountPage(
              formWithErrors, mockJourney.remainingToPay, AmountInPence(mockJourney.remainingToPay.value / 6)))),
        (s: BigDecimal) => {
          Future(Redirect(routes.PaymentDayController.paymentDay()))
        }
      )
  }
}

object MonthlyPaymentAmountController {
  val key: String = "MonthlyPaymentAmount"

  def monthlyPaymentAmountForm(journey: MockJourney): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(AmountInPence(journey.remainingToPay.value / 6).inPounds > _, journey.remainingToPay.inPounds < _))
    )(identity)(Some(_))
  )
}
