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
import actionsmodel.AuthenticatedJourneyRequest
import cats.data.OptionT
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheckF
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.{Journey, JourneyStage}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.TaxRegime.Sa
import essttp.rootmodel.ttp.eligibility.{EligibilityCheckResult, EligibilityPass, EligibilityStatus}
import play.api.mvc.*
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
)(using ExecutionContext, AppConfig)
    extends FrontendController(mcc),
      Logging {

  val determineEligibility: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.Started                      => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.ComputedTaxId                => determineEligibilityAndUpdateJourney(j)
      case j: JourneyStage.AfterEligibilityChecked =>
        val proposedResult = {
          JourneyLogger.info("Eligibility already determined, skipping.")
          Redirect(EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime))
        }
        finalStateCheckF(j, proposedResult)
    }
  }

  def determineEligibilityAndUpdateJourney(
    journey: Journey.ComputedTaxId
  )(using r: AuthenticatedJourneyRequest[?]): Future[Result] = {

    val maybeEligibilityCheckResult: OptionT[Future, EligibilityCheckResult] = for {
      _                      <- journey.taxRegime match {
                                  case Sa => OptionT.fromOption[Future](checkNinoExists(journey))
                                  case _  => OptionT.some[Future](())
                                }
      eligibilityCheckResult <- OptionT(ttpService.determineEligibility(journey))
    } yield
      if (eligibilityCheckResult.isEligible) {
        journey.taxRegime match {
          case Sa if r.enrolments.getEnrolment("HMRC-MTD-IT").isDefined =>
            mtdEligibleResult(eligibilityCheckResult)
          case Sa                                                       =>
            mtdIneligibleResult(eligibilityCheckResult)
          case _                                                        =>
            eligibilityCheckResult
        }
      } else {
        eligibilityCheckResult
      }

    maybeEligibilityCheckResult.value
      .flatMap {
        _.fold {
          val redirect = journey.taxRegime match {
            case TaxRegime.Epaye => routes.IneligibleController.payeGenericIneligiblePage.url
            case TaxRegime.Vat   => routes.IneligibleController.vatGenericIneligiblePage.url
            case TaxRegime.Sa    => routes.IneligibleController.saGenericIneligiblePage.url
            case TaxRegime.Simp  => routes.IneligibleController.simpGenericIneligiblePage.url
          }
          resultToFutureResult(Redirect(redirect))
        } { eligibilityCheckResult =>
          for {
            updatedJourney <- journeyService.updateEligibilityCheckResult(journey.id, eligibilityCheckResult)
            _               = auditService.auditEligibilityCheck(journey, eligibilityCheckResult)
            // below log message used by Kibana dashboard.
            _               = if (eligibilityCheckResult.isEligible)
                                JourneyLogger.info(s"Eligible journey being started for ${journey.taxRegime.toString}")
          } yield Routing.redirectToNext(
            routes.DetermineEligibilityController.determineEligibility,
            updatedJourney,
            submittedValueUnchanged = false
          )
        }
      }
  }

  // if SA users have IR-SA enrollment, but no NINO is found, they are directed to the generic kickout page
  def checkNinoExists(journey: Journey.ComputedTaxId)(using r: AuthenticatedJourneyRequest[?]): Option[Unit] =
    journey.taxRegime match {
      case TaxRegime.Sa => r.nino.map(_ => ())
      case _            => Some(())
    }
  // if SA users have IR-SA enrollment, but no HMRC-MTD-IT enrollment, they are directed to the 'Sign up tp MDT' kickout page
  private def mtdIneligibleResult(result: EligibilityCheckResult): EligibilityCheckResult                    =
    result.copy(
      eligibilityRules = result.eligibilityRules.copy(
        noMtditsaEnrollment = Some(true)
      ),
      eligibilityStatus = EligibilityStatus(EligibilityPass(value = false))
    )

  private def mtdEligibleResult(result: EligibilityCheckResult): EligibilityCheckResult =
    result.copy(
      eligibilityRules = result.eligibilityRules.copy(
        noMtditsaEnrollment = Some(false)
      )
    )

}
