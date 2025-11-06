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

import cats.implicits.catsSyntaxOptionId
import messages.Messages
import models.Language
import models.enumsforforms.TypeOfAccountFormValue
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

final case class TypeOfBankAccountForm(
  typeOfBankAccount: TypeOfAccountFormValue
)

object TypeOfBankAccountForm {

  def typeOfBankAccountMapping(using Language): Mapping[TypeOfAccountFormValue] = Forms.of(
    EnumFormatter.format(
      `enum` = TypeOfAccountFormValue,
      errorMessageIfMissing = Messages.TypeOfBankAccount.`Select what type of account details you are providing`.show,
      errorMessageIfEnumError = Messages.TypeOfBankAccount.`Select what type of account details you are providing`.show
    )
  )

  def form(using Language): Form[TypeOfBankAccountForm] =
    Form(
      mapping(
        "accountType" -> typeOfBankAccountMapping
      )(TypeOfBankAccountForm.apply)(_.typeOfBankAccount.some)
    )

}
