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

import essttp.rootmodel.bank.{AccountName, AccountNumber, SortCode}
import models.forms.helper.FormErrorWithFieldMessageOverrides
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, FormError, Mapping}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

final case class BankDetailsForm(
    name:          AccountName,
    sortCode:      SortCode,
    accountNumber: AccountNumber
)

object BankDetailsForm {

  private val accountNameMinLength: Int = 2
  private val accountNameMaxLength: Int = 39
  private val accountNameAllowedSpecialCharacters: Set[Char] =
    Set(' ', '&', '@', '(', ')', '!', ':', ',', '+', '`', '-', '\\', '\'', '.', '/', '^')

  val accountNameConstraintRegex: Constraint[AccountName] = Constraint { encryptedAccountName =>
    val accountName = encryptedAccountName.value.decryptedValue.filter(!_.isControl).trim

    if (accountName.isEmpty) Invalid("error.required")
    else if (accountName.length < accountNameMinLength) Invalid("error.minLength")
    else if (accountName.length > accountNameMaxLength) Invalid("error.maxLength")
    else {
      val disallowedCharacters = accountName.filterNot(
        c => c.isLetter || c.isDigit || accountNameAllowedSpecialCharacters.contains(c)
      ).toList.distinct

      if (disallowedCharacters.nonEmpty) Invalid("error.disallowedCharacters", disallowedCharacters: _*)
      else Valid
    }
  }

  val accountNameMapping: Mapping[AccountName] =
    nonEmptyText.transform[AccountName](name => AccountName(SensitiveString.apply(name)), _.value.decryptedValue).verifying(accountNameConstraintRegex)

  val allowedSeparators: Set[Char] = Set(' ', '-', '–', '−', '—')

  val sortCodeRegex: String = "^[0-9]{6}$"
  val sortCodeConstraint: Constraint[SortCode] =
    Constraint(sortCode =>
      if (!sortCode.value.decryptedValue.forall(_.isDigit)) Invalid("error.nonNumeric")
      else if (sortCode.value.decryptedValue.matches(sortCodeRegex)) Valid
      else Invalid("error.invalid"))

  val sortCodeMapping: Mapping[SortCode] = nonEmptyText
    .transform[SortCode](
      sortCode => SortCode(SensitiveString.apply(sortCode.filterNot(allowedSeparators.contains))),
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
      accountNumber => AccountNumber(SensitiveString.apply(accountNumber.filterNot(allowedSeparators.contains))),
      _.value.decryptedValue
    ).verifying(accountNumberConstraint)

  private val sortCodeAndAccountNumberOverrides: Seq[FormError] = Seq(
    FormError("sortCode", ""), // 'turns off' the sortCode field error
    FormError("accountNumber", ""), // 'turns off' the accountNumber field error
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
      formError = FormError("sortCode", "sortCode.validate.sortCodeSupportsDirectDebit.no")
    )
  val nameDoesNotMatch: FormErrorWithFieldMessageOverrides =
    FormErrorWithFieldMessageOverrides(
      formError = FormError("name", "name.verify.nameMatches.no")
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

  val form: Form[BankDetailsForm] =
    Form(
      mapping(
        "name" -> accountNameMapping,
        "sortCode" -> sortCodeMapping,
        "accountNumber" -> accountNumberMapping
      )(BankDetailsForm.apply)(BankDetailsForm.unapply)
    )

}
