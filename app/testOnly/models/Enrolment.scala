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

package testOnly.models

import cats.Eq

sealed trait Enrolment extends Product with Serializable

object Enrolment {
  case object EPAYE extends Enrolment
  case object VAT extends Enrolment
  val fromInput: Map[String, Enrolment] = Map(
    "IR-PAYE" -> EPAYE,
    "VAT" -> VAT)
  val toInput: Map[Enrolment, String] = Map(
    EPAYE -> "IR-PAYE",
    VAT -> "VAT")
  implicit val eq: Eq[Enrolment] = Eq.fromUniversalEquals
}
