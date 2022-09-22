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
  val landingPageUrl: String = serviceBaseUrl
  val govUkEpayeStartUrl: String = s"$serviceBaseUrl/govuk/epaye/start"
  val determineTaxIdUrl: String = s"$serviceBaseUrl/determine-taxId"
  val determineEligibilityUrl: String = s"$serviceBaseUrl/determine-eligibility"
  val notEnrolledUrl: String = s"$serviceBaseUrl/not-enrolled"
  val notEligibleUrl: String = s"$serviceBaseUrl/not-eligible"
  val debtTooLargeUrl: String = s"$serviceBaseUrl/debt-too-large"
  val debtTooOldUrl: String = s"$serviceBaseUrl/debt-too-old"
  val fileYourReturnUrl: String = s"$serviceBaseUrl/file-your-return"
  val alreadyHaveAPaymentPlanUrl: String = s"$serviceBaseUrl/already-have-a-payment-plan"
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
  val submitArrangementUrl: String = s"$serviceBaseUrl/submit-arrangement"
  val confirmationUrl: String = s"$serviceBaseUrl/payment-plan-set-up"
  val printPlanUrl: String = s"$serviceBaseUrl/payment-plan-print-summary"
}
