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

package models.enumsforforms

import essttp.rootmodel.CanPayUpfront
import enumeratum.Enum

import scala.collection.immutable

sealed trait CanPayUpfrontFormValue extends enumeratum.EnumEntry {
  def asCanPayUpfront: CanPayUpfront = this match {
    case CanPayUpfrontFormValue.Yes => CanPayUpfront(true)
    case CanPayUpfrontFormValue.No  => CanPayUpfront(false)
  }
}
object CanPayUpfrontFormValue extends Enum[CanPayUpfrontFormValue] {
  case object Yes extends CanPayUpfrontFormValue
  case object No extends CanPayUpfrontFormValue
  override def values: immutable.IndexedSeq[CanPayUpfrontFormValue] = findValues

  def canPayUpfrontToFormValue(canPayUpfront: CanPayUpfront): CanPayUpfrontFormValue = canPayUpfront match {
    case CanPayUpfront(true)  => CanPayUpfrontFormValue.Yes
    case CanPayUpfront(false) => CanPayUpfrontFormValue.No
  }
}
