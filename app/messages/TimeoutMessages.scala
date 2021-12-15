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

object TimeoutMessages {
  val loggedOut_message: Message = Message(
    english = "For your security, we will time you out in",
    welsh = "Er eich diogelwch, byddwn yn dod â’ch sesiwn i ben cyn pen")

  val loggedOut_keepAlive: Message = Message(
    english = "Continue",
    welsh = "Nesaf")

  val loggedOut_signOut: Message = Message(
    english = "Delete your answers",
    welsh = "Dileu’ch atebion")
  val loggedIn_message: Message = Message(
    english = "For your security, we will sign you out in",
    welsh = "Er eich diogelwch, byddwn yn eich allgofnodi cyn pen")

  val loggedIn_keepAlive: Message = Message(
    english = "Stay signed in",
    welsh = "Parhau i fod wedi’ch mewngofnodi")

  val loggedIn_signOut: Message = Message(
    english = "Sign out",
    welsh = "Allgofnodi")

  val timeoutPage_heading: Message = Message(
    english = "For your security, we timed you out",
    welsh = "Er eich diogelwch, rydym wedi dod â’ch sesiwn i ben")

  val timeoutPage_title: Message = Message(
    english = "For your security, we timed you out - Pay your tax - GOV.UK",
    welsh = "Er eich diogelwch, rydym wedi dod â’ch sesiwn i ben - Talwch eich treth - GOV.UK")

  val timeoutPage_button: Message = Message(
    english = "Start again",
    welsh = "Dechrau eto")
}
