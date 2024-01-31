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

package testOnly.models.formsmodel

import essttp.rootmodel.TaxRegime
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

object TaxRegimeForm {

  val form: Form[TaxRegime] = {
    val taxRegimeMapping: Mapping[TaxRegime] = Forms.of(
      EnumFormatter.format(
        `enum`                  = TaxRegime,
        errorMessageIfMissing   = "Select a tax regime",
        errorMessageIfEnumError = "Select a tax regime",
        insensitive             = true
      )
    )

    Form(
      mapping(
        "taxRegime" -> taxRegimeMapping
      )(identity)(Some(_))
    )
  }

}
