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

package messages

import essttp.rootmodel.AmountInPence
import models.forms.BankDetailsForm._

object Messages {

  val continue: Message = Message(
    english = "Continue",
    welsh   = "Yn eich blaen"
  )

  val cancel: Message = Message(
    english = "Cancel",
    welsh   = "Canslo"
  )

  val back: Message = Message(
    english = "Back",
    welsh   = "Yn ôl"
  )
  val `Sign out`: Message = Message(
    english = "Sign out",
    welsh   = "Allgofnodi"
  )

  val `Sign in`: Message = Message(
    english = "Sign in"
  )

  val error: Message = Message(
    english = "Error: ",
    welsh   = "Gwall: "
  )

  val change: Message = Message(
    english = "Change"
  )

  val `There is a problem`: Message = Message(
    english = "There is a problem"
  )

  val to: Message = Message(
    english = "to"
  )

  val month: Message = Message(
    english = "month"
  )

  val `Bill due`: Message = Message(
    english = "Bill due"
  )

  val `(includes interest added to date)`: Message = Message(
    english = "(includes interest added to date)"
  )

  val `full stop`: Message = Message(
    english = ".",
    welsh   = "."
  )

  def yourBillIs(amount: AmountInPence): Message = Message(
    english = s"Your PAYE bill is ${amount.gdsFormatInPounds}"
  )

  val `Overdue payments`: Message = Message(
    english = "Overdue payments"
  )

  object TimeOut {

    val `For your security, we signed you out`: Message = Message(
      english = "For your security, we signed you out"
    )

    val `You’re about to be signed out`: Message = Message(
      english = "You’re about to be signed out"
    )

    val `For security reasons, you will be signed out of this service in`: Message = Message(
      english = "For security reasons, you will be signed out of this service in"
    )

    val `Stay signed in`: Message = Message(
      english = "Stay signed in"
    )

  }

  object ServicePhase {

    val `Set up a payment plan`: Message = Message(
      english = "Set up a payment plan"
    )

    val `Set up an Employers' PAYE payment plan`: Message = Message(
      english = "Set up an Employers’ PAYE payment plan"
    )

    val beta: Message = Message(
      english = "beta"
    )

    def bannerText(link: String): Message = Message(
      english = s"""This is a new service – your <a class="govuk-link" href="$link">feedback</a> will help us to improve it.""".stripMargin
    )
  }

  object NotEligible {

    val `If you need to speak to an adviser call us...`: Message = Message(
      english = s"""If you need to speak to an adviser call us on <strong>0300 200 3835</strong> at the Business Support Service to talk about your payment options."""
    )

    val `If you cannot use speech recognition software`: Message = Message(
      english = "If you cannot use speech recognition software"
    )

    def extraSupportLink(link: String): Message = Message(
      english = s"""Find out how to <a href="${link}" class="govuk-link">deal with HMRC if you need extra support</a>.""".stripMargin
    )

    def relayLink(link: String): Message = Message(
      english = s"""You can also use <a href="${link}" class="govuk-link">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."""
    )

    val `If you are outside the UK...`: Message = Message(
      english = "If you are outside the UK: <strong>+44 2890 538 192</strong>"
    )

    val `Before you call, make sure you have:`: Message = Message(
      english = "Before you call, make sure you have:"
    )

    val `your Accounts Office reference...`: Message = Message(
      english = "your Accounts Office reference. This is 13 characters, for example, 123PX00123456"
    )

    val `your bank details`: Message = Message(
      english = "your bank details"
    )

    val `We're likely to ask:`: Message = Message(
      english = "We’re likely to ask:"
    )

    val `what you've done to try to pay the bill`: Message = Message(
      english = "what you’ve done to try to pay the bill"
    )

    val `if you can pay some of the bill now`: Message = Message(
      english = "if you can pay some of the bill now"
    )

    val `Our opening times are Monday to Friday: 8am to 6pm`: Message = Message(
      english = "Our opening times are Monday to Friday: 8am to 6pm (we are closed on bank holidays)"
    )

    val `Call us`: Message = Message(
      english = "Call us"
    )

    val `You must owe £15,000 or less to be eligible for a payment plan online...`: Message = Message(
      english = "You must owe £15,000 or less to be eligible for a payment plan online. You may still be able to set up a plan over the phone."
    )

    val `Your overdue amount must have a due date that is less than 35 days ago ...`: Message = Message(
      english = "Your overdue amount must have a due date that is less than 35 days ago for you to be eligible for a payment plan online. You may still be able to set up a plan over the phone. "
    )

