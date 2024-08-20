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
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import essttp.journey.model.{Journey, PaymentPlanAnswers}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import essttp.utils.Errors
import play.api.mvc._
import services.{JourneyService, TtpService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineAffordableQuotesController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    ttpService:     TtpService,
    journeyService: JourneyService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val retrieveAffordableQuotes: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeStartDatesResponse =>
        logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterStartDatesResponse =>
        finalStateCheckF(j, determineAffordableQuotesAndUpdateJourney(Left(j), request.eligibilityCheckResult))
      case j: Journey.AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case p: PaymentPlanAnswers.PaymentPlanNoAffordability =>
            finalStateCheckF(j, determineAffordableQuotesAndUpdateJourney(Right(j -> p), request.eligibilityCheckResult))
          case _: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
            Errors.throwServerErrorException("Not expecting to retrieve affordable quotes when payment plan has been checked on affordability journey")
        }

      case _: Journey.AfterStartedPegaCase =>
        Errors.throwServerErrorException("Not expecting to retrieve affordable quotes when started PEGA case")

    }
  }

  def determineAffordableQuotesAndUpdateJourney(
      journey:                Either[Journey.AfterStartDatesResponse, (Journey.AfterCheckedPaymentPlan, PaymentPlanAnswers.PaymentPlanNoAffordability)],
      eligibilityCheckResult: EligibilityCheckResult
  )(implicit request: Request[_]): Future[Result] = {
    for {
      affordableQuotes <- ttpService.determineAffordableQuotes(journey, eligibilityCheckResult)
      updatedJourney <- journeyService.updateAffordableQuotes(journey.fold(_.id, _._1.id), affordableQuotes)
    } yield Routing.redirectToNext(routes.DetermineAffordableQuotesController.retrieveAffordableQuotes, updatedJourney, submittedValueUnchanged = false)
  }

}
