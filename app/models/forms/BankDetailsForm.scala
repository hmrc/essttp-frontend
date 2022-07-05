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

import essttp.rootmodel.bank.{SortCode, AccountNumber}
import models.enumsforforms.IsSoleSignatoryFormValue
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Forms, Mapping}
import util.EnumFormatter

final case class BankDetailsForm(
    name:            String,
    sortCode:        SortCode,
    accountNumber:   AccountNumber,
    isSoleSignatory: IsSoleSignatoryFormValue
)
object BankDetailsForm {
  def form: Form[BankDetailsForm] = {

    val sortCodeRegex: String = "^[0-9]{6}$"

    val sortCodeConstraint: Constraint[SortCode] =
      Constraint(sortCode =>
        if (!sortCode.value.forall(_.isDigit)) Invalid("error.nonNumeric")
        else if (sortCode.value.matches(sortCodeRegex)) Valid
        else Invalid("error.invalid"))

    val sortCodeMapping: Mapping[SortCode] = nonEmptyText
      .transform[SortCode](
        s => SortCode(s.replaceAllLiterally("-", "").replaceAll("\\s", "")),
        _.value
      ).verifying(sortCodeConstraint)

    val accountNumberRegex: String = "^[0-9]{6,8}$"

    val accountNumberConstraint: Constraint[AccountNumber] =
      Constraint(accountNumber =>
        if (!accountNumber.value.forall(_.isDigit)) Invalid("error.nonNumeric")
        else if (accountNumber.value.matches(accountNumberRegex)) Valid
        else Invalid("error.invalid"))

    val accountNumberMapping: Mapping[AccountNumber] = nonEmptyText
      .transform[AccountNumber](
        s => AccountNumber(s.replaceAll("\\s", "")),
        _.value
      ).verifying(accountNumberConstraint)

    val isSoleSignatoryFormMapping: Mapping[IsSoleSignatoryFormValue] = Forms.of(EnumFormatter.format(
      enum                    = IsSoleSignatoryFormValue,
      errorMessageIfMissing   = "error.required",
      errorMessageIfEnumError = "error.required"
    ))

    Form(
      mapping(
        "name" -> nonEmptyText(maxLength = 100),
        "sortCode" -> sortCodeMapping,
        "accountNumber" -> accountNumberMapping,
        "isSoleSignatory" -> isSoleSignatoryFormMapping
      )(BankDetailsForm.apply)(BankDetailsForm.unapply)
    )

  }
}
