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

import config.AppConfig
import essttp.bars.BarsVerifyStatusConnector
import essttp.journey.model.Journey.{AfterChosenTypeOfBankAccount, Stages}
import essttp.rootmodel.bank.{BankDetails, TypeOfBankAccount, TypesOfBankAccount}
import models.bars.request.{BarsBankAccount, BarsBusiness, BarsSubject}
import models.bars.response.{BarsError, TooManyAttempts, VerifyResponse}
import models.bars.{BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import play.api.mvc.RequestHeader
import services.EssttpBarsService._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * essttp-specific interface to Bank Account Reputation service (BARs).
 */
@Singleton
class EssttpBarsService @Inject() (
    barsService:               BarsService,
    barsVerifyStatusConnector: BarsVerifyStatusConnector,
    auditService:              AuditService,
    appConfig:                 AppConfig
)(implicit ec: ExecutionContext) {

  def verifyBankDetails(
      bankDetails:       BankDetails,
      typeOfBankAccount: TypeOfBankAccount,
      journey:           AfterChosenTypeOfBankAccount
  )(implicit requestHeader: RequestHeader): Future[Either[BarsError, VerifyResponse]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(requestHeader)

    val taxId = journey match {
      case account: Stages.ChosenTypeOfBankAccount     => account.taxId
      case details: Stages.EnteredDirectDebitDetails   => details.taxId
      case details: Stages.ConfirmedDirectDebitDetails => details.taxId
      case conditions: Stages.AgreedTermsAndConditions => conditions.taxId
      case arrangement: Stages.SubmittedArrangement    => arrangement.taxId
    }

    barsService.verifyBankDetails(
      bankAccount       = toBarsBankAccount(bankDetails),
      subject           = toBarsSubject(bankDetails),
      business          = toBarsBusiness(bankDetails),
      typeOfBankAccount = toBarsTypeOfBankAccount(typeOfBankAccount)
    ).flatMap { result: Either[BarsError, VerifyResponse] =>
        barsVerifyStatusConnector.update(taxId)
          .map { verifyStatus =>
            auditService.auditBarsCheck(journey, bankDetails, typeOfBankAccount, result)
            // here we catch a lockout BarsStatus condition,
            // and force a TooManyAttempts (BarsError) response
            verifyStatus.lockoutExpiryDateTime
              .fold(result) { expiry =>
                result.flatMap(resp => Left(TooManyAttempts(resp, expiry)))
              }
          }
      }
  }
}

object EssttpBarsService {
  def toBarsBankAccount(bankDetails: BankDetails): BarsBankAccount =
    BarsBankAccount.padded(bankDetails.sortCode.value, bankDetails.accountNumber.value)

  def toBarsSubject(bankDetails: BankDetails): BarsSubject = BarsSubject(
    title     = None,
    name      = Some(bankDetails.name.value),
    firstName = None,
    lastName  = None,
    dob       = None,
    address   = None
  )

  def toBarsBusiness(bankDetails: BankDetails): BarsBusiness =
    BarsBusiness(companyName = bankDetails.name.value, address = None)

  def toBarsTypeOfBankAccount(accountType: TypeOfBankAccount): BarsTypeOfBankAccount =
    accountType match {
      case TypesOfBankAccount.Personal => BarsTypesOfBankAccount.Personal
      case TypesOfBankAccount.Business => BarsTypesOfBankAccount.Business
    }

}
