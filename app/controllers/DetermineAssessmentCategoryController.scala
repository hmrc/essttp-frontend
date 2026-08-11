/*
 * Copyright 2026 HM Revenue & Customs
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
import actionsmodel.AuthenticatedJourneyRequest
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheckF
import essttp.journey.model.{Journey, JourneyStage}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DetermineAssessmentCategoryController @Inject() (
  as:             Actions,
  mcc:            MessagesControllerComponents,
  journeyService: JourneyService
)(using ExecutionContext, AppConfig)
    extends FrontendController(mcc),
      Logging {

  val determineAssessmentCategory: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: JourneyStage.AfterAssessmentCategoryDetermined =>
        finalStateCheckF(j, redirectToNext(j))

      case j: JourneyStage.BeforeAssessmentCategoryDetermined =>
        finalStateCheckF(j, determineAffordabilityAndUpdateJourney(j, request.eligibilityCheckResult))
    }
  }

  private def determineAffordabilityAndUpdateJourney(
    j:                      Journey & JourneyStage.BeforeAssessmentCategoryDetermined,
    eligibilityCheckResult: EligibilityCheckResult
  )(using AuthenticatedJourneyRequest[?]): Future[Result] = {
    val assessmentCategory = eligibilityCheckResult.foldOnAssessmentCategory(
      Some(_),
      Some(_),
      Some(_),
      (debts, liabilities, debtsAndLiabilities) =>
        if (
          debts.assessmentEligibilityStatus && liabilities.assessmentEligibilityStatus && debtsAndLiabilities.assessmentEligibilityStatus
        ) {
          // if we have debtsAndLiabilities together then user needs to go on a journey beyond here to determine the
          // assessment category to use for the rest of the journey
          None
        } else {
          throw new Exception(
            s"Got unexpected eligibility status for debts and liabilities: " +
              s"debts: ${debts.assessmentEligibilityStatus}, liabilities: ${liabilities.assessmentEligibilityStatus}, debtsAndLiabilities: ${debtsAndLiabilities.assessmentEligibilityStatus}"
          )
        }
    )

    assessmentCategory match {
      case None =>
        redirectToNext(j)

      case Some(assessmentCategory) =>
        journeyService
          .updateAssessmentCategory(j.journeyId, assessmentCategory.assessmentCategory)
          .map(_ => redirectToNext(j))

    }
  }

  private def redirectToNext(j: Journey)(using AuthenticatedJourneyRequest[?]): Result = Routing.redirectToNext(
    routes.DetermineAssessmentCategoryController.determineAssessmentCategory,
    j,
    submittedValueUnchanged = false
  )

}
