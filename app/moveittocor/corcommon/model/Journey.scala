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

package moveittocor.corcommon.model

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

final case class Journey(
  _id: JourneyId,
  createdDate: Instant,
  lastUpdated: Instant = Instant.now,
  status: JourneyStatus,
  amount: Option[AmountInPence]
  ) {
  def id: JourneyId = _id

  def getAmount: AmountInPence = amount.getOrElse(throw new RuntimeException("amount should be there"))
}

object Journey {
  implicit val format: OFormat[Journey] = Json.format[Journey]
}