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
import essttp.journey.model.Journey
import essttp.rootmodel.{TaxId, TaxRegime}
import play.api.mvc._
import services.EnrolmentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineTaxIdController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    enrolmentService: EnrolmentService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  def determineTaxId(): Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val (maybeTaxId, taxRegime): (Future[Option[TaxId]], TaxRegime) = request.journey match {
      case j: Journey.Stages.Started =>
        if (j.taxRegime == TaxRegime.Sia) {
          (Future(request.nino), j.taxRegime)
        } else {
          enrolmentService.determineTaxIdAndUpdateJourney(j, request.enrolments) -> j.taxRegime
        }
      case j: Journey.AfterComputedTaxId =>
        JourneyLogger.info("TaxId already determined, skipping.")
        Future.successful(Some(j.taxId)) -> j.taxRegime
    }

    maybeTaxId.map {
      case Some(_) =>
        Routing.redirectToNext(routes.DetermineTaxIdController.determineTaxId, request.journey, submittedValueUnchanged = false)

      case None => taxRegime match {
        case TaxRegime.Epaye => Redirect(routes.NotEnrolledController.notEnrolled)
        case TaxRegime.Vat   => Redirect(routes.NotEnrolledController.notVatRegistered)
        case TaxRegime.Sa    => Redirect(routes.NotEnrolledController.notSaEnrolled)
        case TaxRegime.Sia   => Redirect(routes.NotEnrolledController.siaNoNino)
      }
    }
  }
}
