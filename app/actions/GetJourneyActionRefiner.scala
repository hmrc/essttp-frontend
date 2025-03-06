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

package actions

import actionsmodel.{AuthenticatedJourneyRequest, AuthenticatedRequest}
import cats.data.OptionT
import cats.syntax.traverse._
import connectors.EssttpBackendConnector
import requests.RequestSupport.hc
import essttp.journey.JourneyConnector
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import play.api.Logger
import play.api.mvc.{ActionRefiner, Request, Result, Results}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetJourneyActionRefiner @Inject() (
  journeyConnector:       JourneyConnector,
  essttpBackendConnector: EssttpBackendConnector
)(using
  ec:                     ExecutionContext
) extends ActionRefiner[AuthenticatedRequest, AuthenticatedJourneyRequest] {

  private val logger = Logger(getClass)

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedJourneyRequest[A]]] = {
    given Request[A] = request

    findJourney(request).map {
      case Some(journey) =>
        Right(
          new AuthenticatedJourneyRequest(
            request,
            request.enrolments,
            journey,
            request.ggCredId,
            request.nino,
            request.lang
          )
        )
      case None          =>
        logger.error(s"No journey found for sessionId: [ ${hc.sessionId.toString} ]")
        Left(Results.Redirect(controllers.routes.WhichTaxRegimeController.whichTaxRegime))
    }
  }

  // if no journey is found for the session id, the user have have just come back from PEGA. In this case the
  // session id would change - see if the backend can reconstruct the session data saved just before going to PEGA
  private def findJourney[A](request: AuthenticatedRequest[A])(using Request[?]): Future[Option[Journey]] =
    OptionT(journeyConnector.findLatestJourneyBySessionId())
      .orElseF(
        taxRegime(request)
          .map(regime => essttpBackendConnector.recreateSession(regime))
          .flatSequence
      )
      .value

  private def taxRegime[A](request: AuthenticatedRequest[A]): Option[TaxRegime] =
    request.getQueryString("regime").flatMap(TaxRegime.withNameInsensitiveOption)

  override protected def executionContext: ExecutionContext = ec

}
