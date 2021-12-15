/*
 * Copyright 2021 HM Revenue & Customs
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

object SelectBankMessages {
  val selectBank_title: Message = Message(
    english = "Choose your bank",
    welsh = "Dewis eich banc")

  val selectBank_p1: Message = Message(
    english = "<p class=\"govuk-body govuk-!-font-size-24\">Select your bank from the list. You can then sign in and approve a one-off payment.</p><p class=\"govuk-body govuk-!-font-size-24\">We will not get access to your bank account or store your banking details.</p>",
    welsh = "<p class=\"govuk-body govuk-!-font-size-24\">Dewiswch eich banc o’r rhestr. Wedyn, gallwch fewngofnodi a chymeradwyo taliad unigol.</p><p class=\"govuk-body govuk-!-font-size-24\">Ni fyddwn yn cael mynediad at eich cyfrif banc nac yn storio’ch manylion bancio.</p>")

  val selectBank_p2: Message = Message(
    english = "We will not get access to your bank account or store your banking details.",
    welsh = "Ni fyddwn yn cael mynediad at eich cyfrif banc nac yn storio’ch manylion bancio.")

  val selectBank_p3: Message = Message(
    english = "My bank is not listed",
    welsh = "Nid yw fy manc ar y rhestr")

  val selectBank_validation: Message = Message(
    english = "Select a bank from the list",
    welsh = "Dewiswch fanc o’r rhestr")

  val selectBank_options: Message = Message(
    english = "Choose your account",
    welsh = "Dewis eich cyfrif")

  val selectBank_select_default: Message = Message(
    english = "Select an option",
    welsh = "Dewis opsiwn")

  val selectBank_select_validation: Message = Message(
    english = "Select which type of account you want to pay from",
    welsh = "Dewiswch y math o gyfrif yr ydych eisiau gwneud taliad ohono")
}
