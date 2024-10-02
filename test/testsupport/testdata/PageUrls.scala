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

import essttp.rootmodel.TaxRegime
import models.Language

import java.util.Locale

object PageUrls {
  val serviceBaseUrl = "/set-up-a-payment-plan"
  val whichTaxRegimeUrl: String = s"$serviceBaseUrl/which-tax"
  val epayeLandingPageUrl: String = s"$serviceBaseUrl/epaye-payment-plan"
  val vatLandingPageUrl: String = s"$serviceBaseUrl/vat-payment-plan"
  val saLandingPageUrl: String = s"$serviceBaseUrl/sa-payment-plan"
  val siaLandingPageUrl: String = s"$serviceBaseUrl/sia-payment-plan"
  val govUkEpayeStartUrl: String = s"$serviceBaseUrl/govuk/epaye/start"
  val determineTaxIdUrl: String = s"$serviceBaseUrl/determine-taxId"
  val determineEligibilityUrl: String = s"$serviceBaseUrl/determine-eligibility"
  val notEnrolledUrl: String = s"$serviceBaseUrl/not-enrolled"
  val payeNotEligibleUrl: String = s"$serviceBaseUrl/not-eligible-epaye"
  val vatNotEligibleUrl: String = s"$serviceBaseUrl/not-eligible-vat"
  val saNotEligibleUrl: String = s"$serviceBaseUrl/not-eligible-sa"
  val siaNotEligibleUrl: String = s"$serviceBaseUrl/not-eligible-sia"
  val epayeDebtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large-epaye"
  val vatDebtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large-vat"
  val saDebtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large-sa"
  val siaDebtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large-sia"
  val epayeDebtTooSmallUrl: String = s"$serviceBaseUrl/pay-paye-bill-in-full"
  val vatDebtTooSmallUrl: String = s"$serviceBaseUrl/pay-vat-bill-in-full"
  val saDebtTooSmallUrl: String = s"$serviceBaseUrl/pay-self-assessment-tax-bill-in-full"
  val siaDebtTooSmallUrl: String = s"$serviceBaseUrl/pay-simple-assessment-tax-bill-in-full"
  val epayeDebtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old-epaye"
  val vatDebtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old-vat"
  val saDebtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old-sa"
  val siaDebtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old-sia"
  val vatDebtBeforeAccountingDateUrl: String = s"$serviceBaseUrl/debt-before-accounting-date-vat"
  val epayeFileYourReturnUrl: String = s"$serviceBaseUrl/file-your-return"
  val vatFileYourReturnUrl: String = s"$serviceBaseUrl/return-not-filed-vat"
  val saFileYourReturnUrl: String = s"$serviceBaseUrl/return-not-filed-sa"
  val siaFileYourReturnUrl: String = s"$serviceBaseUrl/return-not-filed-sia"
  val epayeAlreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-payment-plan-epaye"
  val vatAlreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-payment-plan-vat"
  val saAlreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-plan-sa"
  val siaAlreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-plan-sia"
  val yourBillIsUrl: String = s"$serviceBaseUrl/your-bill"
  val youAlreadyHaveDirectDebit: String = s"$serviceBaseUrl/you-already-have-a-direct-debit"
  val whyCannotPayInFull: String = s"$serviceBaseUrl/why-are-you-unable-to-pay-in-full"
  val canYouMakeAnUpfrontPaymentUrl: String = s"$serviceBaseUrl/can-you-make-an-upfront-payment"
  val howMuchCanYouPayUpfrontUrl: String = s"$serviceBaseUrl/how-much-can-you-pay-upfront"
  val upfrontPaymentSummaryUrl: String = s"$serviceBaseUrl/upfront-payment-summary"
  def upfrontPaymentSummaryChangeUrl(pageId: String): String = s"$serviceBaseUrl/upfront-payment-summary/change/$pageId"
  val retrievedExtremeDatesUrl: String = s"$serviceBaseUrl/retrieve-extreme-dates"
  val determineAffordabilityUrl: String = s"$serviceBaseUrl/determine-affordability"
  def canPayWithinSixMonthsUrl(regime: TaxRegime, lang: Option[Language]): String =
    s"$serviceBaseUrl/paying-within-six-months?regime=${regime.entryName.toLowerCase(Locale.UK)}${lang.fold("")("&" + _.code)}"
  val startPegaCaseUrl: String = s"$serviceBaseUrl/pega-start"
  val howMuchCanYouPayEachMonthUrl: String = s"$serviceBaseUrl/how-much-can-you-pay-each-month"
  val whichDayDoYouWantToPayUrl: String = s"$serviceBaseUrl/which-day-do-you-want-to-pay-each-month"
  val retrieveStartDatesUrl: String = s"$serviceBaseUrl/retrieve-start-dates"
  val determineAffordableQuotesUrl: String = s"$serviceBaseUrl/determine-affordable-quotes"
  val instalmentsUrl: String = s"$serviceBaseUrl/how-many-months-do-you-want-to-pay-over"
  val checkPaymentPlanUrl: String = s"$serviceBaseUrl/check-your-payment-plan"
  def checkPaymentPlanChangeUrl(pageId: String, taxRegime: TaxRegime, lang: Option[Language]): String =
    s"$serviceBaseUrl/check-your-payment-plan/change/$pageId?regime=${taxRegime.entryName.toLowerCase(Locale.UK)}${lang.fold("")("&" + _.code)}"
  val aboutYourBankAccountUrl: String = s"$serviceBaseUrl/about-your-bank-account"
  val directDebitDetailsUrl: String = s"$serviceBaseUrl/set-up-direct-debit"
  val cannotSetupDirectDebitOnlineUrl: String = s"$serviceBaseUrl/you-cannot-set-up-a-direct-debit-online"
  val checkDirectDebitDetailsUrl: String = s"$serviceBaseUrl/check-your-direct-debit-details"
  val lockoutUrl: String = s"$serviceBaseUrl/lockout"
  val termsAndConditionsUrl: String = s"$serviceBaseUrl/terms-and-conditions"
  val whichEmailDoYouWantToUseUrl: String = s"$serviceBaseUrl/which-email-do-you-want-to-use"
  val enterEmailAddressUrl: String = s"$serviceBaseUrl/enter-your-email-address"
  val requestEmailVerificationUrl: String = s"$serviceBaseUrl/email-verification"
  val tooManyPasscodeJourneysStartedUrl: String = s"$serviceBaseUrl/email-verification-too-many-passcodes"
  val tooManyEmailAddressesEnteredUrl: String = s"$serviceBaseUrl/email-verification-too-many-addresses"
  val tooManyPasscodeAttemptsUrl: String = s"$serviceBaseUrl/email-verification-code-entered-too-many-times"
  val emailAddressConfirmedUrl: String = s"$serviceBaseUrl/email-address-confirmed"
  val submitArrangementUrl: String = s"$serviceBaseUrl/submit-arrangement"
  val epayeConfirmationUrl: String = s"$serviceBaseUrl/epaye-payment-plan-set-up"
  val vatConfirmationUrl: String = s"$serviceBaseUrl/vat-payment-plan-set-up"
  val saConfirmationUrl: String = s"$serviceBaseUrl/sa-payment-plan-set-up"
  val siaConfirmationUrl: String = s"$serviceBaseUrl/sia-payment-plan-set-up"
  val saPrintPlanUrl: String = s"$serviceBaseUrl/confirmation-of-your-plan-to-pay"
  val epayeVatPrintPlanUrl: String = s"$serviceBaseUrl/your-payment-plan"
  val exitSurveyEpayeUrl: String = s"$serviceBaseUrl/exit-survey/paye"
  val exitSurveyVatUrl: String = s"$serviceBaseUrl/exit-survey/vat"
  val exitSurveySaUrl: String = s"$serviceBaseUrl/exit-survey/sa"
  val exitSurveySiaUrl: String = s"$serviceBaseUrl/exit-survey/sia"
  val payeNoDueDatesReachedUrl: String = s"$serviceBaseUrl/bill-not-overdue-epaye"
  val vatNoDueDatesReachedUrl: String = s"$serviceBaseUrl/bill-not-overdue-vat"
  val siaNoDueDatesReachedUrl: String = s"$serviceBaseUrl/bill-not-overdue-sia"
  val epayeRLSUrl: String = s"$serviceBaseUrl/update-personal-details-epaye"
  val vatRLSUrl: String = s"$serviceBaseUrl/update-personal-details-vat"
  val saRLSUrl: String = s"$serviceBaseUrl/update-personal-details-sa"
  val siaRLSUrl: String = s"$serviceBaseUrl/update-personal-details-sia"
}
