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

import essttp.rootmodel.DayOfMonth
import uk.gov.hmrc.crypto.Encrypter

object JourneyJsonTemplates {

  val Started: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.started,
    journeyInfo = JourneyInfo.started
  )

  val `Computed Tax Id`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.computedTaxId,
    journeyInfo = JourneyInfo.taxIdDetermined
  )

  def `Eligibility Checked - Eligible`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedEligible,
    journeyInfo = JourneyInfo.eligibilityCheckedEligible(encrypter)
  )

  def `Eligibility Checked - Ineligible - HasRlsOnAddress`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls(encrypter)
  )

  def `Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebt(encrypter)
  )

  def `Eligibility Checked - Ineligible - ExistingTTP`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleExistingTtp(encrypter)
  )

  def `Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebtAge(encrypter)
  )

  def `Eligibility Checked - Ineligible - MissingFiledReturns`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMissingFiledReturns(encrypter)
  )

  def `Eligibility Checked - Ineligible - MultipleReasons`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMultipleReasons(encrypter)
  )

  def `Answered Can Pay Upfront - Yes`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontYes,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontYes(encrypter)
  )

  def `Answered Can Pay Upfront - No`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontNo,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontNo(encrypter)
  )

  def `Entered Upfront payment amount`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredUpfrontPaymentAmount,
    journeyInfo = JourneyInfo.answeredUpfrontPaymentAmount(encrypter)
  )

  def `Retrieved Extreme Dates Response`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedExtremeDates,
    journeyInfo = JourneyInfo.retrievedExtremeDates(encrypter)
  )

  def `Retrieved Affordability`(minimumInstalmentAmount: Int = 29997, encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResult(minimumInstalmentAmount, encrypter)
  )

  def `Retrieved Affordability no upfront payment`(minimumInstalmentAmount: Int = 29997, encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount, encrypter)
  )

  def `Entered Monthly Payment Amount`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredMonthlyPaymentAmount,
    journeyInfo = JourneyInfo.enteredMonthlyPaymentAmount(encrypter)
  )

  def `Entered Day of Month`(dayOfMonth: DayOfMonth, encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDayOfMonth,
    journeyInfo = JourneyInfo.enteredDayOfMonth(dayOfMonth, encrypter)
  )

  def `Retrieved Start Dates`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedStartDates,
    journeyInfo = JourneyInfo.retrievedStartDates(encrypter)
  )

  def `Retrieved Affordable Quotes`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordableQuotes,
    journeyInfo = JourneyInfo.retrievedAffordableQuotes(encrypter)
  )

  def `Chosen Payment Plan`(upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}""", encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
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

  def `Has Checked Payment Plan`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.hasCheckedPaymentPlan,
    journeyInfo = JourneyInfo.hasCheckedPaymentPlan(encrypter)
  )

  def `Entered Details About Bank Account - Business`(isAccountHolder: Boolean, encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isAccountHolder) StageInfo.enteredDetailsAboutBankAccount else StageInfo.enteredDetailsAboutBankAccountNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccountBusiness(isAccountHolder, encrypter)
  )

  def `Entered Details About Bank Account - Personal`(isAccountHolder: Boolean, encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = if (isAccountHolder) StageInfo.enteredDetailsAboutBankAccountPersonal else StageInfo.enteredDetailsAboutBankAccountNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccountPersonal(isAccountHolder, encrypter)
  )

  def `Entered Direct Debit Details`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDirectDebitDetails,
    journeyInfo = JourneyInfo.enteredDirectDebitDetails(encrypter)
  )

  def `Confirmed Direct Debit Details`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.confirmedDirectDebitDetails,
    journeyInfo = JourneyInfo.confirmedDirectDebitDetails(encrypter)
  )

  def `Agreed Terms and Conditions`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.agreedTermsAndConditions,
    journeyInfo = JourneyInfo.agreedTermsAndConditions(encrypter)
  )

  def `Arrangement Submitted - with upfront payment`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithUpfrontPayment(encrypter)
  )

  def `Arrangement Submitted - No upfront payment`(encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementNoUpfrontPayment(encrypter)
  )

}
