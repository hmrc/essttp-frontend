/*
 * Copyright 2023 HM Revenue & Customs
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

import actionsmodel.EligibleJourneyRequest
import essttp.bars.BarsVerifyStatusConnector
import essttp.bars.model.BarsVerifyStatusResponse
import essttp.journey.model.Journey
import essttp.journey.model.JourneyStage.{AfterComputedTaxId, AfterEnteredCanYouSetUpDirectDebit}
import essttp.rootmodel.TaxId
import essttp.rootmodel.bank.{BankDetails, TypeOfBankAccount, TypesOfBankAccount}
import essttp.utils.RequestSupport.hc
import models.bars.request.{BarsBankAccount, BarsBusiness, BarsSubject}
import models.bars.response.*
import models.bars.{BarsTypeOfBankAccount, BarsTypesOfBankAccount}
import play.api.mvc.RequestHeader
import services.EssttpBarsServiceUtils.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/** essttp-specific interface to Bank Account Reputation service (BARs).
  */
@Singleton
class EssttpBarsService @Inject() (
  barsService:               BarsService,
  barsVerifyStatusConnector: BarsVerifyStatusConnector,
  auditService:              AuditService
)(using ExecutionContext) {

  def verifyBankDetails(
    bankDetails: BankDetails,
    journey:     AfterEnteredCanYouSetUpDirectDebit & Journey
  )(using request: EligibleJourneyRequest[?]): Future[Either[BarsError, VerifyResponse]] = {
    val taxId = journey match {
      case j: AfterComputedTaxId => j.taxId
    }

    barsService
      .verifyBankDetails(
        bankAccount = toBarsBankAccount(bankDetails),
        subject = toBarsSubject(bankDetails),
        business = toBarsBusiness(bankDetails),
        typeOfBankAccount = toBarsTypeOfBankAccount(bankDetails.typeOfBankAccount)
      )
      .flatMap { result =>
        def auditBars(barsVerifyStatusResponse: BarsVerifyStatusResponse): Unit =
          auditService.auditBarsCheck(journey, bankDetails, result, barsVerifyStatusResponse)

        result match {
          case Right(_) | Left(_: BarsValidateError) =>
            // don't update the verify count on success or validate error
            auditBars(BarsVerifyStatusResponse(request.numberOfBarsVerifyAttempts, None))
            Future.successful(result)

          case Left(bve: BarsVerifyError) =>
            updateVerifyStatus(taxId, result, bve.barsResponse, auditBars)
        }
      }
  }

  private def updateVerifyStatus(
    taxId:     TaxId,
    result:    Either[BarsError, VerifyResponse],
    br:        BarsResponse,
    auditBars: BarsVerifyStatusResponse => Unit
  )(using RequestHeader): Future[Either[BarsError, VerifyResponse]] =
    barsVerifyStatusConnector
      .update(taxId)
      .map { verifyStatus =>
        auditBars(verifyStatus)
        // here we catch a lockout BarsStatus condition,
        // and force a TooManyAttempts (BarsError) response
        verifyStatus.lockoutExpiryDateTime
          .fold(result) { expiry =>
            Left(TooManyAttempts(br, expiry))
          }
      }

}

object EssttpBarsServiceUtils {
  def toBarsBankAccount(bankDetails: BankDetails): BarsBankAccount =
    BarsBankAccount.padded(bankDetails.sortCode.value.decryptedValue, bankDetails.accountNumber.value.decryptedValue)

  def toBarsSubject(bankDetails: BankDetails): BarsSubject = BarsSubject(
    title = None,
    name = Some(bankDetails.name.value.decryptedValue),
    firstName = None,
    lastName = None,
    dob = None,
    address = None
  )

  def toBarsBusiness(bankDetails: BankDetails): BarsBusiness =
    BarsBusiness(companyName = bankDetails.name.value.decryptedValue, address = None)

  def toBarsTypeOfBankAccount(accountType: TypeOfBankAccount): BarsTypeOfBankAccount =
    accountType match {
      case TypesOfBankAccount.Personal => BarsTypesOfBankAccount.Personal
      case TypesOfBankAccount.Business => BarsTypesOfBankAccount.Business
    }

}
