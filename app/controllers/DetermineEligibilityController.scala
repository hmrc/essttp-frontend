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
import essttp.journey.JourneyConnector
import essttp.journey.model.Journey
import essttp.journey.model.Journey.HasEligibilityCheckResult
import essttp.journey.model.ttp.EligibilityCheckResult
import play.api.mvc._
import services.TtpService
import testOnly.models.EligibilityErrors
import testOnly.models.EligibilityErrors._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineEligibilityController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    ttpService:       TtpService,
    journeyConnector: JourneyConnector,
    views:            Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val determineEligibility: Action[AnyContent] = as.journeyAction.async { implicit request =>
    request.journey match {
      case j: Journey.Stages.AfterStarted =>
        Future.failed[Result](new RuntimeException(
          "Journey in incorrect state during 'determineEligibility'. " +
            "Please investigate why. " +
            "Sending user back to the landing page."
        ))
      case j: Journey.Stages.AfterComputedTaxId =>
        determineEligibilityAndUpdateJourney(j)
      case j: HasEligibilityCheckResult =>
        JourneyLogger.info("Eligibility already determined, skipping.")
        Future.successful(route(j.eligibilityCheckResult))
    }
  }

  def determineEligibilityAndUpdateJourney(journey: Journey.Stages.AfterComputedTaxId)(implicit request: Request[_]): Future[Result] = {
    for {
      eligibilityCheckResult <- journey match {
        case journey: Journey.Epaye.AfterComputedTaxIds => ttpService.determineEligibility(journey)
      }
      _ <- journeyConnector.updateEligibilityCheckResult(journey.id, eligibilityCheckResult)
    } yield route(eligibilityCheckResult)
  }

  private def route(eligibilityResult: EligibilityCheckResult)(implicit request: RequestHeader): Result = {
    if (eligibilityResult.isEligible) {
      Redirect(routes.YourBillController.yourBill())
    } else
      EligibilityErrors.toEligibilityError(eligibilityResult.eligibilityRules) match {
        case MultipleReasons            => Redirect(routes.IneligibleController.genericIneligiblePage())
        case HasRlsOnAddress            => Redirect(routes.IneligibleController.genericIneligiblePage())
        case MarkedAsInsolvent          => Redirect(routes.IneligibleController.genericIneligiblePage())
        case IsLessThanMinDebtAllowance => Redirect(routes.IneligibleController.genericIneligiblePage())
        case IsMoreThanMaxDebtAllowance => Redirect(routes.IneligibleController.debtTooLargePage())
        case DisallowedChargeLocks      => Redirect(routes.IneligibleController.genericIneligiblePage())
        case ExistingTTP                => Redirect(routes.IneligibleController.alreadyHaveAPaymentPlanPage())
        case ExceedsMaxDebtAge          => Redirect(routes.IneligibleController.debtTooOldPage())
        case EligibleChargeType         => Redirect(routes.IneligibleController.genericIneligiblePage())
        case MissingFiledReturns        => Redirect(routes.IneligibleController.fileYourReturnPage())
      }
  }

}
