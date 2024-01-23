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

import actionsmodel.{AuthenticatedJourneyRequest, BarsLockedOutRequest}
import config.AppConfig
import controllers.JourneyIncorrectStateRouter
import essttp.bars.BarsVerifyStatusConnector
import essttp.journey.model.Journey
import play.api.Logging
import play.api.mvc.{ActionRefiner, Request, Result, Results}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BarsLockedOutJourneyRefiner @Inject() (barsVerifyStatusConnector: BarsVerifyStatusConnector)(
    implicit
    ec: ExecutionContext, appConfig: AppConfig
) extends ActionRefiner[AuthenticatedJourneyRequest, BarsLockedOutRequest] with Logging with Results {

  override protected def refine[A](request: AuthenticatedJourneyRequest[A]): Future[Either[Result, BarsLockedOutRequest[A]]] = {
    implicit val rh: Request[A] = request.request

    request.journey match {
      case j: Journey.BeforeComputedTaxId =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j).map(Left(_))
      case j: Journey.AfterComputedTaxId =>
        barsVerifyStatusConnector.status(j.taxId).map { status =>
          status.lockoutExpiryDateTime match {
            case Some(expiresAt) =>
              Right(
                new BarsLockedOutRequest(
                  request.request,
                  request.enrolments,
                  request.journey,
                  request.ggCredId,
                  status.attempts,
                  expiresAt
                )
              )

            case None =>
              Left(JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(request.journey))
          }
        }
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
