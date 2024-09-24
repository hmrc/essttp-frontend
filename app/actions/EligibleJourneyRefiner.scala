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

import actionsmodel.{BarsNotLockedOutRequest, EligibleJourneyRequest}
import controllers.JourneyIncorrectStateRouter
import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.Journey
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EligibleJourneyRefiner @Inject() (ec: ExecutionContext) extends ActionRefiner[BarsNotLockedOutRequest, EligibleJourneyRequest] {

  override protected def refine[A](
      request: BarsNotLockedOutRequest[A]
  ): Future[Either[Result, EligibleJourneyRequest[A]]] = {
    implicit val r: Request[A] = request
    val result: Either[Result, EligibleJourneyRequest[A]] = request.journey match {
      case j: Journey.Stages.Started =>
        Left(JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j))

      case j: Journey.Stages.ComputedTaxId =>
        Left(JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j))

      case j: Journey.AfterEligibilityChecked =>
        if (j.eligibilityCheckResult.isEligible) {
          Right(
            new EligibleJourneyRequest[A](
              journey    = j,
              enrolments = request.enrolments,
              request    = request,
              request.ggCredId,
              request.nino,
              request.numberOfBarsVerifyAttempts,
              j.eligibilityCheckResult,
              request.lang
            )
          )
        } else {
          Left(Redirect(EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime)))
        }
    }
    Future.successful(result)
  }

  override protected def executionContext: ExecutionContext = ec

}
