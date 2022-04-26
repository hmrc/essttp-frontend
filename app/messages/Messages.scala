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

import moveittocor.corcommon.model.AmountInPence

object Messages {

  val ErrorMessages = messages.ErrorMessages

  val continue: Message = Message(
    english = "Continue",
    welsh = "Yn eich blaen")

  val cancel: Message = Message(
    english = "Cancel",
    welsh = "Canslo")

  val back: Message = Message(
    english = "Back",
    welsh = "Yn ôl")

  val `Sign out`: Message = Message(
    english = "Sign out",
    welsh = "Allgofnodi")

  val `Sign in`: Message = Message(
    english = "Sign in")

  val error: Message = Message(
    english = "Error: ",
    welsh = "Gwall: ")

  val change: Message = Message(
    "Change")

  val `There is a problem`: Message = Message(
    "There is a problem")

  val to: Message = Message("to")
  val month: Message = Message("month")
  val `Bill due`: Message = Message("Bill due")
  val `(includes interest added to date)`: Message = Message(
    "(includes interest added to date)")

  def yourBillIs(amount: AmountInPence): Message = Message(
    s"Your PAYE bill is ${amount.formatInPounds}")

  object TimeOut {
    val `For your security, we signed you out`: Message = Message(
      english = "For your security, we signed you out")

    val `You’re about to be signed out`: Message = Message(
      english = "You’re about to be signed out")

    val `For security reasons, you will be signed out of this service in`: Message = Message(
      "For security reasons, you will be signed out of this service in")
    val `Stay signed in`: Message = Message(
      "Stay signed in")

  }

  object ServicePhase {
    val `Set up a payment plan`: Message = Message(
      "Set up a payment plan")

    val beta: Message = Message(
      "beta")

    def bannerText(link: String): Message = Message(
      s"""This is a new service – your <a class="govuk-link" href="$link">feedback</a> will help us to improve it.""".stripMargin)
  }

  object NotEligible {
    val `If you need to speak to an adviser call us...`: Message = Message(
      s"""If you need to speak to an adviser call us on <strong>0300 200 3835</strong> at the Business Support Service to talk about your payment options.""")

    val `If you cannot use speech recognition software`: Message = Message(
      "If you cannot use speech recognition software")

    def extraSupportLink(link: String): Message = Message(
      s"""Find out how to <a href="${link}" class="govuk-link">deal with HMRC if you need extra support</a>.""".stripMargin)
    def relayLink(link: String): Message = Message(
      s"""You can also use <a href="${link}" class="govuk-link">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>.""")

    val `If you are outside the UK...`: Message = Message(
      "If you are outside the UK: <strong>+44 2890 538 192</strong>")

    val `Before you call, make sure you have:`: Message = Message(
      "Before you call, make sure you have:")

    val `your Accounts Office reference...`: Message = Message(
      "your Accounts Office reference. This is 13 characters, for example, 123PX00123456")

    val `your bank details`: Message = Message(
      "your bank details")

    val `We're likely to ask:`: Message = Message(
      "We’re likely to ask:")

    val `what you've done to try to pay the bill`: Message = Message(
      "what you’ve done to try to pay the bill")

    val `if you can pay some of the bill now`: Message = Message(
      "if you can pay some of the bill now")

    val `Our opening times are Monday to Friday: 8am to 6pm`: Message = Message(
      "Our opening times are Monday to Friday: 8am to 6pm.")
  }

  object EnrolmentMissing {
    val `You are not enrolled`: Message = Message(
      "You are not enrolled")

    val `You are not eligible for an online payment...`: Message = Message(
      "You are not eligible for an online payment plan because you need to enrol for PAYE Online. ")

    val `Find out how to enrol`: Message = Message(
      "Find out how to enrol")
  }

