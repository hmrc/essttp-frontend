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
import essttp.journey.JourneyConnector
import essttp.journey.model.Journey
import essttp.rootmodel.EmpRef
import play.api.mvc._
import services.{JourneyService, TtpService}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineTaxIdController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    ttpService:     TtpService,
    journeyService: JourneyService,
    views:          Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  def determineTaxId(): Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val f = request.journey match {
      case j: Journey.Stages.AfterStarted => determineTaxId(j, request.enrolments)
      case j: Journey.HasTaxId =>
        JourneyLogger.info("TaxId already determined, skipping.")
        Future.successful(())
    }

    f.map(_ => Redirect(routes.DetermineEligibilityController.determineEligibility()))
  }

  private def determineTaxId(journey: Journey.Stages.AfterStarted, enrolments: Enrolments)(implicit request: RequestHeader): Future[Unit] = {
    //todo move this to enrolment service or something
    val computeEmpRef: Future[EmpRef] = Future{
      val (taxOfficeNumber, taxOfficeReference) = EnrolmentDef
        .Epaye
        .findEnrolmentValues(enrolments)
        .getOrElse(throw new RuntimeException("TaxOfficeNumber and TaxOfficeReference not found"))
      EmpRef.makeEmpRef(taxOfficeNumber, taxOfficeReference)
    }

    for {
      empRef <- computeEmpRef
      _ <- journeyService.UpdateTaxRef.updateEpayeTaxId(journey.id, empRef)
    } yield ()
  }
}
