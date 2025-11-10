/*
 * Copyright 2025 HM Revenue & Customs
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

import enumeratum.{Enum, EnumEntry}
import essttp.rootmodel.TaxRegime

sealed trait TaxRegimeFormValue extends EnumEntry derives CanEqual

object TaxRegimeFormValue extends Enum[TaxRegimeFormValue] {

  case object SA    extends TaxRegimeFormValue
  case object Epaye extends TaxRegimeFormValue
  case object Vat   extends TaxRegimeFormValue
  case object Simp  extends TaxRegimeFormValue
  case object Other extends TaxRegimeFormValue

  override def values: IndexedSeq[TaxRegimeFormValue] = findValues

  def toTaxRegime(taxRegimeFormValue: TaxRegimeFormValue): Option[TaxRegime] = taxRegimeFormValue match {
    case SA    => Some(TaxRegime.Sa)
    case Epaye => Some(TaxRegime.Epaye)
    case Vat   => Some(TaxRegime.Vat)
    case Simp  => Some(TaxRegime.Simp)
    case Other => None
  }

}
