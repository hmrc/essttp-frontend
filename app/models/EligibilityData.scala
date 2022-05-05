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

package models
import play.api.libs.json.{Format, Json}
import testOnly.models.EligibilityError
import testOnly.models.EligibilityErrors.format

final case class EligibilityData(rejections: Set[EligibilityError], overduePayments: OverDuePayments) {
  def hasRejections: Boolean = rejections.nonEmpty
  def hasMultipleRejections: Boolean = rejections.size > 1
}
// todo maybe add this in, but this is currently using the testonly eligibility errors.... wrong
//object EligibilityData {
//  implicit val format: Format[EligibilityData] = Json.format[EligibilityData]
//}
