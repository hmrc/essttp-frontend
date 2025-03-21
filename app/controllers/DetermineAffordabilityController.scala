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

package controllers

import actions.Actions
import actionsmodel.AuthenticatedRequest
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Journey, JourneyStage}
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import play.api.mvc.*
import services.{JourneyService, TtpService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineAffordabilityController @Inject() (
  as:             Actions,
  mcc:            MessagesControllerComponents,
  ttpService:     TtpService,
  journeyService: JourneyService
)(using ExecutionContext, AppConfig)
    extends FrontendController(mcc),
      Logging {

  val determineAffordability: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeUpfrontPaymentAnswers => logErrorAndRouteToDefaultPageF(j)
      case j: JourneyStage.AfterUpfrontPaymentAnswers  =>
        finalStateCheckF(j, determineAffordabilityAndUpdateJourney(j, request.eligibilityCheckResult))
    }
  }

  def determineAffordabilityAndUpdateJourney(
    journey:                JourneyStage.AfterUpfrontPaymentAnswers & Journey,
    eligibilityCheckResult: EligibilityCheckResult
  )(using AuthenticatedRequest[?]): Future[Result] =
    for {
      instalmentAmounts <- ttpService.determineAffordability(journey, eligibilityCheckResult)
      updatedJourney    <- updateJourney(journey, instalmentAmounts)
    } yield Routing.redirectToNext(
      routes.DetermineAffordabilityController.determineAffordability,
      updatedJourney,
      submittedValueUnchanged = false
    )

  // if affordability is not enabled then we skip the "can you pay within 6 months" page - we need to update the journey
  // further in that case to get it to the right stage
  private def updateJourney(
    journey:           JourneyStage.AfterUpfrontPaymentAnswers & Journey,
    instalmentAmounts: InstalmentAmounts
  )(implicit
    rh:                RequestHeader
  ): Future[Journey] = {
    lazy val updateAffordabilityResult = journeyService.updateAffordabilityResult(journey.id, instalmentAmounts)

    if (journey.affordabilityEnabled.contains(false))
      updateAffordabilityResult.flatMap(_ =>
        journeyService.updateCanPayWithinSixMonths(journey.id, CanPayWithinSixMonthsAnswers.AnswerNotRequired)
      )
    else
      updateAffordabilityResult
  }

}
