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

import actions.Actions
import actionsmodel.AuthenticatedJourneyRequest
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.{Journey, JourneyStage}
import essttp.rootmodel.{TaxId, TaxRegime}
import models.audit.eligibility.EnrollmentReasons
import play.api.mvc.*
import services.{AuditService, EnrolmentService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}

import java.time.LocalTime
import java.time.temporal.ChronoField
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineTaxIdController @Inject() (
  as:               Actions,
  mcc:              MessagesControllerComponents,
  journeyConnector: JourneyConnector,
  enrolmentService: EnrolmentService,
  auditService:     AuditService
)(using ec: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc),
      Logging {

  private def redirectToLegacySaOr(f: Future[Result])(using r: AuthenticatedJourneyRequest[?]): Future[Result] = {
    val redirectToLegacySa: Boolean = r.journey.taxRegime match {
      case TaxRegime.Sa =>
        if (appConfig.saLegacyPassThroughPercentage == 0)
          true
        else if (appConfig.saLegacyPassThroughPercentage == 100)
          false
        else {
          val timeNowMillis = LocalTime.now().get(ChronoField.MILLI_OF_SECOND)
          // (timeNowMillis % 100) will give a number between 0 and 99. Adding 1 gives us
          // a number between 1 and 100
          (timeNowMillis % 100) + 1 <= appConfig.saLegacyPassThroughPercentage
        }

      case _ =>
        false
    }

    if (redirectToLegacySa) Redirect(appConfig.saLegacyRedirectUrl) else f
  }

  val determineTaxId: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    redirectToLegacySaOr {
      val maybeTaxId: Future[Option[TaxId]] = request.journey match {
        case j: Journey.Started                 =>
          if (j.taxRegime == TaxRegime.Simp) {
            request.nino match {
              case None =>
                auditService.auditEligibilityCheck(j, EnrollmentReasons.NoNino())
                Future.successful(None)

              case Some(nino) =>
                journeyConnector
                  .updateTaxId(j.journeyId, nino)
                  .map(_ => Some(nino))
            }
          } else {
            enrolmentService.determineTaxIdAndUpdateJourney(j, request.enrolments)
          }
        case j: JourneyStage.AfterComputedTaxId =>
          JourneyLogger.info("TaxId already determined, skipping.")
          Future.successful(Some(j.taxId))
      }

      maybeTaxId.map {
        case Some(_) =>
          Routing.redirectToNext(
            routes.DetermineTaxIdController.determineTaxId,
            request.journey,
            submittedValueUnchanged = false
          )

        case None =>
          request.journey.taxRegime match {
            case TaxRegime.Epaye => Redirect(routes.NotEnrolledController.notEnrolled)
            case TaxRegime.Vat   => Redirect(routes.NotEnrolledController.notVatRegistered)
            case TaxRegime.Sa    => Redirect(routes.NotEnrolledController.notSaEnrolled)
            case TaxRegime.Simp  => Redirect(routes.IneligibleController.simpGenericIneligiblePage)
          }
      }
    }
  }

}
