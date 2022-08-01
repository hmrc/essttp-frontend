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
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import essttp.journey.model.{Journey, Origins}
import essttp.rootmodel.ttp.{ChargeTypeAssessment, Charges, EligibilityCheckResult}
import essttp.rootmodel.AmountInPence
import models.{InvoicePeriod, OverDuePayments, OverduePayment}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

@Singleton
class YourBillController @Inject() (
    as:    Actions,
    mcc:   MessagesControllerComponents,
    views: Views
)
  extends FrontendController(mcc)
  with Logging {

  val yourBill: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEligibilityChecked => logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterEligibilityChecked  => finalStateCheck(j, displayPage(j))
    }
  }

  def displayPage(journey: Journey.AfterEligibilityChecked)(implicit request: Request[_]): Result = {
    val backUrl = journey.origin match {
      case Origins.Epaye.Bta         => Some(routes.LandingController.landingPage.url)
      case Origins.Epaye.DetachedUrl => Some(routes.LandingController.landingPage.url)
      case Origins.Epaye.GovUk       => journey.backUrl.map(_.value)
    }
    Ok(views.yourBillIs(YourBillController.overDuePayments(journey.eligibilityCheckResult), backUrl))
  }

  val yourBillSubmit: Action[AnyContent] = as.eligibleJourneyAction { _ =>
    Redirect(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment)
  }

}

object YourBillController {
  def chargeDueDate(chargeTypeAssessments: List[ChargeTypeAssessment]): LocalDate = {
    chargeTypeAssessments.headOption.map { (chargeTypeAssessment: ChargeTypeAssessment) =>
      chargeTypeAssessment.charges.headOption.map { charges: Charges =>
        parseLocalDate(charges.interestStartDate.value.toString)
      }
    }.getOrElse(throw new IllegalArgumentException("missing charge list")).getOrElse(throw new IllegalArgumentException("missing charge list"))
  }

  val LocalDateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def parseLocalDate(s: String): LocalDate = LocalDateTimeFmt.parse(s, LocalDate.from)

  def formatLocalDateTime(d: LocalDate): String = LocalDateTimeFmt.format(d)

  def invoicePeriod(ass: ChargeTypeAssessment): InvoicePeriod = {
    val dueDate: LocalDate = chargeDueDate(List(ass))
    val startDate: LocalDate = parseLocalDate(ass.taxPeriodFrom.value)
    val endDate: LocalDate = parseLocalDate(ass.taxPeriodTo.value)
    InvoicePeriod(monthNumber(startDate), startDate, endDate, dueDate)
  }

  def monthNumber(date: LocalDate): Int = (date.getMonth.getValue - 4) % 12

  private def overDuePaymentOf(ass: ChargeTypeAssessment): OverduePayment =
    OverduePayment(invoicePeriod(ass), ass.debtTotalAmount.value)

  private def overDuePayments(eligibilityResult: EligibilityCheckResult): OverDuePayments = {
    val qualifyingDebt: AmountInPence = AmountInPence(eligibilityResult.chargeTypeAssessment.map(_.debtTotalAmount.value.value).sum)
    val payments = eligibilityResult.chargeTypeAssessment.map(overDuePaymentOf)
    OverDuePayments(qualifyingDebt, payments)
  }
}
