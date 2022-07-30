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
import essttp.rootmodel.bank.{AccountNumber, BankDetails, SortCode}
import play.api.libs.json.{Format, Json}

object BarsModel {
  final case class BankAccount(
      sortCode:      SortCode,
      accountNumber: AccountNumber
  )

  object BankAccount {
    implicit val format: Format[BankAccount] = Json.format
  }

  final case class BarsValidateRequest(
      account: BankAccount
  )

  object BarsValidateRequest {
    implicit val format: Format[BarsValidateRequest] = Json.format

    def apply(bankDetails: BankDetails): BarsValidateRequest =
      BarsValidateRequest(BankAccount(bankDetails.sortCode, bankDetails.accountNumber))
  }

  import enumeratum._

  sealed trait BarsAssessmentType extends EnumEntry

  object BarsAssessmentType extends Enum[BarsAssessmentType] with PlayInsensitiveJsonEnum[BarsAssessmentType] {
    implicit val eq: Eq[BarsAssessmentType] = Eq.fromUniversalEquals

    val values = findValues

    case object Yes extends BarsAssessmentType

    case object No extends BarsAssessmentType

    case object Error extends BarsAssessmentType

    case object Indeterminate extends BarsAssessmentType

    case object Inapplicable extends BarsAssessmentType

    case object Partial extends BarsAssessmentType
  }

  final case class BarsResponse(
      accountNumberIsWellFormatted:             BarsAssessmentType,
      nonStandardAccountDetailsRequiredForBacs: BarsAssessmentType,
      sortCodeIsPresentOnEISCD:                 BarsAssessmentType,
      sortCodeSupportsDirectDebit:              Option[BarsAssessmentType],
      sortCodeSupportsDirectCredit:             Option[BarsAssessmentType],
      sortCodeBankName:                         Option[String],
      iban:                                     Option[String]
  )

  object BarsResponse {
    implicit val format: Format[BarsResponse] = Json.format

    import cats.syntax.eq._

    object sortCodeIsPresentOnEiscdError {
      def unapply(response: BarsResponse): Boolean = response.sortCodeIsPresentOnEISCD === BarsAssessmentType.Error
    }

    object sortCodeIsPresentOnEiscdNo {
      def unapply(response: BarsResponse): Boolean = response.sortCodeIsPresentOnEISCD === BarsAssessmentType.No
    }

    object accountNumberIsWellFormattedNo {
      def unapply(response: BarsResponse): Boolean = response.accountNumberIsWellFormatted === BarsAssessmentType.No
    }

    object sortCodeSupportsDirectDebitNo {
      def unapply(response: BarsResponse): Boolean = response.sortCodeSupportsDirectDebit.contains(BarsAssessmentType.No)
    }
  }

  sealed trait BarsValidateErrorMessageKeys extends EnumEntry

  object BarsValidateErrorMessageKeys extends Enum[BarsValidateErrorMessageKeys] {
    val values = findValues

    case object SortCodeIsPresentOnEiscdNo extends BarsValidateErrorMessageKeys

    case object AccountNumberIsWellFormattedNo extends BarsValidateErrorMessageKeys

    case object SortCodeSupportsDirectDebitNo extends BarsValidateErrorMessageKeys
  }
}
