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

import uk.gov.hmrc.crypto.Encrypter

object JourneyInfo {
  type JourneyInfoAsJson = String

  /** Represents small bits of json that get added to the journey at each stage **/
  val taxId: JourneyInfoAsJson = TdJsonBodies.taxIdJourneyInfo()
  def eligibilityCheckEligible(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(encrypter = encrypter)
  def ineligibleHasRls(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress, encrypter)
  def ineligibleMaxDebt(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsMoreThanMaxDebtAllowance, encrypter)
  def ineligibleExistingTtp(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExistingTTP, encrypter)
  def ineligibleMaxDebtAge(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExceedsMaxDebtAge, encrypter)
  def ineligibleMissingFiledReturns(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleMissingFiledReturns, encrypter)
  def multipleIneligibleReasons(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress.copy(markedAsInsolvent = true), encrypter)
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
  def directDebitDetails(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.directDebitDetailsJourneyInfo(isAccountHolder = true, encrypter)
  def directDebitDetailsNotAccountHolder(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.directDebitDetailsJourneyInfo(isAccountHolder = false, encrypter)
  val arrangementSubmitted: JourneyInfoAsJson = TdJsonBodies.arrangementResponseJourneyInfo()
  /** * **/

  /** accumulation of journey info, in essence it's up to stage X */
  val started: List[JourneyInfoAsJson] = List.empty
  val taxIdDetermined: List[JourneyInfoAsJson] = taxId :: started

  def eligibilityCheckedEligible(encrypter: Encrypter): List[JourneyInfoAsJson] = eligibilityCheckEligible(encrypter) :: taxIdDetermined
  def eligibilityCheckedIneligibleHasRls(encrypter: Encrypter): List[JourneyInfoAsJson] = ineligibleHasRls(encrypter) :: taxIdDetermined
  def eligibilityCheckedIneligibleMaxDebt(encrypter: Encrypter): List[JourneyInfoAsJson] = ineligibleMaxDebt(encrypter) :: taxIdDetermined
  def eligibilityCheckedIneligibleExistingTtp(encrypter: Encrypter): List[JourneyInfoAsJson] = ineligibleExistingTtp(encrypter) :: taxIdDetermined
  def eligibilityCheckedIneligibleMaxDebtAge(encrypter: Encrypter): List[JourneyInfoAsJson] = ineligibleMaxDebtAge(encrypter) :: taxIdDetermined
  def eligibilityCheckedIneligibleMissingFiledReturns(encrypter: Encrypter): List[JourneyInfoAsJson] = ineligibleMissingFiledReturns(encrypter) :: taxIdDetermined
  def eligibilityCheckedIneligibleMultipleReasons(encrypter: Encrypter): List[JourneyInfoAsJson] = multipleIneligibleReasons(encrypter) :: taxIdDetermined
  def answeredCanPayUpfrontYes(encrypter: Encrypter): List[JourneyInfoAsJson] = canPayUpfront :: eligibilityCheckedEligible(encrypter)
  def answeredCanPayUpfrontNo(encrypter: Encrypter): List[JourneyInfoAsJson] = cannotPayUpfront :: eligibilityCheckedEligible(encrypter)
  def answeredUpfrontPaymentAmount(encrypter: Encrypter): List[JourneyInfoAsJson] = upfrontPaymentAmount :: answeredCanPayUpfrontYes(encrypter)
  def retrievedExtremeDates(encrypter: Encrypter): List[JourneyInfoAsJson] = extremeDates :: upfrontPaymentAnswers :: eligibilityCheckedEligible(encrypter)
  def retrievedExtremeDatesNoUpfrontPayment(encrypter: Encrypter): List[JourneyInfoAsJson] = extremeDates :: upfrontPaymentAnswersNoUpfrontPayment :: eligibilityCheckedEligible(encrypter)
  def retrievedAffordabilityResult(minimumInstalmentAmount: Int = 29997, encrypter: Encrypter): List[JourneyInfoAsJson] = affordableResult(minimumInstalmentAmount) :: retrievedExtremeDates(encrypter)
  def enteredMonthlyPaymentAmount(encrypter: Encrypter): List[JourneyInfoAsJson] = monthlyPaymentAmount :: retrievedAffordabilityResult(encrypter = encrypter)
  def enteredDayOfMonth(encrypter: Encrypter): List[JourneyInfoAsJson] = dayOfMonth :: enteredMonthlyPaymentAmount(encrypter)
  def retrievedStartDates(encrypter: Encrypter): List[JourneyInfoAsJson] = startDates :: enteredDayOfMonth(encrypter)
  def retrievedAffordableQuotes(encrypter: Encrypter): List[JourneyInfoAsJson] = affordableQuotes :: retrievedStartDates(encrypter)
  def chosenPaymentPlan(encrypter: Encrypter): List[JourneyInfoAsJson] = selectedPlan :: retrievedAffordableQuotes(encrypter)
  def hasCheckedPaymentPlan(encrypter: Encrypter): List[JourneyInfoAsJson] = chosenPaymentPlan(encrypter)
  def chosenTypeOfBankAccountBusiness(encrypter: Encrypter): List[JourneyInfoAsJson] = typeOfBankAccountBusiness :: chosenPaymentPlan(encrypter)
  def chosenTypeOfBankAccountPersonal(encrypter: Encrypter): List[JourneyInfoAsJson] = typeOfBankAccountPersonal :: chosenPaymentPlan(encrypter)
  def enteredDirectDebitDetailsIsAccountHolder(encrypter: Encrypter): List[JourneyInfoAsJson] = directDebitDetails(encrypter) :: chosenTypeOfBankAccountBusiness(encrypter)
  def enteredDirectDebitDetailsIsNotAccountHolder(encrypter: Encrypter): List[JourneyInfoAsJson] = directDebitDetailsNotAccountHolder(encrypter) :: chosenTypeOfBankAccountBusiness(encrypter)
  def confirmedDirectDebitDetails(encrypter: Encrypter): List[JourneyInfoAsJson] = enteredDirectDebitDetailsIsAccountHolder(encrypter)
  def agreedTermsAndConditions(encrypter: Encrypter): List[JourneyInfoAsJson] = confirmedDirectDebitDetails(encrypter)
  def submittedArrangementWithUpfrontPayment(encrypter: Encrypter): List[JourneyInfoAsJson] = arrangementSubmitted :: confirmedDirectDebitDetails(encrypter)

  //used in final page test
  def submittedArrangementNoUpfrontPayment(encrypter: Encrypter): List[JourneyInfoAsJson] =
    arrangementSubmitted :: directDebitDetails(encrypter) :: typeOfBankAccountBusiness :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(encrypter)

}
