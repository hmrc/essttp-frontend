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

import connectors.DatesApiConnector
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.CanPayUpfront
import essttp.rootmodel.dates.InitialPayment
import essttp.rootmodel.dates.extremedates.{ExtremeDatesRequest, ExtremeDatesResponse}
import essttp.rootmodel.dates.startdates.{PreferredDayOfMonth, StartDatesRequest, StartDatesResponse}
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
/**
 * Essttp-dates Service.
 */
@Singleton
class DatesService @Inject() (datesApiConnector: DatesApiConnector) {

  def startDates(journey: Journey.AfterEnteredDayOfMonth)(implicit request: RequestHeader): Future[StartDatesResponse] = {
    val dayOfMonth: PreferredDayOfMonth = PreferredDayOfMonth(journey.dayOfMonth.value)
    val initialPayment: InitialPayment = journey match {
      case j: Journey.Epaye.EnteredDayOfMonth           => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.RetrievedStartDates         => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.RetrievedAffordableQuotes   => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.ChosenPaymentPlan           => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.CheckedPaymentPlan          => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.ChosenTypeOfBankAccount     => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.EnteredDirectDebitDetails   => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.ConfirmedDirectDebitDetails => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
      case j: Journey.Epaye.AgreedTermsAndConditions    => DatesService.deriveInitialPayment(j.upfrontPaymentAnswers)
    }
    val startDatesRequest: StartDatesRequest = StartDatesRequest(initialPayment, dayOfMonth)
    datesApiConnector.startDates(startDatesRequest)
  }

  def extremeDates(journey: Journey.AfterAnsweredCanPayUpfront)(implicit request: RequestHeader): Future[ExtremeDatesResponse] = {
    val extremeDatesRequest: ExtremeDatesRequest = journey.canPayUpfront match {
      case CanPayUpfront(true)  => ExtremeDatesRequest(InitialPayment(true))
      case CanPayUpfront(false) => ExtremeDatesRequest(InitialPayment(false))
    }
    datesApiConnector.extremeDates(extremeDatesRequest)
  }

}

object DatesService {
  def deriveInitialPayment(upfrontPaymentAnswers: UpfrontPaymentAnswers): InitialPayment = upfrontPaymentAnswers match {
    case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => InitialPayment(true)
    case UpfrontPaymentAnswers.NoUpfrontPayment          => InitialPayment(false)
  }
}
