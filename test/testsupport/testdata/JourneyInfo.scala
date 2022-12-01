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
import essttp.rootmodel.{DayOfMonth, TaxRegime}
import uk.gov.hmrc.crypto.Encrypter

object JourneyInfo {
  type JourneyInfoAsJson = String

  /** Represents small bits of json that get added to the journey at each stage **/
  def taxId(taxReference: String): JourneyInfoAsJson = TdJsonBodies.taxIdJourneyInfo(taxReference)
  def eligibilityCheckEligible(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(encrypter = encrypter, taxRegime = taxRegime)
  def ineligibleHasRls(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress, taxRegime, encrypter)
  def ineligibleMarkedAsInsolvent(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleMarkedAsInsolvent, taxRegime, encrypter)
  def ineligibleMinDebt(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsLessThanMinDebtAllowance, taxRegime, encrypter)
  def ineligibleMaxDebt(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsMoreThanMaxDebtAllowance, taxRegime, encrypter)
  def ineligibleDisallowedCharge(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleDisallowedChargeLockTypes, taxRegime, encrypter)
  def ineligibleExistingTtp(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExistingTTP, taxRegime, encrypter)
  def ineligibleMaxDebtAge(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExceedsMaxDebtAge, taxRegime, encrypter)
  def ineligibleChargeType(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleEligibleChargeType, taxRegime, encrypter)
  def ineligibleMissingFiledReturns(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleMissingFiledReturns, taxRegime, encrypter)
  def ineligibleNoDueDatesReached(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleNoDueDatesReached, taxRegime, encrypter)
  def multipleIneligibleReasons(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress.copy(markedAsInsolvent = true), taxRegime, encrypter)
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
  def arrangementSubmitted(taxRegime: TaxRegime): JourneyInfoAsJson = TdJsonBodies.arrangementResponseJourneyInfo(taxRegime)
  /** * **/

  /** accumulation of journey info, in essence it's up to stage X */
  val started: List[JourneyInfoAsJson] = List.empty
  def taxIdDetermined(taxReference: String = "864FZ00049"): List[JourneyInfoAsJson] = taxId(taxReference) :: started

  def eligibilityCheckedEligible(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    eligibilityCheckEligible(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleHasRls(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleHasRls(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedMarkedAsInsolvent(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMarkedAsInsolvent(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMinDebt(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMinDebt(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMaxDebt(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMaxDebt(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleDisallowedCharge(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleDisallowedCharge(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleExistingTtp(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleExistingTtp(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMaxDebtAge(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMaxDebtAge(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleChargeType(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleChargeType(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMissingFiledReturns(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMissingFiledReturns(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleNoDueDatesReached(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleNoDueDatesReached(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMultipleReasons(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    multipleIneligibleReasons(taxRegime, encrypter) :: taxIdDetermined()

  def answeredCanPayUpfrontYes(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    canPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def answeredCanPayUpfrontNo(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def answeredUpfrontPaymentAmount(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    upfrontPaymentAmount :: answeredCanPayUpfrontYes(taxRegime, encrypter)

  def retrievedExtremeDates(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    extremeDates :: upfrontPaymentAnswers :: eligibilityCheckedEligible(taxRegime, encrypter)

  def retrievedExtremeDatesNoUpfrontPayment(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    extremeDates :: upfrontPaymentAnswersNoUpfrontPayment :: eligibilityCheckedEligible(taxRegime, encrypter)

  def retrievedAffordabilityResult(minimumInstalmentAmount: Int = 29997, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableResult(minimumInstalmentAmount) :: retrievedExtremeDates(taxRegime, encrypter)

  def retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount: Int = 29997, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableResult(minimumInstalmentAmount) :: retrievedExtremeDatesNoUpfrontPayment(taxRegime, encrypter)

  def enteredMonthlyPaymentAmount(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    monthlyPaymentAmount :: retrievedAffordabilityResult(taxRegime = taxRegime, encrypter = encrypter)

  def enteredDayOfMonth(day: DayOfMonth, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    dayOfMonth(day) :: enteredMonthlyPaymentAmount(taxRegime, encrypter)

  def retrievedStartDates(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    startDates :: enteredDayOfMonth(TdAll.dayOfMonth(), taxRegime, encrypter)

  def retrievedAffordableQuotes(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableQuotes :: retrievedStartDates(taxRegime, encrypter)

  def chosenPaymentPlan(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    selectedPlan :: retrievedAffordableQuotes(taxRegime, encrypter)

  def hasCheckedPaymentPlan(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    chosenPaymentPlan(taxRegime, encrypter)

  def enteredDetailsAboutBankAccountBusiness(isAccountHolder: Boolean, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    detailsAboutBankAccountBusiness(isAccountHolder) :: chosenPaymentPlan(taxRegime, encrypter)

  def enteredDetailsAboutBankAccountPersonal(isAccountHolder: Boolean, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    detailsAboutBankAccountPersonal(isAccountHolder) :: chosenPaymentPlan(taxRegime, encrypter)

  def enteredDirectDebitDetails(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    directDebitDetails(encrypter) :: enteredDetailsAboutBankAccountBusiness(isAccountHolder = true, taxRegime, encrypter)

  def confirmedDirectDebitDetails(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    enteredDirectDebitDetails(taxRegime, encrypter)

  def agreedTermsAndConditions(isEmailAddressRequired: Boolean, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailAddressRequired(isEmailAddressRequired) :: confirmedDirectDebitDetails(taxRegime, encrypter)

  def selectedEmailToBeVerified(email: String, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailToBeVerified(email, encrypter) :: agreedTermsAndConditions(isEmailAddressRequired = true, taxRegime, encrypter)

  def emailVerificationComplete(email: String, status: EmailVerificationStatus, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailVerificationStatus(status) :: emailVerificationAnswersEmailRequired(email, status, encrypter) :: selectedEmailToBeVerified(email, taxRegime, encrypter)

  def emailVerificationAnswers(isEmailAddressRequired: Boolean, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailVerificationAnswersNoEmailRequired :: agreedTermsAndConditions(isEmailAddressRequired, taxRegime, encrypter)

  def submittedArrangementWithUpfrontPayment(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    arrangementSubmitted(taxRegime) :: emailVerificationAnswers(isEmailAddressRequired = false, taxRegime, encrypter)

  //used in final page test
  def submittedArrangementNoUpfrontPayment(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    arrangementSubmitted(taxRegime) :: emailVerificationAnswersNoEmailRequired :: emailAddressRequired(isEmailAddressRequired = false) :: directDebitDetails(encrypter) ::
      detailsAboutBankAccountBusiness(isAccountHolder = true) :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth() :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def submittedArrangementWithEmailParams(
      email:     String,
      taxRegime: TaxRegime,
      encrypter: Encrypter
  ): List[JourneyInfoAsJson] =
    arrangementSubmitted(taxRegime) :: emailVerificationAnswersEmailRequired(email, EmailVerificationStatus.Verified, encrypter) ::
      emailAddressRequired(isEmailAddressRequired = true) :: directDebitDetails(encrypter) ::
      detailsAboutBankAccountBusiness(isAccountHolder = true) :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth() :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

}
