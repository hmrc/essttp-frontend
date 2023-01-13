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

package models.audit.paymentplansetup

import essttp.crypto.CryptoFormat
import essttp.journey.model.CorrelationId
import essttp.rootmodel.Email
import essttp.rootmodel.bank.BankDetails
import essttp.rootmodel.ttp.eligibility.{EmailSource, RegimeDigitalCorrespondence}
import models.audit.{AuditDetail, Schedule, TaxDetail}
import play.api.libs.json.{Json, OWrites}

final case class PaymentPlanSetUpAuditDetail(
    bankDetails:                 BankDetails,
    schedule:                    Schedule,
    status:                      String,
    failedSubmissionReason:      Int,
    origin:                      String,
    taxType:                     String,
    taxDetail:                   TaxDetail,
    correlationId:               CorrelationId,
    ppReferenceNo:               String,
    authProviderId:              String,
    regimeDigitalCorrespondence: Option[RegimeDigitalCorrespondence],
    emailAddress:                Option[Email],
    emailSource:                 Option[EmailSource]
) extends AuditDetail {
  val auditType: String = "PlanSetUp"
}

object PaymentPlanSetUpAuditDetail {

  implicit def writes(implicit crypto: CryptoFormat): OWrites[PaymentPlanSetUpAuditDetail] = Json.writes

}
