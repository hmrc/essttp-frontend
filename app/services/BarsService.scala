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
import models.bars.BarsCommon.BarsResponse.{validateFailure, verifySuccess}
import models.bars.BarsValidateRequest
import models.bars.BarsCommon.{BarsBankAccount, BarsResponse, BarsTypeOfBankAccount}
import models.bars.BarsVerifyRequest._
import play.api.Logging
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Bank Account Reputation service (BARs).
 */
@Singleton
class BarsService @Inject() (barsConnector: BarsConnector)(implicit ec: ExecutionContext) extends Logging {

  def validateBankAccount(bankAccount: BarsBankAccount)(implicit requestHeader: RequestHeader): Future[BarsResponse] =
    barsConnector.validateBankDetails(BarsValidateRequest(bankAccount)).map(BarsResponse.apply)

  def verifyPersonal(bankAccount: BarsBankAccount, subject: BarsSubject)(
      implicit
      requestHeader: RequestHeader
  ): Future[BarsResponse] =
    barsConnector.verifyPersonal(BarsVerifyPersonalRequest(bankAccount, subject)).map(BarsResponse.apply)

  def verifyBusiness(bankAccount: BarsBankAccount, business: Option[BarsBusiness])(
      implicit
      requestHeader: RequestHeader
  ): Future[BarsResponse] =
    barsConnector.verifyBusiness(BarsVerifyBusinessRequest(bankAccount, business)).map(BarsResponse.apply)

  def verifyBankDetails(
      bankAccount:       BarsBankAccount,
      subject:           BarsSubject,
      business:          BarsBusiness,
      typeOfBankAccount: BarsTypeOfBankAccount
  )(implicit requestHeader: RequestHeader): Future[BarsResponse] = {

    logger.debug(s"verifyBankDetails - account: $bankAccount, typeOfBankAccount: $typeOfBankAccount")

    val resp = {
      logger.debug("******* validate")
      validateBankAccount(bankAccount).flatMap {
        case validateResponse @ validateFailure() =>
          Future.successful(validateResponse)
        case _ =>
          logger.debug("******* verifyPersonal")
          verifyPersonal(bankAccount, subject).flatMap {
            case verifyPersonalResp @ verifySuccess() =>
              Future.successful(verifyPersonalResp)
            case _ =>
              logger.debug("******* verifyBusiness")
              val verifyBusinessResp = verifyBusiness(bankAccount, Some(business))
              verifyBusinessResp
          }
      }
    }
    resp
  }

}
