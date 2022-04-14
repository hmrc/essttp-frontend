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

import cats.implicits.{ catsSyntaxValidatedId, toFunctorOps }
import cats.syntax.apply._
import cats.data.{ NonEmptyList, ValidatedNel }
import connectors.EligibilityStubConnector
import essttp.rootmodel.{ TaxId, TaxRegime }
import models.ttp.{ ChargeTypeAssessment, EligibilityRules, TaxPeriodCharges, TtpEligibilityData }
import models.{ EligibilityData, InvoicePeriod, OverDuePayments, OverduePayment }
import moveittocor.corcommon.model.AmountInPence
import services.EligibilityDataService._
import testOnly.models.EligibilityError
import testOnly.models.EligibilityError._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class EligibilityDataService @Inject() (connector: EligibilityStubConnector) {

  def data(idType: String, regime: TaxRegime, id: TaxId, showFinancials: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    for {
      items <- connector.eligibilityData(idType, regime, id, showFinancials)
    } yield EligibilityData(rejectionsOf(items.eligibilityRules), overDuePayments(items))
}

object EligibilityDataService {

  type ValidatedResult[A] = ValidatedNel[EligibilityError, A]

  val unitResult = ().validNel

  def rejections(rules: EligibilityRules): Either[NonEmptyList[EligibilityError], Unit] = {
    val validAddress: ValidatedResult[Unit] = if (rules.rlsOnAddress) RLSFlagIsSet.invalidNel else unitResult
    val markedAsInsolvent: ValidatedResult[Unit] = if (rules.markedAsInsolvent) PayeIsInsolvent.invalidNel else unitResult
    val minimumDebtAllowance: ValidatedResult[Unit] = unitResult
    val maxDebtAllowance: ValidatedResult[Unit] = if (rules.maxDebtAllowance) DebtIsTooLarge.invalidNel else unitResult
    val disallowedChargeLock: ValidatedResult[Unit] = unitResult
    val existingTTP: ValidatedResult[Unit] = if (rules.existingTTP) YouAlreadyHaveAPaymentPlan.invalidNel else unitResult
    val maxDebtAge: ValidatedResult[Unit] = if (rules.maxDebtAge) DebtIsTooOld.invalidNel else unitResult
    val eligibleChargeType: ValidatedResult[Unit] = if (rules.eligibleChargeType) PayeHasDisallowedCharges.invalidNel else unitResult
    val returnsFiled: ValidatedResult[Unit] = if (rules.returnsFiled) ReturnsAreNotUpToDate.invalidNel else unitResult
    (validAddress, markedAsInsolvent, minimumDebtAllowance, maxDebtAllowance, disallowedChargeLock,
      existingTTP, maxDebtAge, eligibleChargeType, returnsFiled).tupled.void.toEither
  }

  def rejectionsOf(rules: EligibilityRules): List[EligibilityError] = {
    rejections(rules).fold(_.toList, _ => List.empty)
  }

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
