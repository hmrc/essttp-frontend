/*
 * Copyright 2024 HM Revenue & Customs
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

package models.audit.canUserPayInSixMonths

import essttp.journey.model.CorrelationId
import essttp.rootmodel.pega.PegaCaseId
import models.audit.AuditDetail
import play.api.libs.json.{Json, OWrites}

final case class CanUserPayInSixMonthsAuditDetail(
  regime:             String,
  taxIdentifier:      String,
  pegaCaseId:         Option[PegaCaseId],
  correlationId:      CorrelationId,
  pegaCorrelationId:  Option[String],
  userEnteredDetails: UserEnteredDetails
) extends AuditDetail {

  override val auditType: String = "CanUserPayInSixMonths"

}

object CanUserPayInSixMonthsAuditDetail {
  given OWrites[CanUserPayInSixMonthsAuditDetail] = Json.writes
}
