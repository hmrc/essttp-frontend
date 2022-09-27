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

import models.enumsforforms.{IsSoleSignatoryFormValue, TypeOfAccountFormValue}
import play.api.data.{Form, Forms, Mapping}
import play.api.data.Forms.mapping
import util.EnumFormatter
import models.Language
import messages.Messages

final case class DetailsAboutBankAccountForm(typeOfAccount: TypeOfAccountFormValue, isSoleSignatory: IsSoleSignatoryFormValue)

object DetailsAboutBankAccountForm {

  def form(implicit language: Language): Form[DetailsAboutBankAccountForm] = {
    val typeOfAccountFormMapping: Mapping[TypeOfAccountFormValue] = Forms.of(EnumFormatter.format(
      enum                    = TypeOfAccountFormValue,
      errorMessageIfMissing   = Messages.AboutYourBankAccount.`Select what type of account details you are providing`.show,
      errorMessageIfEnumError = Messages.AboutYourBankAccount.`Select what type of account details you are providing`.show
    ))
    Form(
      mapping(
        "typeOfAccount" -> typeOfAccountFormMapping,
        "isSoleSignatory" -> isSoleSignatoryFormMapping
      )(DetailsAboutBankAccountForm.apply)(DetailsAboutBankAccountForm.unapply)
    )
  }

  private def isSoleSignatoryFormMapping(implicit language: Language): Mapping[IsSoleSignatoryFormValue] = Forms.of(EnumFormatter.format(
    enum                    = IsSoleSignatoryFormValue,
    errorMessageIfMissing   = Messages.AboutYourBankAccount.`Select yes if you are the account holder`.show,
    errorMessageIfEnumError = Messages.AboutYourBankAccount.`Select yes if you are the account holder`.show
  ))
}
