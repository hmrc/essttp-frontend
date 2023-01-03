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

import actionsmodel.{AuthenticatedJourneyRequest, BarsNotLockedOutRequest}
import controllers.{JourneyIncorrectStateRouter, routes}
import essttp.bars.BarsVerifyStatusConnector
import essttp.journey.model.Journey
import play.api.Logging
import play.api.mvc.{ActionRefiner, Request, Result, Results}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsLockoutActionRefiner @Inject() (barsVerifyStatusConnector: BarsVerifyStatusConnector)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[AuthenticatedJourneyRequest, BarsNotLockedOutRequest] with Logging with Results {

  override protected def refine[A](request: AuthenticatedJourneyRequest[A]): Future[Either[Result, BarsNotLockedOutRequest[A]]] = {
    implicit val rh: Request[A] = request.request

    request.journey match {
      case j: Journey.BeforeComputedTaxId =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j).map(Left(_))
      case j: Journey.AfterComputedTaxId =>
        barsVerifyStatusConnector.status(j.taxId).map { status =>
          status.lockoutExpiryDateTime match {
            case Some(_) =>
              Left(Redirect(routes.BankDetailsController.barsLockout))
            case None =>
              Right(
                new BarsNotLockedOutRequest(
                  request.request,
                  request.enrolments,
                  j,
                  request.ggCredId,
                  status.attempts
                )
              )
          }
        }
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
