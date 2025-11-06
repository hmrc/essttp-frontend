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

import com.google.inject.{Inject, Singleton}
import essttp.journey.JourneyConnector
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Journey, JourneyId, PaymentPlanAnswers}
import essttp.rootmodel.bank.{BankDetails, CanSetUpDirectDebit, TypeOfBankAccount}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import essttp.rootmodel.*
import paymentsEmailVerification.models.EmailVerificationResult
import play.api.mvc.RequestHeader
import util.Logging

import scala.concurrent.Future

@Singleton
class JourneyService @Inject() (journeyConnector: JourneyConnector) extends Logging {

  def updateEligibilityCheckResult(journeyId: JourneyId, eligibilityCheckResult: EligibilityCheckResult)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateEligibilityCheckResult(journeyId, eligibilityCheckResult)

  object UpdateTaxRef {

    def updateEpayeTaxId(journeyId: JourneyId, empRef: EmpRef)(using RequestHeader): Future[Journey] =
      journeyConnector.updateTaxId(journeyId, empRef)

    def updateVatTaxId(journeyId: JourneyId, vrn: Vrn)(using RequestHeader): Future[Journey] =
      journeyConnector.updateTaxId(journeyId, vrn)

    def updateSaTaxId(journeyId: JourneyId, saTaxId: SaUtr)(using RequestHeader): Future[Journey] =
      journeyConnector.updateTaxId(journeyId, saTaxId)

  }

  def updateCanPayUpfront(journeyId: JourneyId, canPayUpfront: CanPayUpfront)(using RequestHeader): Future[Journey] =
    journeyConnector.updateCanPayUpfront(journeyId, canPayUpfront)

  def updateUpfrontPaymentAmount(journeyId: JourneyId, upfrontPaymentAmount: UpfrontPaymentAmount)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateUpfrontPaymentAmount(journeyId, upfrontPaymentAmount)

  def updateExtremeDatesResult(journeyId: JourneyId, extremeDatesResponse: ExtremeDatesResponse)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateExtremeDates(journeyId, extremeDatesResponse)

  def updateAffordabilityResult(journeyId: JourneyId, instalmentAmounts: InstalmentAmounts)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateAffordabilityResult(journeyId, instalmentAmounts)

  def updateCanPayWithinSixMonths(journeyId: JourneyId, canPayWithinSixMonths: CanPayWithinSixMonthsAnswers)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateCanPayWithinSixMonthsAnswers(journeyId, canPayWithinSixMonths)

  def updateMonthlyPaymentAmount(journeyId: JourneyId, monthlyPaymentAmount: MonthlyPaymentAmount)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateMonthlyPaymentAmount(journeyId, monthlyPaymentAmount)

  def updateDayOfMonth(journeyId: JourneyId, dayOfMonth: DayOfMonth)(using RequestHeader): Future[Journey] =
    journeyConnector.updateDayOfMonth(journeyId, dayOfMonth)

  def updateStartDates(journeyId: JourneyId, startDatesResponse: StartDatesResponse)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateStartDates(journeyId, startDatesResponse)

  def updateAffordableQuotes(journeyId: JourneyId, affordableQuotesResponse: AffordableQuotesResponse)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateAffordableQuotes(journeyId, affordableQuotesResponse)

  def updateChosenPaymentPlan(journeyId: JourneyId, paymentPlan: PaymentPlan)(using RequestHeader): Future[Journey] =
    journeyConnector.updateChosenPaymentPlan(journeyId, paymentPlan)

  def updateHasCheckedPaymentPlan(journeyId: JourneyId, paymentPlanAnswers: PaymentPlanAnswers)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateHasCheckedPaymentPlan(journeyId, paymentPlanAnswers)

  def updateCanSetUpDirectDebit(journeyId: JourneyId, canSetUpDirectDebit: CanSetUpDirectDebit)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateCanSetUpDirectDebit(journeyId, canSetUpDirectDebit)

  def updateTypeOfBankAccount(journeyId: JourneyId, typeOfBankAccount: TypeOfBankAccount)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateTypeOfBankAccount(journeyId, typeOfBankAccount)

  def updateDirectDebitDetails(journeyId: JourneyId, directDebitDetails: BankDetails)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateDirectDebitDetails(journeyId, directDebitDetails)

  def updateHasConfirmedDirectDebitDetails(journeyId: JourneyId)(using RequestHeader): Future[Journey] =
    journeyConnector.updateHasConfirmedDirectDebitDetails(journeyId)

  def updateAgreedTermsAndConditions(
    journeyId:              JourneyId,
    isEmailAddressRequired: IsEmailAddressRequired
  )(using RequestHeader): Future[Journey] =
    journeyConnector.updateHasAgreedTermsAndConditions(journeyId, isEmailAddressRequired)

  def updateSelectedEmailToBeVerified(journeyId: JourneyId, email: Email)(using RequestHeader): Future[Journey] =
    journeyConnector.updateSelectedEmailToBeVerified(journeyId, email)

  def updateEmailVerificationResult(journeyId: JourneyId, emailVerificationResult: EmailVerificationResult)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateEmailVerificationResult(journeyId, emailVerificationResult)

  def updateArrangementResponse(journeyId: JourneyId, arrangementResponse: ArrangementResponse)(using
    RequestHeader
  ): Future[Journey] =
    journeyConnector.updateArrangement(journeyId, arrangementResponse)

}
