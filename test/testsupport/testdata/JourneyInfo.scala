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

import essttp.rootmodel.{DayOfMonth, TaxRegime}
import paymentsEmailVerification.models.EmailVerificationResult
import uk.gov.hmrc.crypto.Encrypter

object JourneyInfo {
  type JourneyInfoAsJson = String

  /** Represents small bits of json that get added to the journey at each stage **/
  def taxId(taxReference: String): JourneyInfoAsJson = TdJsonBodies.taxIdJourneyInfo(taxReference)
  def eligibilityCheckEligible(
      taxRegime:                          TaxRegime,
      encrypter:                          Encrypter,
      regimeDigitalCorrespondence:        Boolean,
      email:                              Option[String],
      maybeChargeIsInterestBearingCharge: Option[Boolean],
      maybeChargeUseChargeReference:      Option[Boolean],
      maybeDdInProgress:                  Option[Boolean],
      eligibilityMinPlanLength:           Int,
      eligibilityMaxPlanLength:           Int

  ): JourneyInfoAsJson =
    TdJsonBodies.eligibilityCheckJourneyInfo(
      encrypter                          = encrypter,
      taxRegime                          = taxRegime,
      regimeDigitalCorrespondence        = regimeDigitalCorrespondence,
      email                              = email,
      maybeChargeIsInterestBearingCharge = maybeChargeIsInterestBearingCharge,
      maybeChargeUseChargeReference      = maybeChargeUseChargeReference,
      maybeDdInProgress                  = maybeDdInProgress,
      eligibilityMinPlanLength           = eligibilityMinPlanLength,
      eligibilityMaxPlanLength           = eligibilityMaxPlanLength
    )

