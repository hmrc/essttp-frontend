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

import _root_.actions.{Actions, EnrolmentDef}
import essttp.journey.model.ttp.EligibilityCheckResult
import play.api.mvc._
import services.{EpayeService, TtpService}
import testOnly.models.EligibilityErrors
import testOnly.models.EligibilityErrors._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DetermineEligibilityController @Inject() (
    as:           Actions,
    mcc:          MessagesControllerComponents,
    ttpService:   TtpService,
    epayeService: EpayeService,
    views:        Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val determineEligibility: Action[AnyContent] = as.journeyAction.async { implicit request =>
    //TODO: doesn't support Vat yet

    val (taxOfficeNumber, taxOfficeReference) = EnrolmentDef
      .Epaye
      .findEnrolmentValues(request.enrolments)
      .getOrElse(throw new RuntimeException("TaxOfficeNumber and TaxOfficeReference not found"))

    for {
      aor <- epayeService.retrieveAor(taxOfficeNumber, taxOfficeReference)
      eligibilityResult <- ttpService.determineEligibility(aor)
      //TODO: update journey with Aor, epaye enrolments and EligibilityResult (PAWEL)
      result = computeWhereToRoute(eligibilityResult)
    } yield result
  }

  private def computeWhereToRoute(eligibilityResult: EligibilityCheckResult)(implicit request: RequestHeader): Result = {
    if (eligibilityResult.isEligible) {
      Redirect(routes.YourBillController.yourBill())
    } else {
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

}
