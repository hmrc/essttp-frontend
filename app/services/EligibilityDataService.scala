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

package services

import connectors.EligibilityStubConnector
import essttp.rootmodel.TaxId
import models.ttp.{ ChargeTypeAssessment, TtpEligibilityData }
import models.{ InvoicePeriod, OverDuePayments, OverduePayment, TaxRegimeFE }
import moveittocor.corcommon.model.AmountInPence
import services.EligibilityDataService.overDuePayments
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{ LocalDate }
import java.time.format.DateTimeFormatter
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EligibilityDataService @Inject() (connector: EligibilityStubConnector) {

  def data(idType: String, regime: TaxRegimeFE, id: TaxId, showFinancials: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OverDuePayments] =
    for {
      items <- connector.eligibilityData(idType, regime, id, showFinancials)
    } yield (overDuePayments(items))

}

object EligibilityDataService {

  def invoicePeriod(ass: ChargeTypeAssessment): InvoicePeriod = {
    val monthNumber: Int = 0
    val dueDate: LocalDate = parseLocalDate(ass.taxPeriodFrom)
    val startDate: LocalDate = parseLocalDate(ass.taxPeriodFrom)
    val endDate: LocalDate = parseLocalDate(ass.taxPeriodTo)
    InvoicePeriod(0, dueDate, startDate, endDate)
  }

  def overDuePaymentOf(ass: ChargeTypeAssessment): OverduePayment =
    OverduePayment(invoicePeriod(ass), AmountInPence(ass.debtTotalAmount))

  def overDuePayments(ttp: TtpEligibilityData): OverDuePayments = {
    val qualifyingDebt: AmountInPence = AmountInPence(ttp.chargeTypeAssessment.map(_.debtTotalAmount).sum)
    val op = ttp.chargeTypeAssessment.map(overDuePaymentOf).toList
    OverDuePayments(qualifyingDebt, op)
    //    val qualifyingDebt: AmountInPence = AmountInPence(296345)
    //    OverDuePayments(
    //      total = qualifyingDebt,
    //      payments = List(
    //        OverduePayment(
    //          InvoicePeriod(
    //            monthNumber = 7,
    //            dueDate = LocalDate.of(2022, 1, 22),
    //            start = LocalDate.of(2021, 11, 6),
    //            end = LocalDate.of(2021, 12, 5)),
    //          amount = AmountInPence((qualifyingDebt.value * 0.4).longValue())),
    //        OverduePayment(
    //          InvoicePeriod(
    //            monthNumber = 8,
    //            dueDate = LocalDate.of(2021, 12, 22),
    //            start = LocalDate.of(2021, 10, 6),
    //            end = LocalDate.of(2021, 11, 5)),
    //          amount = AmountInPence((qualifyingDebt.value * 0.6).longValue()))))
  }

  val LocalDateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def parseLocalDate(s: String): LocalDate = LocalDateTimeFmt.parse(s, LocalDate.from)

  def formatLocalDateTime(d: LocalDate): String = LocalDateTimeFmt.format(d)

}