    val `For further support you can contact the Payment Support Service on 0300 200 3835 to speak to an advisor.`: Message = Message(
      english = "For further support you can contact the Payment Support Service on <strong>0300 200 3835</strong> to speak to an advisor."
    )

    val `You already have a payment plan with HMRC`: Message = Message(
      english = "You already have a payment plan with HMRC"
    )

    val `You can only have one payment plan at a time.`: Message = Message(
      english = "You can only have one payment plan at a time."
    )

    val `Generic ineligible message`: Message = Message(
      english = "You are not eligible for an online payment plan. You may still be able to set up a payment plan over the phone."
    )

    val `File your return to use this service`: Message = Message(
      english = "File your return to use this service"
    )

    val `To be eligible for a payment plan online, you need to be up to date with your PAYE for Employers returns...`: Message = Message(
      english = "To be eligible for a payment plan online, you need to be up to date with your PAYE for Employers returns. Once you have done this, you can return to this service."
    )

    val `Go to your tax account`: Message = Message(
      english = "Go to your tax account"
    )

    val `to file your tax return.`: Message = Message(
      english = " to file your tax return. "
    )
  }

  object EnrolmentMissing {
    val `You are not enrolled`: Message = Message(
      english = "You are not enrolled"
    )
    val `You are not eligible for an online payment...`: Message = Message(
      english = "You are not eligible for an online payment plan because you need to enrol for PAYE Online. "
    )
    val `Find out how to enrol`: Message = Message(
      english = "Find out how to enrol"
    )
  }

  object UpfrontPayment {
    val `Can you make an upfront payment?`: Message = Message(
      english = "Can you make an upfront payment?"
    )
    val `Your monthly payments will be lower if you ...`: Message = Message(
      english = "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 10 working days."
    )

    val `Yes`: Message = Message(
      english = "Yes"
    )

    val `No`: Message = Message(
      english = "No"
    )

    val `Select yes if you can make an upfront payment`: Message = Message(
      english = "Select yes if you can make an upfront payment"
    )

  }

  object Epaye {

    val `Set up an Employers' PAYE payment plan`: Message = Message(
      english = "Set up an Employers’ PAYE payment plan"
    )

    val `You can use this service to pay overdue payments...`: Message = Message(
      english = "You can use this service to pay overdue payments in instalments over a period of up to 6 months. The payments you make may incur interest."
    )

    val `You are eligible to set up an online payment plan if:`: Message = Message(
      english = "You are eligible to set up an online payment plan if:"
    )

    val `you owe £15,000 or less`: Message = Message(
      english = "you owe £15,000 or less"
    )

    val `you do not have any other payment plans or debts with HMRC`: Message = Message(
      english = "you do not have any other payment plans or debts with HMRC"
    )

    val `your tax returns are up to date`: Message = Message(
      english = "your tax returns are up to date"
    )

    val `you have no outstanding penalties`: Message = Message(
      english = "you have no outstanding penalties"
    )

    val `You can use this service within 35 days of the overdue payment deadline.`: Message = Message(
      english = "You can use this service within 35 days of the overdue payment deadline."
    )

    val `You can choose to pay:`: Message = Message(
      english = "You can choose to pay:"
    )

    val `part of the payment upfront and part in monthly instalments`: Message = Message(
      english = "part of the payment upfront and part in monthly instalments"
    )

    val `monthly instalments only`: Message = Message(
      english = "monthly instalments only"
    )

    val `Before you start`: Message = Message(
      english = "Before you start"
    )

    val `You must be:`: Message = Message(
      english = "You must be:"
    )

    val `a named account holder for the UK bank account you intend to use`: Message = Message(
      english = "a named account holder for the UK bank account you intend to use"
    )

    val `able to authorise a Direct Debit`: Message = Message(
      english = "able to authorise a Direct Debit"
    )

    val `Start now`: Message = Message(
      english = "Start now"
    )

    val `HMRC intend this as a one-off payment plan...`: Message = Message(
      english = "HMRC intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the total outstanding amount."
    )

  }

  object UpfrontPaymentAmount {

    val `How much can you pay upfront?`: Message = Message(
      english = "How much can you pay upfront?"
    )

    def getError(key: String, max: AmountInPence, min: AmountInPence): Message = key match {
      case "error.required" => Message(english = "Enter your upfront payment")
      case "error.pattern"  => Message(english = "How much you can pay upfront must be an amount of money")
      case "error.tooSmall" => Message(english = s"Your upfront payment must be between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}")
      case "error.tooLarge" => Message(english = s"Your upfront payment must be between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}")
    }

  }

