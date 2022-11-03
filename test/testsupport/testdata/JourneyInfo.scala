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
import essttp.rootmodel.DayOfMonth
import uk.gov.hmrc.crypto.Encrypter

object JourneyInfo {
  type JourneyInfoAsJson = String

  /** Represents small bits of json that get added to the journey at each stage **/
  def taxId(taxReference: String): JourneyInfoAsJson = TdJsonBodies.taxIdJourneyInfo(taxReference)
  def eligibilityCheckEligible(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(encrypter = encrypter)
  def ineligibleHasRls(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress, encrypter)
  def ineligibleMarkedAsInsolvent(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleMarkedAsInsolvent, encrypter)
  def ineligibleMinDebt(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsLessThanMinDebtAllowance, encrypter)
  def ineligibleMaxDebt(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsMoreThanMaxDebtAllowance, encrypter)
  def ineligibleDisallowedCharge(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleDisallowedChargeLockTypes, encrypter)
  def ineligibleExistingTtp(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExistingTTP, encrypter)
  def ineligibleMaxDebtAge(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExceedsMaxDebtAge, encrypter)
  def ineligibleChargeType(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleEligibleChargeType, encrypter)
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
  def dayOfMonth(dayOfMonth: DayOfMonth = TdAll.dayOfMonth()): JourneyInfoAsJson = TdJsonBodies.dayOfMonthJourneyInfo(dayOfMonth)
  val startDates: JourneyInfoAsJson = TdJsonBodies.startDatesJourneyInfo
  val affordableQuotes: JourneyInfoAsJson = TdJsonBodies.affordableQuotesJourneyInfo
  val selectedPlan: JourneyInfoAsJson = TdJsonBodies.selectedPlanJourneyInfo
  def detailsAboutBankAccountBusiness(isAccountHolder: Boolean): JourneyInfoAsJson = TdJsonBodies.detailsAboutBankAccountJourneyInfo("Business", isAccountHolder)
  def detailsAboutBankAccountPersonal(isAccountHolder: Boolean): JourneyInfoAsJson = TdJsonBodies.detailsAboutBankAccountJourneyInfo("Personal", isAccountHolder)
  def directDebitDetails(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.directDebitDetailsJourneyInfo(encrypter)
  def directDebitDetailsNotAccountHolder(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.directDebitDetailsJourneyInfo(encrypter)
  def emailAddressRequired(isEmailAddressRequired: Boolean): JourneyInfoAsJson = TdJsonBodies.isEmailAddressRequiredJourneyInfo(isEmailAddressRequired)
  def emailToBeVerified(email: String, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.emailAddressSelectedToBeVerified(email, encrypter)
  def emailVerificationStatus(status: EmailVerificationStatus): JourneyInfoAsJson = TdJsonBodies.emailVerificationStatus(status)
  def emailVerificationAnswersNoEmailRequired: JourneyInfoAsJson = TdJsonBodies.emailVerificationAnswersNoEmailJourney
  def emailVerificationAnswersEmailRequired(email: String, status: EmailVerificationStatus, encrypter: Encrypter): JourneyInfoAsJson =
    TdJsonBodies.emailVerificationAnswersEmailRequired(email, status, encrypter)
  val arrangementSubmitted: JourneyInfoAsJson = TdJsonBodies.arrangementResponseJourneyInfo()
  /** * **/

  /** accumulation of journey info, in essence it's up to stage X */
  val started: List[JourneyInfoAsJson] = List.empty
  def taxIdDetermined(taxReference: String = "864FZ00049"): List[JourneyInfoAsJson] = taxId(taxReference) :: started

  def eligibilityCheckedEligible(encrypter: Encrypter): List[JourneyInfoAsJson] =
    eligibilityCheckEligible(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleHasRls(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleHasRls(encrypter) :: taxIdDetermined()

  def eligibilityCheckedMarkedAsInsolvent(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMarkedAsInsolvent(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMinDebt(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMinDebt(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMaxDebt(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMaxDebt(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleDisallowedCharge(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleDisallowedCharge(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleExistingTtp(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleExistingTtp(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMaxDebtAge(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMaxDebtAge(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleChargeType(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleChargeType(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMissingFiledReturns(encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMissingFiledReturns(encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMultipleReasons(encrypter: Encrypter): List[JourneyInfoAsJson] =
    multipleIneligibleReasons(encrypter) :: taxIdDetermined()

  def answeredCanPayUpfrontYes(encrypter: Encrypter): List[JourneyInfoAsJson] =
    canPayUpfront :: eligibilityCheckedEligible(encrypter)

  def answeredCanPayUpfrontNo(encrypter: Encrypter): List[JourneyInfoAsJson] =
    cannotPayUpfront :: eligibilityCheckedEligible(encrypter)

  def answeredUpfrontPaymentAmount(encrypter: Encrypter): List[JourneyInfoAsJson] =
    upfrontPaymentAmount :: answeredCanPayUpfrontYes(encrypter)

  def retrievedExtremeDates(encrypter: Encrypter): List[JourneyInfoAsJson] =
    extremeDates :: upfrontPaymentAnswers :: eligibilityCheckedEligible(encrypter)

  def retrievedExtremeDatesNoUpfrontPayment(encrypter: Encrypter): List[JourneyInfoAsJson] =
    extremeDates :: upfrontPaymentAnswersNoUpfrontPayment :: eligibilityCheckedEligible(encrypter)

  def retrievedAffordabilityResult(minimumInstalmentAmount: Int = 29997, encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableResult(minimumInstalmentAmount) :: retrievedExtremeDates(encrypter)

  def retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount: Int = 29997, encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableResult(minimumInstalmentAmount) :: retrievedExtremeDatesNoUpfrontPayment(encrypter)

  def enteredMonthlyPaymentAmount(encrypter: Encrypter): List[JourneyInfoAsJson] =
    monthlyPaymentAmount :: retrievedAffordabilityResult(encrypter = encrypter)

  def enteredDayOfMonth(day: DayOfMonth, encrypter: Encrypter): List[JourneyInfoAsJson] =
    dayOfMonth(day) :: enteredMonthlyPaymentAmount(encrypter)

  def retrievedStartDates(encrypter: Encrypter): List[JourneyInfoAsJson] =
    startDates :: enteredDayOfMonth(TdAll.dayOfMonth(), encrypter)

  def retrievedAffordableQuotes(encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableQuotes :: retrievedStartDates(encrypter)

  def chosenPaymentPlan(encrypter: Encrypter): List[JourneyInfoAsJson] =
    selectedPlan :: retrievedAffordableQuotes(encrypter)

  def hasCheckedPaymentPlan(encrypter: Encrypter): List[JourneyInfoAsJson] =
    chosenPaymentPlan(encrypter)

  def enteredDetailsAboutBankAccountBusiness(isAccountHolder: Boolean, encrypter: Encrypter): List[JourneyInfoAsJson] =
    detailsAboutBankAccountBusiness(isAccountHolder) :: chosenPaymentPlan(encrypter)

  def enteredDetailsAboutBankAccountPersonal(isAccountHolder: Boolean, encrypter: Encrypter): List[JourneyInfoAsJson] =
    detailsAboutBankAccountPersonal(isAccountHolder) :: chosenPaymentPlan(encrypter)

  def enteredDirectDebitDetails(encrypter: Encrypter): List[JourneyInfoAsJson] =
    directDebitDetails(encrypter) :: enteredDetailsAboutBankAccountBusiness(isAccountHolder = true, encrypter)

  def confirmedDirectDebitDetails(encrypter: Encrypter): List[JourneyInfoAsJson] =
    enteredDirectDebitDetails(encrypter)

  def agreedTermsAndConditions(isEmailAddressRequired: Boolean, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailAddressRequired(isEmailAddressRequired) :: confirmedDirectDebitDetails(encrypter)

  def selectedEmailToBeVerified(email: String, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailToBeVerified(email, encrypter) :: agreedTermsAndConditions(isEmailAddressRequired = true, encrypter)

  def emailVerificationComplete(email: String, status: EmailVerificationStatus, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailVerificationStatus(status) :: emailVerificationAnswersEmailRequired(email, status, encrypter) :: selectedEmailToBeVerified(email, encrypter)

  def emailVerificationAnswers(isEmailAddressRequired: Boolean, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailVerificationAnswersNoEmailRequired :: agreedTermsAndConditions(isEmailAddressRequired, encrypter)

  def submittedArrangementWithUpfrontPayment(encrypter: Encrypter): List[JourneyInfoAsJson] =
    arrangementSubmitted :: emailVerificationAnswers(isEmailAddressRequired = false, encrypter)

  //used in final page test
  def submittedArrangementNoUpfrontPayment(encrypter: Encrypter): List[JourneyInfoAsJson] =
    arrangementSubmitted :: emailVerificationAnswersNoEmailRequired :: emailAddressRequired(isEmailAddressRequired = false) :: directDebitDetails(encrypter) ::
      detailsAboutBankAccountBusiness(isAccountHolder = true) :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth() :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(encrypter)

}
