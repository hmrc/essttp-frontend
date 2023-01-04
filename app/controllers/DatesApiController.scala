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
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import controllers.JourneyFinalStateCheck.finalStateCheckF
import essttp.journey.model.Journey
import play.api.mvc._
import services.{DatesService, JourneyService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DatesApiController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    datesService:   DatesService,
    journeyService: JourneyService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val retrieveExtremeDates: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeAnsweredCanPayUpfront        => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.Stages.EnteredUpfrontPaymentAmount => getExtremeDatesAndUpdateJourney(Left(j))
      case j: Journey.Stages.AnsweredCanPayUpfront       => getExtremeDatesAndUpdateJourney(Right(j))
      case j: Journey.AfterExtremeDatesResponse =>
        JourneyLogger.info("ExtremeDates already determined, skipping.") // we will want to update the journey perhaps?
        finalStateCheckF(j, Future.successful(Redirect(routes.DetermineAffordabilityController.determineAffordability)))
    }
  }

  def getExtremeDatesAndUpdateJourney(
      journey: Either[Journey.Stages.EnteredUpfrontPaymentAmount, Journey.Stages.AnsweredCanPayUpfront]
  )(implicit request: Request[_]): Future[Result] = {
    val j = journey.merge
    for {
      extremeDatesResponse <- datesService.extremeDates(j)
      updatedJourney <- journeyService.updateExtremeDatesResult(j.id, extremeDatesResponse)
    } yield Routing.redirectToNext(routes.DatesApiController.retrieveExtremeDates, updatedJourney, submittedValueUnchanged = false)
  }

  val retrieveStartDates: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredDayOfMonth => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterEnteredDayOfMonth  => finalStateCheckF(j, getStartDatesAndUpdateJourney(j))
    }
  }

  def getStartDatesAndUpdateJourney(
      journey: Journey.AfterEnteredDayOfMonth
  )(implicit request: Request[_]): Future[Result] = {
    for {
      startDatesResponse <- datesService.startDates(journey)
      updatedJourney <- journeyService.updateStartDates(journey.id, startDatesResponse)
    } yield Routing.redirectToNext(routes.DatesApiController.retrieveStartDates, updatedJourney, submittedValueUnchanged = false)
  }

}
