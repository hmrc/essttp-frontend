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

package testOnly.models.formsmodel

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait Enrolment extends EnumEntry

object Enrolments extends Enum[Enrolment] {

  case object Epaye extends Enrolment
  case object Vat   extends Enrolment
  case object IrSa  extends Enrolment
  case object MtdIt extends Enrolment

  override def values: immutable.IndexedSeq[Enrolment] = findValues
}
