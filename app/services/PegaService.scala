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
import essttp.journey.model.Journey.AfterCanPayWithinSixMonthsAnswers
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Journey}
import essttp.rootmodel.pega.StartCaseResponse
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PegaService @Inject() (
    essttpConnector:  EssttpBackendConnector,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext) {

  def startCase(journey: Journey)(implicit rh: RequestHeader): Future[StartCaseResponse] = {
      def doStartCase(): Future[StartCaseResponse] =
        for {
          startCaseResponse <- essttpConnector.startPegaCase(journey.journeyId)
          _ <- journeyConnector.updatePegaStartCaseResponse(journey.journeyId, startCaseResponse)
        } yield startCaseResponse

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

}
