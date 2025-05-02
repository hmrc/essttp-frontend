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

package models.audit.eligibility

import play.api.libs.json.{JsString, Writes}

sealed trait EnrollmentReasons extends Product, Serializable

object EnrollmentReasons {

  final case class NotEnrolled() extends EnrollmentReasons

  final case class InactiveEnrollment() extends EnrollmentReasons

  final case class DidNotPassEligibilityCheck() extends EnrollmentReasons

  final case class NoNino() extends EnrollmentReasons

  given Writes[EnrollmentReasons] = Writes {
    case NotEnrolled()                => JsString("not enrolled")
    case InactiveEnrollment()         => JsString("inactive enrollment")
    case DidNotPassEligibilityCheck() => JsString("did not pass eligibility check")
    case NoNino()                     => JsString("no nino found")
  }

}
