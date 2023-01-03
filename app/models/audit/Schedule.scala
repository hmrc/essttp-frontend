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

package models.audit

import essttp.rootmodel.ttp.affordablequotes.PaymentPlan
import essttp.rootmodel.{AmountInPence, DayOfMonth}
import models.audit.planbeforesubmission.AuditCollections
import play.api.libs.json.{Json, OWrites}

final case class Schedule(
    initialPaymentAmount:           BigDecimal,
    collectionDate:                 DayOfMonth,
    collectionLengthCalendarMonths: Int,
    collections:                    List[AuditCollections],
    totalNoPayments:                Int,
    totalInterestCharged:           BigDecimal,
    totalPayable:                   BigDecimal,
    totalPaymentWithoutInterest:    BigDecimal
)

object Schedule {
  implicit val writes: OWrites[Schedule] = Json.writes

  def createSchedule(selectedPaymentPlan: PaymentPlan, dayOfMonth: DayOfMonth): Schedule = {
    val totalNumberOfPaymentsIncludingUpfrontPayment: Int =
      selectedPaymentPlan.collections.regularCollections.size +
        selectedPaymentPlan.collections.initialCollection.fold(0)(_ => 1)
    val auditCollections: List[AuditCollections] = selectedPaymentPlan.instalments.map { instalment =>
      AuditCollections(
        collectionNumber = instalment.instalmentNumber.value,
        amount           = instalment.amountDue.value.inPounds,
        paymentDate      = instalment.dueDate.value
      )
    }
    Schedule(
      initialPaymentAmount           = selectedPaymentPlan.collections.initialCollection.fold(AmountInPence.zero)(_.amountDue.value).inPounds,
      collectionDate                 = dayOfMonth,
      collectionLengthCalendarMonths = selectedPaymentPlan.numberOfInstalments.value,
      collections                    = auditCollections,
      totalNoPayments                = totalNumberOfPaymentsIncludingUpfrontPayment,
      totalInterestCharged           = selectedPaymentPlan.planInterest.value.inPounds,
      totalPayable                   = selectedPaymentPlan.totalDebtIncInt.value.inPounds,
      totalPaymentWithoutInterest    = selectedPaymentPlan.totalDebt.value.inPounds
    )
  }
}
