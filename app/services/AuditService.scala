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
import essttp.bars.model.BarsVerifyStatusResponse
import essttp.crypto.CryptoFormat
import essttp.journey.model.Journey.AfterEnteredDetailsAboutBankAccount
import essttp.journey.model.Journey.Stages._
import essttp.journey.model.{Journey, Origin}
import essttp.rootmodel.bank.{BankDetails, TypeOfBankAccount}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.utils.Errors
import models.audit.bars._
import models.audit.eligibility.{EligibilityCheckAuditDetail, EligibilityResult, EnrollmentReasons}
import models.audit.paymentplansetup.PaymentPlanSetUpAuditDetail
import models.audit.planbeforesubmission.PaymentPlanBeforeSubmissionAuditDetail
import models.audit.{AuditDetail, Schedule, TaxDetail}
import models.bars.response.{BarsError, VerifyResponse}
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.util.{Locale, UUID}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  implicit val cryptoFormat: CryptoFormat = CryptoFormat.NoOpCryptoFormat

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

  def auditPaymentPlanBeforeSubmission(
      journey: ChosenPaymentPlan
  )(implicit headerCarrier: HeaderCarrier): Unit =
    audit(toPaymentPlanBeforeSubmissionAuditDetail(journey))

  def auditBarsCheck(
      journey:              AfterEnteredDetailsAboutBankAccount,
      bankDetails:          BankDetails,
      typeOfBankAccount:    TypeOfBankAccount,
      result:               Either[BarsError, VerifyResponse],
      verifyStatusResponse: BarsVerifyStatusResponse
  )(implicit hc: HeaderCarrier): Unit =
    audit(toBarsCheckAuditDetail(journey, bankDetails, typeOfBankAccount, result, verifyStatusResponse))

  def auditPaymentPlanSetUp(
      journey:         Either[AgreedTermsAndConditions, EmailVerificationComplete],
      responseFromTtp: Either[HttpException, ArrangementResponse]
  )(implicit authenticatedJourneyRequest: AuthenticatedJourneyRequest[_], headerCarrier: HeaderCarrier): Unit = {
    audit(toPaymentPlanSetupAuditDetail(journey, responseFromTtp))
  }

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
      eligibilityResult               = EligibilityResult.Ineligible,
      enrollmentReasons               = Some(enrollmentReason.merge),
      noEligibilityReasons            = 0,
      eligibilityReasons              = List.empty,
      origin                          = toAuditString(journey.origin),
      taxType                         = journey.taxRegime.toString,
      taxDetail                       = TaxDetail(None, None, None, None, None, None),
      authProviderId                  = r.ggCredId.value,
      chargeTypeAssessment            = List.empty,
      correlationId                   = journey.correlationId.value.toString,
      futureChargeLiabilitiesExcluded = false
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def toEligibilityCheck(
      journey:                ComputedTaxId,
      eligibilityCheckResult: EligibilityCheckResult
  )(implicit r: AuthenticatedJourneyRequest[_]): EligibilityCheckAuditDetail = {

    val eligibilityResult =
      if (eligibilityCheckResult.isEligible) EligibilityResult.Eligible else EligibilityResult.Ineligible
    val enrollmentReasons =
      if (eligibilityCheckResult.isEligible) None else Some(EnrollmentReasons.DidNotPassEligibilityCheck())
    val eligibilityReasons = {
      val reasons: Array[String] = eligibilityCheckResult.eligibilityRules.getClass.getDeclaredFields.map(_.getName)
      val values = eligibilityCheckResult.eligibilityRules.productIterator.iterator
      (reasons zip values).toList.collect{
        case (reason, true)       => reason
        case (reason, Some(true)) => reason
      }
    }

    EligibilityCheckAuditDetail(
      eligibilityResult               = eligibilityResult,
      enrollmentReasons               = enrollmentReasons,
      noEligibilityReasons            = eligibilityReasons.size,
      eligibilityReasons              = eligibilityReasons,
      origin                          = toAuditString(journey.origin),
      taxType                         = journey.taxRegime.toString,
      taxDetail                       = toTaxDetail(eligibilityCheckResult),
      authProviderId                  = r.ggCredId.value,
      chargeTypeAssessment            = eligibilityCheckResult.chargeTypeAssessment,
      correlationId                   = journey.correlationId.value.toString,
      futureChargeLiabilitiesExcluded = eligibilityCheckResult.futureChargeLiabilitiesExcluded
    )
  }

  private def toPaymentPlanBeforeSubmissionAuditDetail(journey: ChosenPaymentPlan): PaymentPlanBeforeSubmissionAuditDetail = {
    PaymentPlanBeforeSubmissionAuditDetail(
      schedule      = Schedule.createSchedule(journey.selectedPaymentPlan, journey.dayOfMonth),
      correlationId = journey.correlationId,
      origin        = toAuditString(journey.origin),
      taxType       = journey.taxRegime.toString,
      taxDetail     = toTaxDetail(journey.eligibilityCheckResult)
    )
  }

  private def toBarsCheckAuditDetail(
      journey:              AfterEnteredDetailsAboutBankAccount,
      bankDetails:          BankDetails,
      typeOfBankAccount:    TypeOfBankAccount,
      result:               Either[BarsError, VerifyResponse],
      verifyStatusResponse: BarsVerifyStatusResponse
  ): BarsCheckAuditDetail = {
    val eligibilityCheckResult = journey match {
      case j: Journey.AfterEligibilityChecked => j.eligibilityCheckResult
      case _                                  => Errors.throwServerErrorException("Trying to get eligibility check result for audit event, but they haven't been retrieved yet.")
    }

    BarsCheckAuditDetail(
      toAuditString(journey.origin),
      journey.taxRegime.toString,
      toTaxDetail(eligibilityCheckResult),
      BarsAuditRequest(
        BarsAuditAccount(
          typeOfBankAccount.entryName.toLowerCase(Locale.UK),
          bankDetails.name.value.decryptedValue,
          bankDetails.sortCode.value.decryptedValue,
          bankDetails.accountNumber.value.decryptedValue
        )
      ),
      BarsAuditResponse(
        result.isRight,
        result
      ),
      BarsVerifyDetails(
        verifyStatusResponse.attempts.value,
        verifyStatusResponse.lockoutExpiryDateTime.map(_.toString)
      ),
      correlationId = journey.correlationId.value.toString
    )
  }

  private def toPaymentPlanSetupAuditDetail(
      journey:         Either[AgreedTermsAndConditions, EmailVerificationComplete],
      responseFromTtp: Either[HttpException, ArrangementResponse]
  )(implicit r: AuthenticatedJourneyRequest[_]): PaymentPlanSetUpAuditDetail = {
    val maybeArrangementResponse: Option[ArrangementResponse] = responseFromTtp.toOption
    val status: Int = responseFromTtp.fold(_.responseCode, _ => Status.ACCEPTED)

    val directDebitDetails = journey.fold(_.directDebitDetails, _.directDebitDetails)
    val selectedPaymentPlan = journey.fold(_.selectedPaymentPlan, _.selectedPaymentPlan)
    val dayOfMonth = journey.fold(_.dayOfMonth, _.dayOfMonth)
    val origin = journey.fold(_.origin, _.origin)
    val taxRegime = journey.fold(_.taxRegime, _.taxRegime)
    val eligibilityCheckResult = journey.fold(_.eligibilityCheckResult, _.eligibilityCheckResult)
    val correlationId = journey.fold(_.correlationId, _.correlationId)

    PaymentPlanSetUpAuditDetail(
      bankDetails            = directDebitDetails,
      schedule               = Schedule.createSchedule(selectedPaymentPlan, dayOfMonth),
      status                 = if (Status.isSuccessful(status)) "successfully sent to TTP" else "failed",
      failedSubmissionReason = status,
      origin                 = toAuditString(origin),
      taxType                = taxRegime.toString,
      taxDetail              = toTaxDetail(eligibilityCheckResult),
      correlationId          = correlationId,
      ppReferenceNo          = maybeArrangementResponse.map(_.customerReference.value).getOrElse("N/A"),
      authProviderId         = r.ggCredId.value
    )
  }

  private def toTaxDetail(eligibilityCheckResult: EligibilityCheckResult): TaxDetail =
    TaxDetail(
      utr               = None,
      taxOfficeNo       = None,
      taxOfficeRef      = None,
      employerRef       = getTaxId("EMPREF")(eligibilityCheckResult),
      accountsOfficeRef = getTaxId("BROCS")(eligibilityCheckResult),
      vrn               = None
    )

  private def getTaxId(name: String)(eligibilityCheckResult: EligibilityCheckResult): Option[String] =
    eligibilityCheckResult.identification.find(_.idType.value === name).map(_.idValue.value)

  private def toAuditString(origin: Origin) =
    origin.toString.split('.').lastOption.getOrElse(origin.toString)

  private val auditSource: String = "set-up-payment-plan"

}
