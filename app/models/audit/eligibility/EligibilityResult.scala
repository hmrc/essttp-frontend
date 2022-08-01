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

package models.audit.eligibility

import play.api.libs.json.{JsString, Writes}

sealed trait EligibilityResult extends Product with Serializable

object EligibilityResult {

  case object Eligible extends EligibilityResult

  case object Ineligible extends EligibilityResult

  implicit val writes: Writes[EligibilityResult] = Writes {
    case Eligible   => JsString("eligible")
    case Ineligible => JsString("ineligible")
  }

}
