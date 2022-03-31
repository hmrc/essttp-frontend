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

import play.api.mvc.PathBindable
import uk.gov.hmrc.auth.core.Enrolment

trait TaxRegime {

  def name: String

  def allowEnrolment(enrolment: Enrolment): Boolean

}

object TaxRegime {

  implicit def pathBinder(implicit stringBinder: PathBindable[String]): PathBindable[TaxRegime] = new PathBindable[TaxRegime] {
    override def bind(key: String, value: String): Either[String, TaxRegime] = {
      for {
        regime <- stringBinder.bind(key, value).right
      } yield regimeOf(regime)
    }
    override def unbind(key: String, regime: TaxRegime): String = {
      regime.name
    }
  }

  object EpayeRegime extends TaxRegime {

    val name = "epaye"

    override def allowEnrolment(enrolment: Enrolment): Boolean = enrolment.key == "IR-PAYE"

  }

  object VatRegime extends TaxRegime {

    val name = "vat"

    override def allowEnrolment(enrolment: Enrolment): Boolean = enrolment.key == "IR-VAT"
  }

  def regimeOf(name: String): TaxRegime = name match {
    case "epaye" => EpayeRegime
    case "vat" => VatRegime
    case n => throw new IllegalArgumentException(s"$n is not the name of a tax regime")
  }

}

