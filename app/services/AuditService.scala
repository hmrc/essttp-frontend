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

import actionsmodel.AuthenticatedJourneyRequest
import cats.syntax.eq._
import essttp.journey.model.Journey.Stages.{ComputedTaxId, Started}
import essttp.journey.model.Origin
import essttp.rootmodel.ttp.EligibilityCheckResult
import models.audit.eligibility.{AuditDetail, EligibilityCheckAuditDetail, EligibilityResult, EnrollmentReasons, TaxDetail}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  def auditEligibilityCheck(
      journey:  ComputedTaxId,
      response: EligibilityCheckResult
  )(implicit r: AuthenticatedJourneyRequest[_], hc: HeaderCarrier): Unit =
    audit(toEligibilityCheck(journey, response))

  def auditEligibilityCheck(
      journey:          Started,
      enrollmentReason: Either[EnrollmentReasons.NotEnrolled, EnrollmentReasons.InactiveEnrollment]
  )(implicit r: AuthenticatedJourneyRequest[_], hc: HeaderCarrier): Unit =
    audit(toEligibilityCheck(journey, enrollmentReason))

  private def audit[A <: AuditDetail: Writes](a: A)(implicit hc: HeaderCarrier): Unit = {
    val _ = auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = auditSource,
        auditType   = a.auditType,
        eventId     = UUID.randomUUID().toString,
        tags        = hc.toAuditTags(),
        detail      = Json.toJson(a)
      )
    )
  }

  private def toEligibilityCheck(
      journey:          Started,
      enrollmentReason: Either[EnrollmentReasons.NotEnrolled, EnrollmentReasons.InactiveEnrollment]
  )(implicit r: AuthenticatedJourneyRequest[_]): EligibilityCheckAuditDetail = {
    EligibilityCheckAuditDetail(
      eligibilityResult    = EligibilityResult.Ineligible,
      enrollmentReasons    = Some(enrollmentReason.merge),
      noEligibilityReasons = 0,
      eligibilityReasons   = List.empty,
      origin               = toAuditString(journey.origin),
      taxType              = journey.taxRegime.toString,
      taxDetail            = TaxDetail(None, None, None, None, None, None),
      authProviderId       = r.ggCredId.value,
      chargeTypeAssessment = List.empty
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def toEligibilityCheck(
      journey:  ComputedTaxId,
      response: EligibilityCheckResult
  )(implicit r: AuthenticatedJourneyRequest[_]): EligibilityCheckAuditDetail = {
      def getTaxId(name: String): Option[String] =
        response.identification.find(_.idType.value === name).map(_.idValue.value)

    val eligibilityResult =
      if (response.isEligible) EligibilityResult.Eligible else EligibilityResult.Ineligible
    val enrollmentReasons =
      if (response.isEligible) None else Some(EnrollmentReasons.DidNotPassEligibilityCheck())
    val eligibilityReasons = {
      val reasons = response.eligibilityRules.getClass.getDeclaredFields.map(_.getName)
      val values = response.eligibilityRules.productIterator.to
      (reasons zip values).toList.collect{ case (reason, true) => reason }
    }
    val taxDetail = TaxDetail(
      utr               = None,
      taxOfficeNo       = None,
      taxOfficeRef      = None,
      employerRef       = getTaxId("EMPREF"),
      accountsOfficeRef = getTaxId("BROCS"),
      vrn               = None
    )

    EligibilityCheckAuditDetail(
      eligibilityResult    = eligibilityResult,
      enrollmentReasons    = enrollmentReasons,
      noEligibilityReasons = eligibilityReasons.size,
      eligibilityReasons   = eligibilityReasons,
      origin               = toAuditString(journey.origin),
      taxType              = journey.taxRegime.toString,
      taxDetail            = taxDetail,
      authProviderId       = r.ggCredId.value,
      chargeTypeAssessment = response.chargeTypeAssessment
    )
  }

  private def toAuditString(origin: Origin) =
    origin.toString.split('.').lastOption.getOrElse(origin.toString)

  private val auditSource: String = "set-up-payment-plan"

}
