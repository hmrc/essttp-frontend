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

import connectors.BarsConnector
import models.bars.{BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import models.bars.request._
import models.bars.response.ValidateResponse.validateFailure
import models.bars.response.VerifyResponse.accountDoesNotExist
import models.bars.response._
import play.api.Logging
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Bank Account Reputation service (BARs).
 */
@Singleton
class BarsService @Inject() (barsConnector: BarsConnector)(implicit ec: ExecutionContext) extends Logging {

  def validateBankAccount(bankAccount: BarsBankAccount)(implicit requestHeader: RequestHeader): Future[ValidateResponse] =
    barsConnector.validateBankDetails(BarsValidateRequest(bankAccount)).map(ValidateResponse.apply)

  def verifyPersonal(bankAccount: BarsBankAccount, subject: BarsSubject)(
      implicit
      requestHeader: RequestHeader
  ): Future[VerifyResponse] =
    barsConnector.verifyPersonal(BarsVerifyPersonalRequest(bankAccount, subject)).map(VerifyResponse.apply)

  def verifyBusiness(bankAccount: BarsBankAccount, business: BarsBusiness)(
      implicit
      requestHeader: RequestHeader
  ): Future[VerifyResponse] =
    barsConnector.verifyBusiness(BarsVerifyBusinessRequest(bankAccount, business)).map(VerifyResponse.apply)

  /**
   * Call Validate first and if that fails, then Return the failing response
   * Otherwise, call either Verify/Personal or Verify/Business
   * If success response then Return the response,
   * If there is an AccountDoesNotExist error, then call the other Verify endpoint
   * Return the response (success or fail)
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
      case _ =>
        typeOfBankAccount match {
          case BarsTypesOfBankAccount.Personal =>
            verifyPersonal(bankAccount, subject).flatMap {
              case accountDoesNotExist() =>
                verifyBusiness(bankAccount, business).map(handleVerifyResponse)
              case verifyPersonalResp => Future.successful(handleVerifyResponse(verifyPersonalResp))
            }
          case BarsTypesOfBankAccount.Business =>
            verifyBusiness(bankAccount, business).flatMap {
              case accountDoesNotExist() =>
                verifyPersonal(bankAccount, subject).map(handleVerifyResponse)
              case verifyBusinessResp => Future.successful(handleVerifyResponse(verifyBusinessResp))
            }
        }
    }
  }

  private def handleValidateErrorResponse(response: ValidateResponse): BarsError = {
    response match {
      case ValidateResponse.accountNumberIsWellFormattedNo() => AccountNumberNotWellFormatted(response)
      case ValidateResponse.sortCodeIsPresentOnEiscdNo()     => SortCodeNotPresentOnEiscd(response)
      case ValidateResponse.sortCodeSupportsDirectDebitNo()  => SortCodeDoesNotSupportDirectDebit(response)
    }
  }

  private def handleVerifyResponse(response: VerifyResponse): Either[BarsError, VerifyResponse] = {
    response match {
      case VerifyResponse.thirdPartyError() => Left(ThirdPartyError(response))
      case VerifyResponse.accountNumberIsWellFormattedNo() => Left(AccountNumberNotWellFormatted(response))
      case VerifyResponse.sortCodeIsPresentOnEiscdNo() => Left(SortCodeNotPresentOnEiscd(response))
      case VerifyResponse.sortCodeSupportsDirectDebitNo() => Left(SortCodeDoesNotSupportDirectDebit(response))
      case VerifyResponse.nameMatchesNo() => Left(NameDoesNotMatch(response))
      case VerifyResponse.accountDoesNotExist() => Left(AccountDoesNotExist(response))
      // ok
      case _ => Right(response)
    }
  }
}