  object UpfrontPayment {
    val `Can you make an upfront payment?`: Message = Message(
      "Can you make an upfront payment?")
    val `Your monthly payments will be lower if you ...`: Message = Message(
      "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 7 working days.")
    val `Yes`: Message = Message(
      "Yes")
    val `No`: Message = Message(
      "No")

    def getError(key: String): Message = key match {
      case "error.required" => Message(english = "Select yes if you can make an upfront payment")
    }

  }

  object Epaye {
    val `Set up a PAYE for Employers payment plan`: Message = Message(
      "Set up a PAYE for Employers payment plan")
    val `You can use this service to pay overdue payments...`: Message = Message(
      "You can use this service to pay overdue payments in instalments over a period of up to 6 months. The payments you make will incur interest.")
    val `You are eligible to set up an online payment plan if:`: Message = Message(
      "You are eligible to set up an online payment plan if:")
    val `you owe £15,000 or less`: Message = Message(
      "you owe £15,000 or less")
    val `you do not have any other payment plans or debts with HMRC`: Message = Message(
      "you do not have any other payment plans or debts with HMRC")
    val `your tax returns are up to date`: Message = Message(
      "your tax returns are up to date")
    val `you have no outstanding penalties`: Message = Message(
      "you have no outstanding penalties")
    val `you are a UK resident`: Message = Message(
      "you are a UK resident")
    val `You can use this service within 35 days of the overdue payment deadline.`: Message = Message(
      "You can use this service within 35 days of the overdue payment deadline.")
    val `You can choose to pay:`: Message = Message(
      "You can choose to pay:")
    val `part of the payment upfront and part in monthly instalments`: Message = Message(
      "part of the payment upfront and part in monthly instalments")
    val `monthly instalments only`: Message = Message(
      "monthly instalments only")
    val `Before you start`: Message = Message(
      "Before you start")
    val `You must be:`: Message = Message(
      "You must be:")
    val `a named account holder for the UK bank account you intend to use`: Message = Message(
      "a named account holder for the UK bank account you intend to use")
    val `able to authorise a Direct Debit`: Message = Message(
      "able to authorise a Direct Debit")
    val `Start now`: Message = Message(
      "Start now")
    val `HMRC intend this as a one-off payment plan...`: Message = Message(
      "HMRC intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the total outstanding amount.")

  }

  object UpfrontPaymentAmount {
    val `How much can you pay upfront?`: Message = Message(
      "How much can you pay upfront?")

    def getError(key: String, max: AmountInPence, min: AmountInPence): Message = key match {
      case "error.required" => Message(english = "Enter how much you can pay upfront")
      case "error.pattern" => Message(english = "How much you can pay upfront must be an amount of money")
      case "error.tooSmall" => Message(english = s"How much you can pay upfront must be ${min.formatInPounds} or more")
      case "error.tooLarge" => Message(english = s"How much you can pay upfront must be ${max.formatInPounds} or less")
    }
  }

  object UpfrontPaymentSummary {
    val `Payment summary`: Message = Message(
      "Payment summary")

    val `Upfront payment`: Message = Message(
      "Upfront payment<br><span class=\"govuk-body-s\">Taken within 7 working days</span>")

    val `Taken within 7 working days`: Message = Message(
      "Taken within 7 working days")

    val `Remaining amount to pay`: Message = Message(
      "Remaining amount to pay")

    val `(interest will be added to this amount)`: Message = Message(
      "(interest will be added to this amount)")
  }

  object MonthlyPaymentAmount {
    val `How much can you afford to pay each month?`: Message = Message(
      "How much can you afford to pay each month?")

    def getHint(max: AmountInPence, min: AmountInPence): Message = Message(
      s"Enter an amount between ${min.formatInPounds} and ${max.formatInPounds}")

    def getError(key: String, max: AmountInPence, min: AmountInPence): Message = key match {
      case "error.required" => Message(english = "Enter how much you can afford to pay each month")
      case "error.pattern" => Message(english = "How much you can afford to pay each month must be an amount of money")
      case "error.tooSmall" => Message(english = s"How much you can afford to pay each month must be ${min.formatInPounds} or more")
      case "error.tooLarge" => Message(english = s"How much you can afford to pay each month must be ${max.formatInPounds} or less")
    }

