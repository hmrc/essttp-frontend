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

import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Origin, Origins, WhyCannotPayInFullAnswers}
import essttp.rootmodel.CannotPayReason.{ChangeToPersonalCircumstances, NoMoneySetAside}
import essttp.rootmodel.{CannotPayReason, DayOfMonth}
import paymentsEmailVerification.models.EmailVerificationResult
import uk.gov.hmrc.crypto.Encrypter

object JourneyJsonTemplates {

  def Started(origin: Origin = Origins.Epaye.Bta): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.started,
    journeyInfo = JourneyInfo.started,
    origin = origin
  )

  def `Computed Tax Id`(origin: Origin = Origins.Epaye.Bta, taxReference: String = "864FZ00049"): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.computedTaxId,
      journeyInfo = JourneyInfo.taxIdDetermined(taxReference, origin.taxRegime),
      origin = origin
    )

  def `Eligibility Checked - Eligible`(origin: Origin = Origins.Epaye.Bta)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedEligible,
      journeyInfo = JourneyInfo.eligibilityCheckedEligible(origin.taxRegime, encrypter),
      origin
    )

  def `Eligibility Checked - Eligible - No HMRC-MTD-IT enrolment`(
    origin: Origin = Origins.Sa.Bta
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedEligible,
    journeyInfo = JourneyInfo.eligibilityCheckedNoMtdEnrolment(origin.taxRegime, encrypter),
    origin
  )

  def `Eligibility Checked - Eligible - NoInterestBearingCharge`(origin: Origin = Origins.Epaye.Bta)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedEligible,
    journeyInfo = JourneyInfo
      .eligibilityCheckedEligible(origin.taxRegime, encrypter, maybeChargeIsInterestBearingCharge = Some(false)),
    origin
  )

  def `Eligibility Checked - Eligible- ddInProgress`(origin: Origin = Origins.Epaye.Bta)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedEligible,
    journeyInfo = JourneyInfo.eligibilityCheckedEligible(origin.taxRegime, encrypter, maybeDdInProgress = Some(true)),
    origin
  )

  def `Eligibility Checked - Ineligible - HasRlsOnAddress`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasRls(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - MarkedAsInsolvent`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedMarkedAsInsolvent(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMinDebt(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebt(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleDisallowedCharge(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - ExistingTTP`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleExistingTtp(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebtAge(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - BeforeMaxAccountingDate`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMaxDebtAge(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - EligibleChargeType`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleChargeType(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - MissingFiledReturns`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMissingFiledReturns(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - NoDueDatesReached`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleNoDueDatesReached(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - HasInvalidInterestSignals`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasInvalidInterestSignals(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - HasInvalidInterestSignalsCESA`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasInvalidInterestSignalsCESA(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo =
      JourneyInfo.eligibilityCheckedIneligibleDmSpecialOfficeProcessingRequired(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - CannotFindLockReason`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleCannotFindLockReason(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - CreditsNotAllowed`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleCreditsNotAllowed(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleIsMoreThanMaxPaymentReference(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - ChargesBeforeMaxAccountingDate`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedChargesBeforeMaxAccountingDate(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - HasDisguisedRemuneration`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasDisguisedRemuneration(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - HasCapacitor`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.eligibilityCheckedIneligible,
      journeyInfo = JourneyInfo.eligibilityCheckedIneligibleHasCapacitor(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequiredCDCS`(origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo =
      JourneyInfo.eligibilityCheckedIneligibleDmSpecialOfficeProcessingRequiredCDCS(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - MultipleReasons`(origin: Origin = Origins.Epaye.Bta)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMultipleReasons(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - MultipleReasons - debt too low and old`(origin: Origin = Origins.Epaye.Bta)(
    using encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleMultipleReasonsDebtTooLowAndOld(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Eligibility Checked - Ineligible - isAnMtdCustomer`(origin: Origin = Origins.Sa.Bta)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo = JourneyInfo.eligibilityCheckedIneligibleIsAnMtdCustomer(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Why Cannot Pay in Full - Not Required`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.whyCannotPayInFullNotRequired,
      journeyInfo = JourneyInfo.whyCannotPayInFullNotRequired(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Why Cannot Pay in Full - Required`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.whyCannotPayInFullNotRequired,
      journeyInfo = JourneyInfo.whyCannotPayInFullRequired(origin.taxRegime, encrypter),
      origin = origin
    )
  def `Eligibility Checked - Ineligible - dmSpecialOfficeProcessingRequiredCESA`(origin: Origin = Origins.Sa.Bta)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.eligibilityCheckedIneligible,
    journeyInfo =
      JourneyInfo.eligibilityCheckedIneligibleDmSpecialOfficeProcessingRequiredCESA(origin.taxRegime, encrypter),
    origin = origin
  )

  def `Answered Can Pay Upfront - Yes`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.answeredCanPayUpfrontYes,
      journeyInfo = JourneyInfo.answeredCanPayUpfrontYes(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Answered Can Pay Upfront - No`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.answeredCanPayUpfrontNo,
      journeyInfo = JourneyInfo.answeredCanPayUpfrontNo(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Entered Upfront payment amount`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.enteredUpfrontPaymentAmount,
      journeyInfo = JourneyInfo.answeredUpfrontPaymentAmount(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Retrieved Extreme Dates Response`(origin: Origin, affordabilityEnabled: Boolean = false,
                                         whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(Set(CannotPayReason.WaitingForRefund, CannotPayReason.NoMoneySetAside)))(implicit encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo            = StageInfo.retrievedExtremeDates,
      journeyInfo          = JourneyInfo.retrievedExtremeDates(origin.taxRegime, encrypter, whyCannotPayInFullAnswers = whyCannotPayInFullAnswers),
      origin               = origin,
      affordabilityEnabled = affordabilityEnabled
    )

  def `Retrieved Affordability`(
    origin:                  Origin,
    minimumInstalmentAmount: Int = 29997,
    affordabilityEnabled:    Boolean = false
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.retrievedAffordabilityResult,
    journeyInfo = JourneyInfo.retrievedAffordabilityResult(minimumInstalmentAmount, origin.taxRegime, encrypter),
    origin = origin,
    affordabilityEnabled = affordabilityEnabled
  )

  def `Retrieved Affordability no upfront payment`(origin: Origin, minimumInstalmentAmount: Int = 29997)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.retrievedAffordabilityResult,
    journeyInfo =
      JourneyInfo.retrievedAffordabilityResultNoUpfrontPayment(minimumInstalmentAmount, origin.taxRegime, encrypter),
    origin = origin
  )

  def `Obtained Can Pay Within 6 months - not required`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.obtainedCanPayWithinSixMonthsNotRequired,
      journeyInfo = JourneyInfo.obtainedCanPayWithinSixMonthsNotRequired(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Obtained Can Pay Within 6 months - yes`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.obtainedCanPayWithinSixMonthsNotRequired,
      journeyInfo = JourneyInfo.obtainedCanPayWithinSixMonthsYes(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Obtained Can Pay Within 6 months - no`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.obtainedCanPayWithinSixMonthsNotRequired,
      journeyInfo = JourneyInfo.obtainedCanPayWithinSixMonthsNo(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Started PEGA case`(
    origin:                    Origin,
    whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(
      Set(CannotPayReason.WaitingForRefund, CannotPayReason.NoMoneySetAside)
    ),
    pegaCaseId:                Option[String] = None
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.startedPegaCase,
    journeyInfo =
      JourneyInfo.startedPegaCase(origin.taxRegime, encrypter, whyCannotPayInFullAnswers = whyCannotPayInFullAnswers),
    origin = origin,
    pegaCaseId = pegaCaseId
  )

  def `Entered Monthly Payment Amount`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.enteredMonthlyPaymentAmount,
      journeyInfo = JourneyInfo.enteredMonthlyPaymentAmount(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Entered Day of Month`(dayOfMonth: DayOfMonth, origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.enteredDayOfMonth,
      journeyInfo = JourneyInfo.enteredDayOfMonth(dayOfMonth, origin.taxRegime, encrypter),
      origin = origin
    )

  def `Retrieved Start Dates`(origin: Origin, eligibilityMinPlanLength: Int = 1, eligibilityMaxPlanLength: Int = 12)(
    using encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.retrievedStartDates,
    journeyInfo = JourneyInfo.retrievedStartDates(
      origin.taxRegime,
      encrypter,
      eligibilityMinPlanLength = eligibilityMinPlanLength,
      eligibilityMaxPlanLength = eligibilityMaxPlanLength
    ),
    origin = origin
  )

  def `Retrieved Affordable Quotes`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.retrievedAffordableQuotes,
      journeyInfo = JourneyInfo.retrievedAffordableQuotes(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Chosen Payment Plan`(
    upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}""",
    regimeDigitalCorrespondence:    Boolean = true,
    origin:                         Origin,
    selectedPlanJourneyInfo:        String = TdJsonBodies.selectedPlanTwoMonthsJourneyInfo
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.chosenPaymentPlan,
    journeyInfo = List(
      TdJsonBodies.taxIdJourneyInfo(taxRegime = origin.taxRegime),
      TdJsonBodies.eligibilityCheckJourneyInfo(
        TdAll.eligibleEligibilityPass,
        TdAll.eligibleEligibilityRules,
        origin.taxRegime,
        encrypter,
        regimeDigitalCorrespondence
      ),
      TdJsonBodies.upfrontPaymentAnswersJourneyInfo(upfrontPaymentAmountJsonString),
      TdJsonBodies.whyCannotPayInFull(
        WhyCannotPayInFullAnswers.WhyCannotPayInFull(Set(ChangeToPersonalCircumstances, NoMoneySetAside))
      ),
      TdJsonBodies.canPayWithinSixMonthsJourneyInfo(CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(value = true)),
      TdJsonBodies.extremeDatesJourneyInfo(),
      TdJsonBodies.affordabilityResultJourneyInfo(),
      TdJsonBodies.monthlyPaymentAmountJourneyInfo,
      TdJsonBodies.dayOfMonthJourneyInfo(TdAll.dayOfMonth()),
      TdJsonBodies.startDatesJourneyInfo,
      TdJsonBodies.affordableQuotesJourneyInfo,
      selectedPlanJourneyInfo
    ),
    origin = origin
  )

  def `Has Checked Payment Plan - No Affordability`(
    origin:                    Origin,
    eligibilityMinPlanLength:  Int = 1,
    eligibilityMaxPlanLength:  Int = 12,
    whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.AnswerNotRequired,
    affordabilityEnabled:      Boolean = false
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.hasCheckedPaymentPlan,
    journeyInfo = JourneyInfo.hasCheckedPaymentPlan(
      withAffordability = false,
      origin.taxRegime,
      encrypter,
      eligibilityMinPlanLength = eligibilityMinPlanLength,
      eligibilityMaxPlanLength = eligibilityMaxPlanLength,
      whyCannotPayInFullAnswers = whyCannotPayInFullAnswers
    ),
    origin = origin,
    affordabilityEnabled = affordabilityEnabled
  )

  def `Has Checked Payment Plan - With Affordability`(
    origin:                    Origin,
    eligibilityMinPlanLength:  Int = 1,
    eligibilityMaxPlanLength:  Int = 12,
    whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.AnswerNotRequired,
    affordabilityEnabled:      Boolean = false
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.hasCheckedPaymentPlan,
    journeyInfo = JourneyInfo.hasCheckedPaymentPlan(
      withAffordability = true,
      origin.taxRegime,
      encrypter,
      eligibilityMinPlanLength = eligibilityMinPlanLength,
      eligibilityMaxPlanLength = eligibilityMaxPlanLength,
      whyCannotPayInFullAnswers = whyCannotPayInFullAnswers
    ),
    origin = origin,
    affordabilityEnabled = affordabilityEnabled
  )

  def `Entered Can Set Up Direct Debit`(isAccountHolder: Boolean, origin: Origin = Origins.Epaye.Bta)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo =
      if (isAccountHolder) StageInfo.enteredCanSetUpDirectDebitIsAccountHolder
      else StageInfo.enteredCanSetUpDirectDebitIsNotAccountHolder,
    journeyInfo = JourneyInfo.enteredDetailsAboutBankAccount(isAccountHolder, origin.taxRegime, encrypter),
    origin = origin
  )

  def `Entered Direct Debit Details`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.enteredDirectDebitDetails,
      journeyInfo = JourneyInfo.enteredDirectDebitDetails(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Confirmed Direct Debit Details`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.confirmedDirectDebitDetails,
      journeyInfo = JourneyInfo.confirmedDirectDebitDetails(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Confirmed Direct Debit Details - regimeDigitalCorrespondence flag`(
    origin:                      Origin,
    regimeDigitalCorrespondence: Boolean
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.confirmedDirectDebitDetails,
    journeyInfo = JourneyInfo
      .confirmedDdDetailsWithRegimeDigitalCorrespondance(regimeDigitalCorrespondence, origin.taxRegime, encrypter),
    origin = origin
  )

  def `Agreed Terms and Conditions`(
    isEmailAddresRequired:     Boolean,
    origin:                    Origin,
    etmpEmail:                 Option[String],
    withAffordability:         Boolean = false,
    whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.AnswerNotRequired
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo =
      if (isEmailAddresRequired) StageInfo.agreedTermsAndConditionsEmailAddressRequired
      else StageInfo.agreedTermsAndConditionsEmailAddressNotRequired,
    journeyInfo = JourneyInfo.agreedTermsAndConditions(
      isEmailAddresRequired,
      origin.taxRegime,
      encrypter,
      etmpEmail,
      withAffordability,
      whyCannotPayInFullAnswers
    ),
    origin = origin
  )

  def `Agreed Terms and Conditions - padded account number`(
    isEmailAddresRequired: Boolean,
    origin:                Origin,
    etmpEmail:             Option[String]
  )(using encrypter: Encrypter): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.agreedTermsAndConditionsEmailAddressNotRequired,
    journeyInfo = JourneyInfo.`agreedTermsAndConditions - padded account number`(
      isEmailAddresRequired,
      origin.taxRegime,
      encrypter,
      etmpEmail
    ),
    origin = origin
  )

  def `Selected email to be verified`(email: String, origin: Origin, etmpEmail: Option[String] = Some(TdAll.etmpEmail))(
    using encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.selectedEmailToBeVerified,
    journeyInfo = JourneyInfo.selectedEmailToBeVerified(email, origin.taxRegime, encrypter, etmpEmail),
    origin = origin
  )

  def `Email verification complete`(email: String, status: EmailVerificationResult, origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = status match {
      case EmailVerificationResult.Verified => StageInfo.emailVerificationSuccess
      case EmailVerificationResult.Locked   => StageInfo.emailVerificationLocked
    },
    journeyInfo = JourneyInfo.emailVerificationComplete(email, status, origin.taxRegime, encrypter),
    origin = origin
  )

  def `Arrangement Submitted - with upfront payment`(origin: Origin, withAffordability: Boolean = false)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithUpfrontPayment(origin.taxRegime, encrypter, withAffordability),
    origin = origin
  )

  def `Arrangement Submitted - No upfront payment`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.submittedArrangement,
      journeyInfo = JourneyInfo.submittedArrangementNoUpfrontPayment(origin.taxRegime, encrypter),
      origin = origin
    )

  def `Arrangement Submitted - with upfront payment and email`(email: String, origin: Origin)(using
    encrypter: Encrypter
  ): String = TdJsonBodies.createJourneyJson(
    stageInfo = StageInfo.submittedArrangement,
    journeyInfo = JourneyInfo.submittedArrangementWithEmailParams(email, origin.taxRegime, encrypter),
    origin = origin
  )

  def `Arrangement Submitted - padded account number`(origin: Origin)(using encrypter: Encrypter): String =
    TdJsonBodies.createJourneyJson(
      stageInfo = StageInfo.submittedArrangement,
      journeyInfo = JourneyInfo.submittedArrangementPaddedAccountNumber(origin.taxRegime, encrypter),
      origin = origin
    )
}
