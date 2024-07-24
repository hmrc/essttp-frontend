/*
 * Copyright 2024 HM Revenue & Customs
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

package models.enumsforforms

import enumeratum.Enum
import essttp.journey.model.CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths

import scala.collection.immutable

sealed trait CanPayWithinSixMonthsFormValue extends enumeratum.EnumEntry {

  def asCanPayUpfront: CanPayWithinSixMonths = this match {
    case CanPayWithinSixMonthsFormValue.Yes => CanPayWithinSixMonths(value = true)
    case CanPayWithinSixMonthsFormValue.No  => CanPayWithinSixMonths(value = false)
  }

}
object CanPayWithinSixMonthsFormValue extends Enum[CanPayWithinSixMonthsFormValue] {

  case object Yes extends CanPayWithinSixMonthsFormValue

  case object No extends CanPayWithinSixMonthsFormValue

  override def values: immutable.IndexedSeq[CanPayWithinSixMonthsFormValue] = findValues

  def canPayUpfrontToFormValue(canPayWithinSixMonths: CanPayWithinSixMonths): CanPayWithinSixMonthsFormValue =
    canPayWithinSixMonths match {
      case CanPayWithinSixMonths(true)  => CanPayWithinSixMonthsFormValue.Yes
      case CanPayWithinSixMonths(false) => CanPayWithinSixMonthsFormValue.No
    }
}