    val `I can’t afford the minimum payment`: Message = Message(
      "I can’t afford the minimum payment")

    val `You may still be able to set up a payment plan...`: Message = Message(
      "You may still be able to set up a payment plan over the phone, but you are not eligible for an online payment plan.")

    val `We recommend you speak to an adviser...`: Message = Message(
      "We recommend you speak to an adviser on <strong>0300 200 3835</strong> at the Payment Support Service to talk about your payment options.")
  }

  object PaymentDay {
    val `Which day do you want to pay each month?`: Message = Message(
      "Which day do you want to pay each month?")
    val `28th or next working day`: Message = Message(
      "28th or next working day")
    val `A different day`: Message = Message(
      "A different day")
    def getError(key: String): Message = key match {
      case "PaymentDay.error.required" => Message(english = "Select which day of the month you want to pay on")
      case "DifferentDay.error.required" => Message(english = "Enter the day you want to pay each month")
      case "DifferentDay.error.outOfRange" => Message(english = "The day you enter must be between 1 and 28")
      case "DifferentDay.error.invalid" => Message(english = "The day you enter must be a number")
    }
    val `Enter a day between 1 and 28`: Message = Message(
      "Enter a day between 1 and 28")
  }

  object Instalments {
    val `How many months do you want to pay over?`: Message = Message(
      "How many months do you want to pay over?")
    def getInstalmentOption(numberOfMonths: Int, amount: AmountInPence): Message = Message(
      s"$numberOfMonths month${if (numberOfMonths > 1) "s" else ""} at ${amount.formatInPounds}")
    val `Estimated total interest:`: Message = Message(
      "Estimated total interest:")
    def getInterestDescription(hmrcRate: BigDecimal): Message = Message(
      s"Base rate + ${hmrcRate.toString()}%")
    val `added to the final payment`: Message = Message(
      "added to the final payment")
    def getError(key: String): Message = key match {
      case "error.required" => Message(english = "Select how many months you want to pay over")
    }
  }

  object PaymentSchedule {
    val `Check your payment plan`: Message = Message(
      "Check your payment plan")

    val `Upfront payment`: Message = Message(
      "Upfront payment")
    val `Taken within 7 working days`: Message = Message(
      "Taken within 7 working days")
    val `Monthly payments`: Message = Message(
      "Monthly payments")
    val `Payments collected on`: Message = Message(
      "Payments collected on")
    val `or next working day`: Message = Message(
      "or next working day")
    val `(includes interest)`: Message = Message(
      "(includes interest)")
    val `Total to pay`: Message = Message(
      "Total to pay")
  }

  object BankDetails {
    val `Enter account details to set up a Direct Debit`: Message = Message(
      "Enter account details to set up a Direct Debit")
    val `To continue you must be:`: Message = Message(
      "To continue you must be:")
    val `a named account holder for this account`: Message = Message(
      "a named account holder for this account")
    val `the only person who needs to authorise this Direct Debit`: Message = Message(
      "the only person who needs to authorise this Direct Debit")
    val `Name on the account`: Message = Message(
      "Name on the account")
    val `Sort code`: Message = Message(
      "Sort code")
    val `Must be 6 digits long`: Message = Message(
      "Must be 6 digits long")
    val `Account number`: Message = Message(
      "Account number")
    val `Must be between 6 and 8 digits long`: Message = Message(
      "Must be between 6 and 8 digits long")
    val errors: Map[String, Message] = Map(
      "name.error.required" -> Message("Enter the name on the account"),
      "name.error.pattern" -> Message("Name on the account must only include letters, apostrophes, spaces and hyphens"),
      "sortCode.error.required" -> Message("Enter sort code"),
      "sortCode.error.nonNumeric" -> Message("Sort code must be numbers only"),
      "sortCode.error.invalid" -> Message("Sort code must be 6 numbers only"),
      "accountNumber.error.required" -> Message("Enter account number"),
      "accountNumber.error.nonNumeric" -> Message("Account number must be numbers only"),
      "accountNumber.error.invalid" -> Message("Account number must be between 6 and 8 numbers"))

