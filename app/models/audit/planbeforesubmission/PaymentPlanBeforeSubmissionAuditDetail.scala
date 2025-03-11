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

package models.audit.planbeforesubmission

import essttp.journey.model.CorrelationId
import essttp.rootmodel.CannotPayReason
import essttp.rootmodel.ttp.eligibility.RegimeDigitalCorrespondence
import models.audit.{AuditDetail, Schedule, TaxDetail}
import play.api.libs.json.{Json, OWrites}

final case class PaymentPlanBeforeSubmissionAuditDetail(
  schedule:                    Schedule,
  correlationId:               CorrelationId,
  origin:                      String,
  taxType:                     String,
  taxDetail:                   TaxDetail,
  regimeDigitalCorrespondence: RegimeDigitalCorrespondence,
  canPayInSixMonths:           Option[Boolean],
  unableToPayReason:           Option[Set[CannotPayReason]]
) extends AuditDetail {
  val auditType: String = "PlanDetails"
}

object PaymentPlanBeforeSubmissionAuditDetail {
  given OWrites[PaymentPlanBeforeSubmissionAuditDetail] = Json.writes
}
