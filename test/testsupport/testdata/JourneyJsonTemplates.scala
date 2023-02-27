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

package testsupport.testdata

import essttp.emailverification.EmailVerificationResult
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.DayOfMonth
import uk.gov.hmrc.crypto.Encrypter

object JourneyJsonTemplates {

  def Started(origin: Origin = Origins.Epaye.Bta): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.started,
    journeyInfo = JourneyInfo.started,
    origin      = origin
  )

  def `Computed Tax Id`(origin: Origin = Origins.Epaye.Bta, taxReference: String = "864FZ00049"): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.computedTaxId,
    journeyInfo = JourneyInfo.taxIdDetermined(taxReference),
    origin      = origin
  )

  def `Eligibility Checked - Eligible`(origin: Origin = Origins.Epaye.Bta)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedEligible,
    journeyInfo = JourneyInfo.eligibilityCheckedEligible(origin.taxRegime, encrypter),
    origin
  )

  def `Eligibility Checked - Ineligible - HasRlsOnAddress`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - MarkedAsInsolvent`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - IsLessThanMniDebtAllowance`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMinDebt(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebt(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleDisallowedCharge(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - ExistingTTP`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleExistingTtp(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebtAge(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - EligibleChargeType`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleChargeType(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - MissingFiledReturns`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMissingFiledReturns(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - NoDueDatesReached`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleNoDueDatesReached(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - HasInvalidInterestSignals`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasInvalidInterestSignals(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleDmSpecialOfficeProcessingRequired(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - CannotFindLockReason`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleCannotFindLockReason(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - MultipleReasons`(origin: Origin = Origins.Epaye.Bta)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMultipleReasons(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Answered Can Pay Upfront - Yes`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontYes,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontYes(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Answered Can Pay Upfront - No`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontNo,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontNo(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Entered Upfront payment amount`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredUpfrontPaymentAmount,
    journeyInfo = JourneyInfo.answeredUpfrontPaymentAmount(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Retrieved Extreme Dates Response`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedExtremeDates,
    journeyInfo = JourneyInfo.retrievedExtremeDates(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Retrieved Affordability`(origin: Origin, minimumInstalmentAmount: Int = 29997)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResult(minimumInstalmentAmount, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Retrieved Affordability no upfront payment`(origin: Origin, minimumInstalmentAmount: Int = 29997)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Entered Monthly Payment Amount`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredMonthlyPaymentAmount,
    journeyInfo = JourneyInfo.enteredMonthlyPaymentAmount(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Entered Day of Month`(dayOfMonth: DayOfMonth, origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDayOfMonth,
    journeyInfo = JourneyInfo.enteredDayOfMonth(dayOfMonth, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Retrieved Start Dates`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedStartDates,
    journeyInfo = JourneyInfo.retrievedStartDates(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Retrieved Affordable Quotes`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordableQuotes,
    journeyInfo = JourneyInfo.retrievedAffordableQuotes(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Chosen Payment Plan`(upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}""", regimeDigitalCorrespondence: Boolean = true, origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.chosenPaymentPlan,
    journeyInfo = List(
      TdJsonBodies.taxIdJourneyInfo(),
      TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.eligibleEligibilityPass, TdAll.eligibleEligibilityRules, origin.taxRegime, encrypter, regimeDigitalCorrespondence),
      TdJsonBodies.upfrontPaymentAnswersJourneyInfo(upfrontPaymentAmountJsonString),
      TdJsonBodies.extremeDatesJourneyInfo(),
      TdJsonBodies.affordabilityResultJourneyInfo(),
      TdJsonBodies.monthlyPaymentAmountJourneyInfo,
      TdJsonBodies.dayOfMonthJourneyInfo(TdAll.dayOfMonth()),
      TdJsonBodies.startDatesJourneyInfo,
      TdJsonBodies.affordableQuotesJourneyInfo,
      TdJsonBodies.selectedPlanJourneyInfo
    ),
    origin      = origin
  )

  def `Has Checked Payment Plan`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.hasCheckedPaymentPlan,
    journeyInfo = JourneyInfo.hasCheckedPaymentPlan(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Entered Details About Bank Account - Business`(isAccountHolder: Boolean, origin: Origin = Origins.Epaye.Bta)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isAccountHolder) StageInfo.enteredDetailsAboutBankAccount else StageInfo.enteredDetailsAboutBankAccountNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccountBusiness(isAccountHolder, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Entered Details About Bank Account - Personal`(isAccountHolder: Boolean, origin: Origin = Origins.Epaye.Bta)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isAccountHolder) StageInfo.enteredDetailsAboutBankAccountPersonal else StageInfo.enteredDetailsAboutBankAccountNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccountPersonal(isAccountHolder, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Entered Direct Debit Details`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDirectDebitDetails,
    journeyInfo = JourneyInfo.enteredDirectDebitDetails(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Confirmed Direct Debit Details`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.confirmedDirectDebitDetails,
    journeyInfo = JourneyInfo.confirmedDirectDebitDetails(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Confirmed Direct Debit Details - regimeDigitalCorrespondence flag`(origin: Origin, regimeDigitalCorrespondence: Boolean)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.confirmedDirectDebitDetails,
    journeyInfo = JourneyInfo.confirmedDdDetailsWithRegimeDigitalCorrespondance(regimeDigitalCorrespondence, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Agreed Terms and Conditions`(isEmailAddresRequired: Boolean, origin: Origin, etmpEmail: Option[String])(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isEmailAddresRequired) StageInfo.agreedTermsAndConditionsEmailAddressRequired else StageInfo.agreedTermsAndConditionsEmailAddressNotRequired,
    journeyInfo = JourneyInfo.agreedTermsAndConditions(isEmailAddresRequired, origin.taxRegime, encrypter, etmpEmail),
    origin      = origin
  )

  def `Selected email to be verified`(email: String, origin: Origin, etmpEmail: Option[String] = Some(TdAll.etmpEmail))(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.selectedEmailToBeVerified,
    journeyInfo = JourneyInfo.selectedEmailToBeVerified(email, origin.taxRegime, encrypter, etmpEmail),
    origin      = origin
  )

  def `Email verification complete`(email: String, status: EmailVerificationResult, origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = status match {
      case EmailVerificationResult.Verified => StageInfo.emailVerificationSuccess
      case EmailVerificationResult.Locked   => StageInfo.emailVerificationLocked
    },
    journeyInfo = JourneyInfo.emailVerificationComplete(email, status, origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Arrangement Submitted - with upfront payment`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithUpfrontPayment(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Arrangement Submitted - No upfront payment`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementNoUpfrontPayment(origin.taxRegime, encrypter),
    origin      = origin
  )

  def `Arrangement Submitted - with upfront payment and email`(email: String, origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithEmailParams(email, origin.taxRegime, encrypter),
    origin      = origin
  )
}