  def ineligibleHasRls(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress, taxRegime, encrypter)
  def ineligibleMarkedAsInsolvent(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleMarkedAsInsolvent, taxRegime, encrypter)
  def ineligibleMinDebt(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsLessThanMinDebtAllowance, taxRegime, encrypter)
  def ineligibleMaxDebt(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsMoreThanMaxDebtAllowance, taxRegime, encrypter)
  def ineligibleDisallowedCharge(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleDisallowedChargeLockTypes, taxRegime, encrypter)
  def ineligibleExistingTtp(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExistingTTP, taxRegime, encrypter)
  def ineligibleMaxDebtAge(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleExceedsMaxDebtAge, taxRegime, encrypter)
  def ineligibleBeforeMaxAccountingDate(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleChargesBeforeMaxAccountingDate, taxRegime, encrypter)
  def ineligibleChargeType(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleEligibleChargeType, taxRegime, encrypter)
  def ineligibleMissingFiledReturns(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleMissingFiledReturns, taxRegime, encrypter)
  def ineligibleNoDueDatesReached(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleNoDueDatesReached, taxRegime, encrypter)
  def ineligibleHasInvalidInterestSignals(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasInvalidInterestSignals, taxRegime, encrypter)
  def ineligibleHasInvalidInterestSignalsCESA(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasInvalidInterestSignalsCESA, taxRegime, encrypter)
  def ineligibleDmSpecialOfficeProcessingRequired(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleDmSpecialOfficeProcessingRequired, taxRegime, encrypter)
  def ineligibleCannotFindLockResponse(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleCannotFindLockReason, taxRegime, encrypter)
  def ineligibleCreditsNotAllowed(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleCreditsNotAllowed, taxRegime, encrypter)
  def ineligibleIsMoreThanMaxPaymentReference(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsMoreThanMaxPaymentReference, taxRegime, encrypter)
  def ineligibleChargesBeforeMaxAccountingDate(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleChargesBeforeMaxAccountingDate, taxRegime, encrypter)
  def ineligibleHasDisguisedRemuneration(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasDisguisedRemuneration, taxRegime, encrypter)
  def ineligibleHasCapacitor(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasCapacitor, taxRegime, encrypter)
  def ineligibleDmSpecialOfficeProcessingRequiredCDCS(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleDmSpecialOfficeProcessingRequiredCDCS, taxRegime, encrypter)
  def ineligibleisAnMtdCustomer(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsAnMtdCustomer, taxRegime, encrypter)
  def multipleIneligibleReasons(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleHasRlsOnAddress.copy(markedAsInsolvent = true), taxRegime, encrypter)
  def multipleIneligibleReasonsDebtTooLowAndOld(taxRegime: TaxRegime, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.eligibilityCheckJourneyInfo(TdAll.notEligibleEligibilityPass, TdAll.notEligibleIsLessThanMinDebtAllowance.copy(chargesOverMaxDebtAge = Some(true)), taxRegime, encrypter)
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
  def directDebitDetailsPaddedAccountNumber(encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.paddedDirectDebitDetailsJourneyInfo(encrypter)
  def emailAddressRequired(isEmailAddressRequired: Boolean): JourneyInfoAsJson = TdJsonBodies.isEmailAddressRequiredJourneyInfo(isEmailAddressRequired)
  def emailToBeVerified(email: String, encrypter: Encrypter): JourneyInfoAsJson = TdJsonBodies.emailAddressSelectedToBeVerified(email, encrypter)
  def emailVerificationResult(result: EmailVerificationResult): JourneyInfoAsJson = TdJsonBodies.emailVerificationResult(result)
  def emailVerificationAnswersNoEmailRequired: JourneyInfoAsJson = TdJsonBodies.emailVerificationAnswersNoEmailJourney
  def emailVerificationAnswersEmailRequired(email: String, status: EmailVerificationResult, encrypter: Encrypter): JourneyInfoAsJson =
    TdJsonBodies.emailVerificationAnswersEmailRequired(email, status, encrypter)
  def arrangementSubmitted(taxRegime: TaxRegime): JourneyInfoAsJson = TdJsonBodies.arrangementResponseJourneyInfo(taxRegime)
  /** * **/

  /** accumulation of journey info, in essence it's up to stage X */
  val started: List[JourneyInfoAsJson] = List.empty
  def taxIdDetermined(taxReference: String = "864FZ00049"): List[JourneyInfoAsJson] = taxId(taxReference) :: started

  def eligibilityCheckedEligible(
      taxRegime:                          TaxRegime,
      encrypter:                          Encrypter,
      regimeDigitalCorrespondence:        Boolean         = true,
      etmpEmail:                          Option[String]  = Some(TdAll.etmpEmail),
      maybeChargeIsInterestBearingCharge: Option[Boolean] = Some(true),
      maybeChargeUseChargeReference:      Option[Boolean] = Some(true),
      maybeDdInProgress:                  Option[Boolean] = None,
      eligibilityMinPlanLength:           Int             = 1,
      eligibilityMaxPlanLength:           Int             = 12
  ): List[JourneyInfoAsJson] =
    eligibilityCheckEligible(
      taxRegime,
      encrypter,
      regimeDigitalCorrespondence,
      etmpEmail,
      maybeChargeIsInterestBearingCharge,
      maybeChargeUseChargeReference,
      maybeDdInProgress,
      eligibilityMinPlanLength,
      eligibilityMaxPlanLength
    ) :: taxIdDetermined()

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

  def eligibilityCheckedIneligibleBeforeMaxAccountingDate(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleBeforeMaxAccountingDate(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleChargeType(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleChargeType(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMissingFiledReturns(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMissingFiledReturns(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleNoDueDatesReached(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleNoDueDatesReached(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleHasInvalidInterestSignals(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleHasInvalidInterestSignals(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleHasInvalidInterestSignalsCESA(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleHasInvalidInterestSignalsCESA(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleDmSpecialOfficeProcessingRequired(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleDmSpecialOfficeProcessingRequired(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleCannotFindLockReason(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleCannotFindLockResponse(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleCreditsNotAllowed(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleCreditsNotAllowed(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleIsMoreThanMaxPaymentReference(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleIsMoreThanMaxPaymentReference(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedChargesBeforeMaxAccountingDate(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleChargesBeforeMaxAccountingDate(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleChargesBeforeMaxAccountingDate(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleMaxDebtAge(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleHasDisguisedRemuneration(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleHasDisguisedRemuneration(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleHasCapacitor(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleHasCapacitor(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleDmSpecialOfficeProcessingRequiredCDCS(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleDmSpecialOfficeProcessingRequiredCDCS(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleIsAnMtdCustomer(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    ineligibleisAnMtdCustomer(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMultipleReasons(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    multipleIneligibleReasons(taxRegime, encrypter) :: taxIdDetermined()

  def eligibilityCheckedIneligibleMultipleReasonsDebtTooLowAndOld(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    multipleIneligibleReasonsDebtTooLowAndOld(taxRegime, encrypter) :: taxIdDetermined()

  def answeredCanPayUpfrontYes(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    canPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def answeredCanPayUpfrontNo(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def answeredUpfrontPaymentAmount(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    upfrontPaymentAmount :: answeredCanPayUpfrontYes(taxRegime, encrypter)

  def retrievedExtremeDates(
      taxRegime:                TaxRegime,
      encrypter:                Encrypter,
      etmpEmail:                Option[String] = Some(TdAll.etmpEmail),
      eligibilityMinPlanLength: Int            = 1,
      eligibilityMaxPlanLength: Int            = 12
  ): List[JourneyInfoAsJson] =
    extremeDates :: upfrontPaymentAnswers :: eligibilityCheckedEligible(
      taxRegime,
      encrypter,
      etmpEmail                = etmpEmail,
      eligibilityMinPlanLength = eligibilityMinPlanLength,
      eligibilityMaxPlanLength = eligibilityMaxPlanLength
    )

  def retrievedExtremeDatesNoUpfrontPayment(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    extremeDates :: upfrontPaymentAnswersNoUpfrontPayment :: eligibilityCheckedEligible(taxRegime, encrypter)

  def retrievedAffordabilityResult(
      minimumInstalmentAmount:  Int            = 29997,
      taxRegime:                TaxRegime,
      encrypter:                Encrypter,
      etmpEmail:                Option[String] = Some(TdAll.etmpEmail),
      eligibilityMinPlanLength: Int            = 1,
      eligibilityMaxPlanLength: Int            = 12
  ): List[JourneyInfoAsJson] =
    affordableResult(minimumInstalmentAmount) :: retrievedExtremeDates(taxRegime, encrypter, etmpEmail, eligibilityMinPlanLength, eligibilityMaxPlanLength)

  def retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount: Int = 29997, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    affordableResult(minimumInstalmentAmount) :: retrievedExtremeDatesNoUpfrontPayment(taxRegime, encrypter)

  def enteredMonthlyPaymentAmount(
      taxRegime:                TaxRegime,
      encrypter:                Encrypter,
      etmpEmail:                Option[String] = Some(TdAll.etmpEmail),
      eligibilityMinPlanLength: Int            = 1,
      eligibilityMaxPlanLength: Int            = 12
  ): List[JourneyInfoAsJson] =
    monthlyPaymentAmount :: retrievedAffordabilityResult(
      taxRegime                = taxRegime,
      encrypter                = encrypter,
      etmpEmail                = etmpEmail,
      eligibilityMinPlanLength = eligibilityMinPlanLength,
      eligibilityMaxPlanLength = eligibilityMaxPlanLength
    )

  def enteredDayOfMonth(
      day:                      DayOfMonth,
      taxRegime:                TaxRegime,
      encrypter:                Encrypter,
      etmpEmail:                Option[String] = Some(TdAll.etmpEmail),
      eligibilityMinPlanLength: Int            = 1,
      eligibilityMaxPlanLength: Int            = 12
  ): List[JourneyInfoAsJson] =
    dayOfMonth(day) :: enteredMonthlyPaymentAmount(taxRegime, encrypter, etmpEmail, eligibilityMinPlanLength, eligibilityMaxPlanLength)

  def retrievedStartDates(
      taxRegime:                TaxRegime,
      encrypter:                Encrypter,
      etmpEmail:                Option[String] = Some(TdAll.etmpEmail),
      eligibilityMinPlanLength: Int            = 1,
      eligibilityMaxPlanLength: Int            = 12
  ): List[JourneyInfoAsJson] =
    startDates :: enteredDayOfMonth(TdAll.dayOfMonth(), taxRegime, encrypter, etmpEmail, eligibilityMinPlanLength, eligibilityMaxPlanLength)

  def retrievedAffordableQuotes(taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    affordableQuotes :: retrievedStartDates(taxRegime, encrypter, etmpEmail)

  def chosenPaymentPlan(taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    selectedPlan :: retrievedAffordableQuotes(taxRegime, encrypter, etmpEmail)

  def hasCheckedPaymentPlan(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    chosenPaymentPlan(taxRegime, encrypter)

  def enteredDetailsAboutBankAccountBusiness(isAccountHolder: Boolean, taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    detailsAboutBankAccountBusiness(isAccountHolder) :: chosenPaymentPlan(taxRegime, encrypter, etmpEmail)

  def enteredDetailsAboutBankAccountPersonal(isAccountHolder: Boolean, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    detailsAboutBankAccountPersonal(isAccountHolder) :: chosenPaymentPlan(taxRegime, encrypter)

  def enteredDirectDebitDetails(taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    directDebitDetails(encrypter) :: enteredDetailsAboutBankAccountBusiness(isAccountHolder = true, taxRegime, encrypter, etmpEmail)

  def enteredDirectDebitDetailsPaddedAccountNumber(taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    directDebitDetailsPaddedAccountNumber(encrypter) :: enteredDetailsAboutBankAccountBusiness(isAccountHolder = true, taxRegime, encrypter, etmpEmail)

  def confirmedDirectDebitDetails(taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    enteredDirectDebitDetails(taxRegime, encrypter, etmpEmail)

  def `confirmedDirectDebitDetails - padded account number`(taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    enteredDirectDebitDetailsPaddedAccountNumber(taxRegime, encrypter, etmpEmail)

  def agreedTermsAndConditions(isEmailAddressRequired: Boolean, taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    emailAddressRequired(isEmailAddressRequired) :: confirmedDirectDebitDetails(taxRegime, encrypter, etmpEmail)

  def `agreedTermsAndConditions - padded account number`(isEmailAddressRequired: Boolean, taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = None): List[JourneyInfoAsJson] =
    emailAddressRequired(isEmailAddressRequired) :: `confirmedDirectDebitDetails - padded account number`(taxRegime, encrypter, etmpEmail)

  def selectedEmailToBeVerified(email: String, taxRegime: TaxRegime, encrypter: Encrypter, etmpEmail: Option[String] = Some(TdAll.etmpEmail)): List[JourneyInfoAsJson] =
    emailToBeVerified(email, encrypter) :: agreedTermsAndConditions(isEmailAddressRequired = true, taxRegime, encrypter, etmpEmail)

  def emailVerificationComplete(email: String, result: EmailVerificationResult, taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    emailVerificationResult(result) :: emailVerificationAnswersEmailRequired(email, result, encrypter) :: selectedEmailToBeVerified(email, taxRegime, encrypter)

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

  def submittedArrangementPaddedAccountNumber(taxRegime: TaxRegime, encrypter: Encrypter): List[JourneyInfoAsJson] =
    arrangementSubmitted(taxRegime) :: emailVerificationAnswersNoEmailRequired :: emailAddressRequired(isEmailAddressRequired = false) :: directDebitDetailsPaddedAccountNumber(encrypter) ::
      detailsAboutBankAccountBusiness(isAccountHolder = true) :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth() :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def submittedArrangementWithEmailParams(
      email:     String,
      taxRegime: TaxRegime,
      encrypter: Encrypter
  ): List[JourneyInfoAsJson] =
    arrangementSubmitted(taxRegime) :: emailVerificationAnswersEmailRequired(email, EmailVerificationResult.Verified, encrypter) ::
      emailAddressRequired(isEmailAddressRequired = true) :: directDebitDetails(encrypter) ::
      detailsAboutBankAccountBusiness(isAccountHolder = true) :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth() :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter)

  def confirmedDdDetailsWithRegimeDigitalCorrespondance(
      regimeDigitalCorrespondence: Boolean,
      taxRegime:                   TaxRegime,
      encrypter:                   Encrypter
  ): List[JourneyInfoAsJson] =
    directDebitDetails(encrypter) ::
      detailsAboutBankAccountBusiness(isAccountHolder = true) :: selectedPlan :: affordableQuotes ::
      upfrontPaymentAnswersNoUpfrontPayment :: extremeDates :: affordableResult() :: monthlyPaymentAmount ::
      dayOfMonth() :: startDates :: cannotPayUpfront :: eligibilityCheckedEligible(taxRegime, encrypter, regimeDigitalCorrespondence = regimeDigitalCorrespondence)
}
