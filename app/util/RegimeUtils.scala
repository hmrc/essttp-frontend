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

package util

import essttp.rootmodel.{Aor, TaxId, TaxRegime, Vrn}
import uk.gov.hmrc.auth.core.Enrolment

object RegimeUtils {

  implicit class RegimeOps(regime: TaxRegime) {
    def name: String = regime match {
      case TaxRegime.Epaye => "epaye"
      case TaxRegime.Vat   => "vat"
    }

    def allowEnrolment(enrolment: Enrolment): Boolean = regime match {
      case TaxRegime.Epaye => enrolment.key == "IR-PAYE"
      case TaxRegime.Vat   => enrolment.key == "IR-VAT"
    }

    def taxIdOf(value: String): TaxId = regime match {
      case TaxRegime.Epaye => Aor(value)
      case TaxRegime.Vat   => Vrn(value)
    }
  }

}
