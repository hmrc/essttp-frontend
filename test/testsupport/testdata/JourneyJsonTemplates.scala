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

object JourneyJsonTemplates {

  val `Computed Tax Id`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.computedTaxId,
    journeyInfo = JourneyInfo.taxIdDetermined
  )

  val `Eligibility Checked - Eligible`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedEligible,
    journeyInfo = JourneyInfo.eligibilityCheckedEligible
  )

  val `Eligibility Checked - Ineligible - HasRlsOnAddress`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls
  )

  val `Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebt
  )

  val `Eligibility Checked - Ineligible - ExistingTTP`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleExistingTtp
  )

  val `Eligibility Checked - Ineligible - ExceedsMaxDebtAge`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebtAge
  )

  val `Eligibility Checked - Ineligible - MissingFiledReturns`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMissingFiledReturns
  )

  val `Answered Can Pay Upfront - Yes`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontYes,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontYes
  )

  val `Answered Can Pay Upfront - No`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.answeredCanPayUpfrontNo,
    journeyInfo = JourneyInfo.answeredCanPayUpfrontNo
  )

  val `Entered Upfront payment amount`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredUpfrontPaymentAmount,
    journeyInfo = JourneyInfo.answeredUpfrontPaymentAmount
  )

  val `Retrieved Extreme Dates Response`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedExtremeDates,
    journeyInfo = JourneyInfo.retrievedExtremeDates
  )

  def `Retrieved Affordability`(minimumInstalmentAmount: Int = 29997): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResult(minimumInstalmentAmount)
  )

  val `Entered Monthly Payment Amount`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredMonthlyPaymentAmount,
    journeyInfo = JourneyInfo.enteredMonthlyPaymentAmount
  )

  val `Entered Day of Month`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDayOfMonth,
    journeyInfo = JourneyInfo.enteredDayOfMonth
  )

  val `Retrieved Start Dates`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedStartDates,
    journeyInfo = JourneyInfo.retrievedStartDates
  )

  val `Retrieved Affordable Quotes`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.retrievedAffordableQuotes,
    journeyInfo = JourneyInfo.retrievedAffordableQuotes
  )

  def `Chosen Payment Plan`(upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}"""): String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.chosenPaymentPlan,
    journeyInfo = List(
      TdJsonBodies.taxIdJourneyInfo(),
      TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.eligibleOverallEligibilityStatus, TdAll.eligibleEligibilityRules),
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

  val `Has Checked Payment Plan`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.hasCheckedPaymentPlan,
    journeyInfo = JourneyInfo.hasCheckedPaymentPlan
  )

  val `Chosen Type of Bank Account`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.chosenTypeOfBankAccount,
    journeyInfo = JourneyInfo.chosenTypeOfBankAccount
  )

  val `Entered Direct Debit Details - Is Account Holder`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDirectDebitDetailsIsAccountHolder,
    journeyInfo = JourneyInfo.enteredDirectDebitDetailsIsAccountHolder
  )

  val `Entered Direct Debit Details - Is Not Account Holder`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.enteredDirectDebitDetailsNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDirectDebitDetailsIsNotAccountHolder
  )

  val `Confirmed Direct Debit Details`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.confirmedDirectDebitDetails,
    journeyInfo = JourneyInfo.confirmedDirectDebitDetails
  )

  val `Agreed Terms and Conditions`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.agreedTermsAndConditions,
    journeyInfo = JourneyInfo.agreedTermsAndConditions
  )

  val `Arrangement Submitted - with upfront payment`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithUpfrontPayment
  )

  val `Arrangement Submitted - No upfront payment`: String = TdJsonBodies.createJourneyJson(
    stageInfo   = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementNoUpfrontPayment
  )

}
