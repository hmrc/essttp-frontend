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
import essttp.journey.model.ttp.EligibilityCheckResult
import essttp.journey.model.ttp.affordability.InstalmentAmounts
import essttp.journey.model.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.{CanPayUpfront, DayOfMonth, EmpRef, MonthlyPaymentAmount, UpfrontPaymentAmount}
import play.api.mvc.RequestHeader
import util.Logging

import scala.concurrent.Future

@Singleton
class JourneyService @Inject() (journeyConnector: JourneyConnector) extends Logging {

  def updateEligibilityCheckResult(journeyId: JourneyId, eligibilityCheckResult: EligibilityCheckResult)(
      implicit
      requestHeader: RequestHeader
  ): Future[Unit] = {
    journeyConnector.updateEligibilityCheckResult(journeyId, eligibilityCheckResult)
  }

  object UpdateTaxRef {
    //epaye
    def updateEpayeTaxId(journeyId: JourneyId, empRef: EmpRef)(implicit requestHeader: RequestHeader): Future[Unit] = {
      journeyConnector.updateTaxId(journeyId, empRef)
    }
    //vat
    //others
  }

  def updateCanPayUpfront(journeyId: JourneyId, canPayUpfront: CanPayUpfront)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateCanPayUpfront(journeyId, canPayUpfront)
  }

  def updateUpfrontPaymentAmount(journeyId: JourneyId, upfrontPaymentAmount: UpfrontPaymentAmount)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateUpfrontPaymentAmount(journeyId, upfrontPaymentAmount)
  }

  def updateExtremeDatesResult(journeyId: JourneyId, extremeDatesResponse: ExtremeDatesResponse)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateExtremeDates(journeyId, extremeDatesResponse)
  }

  def updateAffordabilityResult(journeyId: JourneyId, instalmentAmounts: InstalmentAmounts)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateAffordabilityResult(journeyId, instalmentAmounts)
  }

  def updateMonthlyPaymentAmount(journeyId: JourneyId, monthlyPaymentAmount: MonthlyPaymentAmount)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateMonthlyPaymentAmount(journeyId, monthlyPaymentAmount)
  }

  def updateDayOfMonth(journeyId: JourneyId, dayOfMonth: DayOfMonth)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateDayOfMonth(journeyId, dayOfMonth)
  }

  def updateStartDates(journeyId: JourneyId, startDatesResponse: StartDatesResponse)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateStartDates(journeyId, startDatesResponse)
  }

  def updateAffordableQuotes(journeyId: JourneyId, affordableQuotesResponse: AffordableQuotesResponse)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateAffordableQuotes(journeyId, affordableQuotesResponse)
  }

  def updateChosenPaymentPlan(journeyId: JourneyId, paymentPlan: PaymentPlan)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateChosenPaymentPlan(journeyId, paymentPlan)
  }

  def updateHasCheckedPaymentPlan(journeyId: JourneyId)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateHasCheckedPaymentPlan(journeyId)
  }

}
