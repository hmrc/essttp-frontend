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

object JourneyInfo {
  type JourneyInfoAsJson = String

  /** Represents small bits of json that get added to the journey at each stage **/
  val taxId: JourneyInfoAsJson = TdJsonBodies.taxIdJourneyInfo()
  val eligibilityCheckEligible: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo()
  val ineligibleHasRls: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleOverallEligibilityStatus, TdAll.notEligibleHasRlsOnAddress)
  val ineligibleMaxDebt: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleOverallEligibilityStatus, TdAll.notEligibleIsMoreThanMaxDebtAllowance)
  val ineligibleExistingTtp: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleOverallEligibilityStatus, TdAll.notEligibleExistingTTP)
  val ineligibleMaxDebtAge: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleOverallEligibilityStatus, TdAll.notEligibleExceedsMaxDebtAge)
  val ineligibleMissingFiledReturns: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleOverallEligibilityStatus, TdAll.notEligibleMissingFiledReturns)
  val multipleIneligibleReasons: JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleOverallEligibilityStatus, TdAll.notEligibleHasRlsOnAddress.copy(markedAsInsolvent = true))
  val canPayUpfront: JourneyInfoAsJson = TdJsonBodies.canPayUpfrontJourneyInfo(true)
  val cannotPayUpfront: JourneyInfoAsJson = TdJsonBodies.canPayUpfrontJourneyInfo(false)
  val upfrontPaymentAmount: JourneyInfoAsJson = TdJsonBodies.upfrontPaymentAmountJourneyInfo(TdAll.upfrontPaymentAmount)
  val upfrontPaymentAnswers: JourneyInfoAsJson = TdJsonBodies.upfrontPaymentAnswersJourneyInfo()
  val upfrontPaymentAnswersNoUpfrontPayment: JourneyInfoAsJson = TdJsonBodies.upfrontPaymentAnswersJourneyInfo("""{"NoUpfrontPayment": {}}""")
  val extremeDates: JourneyInfoAsJson = TdJsonBodies.extremeDatesJourneyInfo()
  def affordableResult(minimumInstalmentAmount: Int = 29997): JourneyInfoAsJson = TdJsonBodies.affordabilityResultJourneyInfo(minimumInstalmentAmount)
  val monthlyPaymentAmount: JourneyInfoAsJson = TdJsonBodies.monthlyPaymentAmountJourneyInfo
  val dayOfMonth: JourneyInfoAsJson = TdJsonBodies.dayOfMonthJourneyInfo(TdAll.dayOfMonth())
  val startDates: JourneyInfoAsJson = TdJsonBodies.startDatesJourneyInfo
  val affordableQuotes: JourneyInfoAsJson = TdJsonBodies.affordableQuotesJourneyInfo
  val selectedPlan: JourneyInfoAsJson = TdJsonBodies.selectedPlanJourneyInfo
  val typeOfBankAccountBusiness: JourneyInfoAsJson = TdJsonBodies.typeOfBankJourneyInfo()
  val typeOfBankAccountPersonal: JourneyInfoAsJson = TdJsonBodies.typeOfBankJourneyInfo("Personal")
  val directDebitDetails: JourneyInfoAsJson = TdJsonBodies.directDebitDetailsJourneyInfo()
  val directDebitDetailsNotAccountHolder: JourneyInfoAsJson = TdJsonBodies.directDebitDetailsJourneyInfo(false)
  val arrangementSubmitted: JourneyInfoAsJson = TdJsonBodies.arrangementResponseJourneyInfo()
  /** * **/

  /** accumulation of journey info, in essence it's up to stage X */
  val started: List[JourneyInfoAsJson] = List.empty
  val taxIdDetermined: List[JourneyInfoAsJson] = taxId :: started

  val eligibilityCheckedEligible: List[JourneyInfoAsJson] = eligibilityCheckEligible :: taxIdDetermined
  val eligibilityCheckedIneligibleHasRls: List[JourneyInfoAsJson] = ineligibleHasRls :: taxIdDetermined
  val eligibilityCheckedIneligibleMaxDebt: List[JourneyInfoAsJson] = ineligibleMaxDebt :: taxIdDetermined
  val eligibilityCheckedIneligibleExistingTtp: List[JourneyInfoAsJson] = ineligibleExistingTtp :: taxIdDetermined
  val eligibilityCheckedIneligibleMaxDebtAge: List[JourneyInfoAsJson] = ineligibleMaxDebtAge :: taxIdDetermined
  val eligibilityCheckedIneligibleMissingFiledReturns: List[JourneyInfoAsJson] = ineligibleMissingFiledReturns :: taxIdDetermined
  val eligibilityCheckedIneligibleMultipleReasons: List[JourneyInfoAsJson] = multipleIneligibleReasons :: taxIdDetermined
  val answeredCanPayUpfrontYes: List[JourneyInfoAsJson] = canPayUpfront :: eligibilityCheckedEligible
  val answeredCanPayUpfrontNo: List[JourneyInfoAsJson] = cannotPayUpfront :: eligibilityCheckedEligible
  val answeredUpfrontPaymentAmount: List[JourneyInfoAsJson] = upfrontPaymentAmount :: answeredCanPayUpfrontYes
  val retrievedExtremeDates: List[JourneyInfoAsJson] = extremeDates :: upfrontPaymentAnswers :: eligibilityCheckedEligible
  val retrievedExtremeDatesNoUpfrontPayment: List[JourneyInfoAsJson] = extremeDates :: upfrontPaymentAnswersNoUpfrontPayment :: eligibilityCheckedEligible
  def retrievedAffordabilityResult(minimumInstalmentAmount: Int = 29997): List[JourneyInfoAsJson] = affordableResult(minimumInstalmentAmount) :: retrievedExtremeDates
  val enteredMonthlyPaymentAmount: List[JourneyInfoAsJson] = monthlyPaymentAmount :: retrievedAffordabilityResult()
  val enteredDayOfMonth: List[JourneyInfoAsJson] = dayOfMonth :: enteredMonthlyPaymentAmount
  val retrievedStartDates: List[JourneyInfoAsJson] = startDates :: enteredDayOfMonth
  val retrievedAffordableQuotes: List[JourneyInfoAsJson] = affordableQuotes :: retrievedStartDates
  val chosenPaymentPlan: List[JourneyInfoAsJson] = selectedPlan :: retrievedAffordableQuotes
  val hasCheckedPaymentPlan: List[JourneyInfoAsJson] = chosenPaymentPlan
  val chosenTypeOfBankAccountBusiness: List[JourneyInfoAsJson] = typeOfBankAccountBusiness :: chosenPaymentPlan
  val chosenTypeOfBankAccountPersonal: List[JourneyInfoAsJson] = typeOfBankAccountPersonal :: chosenPaymentPlan
  val enteredDirectDebitDetailsIsAccountHolder: List[JourneyInfoAsJson] = directDebitDetails :: chosenTypeOfBankAccountBusiness
  val enteredDirectDebitDetailsIsNotAccountHolder: List[JourneyInfoAsJson] = directDebitDetailsNotAccountHolder :: chosenTypeOfBankAccountBusiness
  val confirmedDirectDebitDetails: List[JourneyInfoAsJson] = enteredDirectDebitDetailsIsAccountHolder
  val agreedTermsAndConditions: List[JourneyInfoAsJson] = confirmedDirectDebitDetails
  val submittedArrangementWithUpfrontPayment: List[JourneyInfoAsJson] = arrangementSubmitted :: confirmedDirectDebitDetails

  //used in final page test
  val submittedArrangementNoUpfrontPayment: List[JourneyInfoAsJson] =
    arrangementSubmitted :: directDebitDetails :: typeOfBankAccountBusiness :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible

}
