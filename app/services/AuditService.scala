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

package services

import actionsmodel.AuthenticatedJourneyRequest
import cats.syntax.eq._
import essttp.bars.model.BarsVerifyStatusResponse
import essttp.crypto.CryptoFormat
import essttp.journey.model.CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths
import essttp.journey.model.Journey.Stages._
import essttp.journey.model.Journey.{AfterCheckedPaymentPlan, AfterEnteredCanYouSetUpDirectDebit, AfterStartedPegaCase}
import essttp.journey.model._
import essttp.rootmodel._
import essttp.rootmodel.bank.BankDetails
import essttp.rootmodel.pega.{GetCaseResponse, StartCaseResponse}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.ttp.eligibility.{EligibilityCheckResult, EmailSource}
import essttp.utils.Errors
import models.audit.bars._
import models.audit.canUserPayInSixMonths.{CanUserPayInSixMonthsAuditDetail, UserEnteredDetails}
import models.audit.ddinprogress.DdInProgressAuditDetail
import models.audit.eligibility.{EligibilityCheckAuditDetail, EligibilityResult, EnrollmentReasons}
import models.audit.emailverification.{EmailVerificationRequestedAuditDetail, EmailVerificationResultAuditDetail}
import models.audit.paymentplansetup.PaymentPlanSetUpAuditDetail
import models.audit.planbeforesubmission.PaymentPlanBeforeSubmissionAuditDetail
import models.audit.returnFromAffordability.{Collections, PlanDetails, ReturnFromAffordabilityAuditDetail}
import models.audit.{AuditDetail, Schedule, TaxDetail}
import models.bars.response.{BarsError, VerifyResponse}
import paymentsEmailVerification.models.EmailVerificationResult
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

  def auditPaymentPlanBeforeSubmission(
      journey:         Either[AfterStartedPegaCase, AfterCheckedPaymentPlan],
      getCaseResponse: GetCaseResponse
  )(implicit headerCarrier: HeaderCarrier): Unit =
    audit(toPaymentPlanBeforeSubmissionAuditDetail(journey, getCaseResponse))

  def auditReturnFromAffordability(
      journey:           Either[AfterStartedPegaCase, AfterCheckedPaymentPlan],
      startCaseResponse: StartCaseResponse,
      getCaseResponse:   GetCaseResponse
  )(implicit headerCarrier: HeaderCarrier): Unit =
    audit(toReturnFromAffordabilityAuditDetail(journey, startCaseResponse, getCaseResponse))

  def auditBarsCheck(
      journey:              AfterEnteredCanYouSetUpDirectDebit,
      bankDetails:          BankDetails,
      result:               Either[BarsError, VerifyResponse],
      verifyStatusResponse: BarsVerifyStatusResponse
  )(implicit hc: HeaderCarrier): Unit =
    audit(toBarsCheckAuditDetail(journey, bankDetails, result, verifyStatusResponse))

  def auditEmailVerificationRequested(journey: Journey, ggCredId: GGCredId, email: Email, result: String)(implicit headerCarrier: HeaderCarrier): Unit =
    audit(toEmailVerificationRequested(journey, ggCredId, email, result))

  def auditEmailVerificationResult(
      journey: Journey, ggCredId: GGCredId, email: Email, result: EmailVerificationResult
  )(implicit headerCarrier: HeaderCarrier): Unit =
    audit(toEmailVerificationResult(journey, ggCredId, email: Email, result))

  def auditPaymentPlanSetUp(
      journey:         Either[AgreedTermsAndConditions, EmailVerificationComplete],
      responseFromTtp: Either[HttpException, ArrangementResponse]
  )(implicit authenticatedJourneyRequest: AuthenticatedJourneyRequest[_], headerCarrier: HeaderCarrier): Unit = {
    audit(toPaymentPlanSetupAuditDetail(journey, responseFromTtp))
  }

  def auditDdInProgress(
      journey:             Journey,
      hasChosenToContinue: Boolean
  )(implicit r: AuthenticatedJourneyRequest[_], hc: HeaderCarrier): Unit =
    audit(toDdinProgressAuditDetail(journey, hasChosenToContinue))

  def auditCanUserPayInSixMonths(journey: Journey, canPay: CanPayWithinSixMonths, maybeStartCaseResponse: Option[StartCaseResponse])(implicit hc: HeaderCarrier): Unit = {
    audit(toCanUserPayInSixMonths(journey, canPay, maybeStartCaseResponse))
  }

  private def toCanUserPayInSixMonths(journey: Journey, canPay: CanPayWithinSixMonths, maybeStartCaseResponse: Option[StartCaseResponse]): CanUserPayInSixMonthsAuditDetail = {
    CanUserPayInSixMonthsAuditDetail(
      regime             = journey.taxRegime.entryName,
      taxIdentifier      = taxIdentifierToAudit(journey),
      pegaCaseId         = journey.pegaCaseId,
      correlationId      = journey.correlationId,
      pegaCorrelationId  = maybeStartCaseResponse.map(_.pegaCorrelationId),
      userEnteredDetails = UserEnteredDetails(
        unableToPayReason    = unableToPayReasonToAudit(journey),
        payUpfront           = upfrontPaymentToAudit(journey)._1,
        upfrontPaymentAmount = upfrontPaymentToAudit(journey)._2,
        canPayInSixMonths    = canPay.value
      )
    )
  }

  private def unableToPayReasonToAudit(journey: Journey): Option[Set[CannotPayReason]] = journey match {
    case j: Journey.AfterWhyCannotPayInFullAnswers => whyCannotPayInFullAnswersToSet(j.whyCannotPayInFullAnswers)
    case _                                         => sys.error("Could not find why cannot pay in full answers in journey")
  }

  private def taxIdentifierToAudit(journey: Journey): String = journey match {
    case j: Journey.AfterComputedTaxId => j.taxId.value
    case _                             => sys.error("Could not find tax ID in journey")
  }

  private def upfrontPaymentToAudit(journey: Journey): (Boolean, BigDecimal) = journey match {
    case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers match {
      case UpfrontPaymentAnswers.NoUpfrontPayment               => (false, BigDecimal(0))
      case UpfrontPaymentAnswers.DeclaredUpfrontPayment(amount) => (true, amount.value.inPounds)
    }
    case _ => sys.error("Could not find upfrontPaymentAmount in journey")
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
      taxDetail                       = TaxDetail(None, None, None, None, None, None, None),
      authProviderId                  = r.ggCredId.value,
      chargeTypeAssessment            = List.empty,
      correlationId                   = journey.correlationId.value.toString,
      futureChargeLiabilitiesExcluded = None,
      regimeDigitalCorrespondence     = None
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
        def extractFields(obj: Product): List[(String, Any)] = {
          val fieldNames = obj.getClass.getDeclaredFields.map(_.getName).toList
          val fieldValues = obj.productIterator.toList
          fieldNames.zip(fieldValues)
        }

      val reasonsPart1 = extractFields(eligibilityCheckResult.eligibilityRules.part1)
      val reasonsPart2 = extractFields(eligibilityCheckResult.eligibilityRules.part2)

      val allReasons = reasonsPart1 ++ reasonsPart2

      allReasons.collect {
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
      futureChargeLiabilitiesExcluded = Some(eligibilityCheckResult.futureChargeLiabilitiesExcluded),
      regimeDigitalCorrespondence     = eligibilityCheckResult.regimeDigitalCorrespondence.map(_.value)
    )
  }

  private def toPaymentPlanBeforeSubmissionAuditDetail(journey: ChosenPaymentPlan): PaymentPlanBeforeSubmissionAuditDetail =
    PaymentPlanBeforeSubmissionAuditDetail(
      schedule                    = Schedule.createSchedule(journey.selectedPaymentPlan, journey.dayOfMonth),
      correlationId               = journey.correlationId,
      origin                      = toAuditString(journey.origin),
      taxType                     = journey.taxRegime.toString,
      taxDetail                   = toTaxDetail(journey.eligibilityCheckResult),
      regimeDigitalCorrespondence = journey.eligibilityCheckResult.regimeDigitalCorrespondence,
      canPayInSixMonths           = canPayWithinSixMonthsAnswersToBoolean(journey.canPayWithinSixMonthsAnswers),
      unableToPayReason           = whyCannotPayInFullAnswersToSet(journey.whyCannotPayInFullAnswers)
    )

  private def toPaymentPlanBeforeSubmissionAuditDetail(
      journey:         Either[AfterStartedPegaCase, AfterCheckedPaymentPlan],
      getCaseResponse: GetCaseResponse
  ): PaymentPlanBeforeSubmissionAuditDetail = {
    val eligibilityCheckResult = journey.merge match {
      case j: Journey.AfterEligibilityChecked =>
        j.eligibilityCheckResult
      case _ =>
        Errors.throwServerErrorException("Trying to get eligibility check result for audit event, but they haven't been retrieved yet.")
    }

    val canPayWithinSixMonthsAnswers = journey.merge match {
      case j: Journey.AfterCanPayWithinSixMonthsAnswers =>
        j.canPayWithinSixMonthsAnswers
      case _ =>
        Errors.throwServerErrorException("Trying to get can pay within 6 months answers for audit event, but they haven't been retrieved yet.")
    }

    val whyCannotPayInFullAnswers = journey.merge match {
      case j: Journey.AfterWhyCannotPayInFullAnswers =>
        j.whyCannotPayInFullAnswers
      case _ =>
        Errors.throwServerErrorException("Trying to get why cannot pay in full answers for audit event, but they haven't been retrieved yet.")
    }

    PaymentPlanBeforeSubmissionAuditDetail(
      schedule                    = Schedule.createSchedule(getCaseResponse.paymentPlan, getCaseResponse.paymentDay),
      correlationId               = journey.fold(_.correlationId, _.correlationId),
      origin                      = toAuditString(journey.fold(_.origin, _.origin)),
      taxType                     = journey.fold(_.taxRegime, _.taxRegime).toString,
      taxDetail                   = toTaxDetail(eligibilityCheckResult),
      regimeDigitalCorrespondence = eligibilityCheckResult.regimeDigitalCorrespondence,
      canPayInSixMonths           = canPayWithinSixMonthsAnswersToBoolean(canPayWithinSixMonthsAnswers),
      unableToPayReason           = whyCannotPayInFullAnswersToSet(whyCannotPayInFullAnswers)
    )
  }

  private def toReturnFromAffordabilityAuditDetail(
      journey:           Either[AfterStartedPegaCase, AfterCheckedPaymentPlan],
      startCaseResponse: StartCaseResponse,
      getCaseResponse:   GetCaseResponse
  ): ReturnFromAffordabilityAuditDetail = {
    val taxId = journey.merge match {
      case j: Journey.AfterComputedTaxId => j.taxId
      case _                             => sys.error("Could not find tax ID in journey")
    }

    val eligibilityCheckResult = journey.merge match {
      case j: Journey.AfterEligibilityChecked => j.eligibilityCheckResult
      case _                                  => sys.error("Could not find eligibility check result")
    }

    val paymentPlan = getCaseResponse.paymentPlan

    val sortedCollections = paymentPlan.collections.regularCollections.sortBy(_.dueDate.value)
    val firstCollection = sortedCollections.headOption
    val lastCollection = sortedCollections.lastOption

    val planDetails = PlanDetails(
      paymentPlan.totalDebt.value.inPounds,
      paymentPlan.collections.initialCollection.map(_.amountDue.value.inPounds),
      paymentPlan.collections.initialCollection.map(_.dueDate.value.toString),
      eligibilityCheckResult.customerPostcodes.flatMap(_.headOption).map(_.addressPostcode.value.decryptedValue).getOrElse(""),
      paymentPlan.numberOfInstalments.value,
      paymentPlan.planInterest.value.inPounds,
      Collections(
        paymentPlan.collections.regularCollections.size,
        firstCollection.map(_.dueDate.value.toString).getOrElse(""),
        firstCollection.map(_.amountDue.value.inPounds).getOrElse(BigDecimal(0)),
        lastCollection.map(_.amountDue.value.inPounds).getOrElse(BigDecimal(0))
      )
    )

    ReturnFromAffordabilityAuditDetail(
      journey.fold(_.correlationId, _.correlationId).value.toString,
      journey.fold(_.taxRegime, _.taxRegime).entryName,
      taxId.value,
      startCaseResponse.caseId.value,
      getCaseResponse.pegaCorrelationId,
      getCaseResponse.expenditure.filter(_._2 > 0),
      getCaseResponse.income.filter(_._2 > 0),
      planDetails
    )
  }

  private def toBarsCheckAuditDetail(
      journey:              AfterEnteredCanYouSetUpDirectDebit,
      bankDetails:          BankDetails,
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
          bankDetails.typeOfBankAccount.entryName.toLowerCase(Locale.UK),
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

  private def toDdinProgressAuditDetail(journey: Journey, hasChosenToContinue: Boolean)
    (implicit r: AuthenticatedJourneyRequest[_]): DdInProgressAuditDetail = {

    DdInProgressAuditDetail(
      toAuditString(journey.origin),
      journey.taxRegime.toString,
      toTaxDetail(toEligibilityCheckResult(journey)),
      journey.correlationId.toString,
      r.ggCredId.toString,
      if (hasChosenToContinue) "continue" else "exit"
    )
  }

  private def toPaymentPlanSetupAuditDetail(
      journey:         Either[AgreedTermsAndConditions, EmailVerificationComplete],
      responseFromTtp: Either[HttpException, ArrangementResponse]
  )(implicit r: AuthenticatedJourneyRequest[_]): PaymentPlanSetUpAuditDetail = {
    val maybeArrangementResponse: Option[ArrangementResponse] = responseFromTtp.toOption
    val status: Int = responseFromTtp.fold(_.responseCode, _ => Status.ACCEPTED)

    val directDebitDetails = journey.fold(_.directDebitDetails, _.directDebitDetails)
    val paymentPlanAnswers = journey.fold(_.paymentPlanAnswers, _.paymentPlanAnswers)
    val selectedPaymentPlan = paymentPlanAnswers.selectedPaymentPlan
    val dayOfMonth = paymentPlanAnswers.dayOfMonth
    val origin = journey.fold(_.origin, _.origin)
    val taxRegime = journey.fold(_.taxRegime, _.taxRegime)
    val eligibilityCheckResult = journey.fold(_.eligibilityCheckResult, _.eligibilityCheckResult)
    val correlationId = journey.fold(_.correlationId, _.correlationId)
    val (maybeEmail, maybeEmailSource) = journey.fold(_ => (None, None), toEmailInfo)
    val canPayWithinSixMonthsAnswers = journey.fold(_.canPayWithinSixMonthsAnswers, _.canPayWithinSixMonthsAnswers)
    val whyCannotPayInFullAnswers = journey.fold(_.whyCannotPayInFullAnswers, _.whyCannotPayInFullAnswers)

    PaymentPlanSetUpAuditDetail(
      bankDetails                 = directDebitDetails,
      schedule                    = Schedule.createSchedule(selectedPaymentPlan, dayOfMonth),
      status                      = if (Status.isSuccessful(status)) "successfully sent to TTP" else "failed",
      failedSubmissionReason      = status,
      origin                      = toAuditString(origin),
      taxType                     = taxRegime.toString,
      taxDetail                   = toTaxDetail(eligibilityCheckResult),
      correlationId               = correlationId,
      ppReferenceNo               = maybeArrangementResponse.map(_.customerReference.value).getOrElse("N/A"),
      authProviderId              = r.ggCredId.value,
      regimeDigitalCorrespondence = eligibilityCheckResult.regimeDigitalCorrespondence,
      emailAddress                = maybeEmail,
      emailSource                 = maybeEmailSource,
      canPayInSixMonths           = canPayWithinSixMonthsAnswersToBoolean(canPayWithinSixMonthsAnswers),
      unableToPayReason           = whyCannotPayInFullAnswersToSet(whyCannotPayInFullAnswers)
    )
  }

  private def toEmailVerificationRequested(
      journey:  Journey,
      ggCredId: GGCredId,
      email:    Email,
      result:   String
  ): EmailVerificationRequestedAuditDetail = {
    EmailVerificationRequestedAuditDetail(
      origin         = toAuditString(journey.origin),
      taxType        = journey.taxRegime.toString,
      taxDetail      = toTaxDetail(toEligibilityCheckResult(journey)),
      correlationId  = journey.correlationId,
      emailAddress   = paymentsEmailVerification.models.Email(email.value.decryptedValue),
      emailSource    = deriveEmailSource(journey, email),
      result         = result,
      authProviderId = ggCredId.value
    )
  }

  private def toEmailVerificationResult(
      journey:  Journey,
      ggCredId: GGCredId,
      email:    Email,
      result:   EmailVerificationResult
  ): EmailVerificationResultAuditDetail = {
    EmailVerificationResultAuditDetail(
      origin         = toAuditString(journey.origin),
      taxType        = journey.taxRegime.toString,
      taxDetail      = toTaxDetail(toEligibilityCheckResult(journey)),
      correlationId  = journey.correlationId,
      emailAddress   = email,
      emailSource    = deriveEmailSource(journey, email),
      result         = result match {
        case EmailVerificationResult.Verified => "Success"
        case EmailVerificationResult.Locked   => "Failed"
      },
      failureReason  = result match {
        case EmailVerificationResult.Verified => None
        case EmailVerificationResult.Locked   => Some("TooManyPasscodeAttempts")
      },
      authProviderId = ggCredId.value
    )
  }

  private def toTaxDetail(eligibilityCheckResult: EligibilityCheckResult): TaxDetail =
    TaxDetail(
      utr               = getTaxId("UTR")(eligibilityCheckResult),
      taxOfficeNo       = None,
      taxOfficeRef      = None,
      employerRef       = getTaxId("EMPREF")(eligibilityCheckResult),
      accountsOfficeRef = getTaxId("BROCS")(eligibilityCheckResult),
      vrn               = getTaxId("VRN")(eligibilityCheckResult),
      nino              = getTaxId("NINO")(eligibilityCheckResult)
    )

  private def toEmailInfo(journey: EmailVerificationComplete): (Option[Email], Option[EmailSource]) = {
    val emailFromEligibility: Option[Email] = journey.eligibilityCheckResult.email
    journey.emailVerificationAnswers match {
      case EmailVerificationAnswers.NoEmailJourney => None -> None
      case EmailVerificationAnswers.EmailVerified(email, _) =>
        if (emailFromEligibility.map(_.value.decryptedValue.toLowerCase(Locale.UK)).contains(email.value.decryptedValue.toLowerCase(Locale.UK))) {
          Some(email) -> Some(EmailSource.ETMP)
        } else {
          Some(email) -> Some(EmailSource.TEMP)
        }
    }
  }

  private def getTaxId(name: String)(eligibilityCheckResult: EligibilityCheckResult): Option[String] =
    eligibilityCheckResult.identification.find(_.idType.value === name).map(_.idValue.value)

  private def deriveEmailSource(journey: Journey, email: Email): EmailSource = {
    val emailFromEligibility = toEligibilityCheckResult(journey).email
    if (emailFromEligibility.map(_.value.decryptedValue.toLowerCase(Locale.UK)).contains(email.value.decryptedValue.toLowerCase(Locale.UK))) {
      EmailSource.ETMP
    } else {
      EmailSource.TEMP
    }
  }

  private def toEligibilityCheckResult(journey: Journey) = journey match {
    case j: Journey.AfterEligibilityChecked => j.eligibilityCheckResult
    case _                                  => Errors.throwServerErrorException("Trying to get eligibility check result for audit event, but it hasn't been retrieved yet.")
  }

  private def canPayWithinSixMonthsAnswersToBoolean(answers: CanPayWithinSixMonthsAnswers): Option[Boolean] =
    answers match {
      case CanPayWithinSixMonthsAnswers.AnswerNotRequired             => None
      case CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(canPay) => Some(canPay)
    }

  private def whyCannotPayInFullAnswersToSet(answers: WhyCannotPayInFullAnswers): Option[Set[CannotPayReason]] =
    answers match {
      case WhyCannotPayInFullAnswers.AnswerNotRequired           => None
      case WhyCannotPayInFullAnswers.WhyCannotPayInFull(reasons) => Some(reasons)
    }

  private def toAuditString(origin: Origin) =
    origin.toString.split('.').lastOption.getOrElse(origin.toString)

  private val auditSource: String = "set-up-payment-plan"

}
