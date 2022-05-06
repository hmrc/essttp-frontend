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
import models.ttp.EligibilityResult
import play.api.libs.json.Json
import play.api.mvc._
import services.{EpayeService, TtpService}
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

    val (taxOfficeNumber, taxOfficeReference) = EnrolmentDef
      .Epaye
      .findEnrolmentValues(request.enrolments)
      .getOrElse(throw new RuntimeException("TaxOfficeNumber and TaxOfficeRefence not found"))

    for {
      aor <- epayeService.retrieveAor(taxOfficeNumber, taxOfficeReference)
      eligibilityResult <- ttpService.determineEligibility(aor)
      //TODO: update journey with Aor, epaye enrolments and ElibibilityResult
      result = nextUrl(eligibilityResult)
    } yield result
  }

  /**
   * Send user to your-bill-is  or not-eligible url
   */
  private def nextUrl(eligibilityResult: EligibilityResult)(implicit request: RequestHeader): Result = {
    if (eligibilityResult.isEligible) {
      Redirect(routes.YourBillController.yourBill())
    } else {
      //TODO: figure out what should be the URL where to send user
      Ok(s"TODO: User ineligible:\n${Json.prettyPrint(Json.toJson(eligibilityResult.eligibilityRules))}")
    }
  }

}
