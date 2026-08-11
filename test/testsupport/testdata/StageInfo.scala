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

final case class StageInfo(stage: String)

object StageInfo {
  val started: StageInfo                       = StageInfo("Started")
  val computedTaxId: StageInfo                 = StageInfo("ComputedTaxId")
  val eligibilityChecked: StageInfo            = StageInfo("EligibilityChecked")
  val assessmentCategoryDetermined: StageInfo  = StageInfo("AssessmentCategoryDetermined")
  val whyCannotPayInFullNotRequired: StageInfo = StageInfo("ObtainedWhyCannotPayInFullAnswers")
  val answeredCanPayUpfront: StageInfo         = StageInfo("AnsweredCanPayUpfront")
  val enteredUpfrontPaymentAmount: StageInfo   = StageInfo("EnteredUpfrontPaymentAmount")
  val retrievedExtremeDates: StageInfo         = StageInfo("RetrievedExtremeDates")
  val retrievedAffordabilityResult: StageInfo  = StageInfo("RetrievedAffordabilityResult")
  val obtainedCanPayWithinSixMonths: StageInfo = StageInfo("ObtainedCanPayWithinSixMonthsAnswers")
  val startedPegaCase: StageInfo               = StageInfo("StartedPegaCase")
  val enteredMonthlyPaymentAmount: StageInfo   = StageInfo("EnteredMonthlyPaymentAmount")
  val enteredDayOfMonth: StageInfo             = StageInfo("EnteredDayOfMonth")
  val retrievedStartDates: StageInfo           = StageInfo("RetrievedStartDates")
  val retrievedAffordableQuotes: StageInfo     = StageInfo("RetrievedAffordableQuotes")
  val chosenPaymentPlan: StageInfo             = StageInfo("ChosenPaymentPlan")
  val hasCheckedPaymentPlan: StageInfo         = StageInfo("CheckedPaymentPlan")
  val enteredCanSetUpDirectDebit: StageInfo    = StageInfo("EnteredCanYouSetUpDirectDebit")
  val enteredDirectDebitDetails: StageInfo     = StageInfo("EnteredDirectDebitDetails")
  val chosenTypeOfBankAccount: StageInfo       = StageInfo("ChosenTypeOfBankAccount")
  val confirmedDirectDebitDetails: StageInfo   = StageInfo("ConfirmedDirectDebitDetails")
  val agreedTermsAndConditions: StageInfo      = StageInfo("AgreedTermsAndConditions")
  val selectedEmailToBeVerified: StageInfo     = StageInfo("SelectedEmailToBeVerified")
  val emailVerificationComplete: StageInfo     = StageInfo("EmailVerificationComplete")
  val submittedArrangement: StageInfo          = StageInfo("SubmittedArrangement")
}
