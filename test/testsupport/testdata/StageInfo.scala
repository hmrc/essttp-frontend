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

object StageInfo {
  val computedTaxId: (String, String) = ("ComputedTaxId", "ComputedTaxId")
  val eligibilityCheckedEligible: (String, String) = ("EligibilityChecked", "Eligible")
  val eligibilityCheckedIneligible: (String, String) = ("EligibilityChecked", "Ineligible")
  val answeredCanPayUpfrontYes: (String, String) = ("AnsweredCanPayUpfront", "Yes")
  val answeredCanPayUpfrontNo: (String, String) = ("AnsweredCanPayUpfront", "No")
  val enteredUpfrontPaymentAmount: (String, String) = ("EnteredUpfrontPaymentAmount", "EnteredUpfrontPaymentAmount")
  val retrievedExtremeDates: (String, String) = ("RetrievedExtremeDates", "ExtremeDatesResponseRetrieved")
  val retrievedAffordabilityResult: (String, String) = ("RetrievedAffordabilityResult", "RetrievedAffordabilityResult")
  val enteredMonthlyPaymentAmount: (String, String) = ("EnteredMonthlyPaymentAmount", "EnteredMonthlyPaymentAmount")
  val enteredDayOfMonth: (String, String) = ("EnteredDayOfMonth", "EnteredDayOfMonth")
  val retrievedStartDates: (String, String) = ("RetrievedStartDates", "StartDatesResponseRetrieved")
  val retrievedAffordableQuotes: (String, String) = ("RetrievedAffordableQuotes", "AffordableQuotesRetrieved")
  val chosenPaymentPlan: (String, String) = ("ChosenPaymentPlan", "SelectedPlan")
  val hasCheckedPaymentPlan: (String, String) = ("CheckedPaymentPlan", "AcceptedPlan")
  val enteredDirectDebitDetailsIsAccountHolder: (String, String) = ("EnteredDirectDebitDetails", "IsAccountHolder")
  val enteredDirectDebitDetailsNotAccountHolder: (String, String) = ("EnteredDirectDebitDetails", "IsNotAccountHolder")
  val confirmedDirectDebitDetails: (String, String) = ("ConfirmedDirectDebitDetails", "ConfirmedDetails")
}
