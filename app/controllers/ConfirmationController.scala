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
import controllers.ConfirmationController.mockQuotation
import essttp.rootmodel.AmountInPence
import models.{InstalmentOption, MockJourney, UserAnswers}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.{Confirmation, PrintSummary}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    confirmationPage: Confirmation,
    printSummaryPage: PrintSummary
) extends FrontendController(mcc)
  with Logging {

  val confirmation: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val j: MockJourney = MockJourney(userAnswers = UserAnswers.empty.copy(paymentDay  = Some("28"), monthsToPay = Some(InstalmentOption(numberOfMonths       = 4, amountToPayEachMonth = AmountInPence(50000L), interestPayment = AmountInPence(3500L)))))
    Future.successful(Ok(confirmationPage(j.userAnswers, mockQuotation(j.userAnswers.getMonthsToPay), "222PX00222222")))
  }

  val printSummary: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val j: MockJourney = MockJourney(userAnswers = UserAnswers.empty.copy(paymentDay  = Some("28"), monthsToPay = Some(InstalmentOption(numberOfMonths       = 4, amountToPayEachMonth = AmountInPence(50000L), interestPayment = AmountInPence(3500L)))))
    Future.successful(Ok(printSummaryPage(j.userAnswers, mockQuotation(j.userAnswers.getMonthsToPay), "222PX00222222")))
  }
}

object ConfirmationController {

  final case class MonthlyPayment(
      month:  Int,
      year:   Int,
      amount: AmountInPence
  )

  def mockQuotation(monthsToPay: InstalmentOption): List[MonthlyPayment] = {
    val today = LocalDate.now()
    for (i <- 1 to monthsToPay.numberOfMonths) yield {
      val paymentDate: LocalDate = today.plusMonths(i)
      MonthlyPayment(
        month  = paymentDate.getMonthValue,
        year   = paymentDate.getYear,
        amount = AmountInPence(monthsToPay.amountToPayEachMonth.value + (monthsToPay.interestPayment.value / monthsToPay.numberOfMonths))
      )
    }
  }.toList

}
