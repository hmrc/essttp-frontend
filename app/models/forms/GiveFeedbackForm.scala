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

import langswitch.Language
import messages.Messages
import models.enumsforforms.GiveFeedbackFormValue
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

object GiveFeedbackForm {
  def form(implicit language: Language): Form[GiveFeedbackFormValue] = {
    val giveFeedbackMapping: Mapping[GiveFeedbackFormValue] = Forms.of(EnumFormatter.format(
      enum                    = GiveFeedbackFormValue,
      errorMessageIfMissing   = Messages.GiveFeedback.`Select yes if you want to give feedback`.show,
      errorMessageIfEnumError = Messages.GiveFeedback.`Select yes if you want to give feedback`.show
    ))

    Form(
      mapping(
        "DoYouWantToGiveFeedback" -> giveFeedbackMapping
      )(identity)(Some(_))
    )
  }
}
