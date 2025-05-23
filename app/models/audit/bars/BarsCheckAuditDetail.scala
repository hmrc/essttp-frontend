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

package models.audit.bars

import models.audit.{AuditDetail, TaxDetail}
import play.api.libs.json.{Json, OWrites}

final case class BarsCheckAuditDetail(
  origin:        String,
  taxType:       String,
  taxDetail:     TaxDetail,
  request:       BarsAuditRequest,
  response:      BarsAuditResponse,
  barsVerify:    BarsVerifyDetails,
  correlationId: String
) extends AuditDetail {

  override val auditType: String = "BarsCheck"

}

object BarsCheckAuditDetail {

  given OWrites[BarsCheckAuditDetail] = Json.writes

}
