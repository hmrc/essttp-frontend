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
import essttp.journey.model.Journey
import play.api.mvc._
import services.{EnrolmentService, JourneyService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DetermineTaxIdController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    journeyService:   JourneyService,
    enrolmentService: EnrolmentService,
    views:            Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  def determineTaxId(): Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val f = request.journey match {
      case j: Journey.Stages.Started => enrolmentService.determineTaxId(j, request.enrolments)
      case _: Journey.AfterComputedTaxId =>
        JourneyLogger.info("TaxId already determined, skipping.")
        Future.successful(())
    }

    f.map(_ => Redirect(routes.DetermineEligibilityController.determineEligibility()))
  }
}
