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
import models.bars.BarsModel.{BarsResponse, BarsValidateRequest}
import essttp.rootmodel.bank.BankDetails
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/**
 * Bank Account Reputation service (BARs).
 */
@Singleton
class BarsService @Inject() (barsConnector: BarsConnector) {

  /**
   * Call BARs to assess the correctness, existence and reputation of bank account details
   * TODO Initially only a 'validate' call. Next step, implement the `verify` call
   */
  def assessBankAccountReputation(bankDetails: BankDetails)(implicit requestHeader: RequestHeader): Future[BarsResponse] =
    barsConnector.validateBankDetails(BarsValidateRequest(bankDetails))
}
