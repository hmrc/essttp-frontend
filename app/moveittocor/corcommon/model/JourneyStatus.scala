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

import enumeratum.{Enum, _}
import moveittocor.corcommon.internal.jsonext.EnumFormat
import play.api.libs.json.Format

import scala.collection.immutable

sealed trait JourneyStatus extends EnumEntry

object JourneyStatus {
  implicit val format: Format[JourneyStatus] = EnumFormat(JourneyStatuses)
}

object JourneyStatuses extends Enum[JourneyStatus] {
  case object Created extends JourneyStatus
  case object InProgress extends JourneyStatus
  case object Finished extends JourneyStatus

  override def values: immutable.IndexedSeq[JourneyStatus] = findValues
}
