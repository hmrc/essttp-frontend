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

package testsupport.testdata

import essttp.emailverification.EmailVerificationStatus
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
    journeyInfo = JourneyInfo.eligibilityCheckedEligible(encrypter),
    origin
  )

  def `Eligibility Checked - Ineligible - HasRlsOnAddress`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls(encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - MarkedAsInsolvent`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls(encrypter)
  )

  def `Eligibility Checked - Ineligible - IsLessThanMniDebtAllowance`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMinDebt(encrypter)
  )

  def `Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebt(encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleDisallowedCharge(encrypter)
  )

  def `Eligibility Checked - Ineligible - ExistingTTP`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleExistingTtp(encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebtAge(encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - EligibleChargeType`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleChargeType(encrypter)
  )

  def `Eligibility Checked - Ineligible - MissingFiledReturns`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMissingFiledReturns(encrypter),
    origin      = origin
  )

  def `Eligibility Checked - Ineligible - MultipleReasons`(origin: Origin = Origins.Epaye.Bta)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMultipleReasons(encrypter),
    origin      = origin
  )

  def `Answered Can Pay Upfront - Yes`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontYes,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontYes(encrypter),
    origin      = origin
  )

  def `Answered Can Pay Upfront - No`(origin: Origin)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontNo,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontNo(encrypter),
    origin      = origin
  )

  def `Entered Upfront payment amount`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredUpfrontPaymentAmount,
    journeyInfo = JourneyInfo.answeredUpfrontPaymentAmount(encrypter)
  )

  def `Retrieved Extreme Dates Response`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedExtremeDates,
    journeyInfo = JourneyInfo.retrievedExtremeDates(encrypter)
  )

  def `Retrieved Affordability`(minimumInstalmentAmount: Int = 29997)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResult(minimumInstalmentAmount, encrypter)
  )

  def `Retrieved Affordability no upfront payment`(minimumInstalmentAmount: Int = 29997)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount, encrypter)
  )

  def `Entered Monthly Payment Amount`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredMonthlyPaymentAmount,
    journeyInfo = JourneyInfo.enteredMonthlyPaymentAmount(encrypter)
  )

  def `Entered Day of Month`(dayOfMonth: DayOfMonth)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDayOfMonth,
    journeyInfo = JourneyInfo.enteredDayOfMonth(dayOfMonth, encrypter)
  )

  def `Retrieved Start Dates`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedStartDates,
    journeyInfo = JourneyInfo.retrievedStartDates(encrypter)
  )

  def `Retrieved Affordable Quotes`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordableQuotes,
    journeyInfo = JourneyInfo.retrievedAffordableQuotes(encrypter)
  )

  def `Chosen Payment Plan`(upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}""")(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.chosenPaymentPlan,
    journeyInfo = List(
      TdJsonBodies.taxIdJourneyInfo(),
      TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.eligibleEligibilityPass, TdAll.eligibleEligibilityRules, encrypter),
      TdJsonBodies.upfrontPaymentAnswersJourneyInfo(upfrontPaymentAmountJsonString),
      TdJsonBodies.extremeDatesJourneyInfo(),
      TdJsonBodies.affordabilityResultJourneyInfo(),
      TdJsonBodies.monthlyPaymentAmountJourneyInfo,
      TdJsonBodies.dayOfMonthJourneyInfo(TdAll.dayOfMonth()),
      TdJsonBodies.startDatesJourneyInfo,
      TdJsonBodies.affordableQuotesJourneyInfo,
      TdJsonBodies.selectedPlanJourneyInfo
    )
  )

  def `Has Checked Payment Plan`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.hasCheckedPaymentPlan,
    journeyInfo = JourneyInfo.hasCheckedPaymentPlan(encrypter)
  )

  def `Entered Details About Bank Account - Business`(isAccountHolder: Boolean)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isAccountHolder) StageInfo.enteredDetailsAboutBankAccount else StageInfo.enteredDetailsAboutBankAccountNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccountBusiness(isAccountHolder, encrypter)
  )

  def `Entered Details About Bank Account - Personal`(isAccountHolder: Boolean)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isAccountHolder) StageInfo.enteredDetailsAboutBankAccountPersonal else StageInfo.enteredDetailsAboutBankAccountNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccountPersonal(isAccountHolder, encrypter)
  )

  def `Entered Direct Debit Details`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDirectDebitDetails,
    journeyInfo = JourneyInfo.enteredDirectDebitDetails(encrypter)
  )

  def `Confirmed Direct Debit Details`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.confirmedDirectDebitDetails,
    journeyInfo = JourneyInfo.confirmedDirectDebitDetails(encrypter)
  )

  def `Agreed Terms and Conditions`(isEmailAddresRequired: Boolean)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isEmailAddresRequired) StageInfo.agreedTermsAndConditionsEmailAddressRequired else StageInfo.agreedTermsAndConditionsEmailAddressNotRequired,
    journeyInfo = JourneyInfo.agreedTermsAndConditions(isEmailAddresRequired, encrypter)
  )

  def `Selected email to be verified`(email: String)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.selectedEmailToBeVerified,
    journeyInfo = JourneyInfo.selectedEmailToBeVerified(email, encrypter)
  )

  def `Email verification complete`(email: String, status: EmailVerificationStatus)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = status match {
      case EmailVerificationStatus.Verified => StageInfo.emailVerificationSuccess
      case EmailVerificationStatus.Locked   => StageInfo.emailVerificationLocked
    },
    journeyInfo = JourneyInfo.emailVerificationComplete(email, status, encrypter)
  )

  def `Arrangement Submitted - with upfront payment`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithUpfrontPayment(encrypter)
  )

  def `Arrangement Submitted - No upfront payment`(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementNoUpfrontPayment(encrypter)
  )

  def `Arrangement Submitted - with upfront payment and email`(email: String)(implicit encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithEmailParams(email, encrypter)
  )
}
