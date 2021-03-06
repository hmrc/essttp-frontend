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

package views

import views.html.EPaye.EPayeLanding
import views.html.EPaye.ineligible.{Ineligible, NotEnrolled}
import views.html.{BankDetailsSummary, CanYouMakeAnUpfrontPayment, ChooseTypeOfAccountPage, EnterBankDetails, InstalmentOptions, MonthlyPaymentAmount, NotSoleSignatoryPage, PaymentDay, PaymentPlanSetUpPage, PrintSummaryPage, TermsAndConditions, UpfrontPaymentAmountPage, UpfrontPaymentSummary, YourBillIs}
import views.partials.Partials

import javax.inject.Inject

class Views @Inject() (
    val epayeLanding:               EPayeLanding,
    val yourBillIs:                 YourBillIs,
    val notEnrolled:                NotEnrolled,
    val ineligible:                 Ineligible,
    val partials:                   Partials,
    val canYouMakeAnUpFrontPayment: CanYouMakeAnUpfrontPayment,
    val upfrontPaymentAmountPage:   UpfrontPaymentAmountPage,
    val upfrontSummaryPage:         UpfrontPaymentSummary,
    val monthlyPaymentAmountPage:   MonthlyPaymentAmount,
    val paymentDayPage:             PaymentDay,
    val instalmentOptionsPage:      InstalmentOptions,
    val chooseTypeOfAccountPage:    ChooseTypeOfAccountPage,
    val enterBankDetailsPage:       EnterBankDetails,
    val bankDetailsSummary:         BankDetailsSummary,
    val termsAndConditions:         TermsAndConditions,
    val cannotSetupDirectDebitPage: NotSoleSignatoryPage,
    val paymentPlanSetUpPage:       PaymentPlanSetUpPage,
    val printSummaryPage:           PrintSummaryPage
)
