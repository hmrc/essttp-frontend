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

  val `There is a problem` = Message(
    "There is a problem")

  def your_bill_is(amount: AmountInPence): Message = Message(
    s"Your PAYE bill is ${amount.formatInPounds}")

  val example_content_in_english: Message = Message(
    s"Example content in english")

  object TimeOut {
    val `For your security, we signed you out` = Message(
      english = "For your security, we signed you out")

    val `You’re about to be signed out` = Message(
      english = "You’re about to be signed out")

    val `For security reasons, you will be signed out of this service in` = Message(
      "For security reasons, you will be signed out of this service in")
    val `Stay signed in` = Message(
      "Stay signed in")

  }

  object ServicePhase {
    val `Set up a payment plan` = Message(
      "Set up a payment plan")

    val beta: Message = Message(
      "beta")

    def bannerText(link: String): Message = Message(
      s"""This is a new service – your <a class="govuk-link" href="$link">feedback</a> will help us to improve it.""".stripMargin)
  }

  object UpfrontPayment {
    val `Can you make an upfront payment?` = Message(
      "Can you make an upfront payment?")
    val `Your monthly payments will be lower if you ...` = Message(
      "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 7 working days.")
    val `Yes` = Message(
      "Yes")
    val `No` = Message(
      "No")

    def getError(key: String): Message = key match {
      case "error.required" => Message(english = "Select yes if you can make an upfront payment")
    }

  }

  object UpfrontPaymentAmount {
    val `How much can you pay upfront?` = Message(
      "How much can you pay upfront?")

    def getError(key: String): Message = key match {
      case "error.required" => Message(english = "Enter how much you can pay upfront")
    }
  }

  object MonthlyPaymentAmount {
    val `How much can you afford to pay each month?` = Message(
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
}