  object UpfrontPaymentSummary {

    val `Payment summary`: Message = Message(
      english = "Payment summary"
    )
    val `Change Payment`: Message = Message(
      english = "upfront payment"
    )
    val `Upfront payment`: Message = Message(
      english = "payment amount"
    )
    val `Upfront payment-visually-hidden-message`: Message = Message(
      english = "payment Taken within 10 working days"
    )

    val `Taken within 10 working days`: Message = Message(
      english = "Taken within 10 working days"
    )

    val `Remaining amount to pay`: Message = Message(
      english = "Remaining amount to pay"
    )

    val `(interest may be added to this amount)`: Message = Message(
      english = """<span class="govuk-body-s">(interest may be added to this amount)</span>"""
    )

  }

  object MonthlyPaymentAmount {

    val `How much can you afford to pay each month?`: Message = Message(
      english = "How much can you afford to pay each month?"
    )

    def getHint(max: AmountInPence, min: AmountInPence): Message = Message(
      english = s"Enter an amount between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}"
    )

    def getError(key: String, max: AmountInPence, min: AmountInPence): Message = key match {
      case "error.required" => Message(english = "Enter how much you can afford to pay each month")
      case "error.pattern"  => Message(english = "How much you can afford to pay each month must be an amount of money")
      case "error.tooSmall" => Message(english = s"How much you can afford to pay each month must be between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}")
      case "error.tooLarge" => Message(english = s"How much you can afford to pay each month must be between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}")
    }

    val `I can’t afford the minimum payment`: Message = Message(
      english = "I can’t afford the minimum payment"
    )

    val `You may still be able to set up a payment plan...`: Message = Message(
      english = "You may still be able to set up a payment plan over the phone, but you are not eligible for an online payment plan."
    )

    val `We recommend you speak to an adviser...`: Message = Message(
      english = "We recommend you speak to an adviser on <strong>0300 200 3835</strong> at the Payment Support Service to talk about your payment options."
    )

  }

  object PaymentDay {

    val `Which day do you want to pay each month?`: Message = Message(
      english = "payment day"
    )

    val `Change months duration`: Message = Message(
      english = "how many months you want to pay over"
    )

    val `28th or next working day`: Message = Message(
      english = "28th or next working day"
    )

    val `A different day`: Message = Message(
      english = "A different day"
    )

    def getError(key: String): Message = key match {
      case "PaymentDay.error.required"     => Message(english = "Select which day you want to pay each month")
      case "DifferentDay.error.required"   => Message(english = "Enter the day you want to pay each month")
      case "DifferentDay.error.outOfRange" => Message(english = "The day you want to pay must be between 1 and 28")
      case "DifferentDay.error.invalid"    => Message(english = "The day you want to pay must be a number")
    }

    val `Enter a day between 1 and 28`: Message = Message(
      english = "Enter a day between 1 and 28"
    )

  }

  object Instalments {

    val `How many months do you want to pay over?`: Message = Message(
      english = "How many months do you want to pay over?"
    )

    private def getInstalmentOptionOneMonth(amount: AmountInPence): Message = Message(
      english = s"1 month at ${amount.gdsFormatInPounds}"
    )

    private def getInstalmentOptionMoreThanOneMonth(numberOfMonths: Int, amount: AmountInPence): Message = Message(
      english = s"$numberOfMonths months at ${amount.gdsFormatInPounds}"
    )

    def getInstalmentOption(numberOfMonths: Int, amount: AmountInPence): Message =
      if (numberOfMonths > 1) getInstalmentOptionMoreThanOneMonth(numberOfMonths, amount)
      else getInstalmentOptionOneMonth(amount)

    def `Estimated total interest of x`(interest: AmountInPence): Message = Message(
      english = s"Estimated total interest of ${interest.gdsFormatInPounds}"
    )

    def getError(key: String): Message = key match {
      case "error.required" => Message(english = "Select how many months you want to pay over")
    }

    val `How we calculate interest`: Message = Message(
      english = "How we calculate interest"
    )

    val `We only charge interest on overdue amounts`: Message = Message(
      english = "We only charge interest on overdue amounts."
    )

    val `We charge the Bank of England base rate`: Message = Message(
      english = "We charge the Bank of England base rate plus 2.5%, calculated as simple interest."
    )

    val `If interest rates change`: Message = Message(
      english = "If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan."
    )

  }

  object PaymentSchedule {

    val `Check your payment plan`: Message = Message(
      english = "Check your payment plan"
    )

