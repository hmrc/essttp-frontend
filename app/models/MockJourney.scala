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

import essttp.journey.model.JourneyId
import essttp.rootmodel.AmountInPence
import play.api.libs.json.{Json, OFormat}

import java.time.Instant

final case class MockJourney(
    qualifyingDebt: AmountInPence = AmountInPence(210000L),
    remainingToPay: AmountInPence = AmountInPence(210000L),
    userAnswers:    UserAnswers   = UserAnswers.empty
)

object MockJourney {
  implicit val format: OFormat[Journey] = Json.format[Journey]
}
//todo this whole file needs to be deleted, but this change is already massive... I've put this in just to make the rest compile where mockJourney is used...
final case class Journey(
    _id:            JourneyId,
    createdDate:    Instant,
    lastUpdated:    Instant       = Instant.now,
    status:         JourneyStatus,
    qualifyingDebt: AmountInPence,
    remainingToPay: AmountInPence,
    userAnswers:    UserAnswers
) {
  def id: JourneyId = _id
}

object Journey {
  implicit val format: OFormat[Journey] = Json.format[Journey]
}
