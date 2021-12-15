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

object SetEmailMessages {
  val setEmail_title: Message = Message(
    english = "What is your email address? (optional)",
    welsh = "Beth yw’ch cyfeiriad e-bost? (dewisol)")

  val setEmail_p1: Message = Message(
    english = "We’ll only use this to confirm you sent a payment",
    welsh = "Byddwn ond yn defnyddio hwn i gadarnhau’ch bod wedi anfon taliad")

  val setEmail_link: Message = Message(
    english = "Skip",
    welsh = "Neidio ymlaen")

  val setEmail_validation: Message = Message(
    english = "Enter your email address in the correct format, like name@example.com",
    welsh = "Nodwch eich cyfeiriad e-bost yn y fformat cywir, megis enw@enghraifft.com")
}
