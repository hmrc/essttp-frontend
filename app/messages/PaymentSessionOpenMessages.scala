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

object PaymentSessionOpenMessages {
  val sessionOpen_title: Message = Message(
    english = "You have a payment session open",
    welsh = "Mae sesiwn talu ar agor gennych")

  val sessionOpen_p1: Message = Message(
    english = "Your payment to HMRC has not been made. You need to go back to your bank account to approve the payment or cancel it.",
    welsh = "Nid yw’ch taliad i CThEM wedi’i wneud. Mae angen i chi fynd yn ôl i’ch cyfrif banc i awdurdodi’r taliad neu ei ganslo.")

  val sessionOpen_button: Message = Message(
    english = "Go back to your bank",
    welsh = "Ewch yn ôl i’ch banc")

  val sessionOpen_link: Message = Message(
    english = "Choose a different way to pay",
    welsh = "Dewiswch ddull gwahanol o dalu")
}
