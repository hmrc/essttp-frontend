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
import com.github.ghik.silencer.silent
import essttp.journey.model.Journey.Stages.ComputedTaxId
import essttp.journey.model.Origin
import essttp.journey.model.ttp.EligibilityCheckResult
import models.audit.eligibility.{EligibilityCheck, EligibilityResult, EnrollmentReasons, TaxDetail}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  def auditEligibilityCheck(
      journey:  ComputedTaxId,
      response: EligibilityCheckResult
  )(implicit r: AuthenticatedJourneyRequest[_], hc: HeaderCarrier): Unit = {
    val auditEvent = toEligibilityCheck(journey, response)
    auditConnector.sendExplicitAudit("EligibilityCheck", auditEvent)
  }

  // TODO: hook in when questions about business rules clarified
  @silent
  private def toEligibilityCheck(
      journey:          ComputedTaxId,
      enrollmentReason: Either[EnrollmentReasons.NotEnrolled, EnrollmentReasons.InactiveEnrollment]
  )(implicit r: AuthenticatedJourneyRequest[_]): EligibilityCheck = {
    EligibilityCheck(
      EligibilityResult.Ineligible,
      Some(enrollmentReason.merge),
      0,
      List.empty,
      toAuditString(journey.origin),
      journey.taxRegime.toString,
      TaxDetail(None, None, None, None, None, None),
      r.ggCredId.value,
      List.empty
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def toEligibilityCheck(
      journey:  ComputedTaxId,
      response: EligibilityCheckResult
  )(implicit r: AuthenticatedJourneyRequest[_]): EligibilityCheck = {
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
      None,
      None,
      None,
      getTaxId("EMPREF"),
      getTaxId("BROCS"),
      None
    )

    EligibilityCheck(
      eligibilityResult,
      enrollmentReasons,
      eligibilityReasons.size,
      eligibilityReasons,
      toAuditString(journey.origin),
      journey.taxRegime.toString,
      taxDetail,
      r.ggCredId.value,
      response.chargeTypeAssessment
    )
  }

  private def toAuditString(origin: Origin) =
    origin.toString.split('.').lastOption.getOrElse(origin.toString)

}
