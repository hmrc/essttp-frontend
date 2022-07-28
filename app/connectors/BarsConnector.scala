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

package connectors

import cats.Eq
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import connectors.BarsConnector.{BarsValidateRequest, BarsValidateResponse}
import essttp.rootmodel.bank.{AccountNumber, BankDetails, SortCode}
import play.api.libs.json.{Format, Json}
import play.api.mvc.RequestHeader
import requests.RequestSupport._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits.{readFromJson, readUnit => _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  /**
   * TODO
   */
  private val validateUrl: String = appConfig.BaseUrl.barsUrl + "/validate/bank-details"

  def validateBankDetails(barsValidateRequest: BarsValidateRequest)(implicit requestHeader: RequestHeader): Future[BarsValidateResponse] = {
    httpClient.POST[BarsValidateRequest, BarsValidateResponse](validateUrl, barsValidateRequest)
  }
}

object BarsConnector {
  final case class BankAccount(
      sortCode:      SortCode, // 6 characters long (whitespace/dashes should be removed)
      accountNumber: AccountNumber // 8 characters long
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

  final case class BarsValidateResponse(
      accountNumberIsWellFormatted:             BarsAssessmentType,
      nonStandardAccountDetailsRequiredForBacs: BarsAssessmentType,
      sortCodeIsPresentOnEISCD:                 BarsAssessmentType,
      sortCodeSupportsDirectDebit:              Option[BarsAssessmentType],
      sortCodeSupportsDirectCredit:             Option[BarsAssessmentType],
      sortCodeBankName:                         Option[String],
      iban:                                     Option[String]
  )

  object BarsValidateResponse {
    implicit val format: Format[BarsValidateResponse] = Json.format

    import connectors.BarsConnector.BarsAssessmentType._
    import cats.syntax.eq._

    object sortCodeIsPresentOnEiscdError {
      def unapply(response: BarsValidateResponse): Boolean = response.sortCodeIsPresentOnEISCD === Error
    }

    object sortCodeIsPresentOnEiscdNo {
      def unapply(response: BarsValidateResponse): Boolean = response.sortCodeIsPresentOnEISCD === No
    }

    object accountNumberIsWellFormattedNo {
      def unapply(response: BarsValidateResponse): Boolean = response.accountNumberIsWellFormatted === No
    }

    object sortCodeSupportsDirectDebitNo {
      def unapply(response: BarsValidateResponse): Boolean = response.sortCodeSupportsDirectDebit.contains(No)
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
