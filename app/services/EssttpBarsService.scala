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

import essttp.rootmodel.bank.{BankDetails, TypeOfBankAccount, TypesOfBankAccount}
import models.bars.BarsCommon.{BarsBankAccount, BarsResponse, BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import models.bars.BarsVerifyRequest._
import models.enumsforforms.IsSoleSignatoryFormValue
import play.api.mvc.RequestHeader
import services.EssttpBarsService._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * ESSTTP-specific interface to Bank Account Reputation service (BARs).
 */
@Singleton
class EssttpBarsService @Inject() (barsService: BarsService)(implicit ec: ExecutionContext) {

  def validateBankAccount(bankDetails: BankDetails)(implicit requestHeader: RequestHeader): Future[BarsResponse] =
    barsService.validateBankAccount(toBarsBankAccount(bankDetails))

  def verifyPersonal(bankDetails: BankDetails)(implicit requestHeader: RequestHeader): Future[BarsResponse] =
    barsService.verifyPersonal(toBarsBankAccount(bankDetails), toBarsSubject(bankDetails))

  def verifyBusiness(bankDetails: BankDetails)(implicit requestHeader: RequestHeader): Future[BarsResponse] =
    barsService.verifyBusiness(toBarsBankAccount(bankDetails), Some(toBarsBusiness(bankDetails)))

  // TODO ItSpec
  def verifyBankDetails(
      bankDetails:       BankDetails,
      isSoleSignatory:   IsSoleSignatoryFormValue,
      typeOfBankAccount: TypeOfBankAccount
  )(implicit requestHeader: RequestHeader): Future[Option[BarsResponse]] = {
    isSoleSignatory match {
      case IsSoleSignatoryFormValue.Yes =>
        barsService.verifyBankDetails(
          bankAccount       = toBarsBankAccount(bankDetails),
          subject           = toBarsSubject(bankDetails),
          business          = toBarsBusiness(bankDetails),
          typeOfBankAccount = toBarsTypeOfBankAccount(typeOfBankAccount)
        ).map(Option.apply)

      case IsSoleSignatoryFormValue.No => Future.successful(None)
    }
  }
}

object EssttpBarsService {
  def toBarsBankAccount(bankDetails: BankDetails): BarsBankAccount =
    BarsBankAccount.padded(bankDetails.sortCode, bankDetails.accountNumber)

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
