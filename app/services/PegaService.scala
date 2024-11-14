/*
 * Copyright 2024 HM Revenue & Customs
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
import essttp.journey.JourneyConnector
import essttp.journey.model.Journey.{AfterCanPayWithinSixMonthsAnswers, AfterCheckedPaymentPlan, AfterStartedPegaCase}
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Journey, PaymentPlanAnswers}
import essttp.rootmodel.pega.{GetCaseResponse, StartCaseResponse}
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PegaService @Inject() (
    essttpConnector:  EssttpBackendConnector,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext) {

  def startCase(journey: Journey, recalculationNeeded: Boolean)(implicit rh: RequestHeader): Future[(Journey, StartCaseResponse)] = {
      def doStartCase(): Future[(Journey, StartCaseResponse)] =
        for {
          startCaseResponse <- essttpConnector.startPegaCase(journey.journeyId, recalculationNeeded)
          updatedJourney <- journeyConnector.updatePegaStartCaseResponse(journey.journeyId, startCaseResponse)
          _ <- essttpConnector.saveJourneyForPega(journey.journeyId)
        } yield (updatedJourney, startCaseResponse)

    journey match {
      case j: AfterCanPayWithinSixMonthsAnswers =>
        j.canPayWithinSixMonthsAnswers match {
          case CanPayWithinSixMonthsAnswers.AnswerNotRequired =>
            sys.error("Cannot start PEGA case when answer to CanPayWithinSixMonths is not required")

          case CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(canPay) =>
            if (canPay) sys.error("Cannot start PEGA case when answer to CanPayWithinSixMonths is 'yes'")
            else doStartCase()
        }

      case other =>
        sys.error(s"Cannot start PEGA case when journey is in state ${other.name}")

    }
  }

  def getCase(journey: Journey)(implicit rh: RequestHeader): Future[GetCaseResponse] = {
      def doGetCase(startCaseResponse: StartCaseResponse): Future[GetCaseResponse] =
        for {
          getCaseResponse <- essttpConnector.getPegaCase(journey.journeyId)
          paymentPlanAnswers = PaymentPlanAnswers.PaymentPlanAfterAffordability(
            startCaseResponse, getCaseResponse.paymentDay, getCaseResponse.paymentPlan
          )
          _ <- journeyConnector.updateHasCheckedPaymentPlan(journey.journeyId, paymentPlanAnswers)
        } yield getCaseResponse

    journey match {
      case j: AfterStartedPegaCase =>
        doGetCase(j.startCaseResponse)

      case j: AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case _: PaymentPlanAnswers.PaymentPlanNoAffordability =>
            sys.error("Trying to get PEGA case on non-affordability journey")

          case p: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
            doGetCase(p.startCaseResponse)
        }

      case other =>
        sys.error(s"Cannot get PEGA case when journey is in state ${other.name}")

    }
  }

}
