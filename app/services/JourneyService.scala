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

import com.google.inject.{Inject, Singleton}
import essttp.journey.JourneyConnector
import essttp.journey.model.JourneyId
import essttp.rootmodel.ttp.EligibilityCheckResult
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.bank.{BankDetails, DetailsAboutBankAccount}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.{CanPayUpfront, DayOfMonth, Email, EmpRef, IsEmailAddressRequired, MonthlyPaymentAmount, UpfrontPaymentAmount}
import play.api.mvc.RequestHeader
import util.Logging

import scala.concurrent.Future

@Singleton
class JourneyService @Inject() (journeyConnector: JourneyConnector) extends Logging {

  def updateEligibilityCheckResult(journeyId: JourneyId, eligibilityCheckResult: EligibilityCheckResult)(
      implicit
      requestHeader: RequestHeader
  ): Future[Unit] =
    journeyConnector.updateEligibilityCheckResult(journeyId, eligibilityCheckResult)

  object UpdateTaxRef {
    //epaye
    def updateEpayeTaxId(journeyId: JourneyId, empRef: EmpRef)(implicit requestHeader: RequestHeader): Future[Unit] =
      journeyConnector.updateTaxId(journeyId, empRef)
    //vat
    //others
  }

  def updateCanPayUpfront(journeyId: JourneyId, canPayUpfront: CanPayUpfront)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateCanPayUpfront(journeyId, canPayUpfront)

  def updateUpfrontPaymentAmount(journeyId: JourneyId, upfrontPaymentAmount: UpfrontPaymentAmount)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateUpfrontPaymentAmount(journeyId, upfrontPaymentAmount)

  def updateExtremeDatesResult(journeyId: JourneyId, extremeDatesResponse: ExtremeDatesResponse)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateExtremeDates(journeyId, extremeDatesResponse)

  def updateAffordabilityResult(journeyId: JourneyId, instalmentAmounts: InstalmentAmounts)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateAffordabilityResult(journeyId, instalmentAmounts)

  def updateMonthlyPaymentAmount(journeyId: JourneyId, monthlyPaymentAmount: MonthlyPaymentAmount)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateMonthlyPaymentAmount(journeyId, monthlyPaymentAmount)

  def updateDayOfMonth(journeyId: JourneyId, dayOfMonth: DayOfMonth)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateDayOfMonth(journeyId, dayOfMonth)

  def updateStartDates(journeyId: JourneyId, startDatesResponse: StartDatesResponse)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateStartDates(journeyId, startDatesResponse)

  def updateAffordableQuotes(journeyId: JourneyId, affordableQuotesResponse: AffordableQuotesResponse)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateAffordableQuotes(journeyId, affordableQuotesResponse)

  def updateChosenPaymentPlan(journeyId: JourneyId, paymentPlan: PaymentPlan)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateChosenPaymentPlan(journeyId, paymentPlan)

  def updateHasCheckedPaymentPlan(journeyId: JourneyId)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateHasCheckedPaymentPlan(journeyId)

  def updateDetailsAboutBankAccount(journeyId: JourneyId, detailsAboutBankAccount: DetailsAboutBankAccount)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateDetailsAboutBankAccount(journeyId, detailsAboutBankAccount)

  def updateDirectDebitDetails(journeyId: JourneyId, directDebitDetails: BankDetails)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateDirectDebitDetails(journeyId, directDebitDetails)

  def updateHasConfirmedDirectDebitDetails(journeyId: JourneyId)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateHasConfirmedDirectDebitDetails(journeyId)

  def updateAgreedTermsAndConditions(
      journeyId:              JourneyId,
      isEmailAddressRequired: IsEmailAddressRequired
  )(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateHasAgreedTermsAndConditions(journeyId, isEmailAddressRequired)

  def updateSelectedEmailToBeVerified(journeyId: JourneyId, email: Email)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateSelectedEmailToBeVerified(journeyId, email)

  def updateArrangementResponse(journeyId: JourneyId, arrangementResponse: ArrangementResponse)(implicit requestHeader: RequestHeader): Future[Unit] =
    journeyConnector.updateArrangement(journeyId, arrangementResponse)

}
