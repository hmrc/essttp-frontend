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

package models.bars.response

import models.bars.response.BarsAssessmentType._

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

  /**
   * The sortCode is good, but account does not exist
   * - in this case, having called verify/personal
   *    calling verify/business may result in a positive response
   *    (or vice versa)
   */
  object accountDoesNotExist {
    def unapply(resp: BarsResponse): Boolean =
      (resp.accountNumberIsWellFormatted === Yes ||
        resp.accountNumberIsWellFormatted === Indeterminate) &&
        resp.accountExists.contains(No) &&
        resp.sortCodeIsPresentOnEISCD === Yes &&
        resp.sortCodeSupportsDirectDebit.contains(Yes)
  }

  object thirdPartyError {
    def unapply(resp: BarsResponse): Boolean =
      resp.accountExists.contains(Error) || resp.nameMatches.contains(Error)
  }

  object sortCodeIsPresentOnEiscdNo {
    def unapply(resp: BarsResponse): Boolean =
      resp.sortCodeIsPresentOnEISCD === No
  }

  object nameMatchesNo {
    def unapply(resp: BarsResponse): Boolean =
      resp.nameMatches.contains(No) &&
        (resp.accountExists.contains(Yes) ||
          resp.accountExists.contains(Indeterminate))
  }

  object accountNumberIsWellFormattedNo {
    def unapply(resp: BarsResponse): Boolean =
      resp.accountNumberIsWellFormatted === No
  }

  object sortCodeSupportsDirectDebitNo {
    def unapply(resp: BarsResponse): Boolean =
      resp.sortCodeSupportsDirectDebit.contains(No)
  }

}
