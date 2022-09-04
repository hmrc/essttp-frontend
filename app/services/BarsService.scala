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

package services

import cats.implicits.catsSyntaxEq
import connectors.BarsConnector
import models.bars.request._
import models.bars.response.ValidateResponse.validateFailure
import models.bars.response.VerifyResponse.accountDoesNotExist
import models.bars.response._
import models.bars.{BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import util.HttpResponseUtils.HttpResponseOps
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Bank Account Reputation service (BARs).
 */
@Singleton
class BarsService @Inject() (barsConnector: BarsConnector)(implicit ec: ExecutionContext) extends Logging {

  // NOTE: if the validate call is removed in the future (it is said to be deprecated)
  // then implement the "SORT_CODE_ON_DENY_LIST" handling in the verify calls below
  def validateBankAccount(bankAccount: BarsBankAccount)(implicit requestHeader: RequestHeader): Future[BarsResponse] = {

    barsConnector.validateBankDetails(BarsValidateRequest(bankAccount)).map { httpResponse: HttpResponse =>
      httpResponse.status match {
        case OK =>
          httpResponse.parseJSON[BarsValidateResponse].map(ValidateResponse.apply)
            .getOrElse(throw UpstreamErrorResponse(httpResponse.body, httpResponse.status))

        case BAD_REQUEST =>
          httpResponse.json.validate[BarsErrorResponse] match {
            case JsSuccess(barsErrorResponse, _) if barsErrorResponse.code === "SORT_CODE_ON_DENY_LIST" =>
              SortCodeOnDenyList(barsErrorResponse)
            case JsError(_) =>
              throw UpstreamErrorResponse(httpResponse.body, httpResponse.status)
          }

        case _ =>
          throw UpstreamErrorResponse(httpResponse.body, httpResponse.status)
      }
    }
  }

  // implement sortCodeOnDenyList (if validate is removed)
  def verifyPersonal(bankAccount: BarsBankAccount, subject: BarsSubject)(
      implicit
      requestHeader: RequestHeader
  ): Future[VerifyResponse] =
    barsConnector.verifyPersonal(BarsVerifyPersonalRequest(bankAccount, subject)).map(VerifyResponse.apply)

  // implement sortCodeOnDenyList (if validate is removed)
  def verifyBusiness(bankAccount: BarsBankAccount, business: BarsBusiness)(
      implicit
      requestHeader: RequestHeader
  ): Future[VerifyResponse] =
    barsConnector.verifyBusiness(BarsVerifyBusinessRequest(bankAccount, business)).map(VerifyResponse.apply)

  /**
   * Call Validate first and if that fails, then Return the failing response
   * Otherwise, call either Verify/Personal or Verify/Business
   */
  def verifyBankDetails(
      bankAccount:       BarsBankAccount,
      subject:           BarsSubject,
      business:          BarsBusiness,
      typeOfBankAccount: BarsTypeOfBankAccount
  )(implicit requestHeader: RequestHeader, ec: ExecutionContext): Future[Either[BarsError, VerifyResponse]] = {

    validateBankAccount(bankAccount).flatMap {
      case validateResponse @ validateFailure() =>
        Future.successful(Left(handleValidateErrorResponse(validateResponse)))
      case response: SortCodeOnDenyList =>
        Future.successful(Left(SortCodeOnDenyListErrorValidateResponse(response)))
      case _ =>
        (typeOfBankAccount match {
          case BarsTypesOfBankAccount.Personal => verifyPersonal(bankAccount, subject)
          case BarsTypesOfBankAccount.Business => verifyBusiness(bankAccount, business)
        }).map(handleVerifyResponse)
    }
  }

  private def handleValidateErrorResponse(response: ValidateResponse): BarsError = {
    import ValidateResponse._
    response match {
      case accountNumberIsWellFormattedNo() => AccountNumberNotWellFormattedValidateResponse(response)
      case sortCodeIsPresentOnEiscdNo()     => SortCodeNotPresentOnEiscdValidateResponse(response)
      case sortCodeSupportsDirectDebitNo()  => SortCodeDoesNotSupportDirectDebitValidateResponse(response)
    }
  }

  private def handleVerifyResponse(response: VerifyResponse): Either[BarsError, VerifyResponse] = {
    import VerifyResponse._
    response match {
      // success
      case verifySuccess()                  => Right(response)
      // defined errors
      case thirdPartyError()                => Left(ThirdPartyError(response))
      case accountNumberIsWellFormattedNo() => Left(AccountNumberNotWellFormatted(response))
      case sortCodeIsPresentOnEiscdNo()     => Left(SortCodeNotPresentOnEiscd(response))
      case sortCodeSupportsDirectDebitNo()  => Left(SortCodeDoesNotSupportDirectDebit(response))
      case nameMatchesNo()                  => Left(NameDoesNotMatch(response))
      case accountDoesNotExist()            => Left(AccountDoesNotExist(response))
      // not an expected error response or a success response, so fallback to this
      case _                                => Left(OtherBarsError(response))
    }
  }
}
