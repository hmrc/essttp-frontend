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

package models.forms

import essttp.rootmodel.TaxRegime
import messages.Messages
import models.Language
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

object TaxRegimeForm {

  def form(implicit language: Language): Form[TaxRegime] = {
    val taxRegimeMapping: Mapping[TaxRegime] = Forms.of(
      EnumFormatter.format(
        enum                    = TaxRegime,
        errorMessageIfMissing   = Messages.WhichTaxRegime.`Select which tax you want to set up a payment plan for`.show,
        errorMessageIfEnumError = Messages.WhichTaxRegime.`Select which tax you want to set up a payment plan for`.show,
        insensitive             = true
      )
    )

    Form(
      mapping(
        "WhichTaxRegime" -> taxRegimeMapping
      )(identity)(Some(_))
    )
  }

}
