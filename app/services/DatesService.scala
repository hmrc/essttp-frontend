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

import connectors.EssttpBackendConnector
import essttp.journey.model.{Journey, JourneyStage, PaymentPlanAnswers, UpfrontPaymentAnswers}
import essttp.rootmodel.CanPayUpfront
import essttp.rootmodel.dates.InitialPayment
import essttp.rootmodel.dates.extremedates.{ExtremeDatesRequest, ExtremeDatesResponse}
import essttp.rootmodel.dates.startdates.{PreferredDayOfMonth, StartDatesRequest, StartDatesResponse}
import essttp.utils.Errors
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/** Essttp-dates Service.
  */
@Singleton
class DatesService @Inject() (datesApiConnector: EssttpBackendConnector) {

  def startDates(
    journey: Either[
      JourneyStage.AfterEnteredDayOfMonth & Journey,
      (JourneyStage.AfterCheckedPaymentPlan & Journey, PaymentPlanAnswers.PaymentPlanNoAffordability)
    ]
  )(using RequestHeader): Future[StartDatesResponse] = {
    val dayOfMonth: PreferredDayOfMonth      = PreferredDayOfMonth(journey.fold(_.dayOfMonth, _._2.dayOfMonth).value)
    val upfrontPaymentAnswers                = DatesService.upfrontPaymentAnswersFromJourney(journey.map[Journey](_._1).merge)
    val initialPayment                       = DatesService.deriveInitialPayment(upfrontPaymentAnswers)
    val startDatesRequest: StartDatesRequest = StartDatesRequest(initialPayment, dayOfMonth)
    datesApiConnector.startDates(startDatesRequest)
  }

  def extremeDates(
    journey: JourneyStage.AfterAnsweredCanPayUpfront
  )(using RequestHeader): Future[ExtremeDatesResponse] = {
    val extremeDatesRequest: ExtremeDatesRequest = journey.canPayUpfront match {
      case CanPayUpfront(true)  => ExtremeDatesRequest(InitialPayment(value = true))
      case CanPayUpfront(false) => ExtremeDatesRequest(InitialPayment(value = false))
    }
    datesApiConnector.extremeDates(extremeDatesRequest)
  }

}

object DatesService {
  private def deriveInitialPayment(upfrontPaymentAnswers: UpfrontPaymentAnswers): InitialPayment =
    upfrontPaymentAnswers match {
      case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => InitialPayment(value = true)
      case UpfrontPaymentAnswers.NoUpfrontPayment          => InitialPayment(value = false)
    }

  private def upfrontPaymentAnswersFromJourney(journey: Journey): UpfrontPaymentAnswers = journey match {
    case j: JourneyStage.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
    case _                                          => Errors.throwServerErrorException("Trying to get upfront payment answers for journey before they exist..")
  }
}
