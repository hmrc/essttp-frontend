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
import essttp.rootmodel.{ TaxId, TaxRegime }
import models.ttp.{ ChargeTypeAssessment, TaxPeriodCharges, TtpEligibilityData }
import models.{ InvoicePeriod, OverDuePayments, OverduePayment }
import moveittocor.corcommon.model.AmountInPence
import services.EligibilityDataService.overDuePayments
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EligibilityDataService @Inject() (connector: EligibilityStubConnector) {

  def data(idType: String, regime: TaxRegime, id: TaxId, showFinancials: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[OverDuePayments] =
    for {
      items <- connector.eligibilityData(idType, regime, id, showFinancials)
    } yield (overDuePayments(items))

}

object EligibilityDataService {

  def chargeDueDate(charges: List[TaxPeriodCharges]): LocalDate = {
    charges.headOption.map(c => parseLocalDate(c.interestStartDate)).getOrElse(throw new IllegalArgumentException("missing charge list"))
  }

  def monthNumber(date: LocalDate): Int = (date.getMonth.getValue - 4) % 12

  def invoicePeriod(ass: ChargeTypeAssessment): InvoicePeriod = {
    val dueDate: LocalDate = chargeDueDate(ass.taxPeriodCharges)
    val startDate: LocalDate = parseLocalDate(ass.taxPeriodFrom)
    val endDate: LocalDate = parseLocalDate(ass.taxPeriodTo)
    InvoicePeriod(monthNumber(startDate), startDate, endDate, dueDate)
  }

  def overDuePaymentOf(ass: ChargeTypeAssessment): OverduePayment =
    OverduePayment(invoicePeriod(ass), AmountInPence(ass.debtTotalAmount))

  def overDuePayments(ttp: TtpEligibilityData): OverDuePayments = {
    val qualifyingDebt: AmountInPence = AmountInPence(ttp.chargeTypeAssessment.map(_.debtTotalAmount).sum)
    val payments = ttp.chargeTypeAssessment.map(overDuePaymentOf)
    OverDuePayments(qualifyingDebt, payments)
  }

  val LocalDateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def parseLocalDate(s: String): LocalDate = LocalDateTimeFmt.parse(s, LocalDate.from)

  def formatLocalDateTime(d: LocalDate): String = LocalDateTimeFmt.format(d)

}
