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
import actionsmodel.AuthenticatedJourneyRequest
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import play.api.mvc._
import services.{AuditService, JourneyService, TtpService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineEligibilityController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    ttpService:     TtpService,
    journeyService: JourneyService,
    auditService:   AuditService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val determineEligibility: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.Stages.Started       => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.Stages.ComputedTaxId => determineEligibilityAndUpdateJourney(j)
      case j: Journey.AfterEligibilityChecked =>
        val proposedResult = {
          JourneyLogger.info("Eligibility already determined, skipping.")
          Redirect(EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime))
        }
        finalStateCheckF(j, proposedResult)
    }
  }

  def determineEligibilityAndUpdateJourney(journey: Journey.Stages.ComputedTaxId)(implicit r: AuthenticatedJourneyRequest[_]): Future[Result] = {
    /**
     * TODO: return this function to whats in comment below, it's been changed to a disgusting hacky fix to cater for ETMP/IF errors downstream
     * for {
     * eligibilityCheckResult <- ttpService.determineEligibility(journey)
     * _ = auditService.auditEligibilityCheck(journey, eligibilityCheckResult)
     * updatedJourney <- journeyService.updateEligibilityCheckResult(journey.id, eligibilityCheckResult)
     * } yield Redirect(Routing.next(updatedJourney))
     */
    val maybeEligibilityCheckResult: Future[Option[EligibilityCheckResult]] = for {
      eligibilityCheckResult: Option[EligibilityCheckResult] <- ttpService.determineEligibility(journey)
    } yield eligibilityCheckResult

    maybeEligibilityCheckResult
      .flatMap {
        _.fold {
          val redirect = journey.taxRegime match {
            case TaxRegime.Epaye => routes.IneligibleController.payeGenericIneligiblePage.url
            case TaxRegime.Vat   => routes.IneligibleController.vatGenericIneligiblePage.url
          }
          toFuture(Redirect(redirect))
        } { eligibilityCheckResult =>
          for {
            updatedJourney <- journeyService.updateEligibilityCheckResult(journey.id, eligibilityCheckResult)
            _ = auditService.auditEligibilityCheck(journey, eligibilityCheckResult)
            // below log message used by Kibana dashboard.
            _ = if (eligibilityCheckResult.isEligible) JourneyLogger.info(s"Eligible journey being started for ${journey.taxRegime.toString}")
          } yield Routing.redirectToNext(routes.DetermineEligibilityController.determineEligibility, updatedJourney, submittedValueUnchanged = false)
        }
      }
  }

}
