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

package models.forms

import messages.Messages
import models.Language
import models.enumsforforms.IsSoleSignatoryFormValue
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

final case class DetailsAboutBankAccountForm(isSoleSignatory: IsSoleSignatoryFormValue)

object DetailsAboutBankAccountForm {

  def form(implicit language: Language): Form[DetailsAboutBankAccountForm] = {
    Form(
      mapping(
        "isSoleSignatory" -> isSoleSignatoryFormMapping
      )(DetailsAboutBankAccountForm.apply)(DetailsAboutBankAccountForm.unapply)
    )
  }

  private def isSoleSignatoryFormMapping(implicit language: Language): Mapping[IsSoleSignatoryFormValue] = Forms.of(EnumFormatter.format(
    `enum`                  = IsSoleSignatoryFormValue,
    errorMessageIfMissing   = Messages.AboutYourBankAccount.`Select yes if you can set up a Direct Debit for this payment plan`.show,
    errorMessageIfEnumError = Messages.AboutYourBankAccount.`Select yes if you can set up a Direct Debit for this payment plan`.show
  ))
}
