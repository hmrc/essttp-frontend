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
import play.api.libs.json.{Format, Json}

case class Enrolment private (ordinal: Int, name: String)

object Enrolment {
  val EPAYE = Enrolment(0, "EPAYE")
  val VAT = Enrolment(1, "VAT")
  implicit val eq: Eq[Enrolment] = Eq.fromUniversalEquals

  def valueOf(name: String) = name match {
    case "EPAYE" => EPAYE
    case "VAT"   => VAT
    case s       => throw new IllegalArgumentException(s"$s is not a valid Enrolment")
  }

  def values = List(EPAYE, VAT)

  implicit val fmt: Format[Enrolment] = Json.format[Enrolment]

}
