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

package viewmodels

import controllers.routes
import enumeratum._
import essttp.rootmodel.TaxRegime
import play.api.mvc.Call

sealed trait CheckPaymentPlanChangeLink extends EnumEntry {
  def targetPage(taxRegime: TaxRegime): Call
  def changeLink(taxRegime: TaxRegime): Call =
    routes.PaymentScheduleController.changeFromCheckPaymentSchedule(entryName, taxRegime, None)
}

object CheckPaymentPlanChangeLink extends Enum[CheckPaymentPlanChangeLink] {

  case object CanPayUpfront extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call = routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment
  }

  case object UpfrontPaymentAmount extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call = routes.UpfrontPaymentController.upfrontPaymentAmount
  }

  case object MonthlyPaymentAmount extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call = routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount
  }

  case object PaymentDay extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call = routes.PaymentDayController.paymentDay
  }

  case object WhyUnableInFull extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call = routes.WhyCannotPayInFullController.whyCannotPayInFull
  }

  case object PayWithin6Months extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call =
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(taxRegime, None)
  }

  case object PaymentPlan extends CheckPaymentPlanChangeLink {
    def targetPage(taxRegime: TaxRegime): Call = routes.InstalmentsController.instalmentOptions
  }

  override val values = findValues

}
