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

  def your_bill_is(amount: AmountInPence): Message = Message(
    s"Your PAYE bill is ${amount.formatInPounds}")

  val example_content_in_english: Message = Message(
    s"Example content in english")

  object TimeOut {
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

  }
}

