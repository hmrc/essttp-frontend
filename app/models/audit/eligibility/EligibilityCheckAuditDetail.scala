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

package models.audit.eligibility

import models.audit.{AuditDetail, TaxDetail}
import play.api.libs.json.{JsValue, Json, OWrites}

final case class EligibilityCheckAuditDetail(
    eligibilityResult:    EligibilityResult,
    enrollmentReasons:    Option[EnrollmentReasons],
    noEligibilityReasons: Int,
    eligibilityReasons:   List[String],
    origin:               String,
    taxType:              String,
    taxDetail:            TaxDetail,
    authProviderId:       String,
    chargeTypeAssessment: JsValue,
    correlationId:        String
) extends AuditDetail {
  val auditType: String = "EligibilityCheck"
}

object EligibilityCheckAuditDetail {

  implicit val writes: OWrites[EligibilityCheckAuditDetail] = Json.writes

}