    val `Check your Direct Debit details`: Message = Message(
      "Check your Direct Debit details")

    val `You are covered by the Direct Debit Guarantee`: Message = Message(
      "You are covered by the Direct Debit Guarantee")

    val `The Direct Debit Guarantee`: Message = Message(
      "The Direct Debit Guarantee")

    val `This Guarantee is offered...`: Message = Message(
      "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.")

    val `If there are any changes to the amount...`: Message = Message(
      "If there are any changes to the amount, date or frequency of your Direct Debit HMRC NDDS will notify you 10 working days in advance of your account being debited or as otherwise agreed. If you request HMRC NDDS to collect a payment, application-complete of the amount and date will be given to you at the time of the request.")

    val `If an error is made in the payment...`: Message = Message(
      "If an error is made in the payment of your Direct Debit by HMRC NDDS or your bank or building society you are entitled to a full and immediate refund of the amount paid from your bank or building society. If you receive a refund you are not entitled to, you must pay it back when HMRC NDDS asks you to.")

    val `You can cancel a Direct Debit...`: Message = Message(
      "You can cancel a Direct Debit at any time by simply contacting your bank or building society. Written application-complete may be required. Please also notify us.")

    val `Terms and conditions`: Message = Message(
      "Terms and conditions")

    val `We can cancel this agreement if you:`: Message = Message(
      "We can cancel this agreement if you:")

    val `pay late or miss a payment`: Message = Message(
      "pay late or miss a payment")
    val `pay another tax bill late`: Message = Message(
      "pay another tax bill late")
    val `do not submit your future tax returns on time`: Message = Message(
      "do not submit your future tax returns on time")
    val `If we cancel this agreement...`: Message = Message(
      "If we cancel this agreement, you will need to pay the total amount you owe straight away.")
    val `We can use any refunds you might get to pay off your tax charges.`: Message = Message(
      "We can use any refunds you might get to pay off your tax charges.")
    val `If your circumstances change...`: Message = Message(
      "If your circumstances change and you can pay more or you can pay in full, you need to let us know.")
    val `Declaration`: Message = Message(
      "Declaration")
    val `I agree to the terms and conditions...`: Message = Message(
      "I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt.")
    val `Agree and continue`: Message = Message(
      "Agree and continue")
  }

  object Confirmation {
    val `Your payment plan is set up`: Message = Message(
      "Your payment plan is set up")
    val `Your payment reference is`: Message = Message(
      "Your payment reference is")
    val `What happens next`: Message = Message(
      "What happens next")
    val `HMRC will send you a letter within 5 days with your payment dates.`: Message = Message(
      "HMRC will send you a letter within 5 days with your payment dates.")
    def paymentInfo(hasUpfrontPayment: Boolean, paymentDate: String): Message = Message(
      s"${if (hasUpfrontPayment) "Your upfront payment will be taken within 7 working days. " else ""}Your next payment will be taken on ${paymentDate} or the next working day.")
    val `Print your plan or save it as a PDF`: Message = Message(
      "Print your plan or save it as a PDF")
    val `If you need to change your payment plan`: Message = Message(
      "If you need to change your payment plan")
    val `Call the HMRC Helpline on 0300 200 3700.`: Message = Message(
      "Call the HMRC Helpline on 0300 200 3700.")
    val `Return to tax account`: Message = Message(
      "Return to tax account")
  }

  object PrintSummary {
    val `Your payment plan`: Message = Message(
      "Your payment plan")
    val `Payment reference`: Message = Message(
      "Payment reference")
  }
}

