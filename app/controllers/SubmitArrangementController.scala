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
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import paymentsEmailVerification.models.EmailVerificationResult
import play.api.mvc._
import services.{JourneyService, TtpService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmitArrangementController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    ttpService:     TtpService,
    journeyService: JourneyService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val submitArrangement: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeAgreedTermsAndConditions =>
        logErrorAndRouteToDefaultPageF(j)

      case j: Journey.Stages.AgreedTermsAndConditions =>
        if (j.isEmailAddressRequired) logErrorAndRouteToDefaultPageF(j) else submitArrangementAndUpdateJourney(Left(j))

      case j: Journey.Stages.SelectedEmailToBeVerified =>
        logErrorAndRouteToDefaultPageF(j)

      case j: Journey.Stages.EmailVerificationComplete =>
        j.emailVerificationResult match {
          case EmailVerificationResult.Verified => submitArrangementAndUpdateJourney(Right(j))
          case EmailVerificationResult.Locked   => logErrorAndRouteToDefaultPageF(j)
        }

      case j: Journey.AfterArrangementSubmitted =>
        JourneyLogger.info("Already submitted arrangement to ttp, showing user the success page")
        Redirect(SubmitArrangementController.whichPaymentPlanSetupPage(j.taxRegime))
    }
  }

  private def submitArrangementAndUpdateJourney(
      journey: Either[Journey.Stages.AgreedTermsAndConditions, Journey.Stages.EmailVerificationComplete]
  )(implicit request: AuthenticatedJourneyRequest[_]): Future[Result] = {
    for {
      arrangementResponse <- ttpService.submitArrangement(journey)
      updatedJourney <- journeyService.updateArrangementResponse(journey.fold(_.id, _.id), arrangementResponse)
    } yield Routing.redirectToNext(routes.SubmitArrangementController.submitArrangement, updatedJourney, submittedValueUnchanged = false)
  }

}

object SubmitArrangementController {
  def whichPaymentPlanSetupPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.PaymentPlanSetUpController.epayePaymentPlanSetUp
    case TaxRegime.Vat   => routes.PaymentPlanSetUpController.vatPaymentPlanSetUp
    case TaxRegime.Sa    => routes.PaymentPlanSetUpController.saPaymentPlanSetUp
    case TaxRegime.Sia   => routes.PaymentPlanSetUpController.siaPaymentPlanSetUp
  }
}
