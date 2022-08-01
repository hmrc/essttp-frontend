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

package controllers

import _root_.actions.Actions
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import essttp.journey.model.Journey
import play.api.mvc._
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
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val determineAffordability: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeUpfrontPaymentAnswers => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterUpfrontPaymentAnswers  => finalStateCheckF(j, determineAffordabilityAndUpdateJourney(j))
    }
  }

  def determineAffordabilityAndUpdateJourney(journey: Journey.AfterUpfrontPaymentAnswers)(implicit request: Request[_]): Future[Result] = {
    for {
      instalmentAmounts <- ttpService.determineAffordability(journey)
      _ <- journeyService.updateAffordabilityResult(journey.id, instalmentAmounts)
    } yield Redirect(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount.url)
  }

}
