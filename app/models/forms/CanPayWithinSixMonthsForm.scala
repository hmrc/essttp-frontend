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

package models.forms

import messages.Messages
import models.Language
import models.enumsforforms.CanPayWithinSixMonthsFormValue
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

object CanPayWithinSixMonthsForm {

  def form(implicit language: Language): Form[CanPayWithinSixMonthsFormValue] = {

    val canPayMapping: Mapping[CanPayWithinSixMonthsFormValue] = Forms.of(EnumFormatter.format(
      `enum`                  = CanPayWithinSixMonthsFormValue,
      errorMessageIfMissing   = Messages.CanPayWithinSixMonths.`Select yes if you can pay within 6 months`.show,
      errorMessageIfEnumError = Messages.CanPayWithinSixMonths.`Select yes if you can pay within 6 months`.show
    ))

    Form(
      mapping(
        "CanPayWithinSixMonths" -> canPayMapping
      )(identity)(Some(_))
    )
  }

}
