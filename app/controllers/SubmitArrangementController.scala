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
import essttp.journey.model.Journey
import play.api.mvc._
import services.{AuditService, JourneyService, TtpService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitArrangementController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    ttpService:     TtpService,
    journeyService: JourneyService,
    auditService:   AuditService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val submitArrangement: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeAgreedTermsAndConditions  => logErrorAndRouteToDefaultPageF(j)
      case j: Journey.Stages.AgreedTermsAndConditions => submitArrangementAndUpdateJourney(j)
      case _: Journey.AfterArrangementSubmitted =>
        JourneyLogger.info("Already submitted arrangement to ttp, showing user the success page")
        Future.successful(Redirect(routes.PaymentPlanSetUpController.paymentPlanSetUp))
    }
  }

  private def submitArrangementAndUpdateJourney(
      journey: Journey.Stages.AgreedTermsAndConditions
  )(implicit request: AuthenticatedJourneyRequest[_]): Future[Result] = {
    for {
      arrangementResponse <- ttpService.submitArrangement(journey)
      _ <- journeyService.updateArrangementResponse(journey.id, arrangementResponse)
    } yield Redirect(routes.PaymentPlanSetUpController.paymentPlanSetUp)
  }

}
