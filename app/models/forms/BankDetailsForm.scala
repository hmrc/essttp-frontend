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

import essttp.rootmodel.bank.{AccountName, AccountNumber, SortCode}
import models.enumsforforms.IsSoleSignatoryFormValue
import models.forms.helper.FormErrorWithFieldMessageOverrides
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormError, Forms, Mapping}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import util.EnumFormatter

final case class BankDetailsForm(
    name:            AccountName,
    sortCode:        SortCode,
    accountNumber:   AccountNumber,
    isSoleSignatory: IsSoleSignatoryFormValue
)

object BankDetailsForm {
  def form: Form[BankDetailsForm] =
    Form(
      mapping(
        "name" -> accountNameMapping,
        "sortCode" -> sortCodeMapping,
        "accountNumber" -> accountNumberMapping,
        "isSoleSignatory" -> isSoleSignatoryFormMapping
      )(BankDetailsForm.apply)(BankDetailsForm.unapply)
    )

  val accountNameConstraint: Constraint[AccountName] = {
    Constraint(accountName =>
      if (accountName.value.decryptedValue.length <= 70) Valid
      else Invalid("error.maxlength"))
  }
  val accountNameMapping: Mapping[AccountName] =
    nonEmptyText.transform[AccountName](name => AccountName(SensitiveString.apply(name)), _.value.decryptedValue).verifying(accountNameConstraint)

  val sortCodeRegex: String = "^[0-9]{6}$"
  val sortCodeConstraint: Constraint[SortCode] =
    Constraint(sortCode =>
      if (!sortCode.value.decryptedValue.forall(_.isDigit)) Invalid("error.nonNumeric")
      else if (sortCode.value.decryptedValue.matches(sortCodeRegex)) Valid
      else Invalid("error.invalid"))

  val sortCodeMapping: Mapping[SortCode] = nonEmptyText
    .transform[SortCode](
      sortCode => SortCode(SensitiveString.apply(sortCode.replaceAllLiterally("-", "").replaceAll("\\s", ""))),
      _.value.decryptedValue
    ).verifying(sortCodeConstraint)

  val accountNumberRegex: String = "^[0-9]{6,8}$"

  val accountNumberConstraint: Constraint[AccountNumber] =
    Constraint(accountNumber =>
      if (!accountNumber.value.decryptedValue.forall(_.isDigit)) Invalid("error.nonNumeric")
      else if (accountNumber.value.decryptedValue.matches(accountNumberRegex)) Valid
      else Invalid("error.invalid"))

  val accountNumberMapping: Mapping[AccountNumber] = nonEmptyText
    .transform[AccountNumber](
      accountNumber => AccountNumber(SensitiveString.apply(accountNumber.replaceAll("\\s", ""))),
      _.value.decryptedValue
    ).verifying(accountNumberConstraint)

  val isSoleSignatoryFormMapping: Mapping[IsSoleSignatoryFormValue] = Forms.of(EnumFormatter.format(
    enum                    = IsSoleSignatoryFormValue,
    errorMessageIfMissing   = "error.required",
    errorMessageIfEnumError = "error.required"
  ))

  private val sortCodeAndAccountNumberOverrides: Seq[FormError] = Seq(
    FormError("sortCode", ""), // FormErrors with usual styling but no content
    FormError("accountNumber", ""), // FormErrors with usual styling but no content
    FormError("sortCodeAndAccountNumber", "sortCode.validate.accountNumberIsWellFormatted.no")
  )
  val accountNumberNotWellFormatted: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "sortCode.validate.accountNumberIsWellFormatted.no"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val sortCodeNotPresentOnEiscd: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "sortCode.validate.sortCodeIsPresentOnEISCD.no"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val sortCodeDoesNotSupportsDirectDebit: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "sortCode.validate.sortCodeSupportsDirectDebit.no"),
      fieldMessageOverrides = Seq()
    )
  val nameDoesNotMatch: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("name", "name.verify.nameMatches.no"),
      fieldMessageOverrides = Seq()
    )
  val accountDoesNotExist: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "sortCode.verify.accountExists.no"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val sortCodeOnDenyList: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "sortCode.verify.sortCodeOnDenyList"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )
  val otherBarsError: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError             = FormError("sortCode", "sortCode.verify.otherError"),
      fieldMessageOverrides = sortCodeAndAccountNumberOverrides
    )

}