    val `Upfront payment`: Message = Message(
      english = "Upfront payment"
    )
    val `Taken within 10 working days`: Message = Message(
      english = "Taken within 10 working days"
    )
    val `Monthly payments`: Message = Message(
      english = "Monthly payments"
    )
    val `Payments collected on`: Message = Message(
      english = "Payments collected on"
    )
    val `or next working day`: Message = Message(
      english = "or next working day"
    )
    val `(includes interest)`: Message = Message(
      english = "(includes interest)"
    )
    val `Total to pay`: Message = Message(
      english = "Total to pay"
    )

  }

  object BankDetails {

    val `What type of account details are you providing?`: Message = Message(
      english = "What type of account details are you providing?"
    )

    val `Business bank account`: Message = Message(
      english = "Business bank account"
    )

    val `Personal bank account`: Message = Message(
      english = "Personal bank account"
    )

    val `Select what type of account details you are providing`: Message = Message(
      english = "Select what type of account details you are providing"
    )

    val `Set up Direct Debit`: Message = Message(
      english = "Set up Direct Debit"
    )

    val `Name on the account`: Message = Message(
      english = "Name on the account"
    )

    val `Sort code`: Message = Message(
      english = "Sort code"
    )

    val `Must be 6 digits long`: Message = Message(
      english = "Must be 6 digits long"
    )

    val `Account number`: Message = Message(
      english = "Account number"
    )

    val `Must be between 6 and 8 digits long`: Message = Message(
      english = "Must be between 6 and 8 digits long"
    )

    val `Are you an account holder`: Message = Message(
      english = "Are you an account holder?"
    )

    val `You must be able to set up a direct debit without permission from...`: Message = Message(
      english = "You must be able to set up a Direct Debit without permission from any other account holders."
    )

    val `Select yes if you are the account holder`: Message = Message(
      english = "Select yes if you are the account holder"
    )

    val errors: Map[String, Message] = Map(
      "name.error.required" -> Message("Enter the name on the account"),
      "name.error.pattern" -> Message("Name on the account must only include letters, apostrophes, spaces and hyphens"),
      "name.error.maxlength" -> Message("Name on the account must be 70 characters or less"),
      "sortCode.error.required" -> Message("Enter sort code"),
      "sortCode.error.nonNumeric" -> Message("Sort code must be a number"),
      "sortCode.error.invalid" -> Message("Sort code must be 6 digits"),
      "accountNumber.error.required" -> Message("Enter account number"),
      "accountNumber.error.nonNumeric" -> Message("Account number must be a number"),
      "accountNumber.error.invalid" -> Message("Account number must be between 6 and 8 digits"),
      "isSoleSignatory.error.required" -> Message("Select yes if you are the account holder"),
      s"bars.${accountNumberNotWellFormatted.message}" -> Message("Enter a valid combination of bank account number and sort code"),
      s"bars.${sortCodeNotPresentOnEiscd.message}" -> Message("Enter a valid combination of bank account number and sort code"),
      s"bars.${sortCodeDoesNotSupportsDirectDebit.message}" -> Message(
        "You have entered a sort code which does not accept this type of payment. " +
          "Check you have entered a valid sort code or enter details for a different account"
      ),
      s"bars.${nameDoesNotMatch.message}" -> Message("Enter a valid account name"),
      s"bars.${accountDoesNotExist.message}" -> Message("Enter a valid combination of bank account number and sort code"),
      s"bars.${otherBarsError.message}" -> Message("Enter a valid combination of bank account number and sort code")
    )

    val `Check your Direct Debit details`: Message = Message(
      english = "Check your Direct Debit details"
    )

    val `Account name`: Message = Message(
      english = "Account name"
    )

    val `You are covered by the Direct Debit Guarantee`: Message = Message(
      english = "You are covered by the Direct Debit Guarantee"
    )

    val `The Direct Debit Guarantee`: Message = Message(
      english = "The Direct Debit Guarantee"
    )

    val `This Guarantee is offered...`: Message = Message(
      english = "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
    )

    val `If there are any changes to the amount...`: Message = Message(
      english = "If there are any changes to the amount, date or frequency of your Direct Debit HMRC NDDS will notify you 10 working days in advance of your account being debited or as otherwise agreed. If you request HMRC NDDS to collect a payment, confirmation of the amount and date will be given to you at the time of the request."
    )

