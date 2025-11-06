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

final case class StageInfo(stage: String, stageValue: String)

object StageInfo {
  val started: StageInfo                                         = StageInfo("Started", "Started")
  val computedTaxId: StageInfo                                   = StageInfo("ComputedTaxId", "ComputedTaxId")
  val eligibilityCheckedEligible: StageInfo                      = StageInfo("EligibilityChecked", "Eligible")
  val eligibilityCheckedIneligible: StageInfo                    = StageInfo("EligibilityChecked", "Ineligible")
  val whyCannotPayInFullNotRequired: StageInfo                   = StageInfo("ObtainedWhyCannotPayInFullAnswers", "AnswerNotRequired")
  val answeredCanPayUpfrontYes: StageInfo                        = StageInfo("AnsweredCanPayUpfront", "Yes")
  val answeredCanPayUpfrontNo: StageInfo                         = StageInfo("AnsweredCanPayUpfront", "No")
  val enteredUpfrontPaymentAmount: StageInfo                     = StageInfo("EnteredUpfrontPaymentAmount", "EnteredUpfrontPaymentAmount")
  val retrievedExtremeDates: StageInfo                           = StageInfo("RetrievedExtremeDates", "ExtremeDatesResponseRetrieved")
  val retrievedAffordabilityResult: StageInfo                    =
    StageInfo("RetrievedAffordabilityResult", "RetrievedAffordabilityResult")
  val obtainedCanPayWithinSixMonthsNotRequired: StageInfo        =
    StageInfo("ObtainedCanPayWithinSixMonthsAnswers", "AnswerNotRequired")
  val obtainedCanPayWithinSixMonthsRequired: StageInfo           =
    StageInfo("ObtainedCanPayWithinSixMonthsAnswers", "AnswerRequired")
  val startedPegaCase: StageInfo                                 = StageInfo("StartedPegaCase", "StartedPegaCase")
  val enteredMonthlyPaymentAmount: StageInfo                     = StageInfo("EnteredMonthlyPaymentAmount", "EnteredMonthlyPaymentAmount")
  val enteredDayOfMonth: StageInfo                               = StageInfo("EnteredDayOfMonth", "EnteredDayOfMonth")
  val retrievedStartDates: StageInfo                             = StageInfo("RetrievedStartDates", "StartDatesResponseRetrieved")
  val retrievedAffordableQuotes: StageInfo                       = StageInfo("RetrievedAffordableQuotes", "AffordableQuotesRetrieved")
  val chosenPaymentPlan: StageInfo                               = StageInfo("ChosenPaymentPlan", "SelectedPlan")
  val hasCheckedPaymentPlan: StageInfo                           = StageInfo("CheckedPaymentPlan", "AcceptedPlan")
  val enteredCanSetUpDirectDebitIsNotAccountHolder: StageInfo    =
    StageInfo("EnteredCanYouSetUpDirectDebit", "CannotSetUpDirectDebit")
  val enteredCanSetUpDirectDebitIsAccountHolder: StageInfo       =
    StageInfo("EnteredCanYouSetUpDirectDebit", "CanSetUpDirectDebit")
  val enteredDirectDebitDetails: StageInfo                       = StageInfo("EnteredDirectDebitDetails", "EnteredDirectDebitDetails")
  val chosenTypeOfBankAccount: StageInfo                         = StageInfo("ChosenTypeOfBankAccount", "ChosenTypeOfBankAccount")
  val confirmedDirectDebitDetails: StageInfo                     = StageInfo("ConfirmedDirectDebitDetails", "ConfirmedDetails")
  val agreedTermsAndConditionsEmailAddressRequired: StageInfo    =
    StageInfo("AgreedTermsAndConditions", "EmailAddressRequired")
  val agreedTermsAndConditionsEmailAddressNotRequired: StageInfo =
    StageInfo("AgreedTermsAndConditions", "EmailAddressNotRequired")
  val selectedEmailToBeVerified: StageInfo                       = StageInfo("SelectedEmailToBeVerified", "EmailChosen")
  val emailVerificationSuccess: StageInfo                        = StageInfo("EmailVerificationComplete", "VerificationSuccess")
  val emailVerificationLocked: StageInfo                         = StageInfo("EmailVerificationComplete", "Locked")
  val submittedArrangement: StageInfo                            = StageInfo("SubmittedArrangement", "Submitted")
}
