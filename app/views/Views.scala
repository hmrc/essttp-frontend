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

package views

import play.api.data.FormError
import views.html._
import views.html.checkpaymentchedule._
import views.html.emailerrors._
import views.html.epaye.EPayeLanding
import views.html.epaye.ineligible.{Ineligible, NotEnrolled}
import views.html.sa.{NotSaEnrolled, SaLanding, NoMtditsaEnrollment}
import views.html.simp.{NoNinoFound, SimpLanding}
import views.html.vat.{NotVatRegistered, VatLanding}
import views.partials.Partials

import javax.inject.Inject

class Views @Inject() (
    val whichTaxRegime:                 WhichTaxRegime,
    val epayeLanding:                   EPayeLanding,
    val vatLanding:                     VatLanding,
    val saLanding:                      SaLanding,
    val simpLanding:                    SimpLanding,
    val yourBillIs:                     YourBillIs,
    val notEnrolled:                    NotEnrolled,
    val notVatRegistered:               NotVatRegistered,
    val notSaEnrolled:                  NotSaEnrolled,
    val noNinoFound:                    NoNinoFound,
    val checkYourPaymentSchedule:       CheckPaymentSchedule,
    val ineligible:                     Ineligible,
    val partials:                       Partials,
    val whyCannotPayInFull:             WhyCannotPayInFull,
    val canYouMakeAnUpFrontPayment:     CanYouMakeAnUpfrontPayment,
    val upfrontPaymentAmountPage:       UpfrontPaymentAmountPage,
    val upfrontSummaryPage:             UpfrontPaymentSummary,
    val canPayWithinSixMonthsPage:      CanPayWithinSixMonths,
    val monthlyPaymentAmountPage:       MonthlyPaymentAmount,
    val paymentDayPage:                 PaymentDay,
    val instalmentOptionsPage:          InstalmentOptions,
    val checkYouCanSetUpDDPage:         CheckYouCanSetUpDDPage,
    val cannotSetupDirectDebitPage:     NotSoleSignatoryPage,
    val enterBankDetailsPage:           EnterBankDetails,
    val bankDetailsSummary:             BankDetailsSummary,
    val barsLockout:                    BarsLockout,
    val termsAndConditions:             TermsAndConditions,
    val chooseEmailPage:                ChooseEmailPage,
    val enterEmailPage:                 EnterEmailAddress,
    val emailAddressConfirmed:          EmailAddressConfirmed,
    val tooManyEmails:                  TooManyEmails,
    val tooManyPasscodes:               TooManyPasscodes,
    val tooManyPasscodeJourneysStarted: TooManyPasscodeJourneysStarted,
    val paymentPlanSetUpPage:           PaymentPlanSetUpPage,
    val saPaymentPlanSetUpPage:         SaPaymentPlanSetUpPage,
    val simpPaymentPlanSetUpPage:       SimpPaymentPlanSetUpPage,
    val printSummaryPage:               PrintSummaryPage,
    val saPrintSummaryPage:             SaPrintSummaryPage,
    val missingInfoPage:                MissingInformation,
    val timedOutPage:                   TimedOut,
    val shuttered:                      shuttering.Shuttered,
    val youAlreadyHaveDirectDebit:      YouAlreadyHaveDirectDebit,
    val noMtditsaEnrollement:           NoMtditsaEnrollment
)

object Views {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def formErrorArgsStringList(e: FormError): List[String] = e.args.toList.map(_.toString)

}
