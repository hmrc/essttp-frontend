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
import essttp.emailverification.EmailVerificationStatus
import essttp.journey.JourneyConnector
import essttp.journey.model.{Journey, JourneyId}
import essttp.rootmodel.ttp.EligibilityCheckResult
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.bank.{BankDetails, DetailsAboutBankAccount}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.{CanPayUpfront, DayOfMonth, Email, EmpRef, IsEmailAddressRequired, MonthlyPaymentAmount, UpfrontPaymentAmount, Vrn}
import play.api.mvc.RequestHeader
import util.Logging

import scala.concurrent.Future

@Singleton
class JourneyService @Inject() (journeyConnector: JourneyConnector) extends Logging {

  def updateEligibilityCheckResult(journeyId: JourneyId, eligibilityCheckResult: EligibilityCheckResult)(
      implicit
      requestHeader: RequestHeader
  ): Future[Journey] =
    journeyConnector.updateEligibilityCheckResult(journeyId, eligibilityCheckResult)

  object UpdateTaxRef {

    def updateEpayeTaxId(journeyId: JourneyId, empRef: EmpRef)(implicit requestHeader: RequestHeader): Future[Journey] =
      journeyConnector.updateTaxId(journeyId, empRef)

    def updateVatTaxId(journeyId: JourneyId, vrn: Vrn)(implicit requestHeader: RequestHeader): Future[Journey] =
      journeyConnector.updateTaxId(journeyId, vrn)

  }

  def updateCanPayUpfront(journeyId: JourneyId, canPayUpfront: CanPayUpfront)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateCanPayUpfront(journeyId, canPayUpfront)

  def updateUpfrontPaymentAmount(journeyId: JourneyId, upfrontPaymentAmount: UpfrontPaymentAmount)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateUpfrontPaymentAmount(journeyId, upfrontPaymentAmount)

  def updateExtremeDatesResult(journeyId: JourneyId, extremeDatesResponse: ExtremeDatesResponse)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateExtremeDates(journeyId, extremeDatesResponse)

  def updateAffordabilityResult(journeyId: JourneyId, instalmentAmounts: InstalmentAmounts)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateAffordabilityResult(journeyId, instalmentAmounts)

  def updateMonthlyPaymentAmount(journeyId: JourneyId, monthlyPaymentAmount: MonthlyPaymentAmount)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateMonthlyPaymentAmount(journeyId, monthlyPaymentAmount)

  def updateDayOfMonth(journeyId: JourneyId, dayOfMonth: DayOfMonth)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateDayOfMonth(journeyId, dayOfMonth)

  def updateStartDates(journeyId: JourneyId, startDatesResponse: StartDatesResponse)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateStartDates(journeyId, startDatesResponse)

  def updateAffordableQuotes(journeyId: JourneyId, affordableQuotesResponse: AffordableQuotesResponse)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateAffordableQuotes(journeyId, affordableQuotesResponse)

  def updateChosenPaymentPlan(journeyId: JourneyId, paymentPlan: PaymentPlan)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateChosenPaymentPlan(journeyId, paymentPlan)

  def updateHasCheckedPaymentPlan(journeyId: JourneyId)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateHasCheckedPaymentPlan(journeyId)

  def updateDetailsAboutBankAccount(journeyId: JourneyId, detailsAboutBankAccount: DetailsAboutBankAccount)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateDetailsAboutBankAccount(journeyId, detailsAboutBankAccount)

  def updateDirectDebitDetails(journeyId: JourneyId, directDebitDetails: BankDetails)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateDirectDebitDetails(journeyId, directDebitDetails)

  def updateHasConfirmedDirectDebitDetails(journeyId: JourneyId)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateHasConfirmedDirectDebitDetails(journeyId)

  def updateAgreedTermsAndConditions(
      journeyId:              JourneyId,
      isEmailAddressRequired: IsEmailAddressRequired
  )(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateHasAgreedTermsAndConditions(journeyId, isEmailAddressRequired)

  def updateSelectedEmailToBeVerified(journeyId: JourneyId, email: Email)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateSelectedEmailToBeVerified(journeyId, email)

  def updateEmailVerificationStatus(journeyId: JourneyId, emailVerificationStatus: EmailVerificationStatus)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateEmailVerificationStatus(journeyId, emailVerificationStatus)

  def updateArrangementResponse(journeyId: JourneyId, arrangementResponse: ArrangementResponse)(implicit requestHeader: RequestHeader): Future[Journey] =
    journeyConnector.updateArrangement(journeyId, arrangementResponse)

}
