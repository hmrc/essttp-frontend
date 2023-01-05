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

package testsupport.testdata

import essttp.emailverification.EmailVerificationState

object EmailVerificationStatusResponses {

  val okToBeVerified = """{"OkToBeVerified":{}}"""
  val alreadyVerified = """{"AlreadyVerified":{}}"""
  val tooManyPasscodeAttempts = """{"TooManyPasscodeAttempts":{}}"""
  val tooManyPasscodeJourneysStarted = """{"TooManyPasscodeJourneysStarted":{}}"""
  val tooManyDifferentEmailAddresses = """{"TooManyDifferentEmailAddresses":{}}"""

  def emailVerificationStatusJson(emailVerificationState: EmailVerificationState): String = emailVerificationState match {
    case EmailVerificationState.OkToBeVerified                 => okToBeVerified
    case EmailVerificationState.AlreadyVerified                => alreadyVerified
    case EmailVerificationState.TooManyPasscodeAttempts        => tooManyPasscodeAttempts
    case EmailVerificationState.TooManyPasscodeJourneysStarted => tooManyPasscodeJourneysStarted
    case EmailVerificationState.TooManyDifferentEmailAddresses => tooManyDifferentEmailAddresses
  }

}
