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
import controllers.PaymentScheduleController.computeMonths
import messages.DateMessages
import models.{InstalmentOption, Journey}
import moveittocor.corcommon.model.AmountInPence
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import requests.RequestSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.CheckPaymentSchedule

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentScheduleController @Inject() (
    as:                  Actions,
    mcc:                 MessagesControllerComponents,
    requestSupport:      RequestSupport,
    paymentSchedulePage: CheckPaymentSchedule
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val checkPaymentSchedule: Action[AnyContent] = as.getJourney.async { implicit request =>
    val j: Journey = request.journey
    Future.successful(Ok(paymentSchedulePage(j.userAnswers, computeMonths(j.userAnswers.getMonthsToPay))))
  }
}

object PaymentScheduleController {
  case class MonthlyPayment(
      month:       Int,
      year:        Int,
      amount:      AmountInPence,
      isLastMonth: Boolean
  )

  def computeMonths(monthsToPay: InstalmentOption): List[MonthlyPayment] = {
    val today = LocalDate.now()
    for (i <- 1 to monthsToPay.numberOfMonths) yield {
      val paymentDate: LocalDate = today.plusMonths(i)
      val lastMonth: Boolean = i === monthsToPay.numberOfMonths
      MonthlyPayment(
        month       = paymentDate.getMonthValue,
        year        = paymentDate.getYear,
        amount      = if (lastMonth) {
          AmountInPence(monthsToPay.amountToPayEachMonth.value + monthsToPay.interestPayment.value)
        } else {
          monthsToPay.amountToPayEachMonth
        },
        isLastMonth = lastMonth
      )
    }
  }.toList
}
