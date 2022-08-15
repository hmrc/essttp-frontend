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

sealed trait BarsResponse
final case class ValidateResponse(barsValidateResponse: BarsValidateResponse) extends BarsResponse
final case class VerifyResponse(barsVerifyResponse: BarsVerifyResponse) extends BarsResponse

object BarsResponse {
  import cats.syntax.eq._

  object validateFailure {
    def unapply(response: ValidateResponse): Boolean =
      response.barsValidateResponse.sortCodeIsPresentOnEISCD === No ||
        response.barsValidateResponse.accountNumberIsWellFormatted === No ||
        response.barsValidateResponse.sortCodeSupportsDirectDebit.contains(No)
  }

  object thirdPartyError {
    def unapply(barsResponse: BarsResponse): Boolean =
      barsResponse match {
        case ValidateResponse(_) => false // N/A
        case VerifyResponse(resp) =>
          resp.accountExists === Error || resp.nameMatches === Error
      }
  }

  object nameMatchesNo {
    def unapply(barsResponse: BarsResponse): Boolean =
      barsResponse match {
        case ValidateResponse(_) => false // N/A
        case VerifyResponse(resp) =>
          resp.nameMatches === No &&
            (resp.accountExists === Yes ||
              resp.accountExists === Indeterminate)
      }
  }

  object accountNumberIsWellFormattedNo {
    def unapply(barsResponse: BarsResponse): Boolean =
      barsResponse match {
        case ValidateResponse(resp) => resp.accountNumberIsWellFormatted === No
        case VerifyResponse(resp)   => resp.accountNumberIsWellFormatted === No
      }
  }

  object sortCodeIsPresentOnEiscdNo {
    def unapply(barsResponse: BarsResponse): Boolean =
      barsResponse match {
        case ValidateResponse(resp) => resp.sortCodeIsPresentOnEISCD === No
        case VerifyResponse(resp)   => resp.sortCodeIsPresentOnEISCD === No
      }
  }

  object sortCodeSupportsDirectDebitNo {
    def unapply(barsResponse: BarsResponse): Boolean =
      barsResponse match {
        case ValidateResponse(resp) => resp.sortCodeSupportsDirectDebit.contains(No)
        case VerifyResponse(resp)   => resp.sortCodeSupportsDirectDebit === No
      }
  }

  /**
   * The sortCode is good, but account does not exist
   * - in this case, having called verify/personal
   *    calling verify/business may result in a positive response
   *    (or vice versa)
   */
  object accountDoesNotExist {
    def unapply(barsResponse: BarsResponse): Boolean =
      barsResponse match {
        case ValidateResponse(_) => false // N/A
        case VerifyResponse(resp) =>
          (resp.accountNumberIsWellFormatted === Yes ||
            resp.accountNumberIsWellFormatted === Indeterminate) &&
            resp.accountExists === No &&
            resp.sortCodeIsPresentOnEISCD === Yes &&
            resp.sortCodeSupportsDirectDebit === Yes
      }
  }

}
