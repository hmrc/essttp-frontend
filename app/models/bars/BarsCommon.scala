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

package models.bars

import cats.Eq
import essttp.rootmodel.bank.{AccountNumber, SortCode}
import org.apache.commons.lang3.StringUtils
import play.api.libs.json.{Format, Json}

object BarsCommon {

  // request

  final case class BarsBankAccount(
      sortCode:      SortCode,
      accountNumber: AccountNumber
  )

  object BarsBankAccount {
    implicit val format: Format[BarsBankAccount] = Json.format

    def padded(sortCode: SortCode, accountNumber: AccountNumber): BarsBankAccount = {
      BarsBankAccount(sortCode, leftPad(accountNumber))
    }

    private val minimumLength = 8
    private val padStr = "0"
    private def leftPad(accountNumber: AccountNumber): AccountNumber = {
      AccountNumber(StringUtils.leftPad(accountNumber.value, minimumLength, padStr))
    }

  }

  final case class BarsAddress(
      lines:    List[String], // One to four lines; cumulative length must be between 1 and 140 characters.
      town:     Option[String], // Must be between 1 and 35 characters long
      postcode: Option[String] // Must be between 5 and 8 characters long, all uppercase. The internal space character can be omitted.
  )

  object BarsAddress {
    implicit val format: Format[BarsAddress] = Json.format
  }

  // response

  import enumeratum._

  sealed trait BarsAssessmentType extends EnumEntry

  object BarsAssessmentType
    extends Enum[BarsAssessmentType]
    with PlayInsensitiveJsonEnum[BarsAssessmentType] {
    implicit val eq: Eq[BarsAssessmentType] = Eq.fromUniversalEquals

    val values = findValues

    case object Yes extends BarsAssessmentType

    case object No extends BarsAssessmentType

    case object Error extends BarsAssessmentType

    case object Indeterminate extends BarsAssessmentType

    case object Inapplicable extends BarsAssessmentType

    case object Partial extends BarsAssessmentType
  }

  // union (or superset) of BarsValidateResponse and BarsVerifyResponse
  final case class BarsResponse(
      accountNumberIsWellFormatted:             BarsAssessmentType,
      nonStandardAccountDetailsRequiredForBacs: BarsAssessmentType,
      sortCodeIsPresentOnEISCD:                 BarsAssessmentType,
      sortCodeSupportsDirectDebit:              Option[BarsAssessmentType],
      sortCodeSupportsDirectCredit:             Option[BarsAssessmentType],
      accountExists:                            Option[BarsAssessmentType],
      nameMatches:                              Option[BarsAssessmentType],
      accountName:                              Option[String],
      sortCodeBankName:                         Option[String],
      iban:                                     Option[String]
  )

  object BarsResponse {
    def apply(resp: BarsValidateResponse): BarsResponse =
      BarsResponse(
        accountNumberIsWellFormatted             = resp.accountNumberIsWellFormatted,
        nonStandardAccountDetailsRequiredForBacs = resp.nonStandardAccountDetailsRequiredForBacs,
        sortCodeIsPresentOnEISCD                 = resp.sortCodeIsPresentOnEISCD,
        sortCodeSupportsDirectDebit              = resp.sortCodeSupportsDirectDebit,
        sortCodeSupportsDirectCredit             = resp.sortCodeSupportsDirectCredit,
        accountExists                            = None,
        nameMatches                              = None,
        accountName                              = None,
        sortCodeBankName                         = resp.sortCodeBankName,
        iban                                     = resp.iban
      )

    def apply(resp: BarsVerifyResponse): BarsResponse =
      BarsResponse(
        accountNumberIsWellFormatted             = resp.accountNumberIsWellFormatted,
        nonStandardAccountDetailsRequiredForBacs = resp.nonStandardAccountDetailsRequiredForBacs,
        sortCodeIsPresentOnEISCD                 = resp.sortCodeIsPresentOnEISCD,
        sortCodeSupportsDirectDebit              = Some(resp.sortCodeSupportsDirectDebit),
        sortCodeSupportsDirectCredit             = Some(resp.sortCodeSupportsDirectCredit),
        accountExists                            = Some(resp.accountExists),
        nameMatches                              = Some(resp.nameMatches),
        accountName                              = resp.accountName,
        sortCodeBankName                         = resp.sortCodeBankName,
        iban                                     = resp.iban
      )

    import cats.syntax.eq._
    import models.bars.BarsCommon.BarsAssessmentType._

    object validateFailure {
      def unapply(response: BarsResponse): Boolean =
        response.sortCodeIsPresentOnEISCD === No ||
          response.accountNumberIsWellFormatted === No ||
          response.sortCodeSupportsDirectDebit.contains(No)
    }

    object verifySuccess {
      def unapply(resp: BarsResponse): Boolean =
        (resp.accountNumberIsWellFormatted === Yes ||
          resp.accountNumberIsWellFormatted === Indeterminate) &&
          (resp.accountExists.contains(Yes) || resp.accountExists.contains(Indeterminate)) &&
          (resp.nameMatches.contains(Yes) || resp.nameMatches.contains(Partial)) &&
          resp.sortCodeSupportsDirectDebit.contains(Yes)
    }

    object sortCodeIsPresentOnEiscdNo {
      def unapply(response: BarsResponse): Boolean =
        response.sortCodeIsPresentOnEISCD === No
    }

    object accountNumberIsWellFormattedNo {
      def unapply(response: BarsResponse): Boolean =
        response.accountNumberIsWellFormatted === No
    }

    object sortCodeSupportsDirectDebitNo {
      def unapply(response: BarsResponse): Boolean =
        response.sortCodeSupportsDirectDebit.contains(No)
    }

  }

  sealed trait BarsTypeOfBankAccount extends EnumEntry

  object BarsTypeOfBankAccount {
    implicit val eq: Eq[BarsTypeOfBankAccount] = Eq.fromUniversalEquals
  }

  object BarsTypesOfBankAccount extends Enum[BarsTypeOfBankAccount] {

    case object Personal extends BarsTypeOfBankAccount
    case object Business extends BarsTypeOfBankAccount

    override val values = findValues
  }

}
