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

import essttp.journey.model.Journey.AfterChosenTypeOfBankAccount
import essttp.rootmodel.bank.{BankDetails, TypeOfBankAccount, TypesOfBankAccount}
import models.bars.request.{BarsBankAccount, BarsBusiness, BarsSubject}
import models.bars.response.{BarsError, VerifyResponse}
import models.bars.{BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import play.api.mvc.RequestHeader
import services.EssttpBarsService._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * ESSTTP-specific interface to Bank Account Reputation service (BARs).
 */
@Singleton
class EssttpBarsService @Inject() (barsService: BarsService, auditService: AuditService)(implicit ec: ExecutionContext) {

  def verifyBankDetails(
      bankDetails:       BankDetails,
      typeOfBankAccount: TypeOfBankAccount,
      journey:           AfterChosenTypeOfBankAccount
  )(implicit requestHeader: RequestHeader): Future[Either[BarsError, VerifyResponse]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(requestHeader)

    barsService.verifyBankDetails(
      bankAccount       = toBarsBankAccount(bankDetails),
      subject           = toBarsSubject(bankDetails),
      business          = toBarsBusiness(bankDetails),
      typeOfBankAccount = toBarsTypeOfBankAccount(typeOfBankAccount)
    ).map { result =>
        auditService.auditBarsCheck(journey, bankDetails, typeOfBankAccount, result)
        result
      }
  }
}

object EssttpBarsService {
  def toBarsBankAccount(bankDetails: BankDetails): BarsBankAccount =
    BarsBankAccount.padded(bankDetails.sortCode.value, bankDetails.accountNumber.value)

  def toBarsSubject(bankDetails: BankDetails): BarsSubject = BarsSubject(
    title     = None, name = Some(bankDetails.name.value), firstName = None, lastName = None, dob = None, address = None
  )

  def toBarsBusiness(bankDetails: BankDetails): BarsBusiness =
    BarsBusiness(companyName = bankDetails.name.value, address = None)

  def toBarsTypeOfBankAccount(accountType: TypeOfBankAccount): BarsTypeOfBankAccount =
    accountType match {
      case TypesOfBankAccount.Personal => BarsTypesOfBankAccount.Personal
      case TypesOfBankAccount.Business => BarsTypesOfBankAccount.Business
    }

}