    val `If an error is made in the payment...`: Message = Message(
      english = "If an error is made in the payment of your Direct Debit by HMRC NDDS or your bank or building society you are entitled to a full and immediate refund of the amount paid from your bank or building society. If you receive a refund you are not entitled to, you must pay it back when HMRC NDDS asks you to."
    )

    val `You can cancel a Direct Debit...`: Message = Message(
      english = "You can cancel a Direct Debit at any time by simply contacting your bank or building society. Written confirmation may be required. Please also notify us."
    )

    val `Terms and conditions`: Message = Message(
      english = "Terms and conditions"
    )

    val `We can cancel this agreement if you:`: Message = Message(
      english = "We can cancel this agreement if you:"
    )

    val `pay late or miss a payment`: Message = Message(
      english = "pay late or miss a payment"
    )

    val `pay another tax bill late`: Message = Message(
      english = "pay another tax bill late"
    )

    val `do not submit your future tax returns on time`: Message = Message(
      english = "do not submit your future tax returns on time"
    )

    val `If we cancel this agreement...`: Message = Message(
      english = "If we cancel this agreement, you will need to pay the total amount you owe straight away."
    )

    val `We can use any refunds you might get to pay off your tax charges.`: Message = Message(
      english = "We can use any refunds you might get to pay off your tax charges."
    )

    val `If your circumstances change...`: Message = Message(
      english = "If your circumstances change and you can pay more or you can pay in full, you need to let us know."
    )

    val `Declaration`: Message = Message(
      english = "Declaration"
    )

    val `I agree to the terms and conditions...`: Message = Message(
      english = "I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt."
    )

    val `Agree and continue`: Message = Message(
      english = "Agree and continue"
    )

    val `You cannot set up a Direct Debit online`: Message = Message(
      english = "You cannot set up a Direct Debit online"
    )

    val `You need a named account holder or someone with authorisation to set up a Direct Debit.`: Message = Message(
      english = "You need a named account holder or someone with authorisation to set up a Direct Debit."
    )
    val `Return to tax account`: Message = Message(
      english = "Return to tax account"
    )

    val `You've tried to confirm your bank details too many times`: Message = Message(
      english = "You’ve tried to confirm your bank details too many times"
    )

    def waitUntil(lockExpires: String): Message = Message(
      english = s"You’ll need to wait until ${lockExpires} before trying to confirm your bank details again."
    )

    val `You may still be able to set up a payment plan over the phone.`: Message = Message(
      english = "You may still be able to set up a payment plan over the phone."
    )

    val `For further support you can contact the Payment Support Service...`: Message = Message(
      english = "For further support you can contact the Payment Support Service on <strong>0300 200 3835</strong> to speak to an adviser."
    )

  }

  object Confirmation {
    val `Your payment plan is set up`: Message = Message(
      english = "Your payment plan is set up"
    )

    val `Your payment reference is`: Message = Message(
      english = "Your payment reference is"
    )

    val `What happens next`: Message = Message(
      english = "What happens next"
    )

    val `HMRC will send you a letter within 5 working days with your payment dates.`: Message = Message(
      english = "HMRC will send you a letter within 5 working days with your payment dates."
    )

    def paymentInfo(hasUpfrontPayment: Boolean, paymentDate: String): Message = Message(
      english = s"${if (hasUpfrontPayment) "Your upfront payment will be taken within 10 working days. " else ""}Your next payment will be taken on $paymentDate or the next working day."
    )

    val `Print your plan or save it as a PDF`: Message = Message(
      english = "Print your plan or save it as a PDF"
    )

    val `If you need to change your payment plan`: Message = Message(
      english = "If you need to change your payment plan"
    )

    val `Call the HMRC Helpline on 0300 123 1813.`: Message = Message(
      english = "Call the HMRC Helpline on 0300 123 1813, or write to us by post:"
    )

    val `DM PAYE`: Message = Message(
      english = "DM PAYE"
    )

    val `HM Revenue and Customs`: Message = Message(
      "HM Revenue and Customs"
    )
    val `BX9 1EW`: Message = Message(
      "BX9 1EW"
    )
    val `United Kingdom`: Message = Message(
      "United Kingdom"
    )

    val `Return to tax account`: Message = Message(
      english = "Return to tax account"
    )

    val `What did you think of this service`: Message = Message(
      english = "What did you think of this service?"
    )

    val `(takes 30 seconds)`: Message = Message(
      english = "(takes 30 seconds)"
    )

  }

  object PrintSummary {

    val `Your payment plan`: Message = Message(
      english = "Your payment plan"
    )

    val `Payment reference`: Message = Message(
      english = "Payment reference"
    )

  }
}

