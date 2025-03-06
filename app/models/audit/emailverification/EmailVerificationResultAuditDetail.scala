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

package models.audit.emailverification

import essttp.crypto.CryptoFormat
import essttp.journey.model.CorrelationId
import essttp.rootmodel.Email
import essttp.rootmodel.ttp.eligibility.EmailSource
import models.audit.{AuditDetail, TaxDetail}
import play.api.libs.json.{Json, OWrites}

final case class EmailVerificationResultAuditDetail(
  origin:         String,
  taxType:        String,
  taxDetail:      TaxDetail,
  correlationId:  CorrelationId,
  emailAddress:   Email,
  emailSource:    EmailSource,
  result:         String,
  failureReason:  Option[String],
  authProviderId: String
) extends AuditDetail {
  val auditType: String = "EmailVerificationResult"
}

object EmailVerificationResultAuditDetail {
  given (using CryptoFormat): OWrites[EmailVerificationResultAuditDetail] = Json.writes
}
