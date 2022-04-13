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
import models.ttp.{ ChargeTypeAssessment, EligibilityRules, TaxPeriodCharges, TtpEligibilityData }
import models.{ EligibilityData, InvoicePeriod, OverDuePayments, OverduePayment }
import moveittocor.corcommon.model.AmountInPence
import services.EligibilityDataService._
import testOnly.models.EligibilityError
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext }

@Singleton
class EligibilityDataService @Inject() (connector: EligibilityStubConnector) {

  def data(idType: String, regime: TaxRegime, id: TaxId, showFinancials: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    for {
      items <- connector.eligibilityData(idType, regime, id, showFinancials)
    } yield EligibilityData(rejections(items.eligibilityRules), overDuePayments(items))

  def rejections(rules: EligibilityRules): List[EligibilityError] = {
    def checkRlsOnAddress(list: List[EligibilityError]): List[EligibilityError] = if (rules.rlsOnAddress) EligibilityError.RLSFlagIsSet :: list else list
    def checkMarkedAsInsolvent(list: List[EligibilityError]): List[EligibilityError] = if (rules.markedAsInsolvent) EligibilityError.PayeIsInsolvent :: list else list
    def checkMinimumDebtAllowance(list: List[EligibilityError]): List[EligibilityError] = list

    def checkMaxDebtAllowance(list: List[EligibilityError]): List[EligibilityError] = if (rules.maxDebtAllowance) EligibilityError.DebtIsTooLarge :: list else list
    def checkDisallowedChargeLock(list: List[EligibilityError]): List[EligibilityError] = list
    def checkExistingTTP(list: List[EligibilityError]): List[EligibilityError] = if (rules.existingTTP) EligibilityError.YouAlreadyHaveAPaymentPlan :: list else list

    def checkMaxDebtAge(list: List[EligibilityError]): List[EligibilityError] = if (rules.maxDebtAge) EligibilityError.DebtIsTooOld :: list else list
    def checkEligibleChargeType(list: List[EligibilityError]): List[EligibilityError] = if (rules.eligibleChargeType) EligibilityError.PayeHasDisallowedCharges :: list else list
    def checkReturnsFiled(list: List[EligibilityError]): List[EligibilityError] = if (rules.returnsFiled) EligibilityError.ReturnsAreNotUpToDate :: list else list

    val check1 = checkRlsOnAddress _ andThen checkMarkedAsInsolvent _ andThen checkMinimumDebtAllowance _

    val check2 = checkMaxDebtAllowance _ andThen checkDisallowedChargeLock _

    val check3 = checkExistingTTP _ andThen checkMaxDebtAge _ andThen checkEligibleChargeType _ andThen checkReturnsFiled _

    val check = check1 andThen check2 andThen check3

    check(List.empty[EligibilityError])
  }

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
