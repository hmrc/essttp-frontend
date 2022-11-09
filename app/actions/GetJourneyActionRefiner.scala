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

package actions

import actionsmodel.{AuthenticatedJourneyRequest, AuthenticatedRequest}
import config.AppConfig
import controllers.support.RequestSupport.hc
import essttp.journey.JourneyConnector
import essttp.journey.model.Journey
import play.api.Logger
import play.api.mvc.{ActionRefiner, Request, Result, Results}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetJourneyActionRefiner @Inject() (journeyConnector: JourneyConnector, appConfig: AppConfig)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[AuthenticatedRequest, AuthenticatedJourneyRequest] {

  private val logger = Logger(getClass)

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedJourneyRequest[A]]] = {
    implicit val r: Request[A] = request
    for {
      maybeJourney: Option[Journey] <- journeyConnector.findLatestJourneyBySessionId()
    } yield maybeJourney match {
      case Some(journey) => Right(new AuthenticatedJourneyRequest(request, request.enrolments, journey, request.ggCredId))
      case None =>
        logger.error(s"No journey found for sessionId: [ ${hc.sessionId} ]")
        val redirectTo =
          if (appConfig.vatEnabled) {
            controllers.routes.WhichTaxRegimeController.whichTaxRegime
          } else {
            controllers.routes.LandingController.epayeLandingPage
          }
        Left(Results.Redirect(redirectTo))
    }
  }

  override protected def executionContext: ExecutionContext = ec

}
