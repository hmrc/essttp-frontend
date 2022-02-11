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
    val `Base rate + 2.5%`: Message = Message(
      "Base rate + 2.5%")
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
      "accountNumber.error.required" -> Message("Enter account number"))
  }
}

