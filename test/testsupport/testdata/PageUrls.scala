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

object PageUrls {
  val serviceBaseUrl = "/set-up-a-payment-plan"
  val whichTaxRegimeUrl: String = s"$serviceBaseUrl/which-tax"
  val epayeLandingPageUrl: String = s"$serviceBaseUrl/epaye-payment-plan"
  val vatLandingPageUrl: String = s"$serviceBaseUrl/vat-payment-plan"
  val govUkEpayeStartUrl: String = s"$serviceBaseUrl/govuk/epaye/start"
  val determineTaxIdUrl: String = s"$serviceBaseUrl/determine-taxId"
  val determineEligibilityUrl: String = s"$serviceBaseUrl/determine-eligibility"
  val notEnrolledUrl: String = s"$serviceBaseUrl/not-enrolled"
  val payeNotEligibleUrl: String = s"$serviceBaseUrl/not-eligible-epaye"
  val vatNotEligibleUrl: String = s"$serviceBaseUrl/not-eligible-vat"
  val epayeDebtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large-epaye"
  val vatDebtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large-vat"
  val epayeDebtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old-epaye"
  val vatDebtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old-vat"
  val epayeFileYourReturnUrl: String = s"$serviceBaseUrl/file-your-return"
  val vatFileYourReturnUrl: String = s"$serviceBaseUrl/return-not-filed-vat"
  val epayeAlreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-payment-plan-epaye"
  val vatAlreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-payment-plan-vat"
  val yourBillIsUrl: String = s"$serviceBaseUrl/your-bill"
  val canYouMakeAnUpfrontPaymentUrl: String = s"$serviceBaseUrl/can-you-make-an-upfront-payment"
  val howMuchCanYouPayUpfrontUrl: String = s"$serviceBaseUrl/how-much-can-you-pay-upfront"
  val upfrontPaymentSummaryUrl: String = s"$serviceBaseUrl/upfront-payment-summary"
  val retrievedExtremeDatesUrl: String = s"$serviceBaseUrl/retrieve-extreme-dates"
  val determineAffordabilityUrl: String = s"$serviceBaseUrl/determine-affordability"
  val howMuchCanYouPayEachMonthUrl: String = s"$serviceBaseUrl/how-much-can-you-pay-each-month"
  val whichDayDoYouWantToPayUrl: String = s"$serviceBaseUrl/which-day-do-you-want-to-pay-each-month"
  val retrieveStartDatesUrl: String = s"$serviceBaseUrl/retrieve-start-dates"
  val determineAffordableQuotesUrl: String = s"$serviceBaseUrl/determine-affordable-quotes"
  val instalmentsUrl: String = s"$serviceBaseUrl/how-many-months-do-you-want-to-pay-over"
  val instalmentScheduleUrl: String = s"$serviceBaseUrl/check-your-payment-plan"
  val aboutYourBankAccountUrl: String = s"$serviceBaseUrl/about-your-bank-account"
  val directDebitDetailsUrl: String = s"$serviceBaseUrl/set-up-direct-debit"
  val cannotSetupDirectDebitOnlineUrl: String = s"$serviceBaseUrl/you-cannot-set-up-a-direct-debit-online"
  val checkDirectDebitDetailsUrl: String = s"$serviceBaseUrl/check-your-direct-debit-details"
  val lockoutUrl: String = s"$serviceBaseUrl/lockout"
  val termsAndConditionsUrl: String = s"$serviceBaseUrl/terms-and-conditions"
  val whichEmailDoYouWantToUseUrl: String = s"$serviceBaseUrl/which-email-do-you-want-to-use"
  val requestEmailVerificationUrl: String = s"$serviceBaseUrl/email-verification"
  val tooManyEmailAddressesUrl: String = s"$serviceBaseUrl/tried-to-confirm-email-too-many-times"
  val submitArrangementUrl: String = s"$serviceBaseUrl/submit-arrangement"
  val confirmationUrl: String = s"$serviceBaseUrl/payment-plan-set-up"
  val printPlanUrl: String = s"$serviceBaseUrl/payment-plan-print-summary"
}
