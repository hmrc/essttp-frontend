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

package services

import connectors.{CallEligibilityApiRequest, TtpConnector}
import essttp.journey.model.Journey
import essttp.journey.model.Journey.HasTaxId
import essttp.journey.model.ttp.EligibilityCheckResult
import essttp.rootmodel.Aor
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
/**
 * Time To Pay (Ttp) Service.
 */
@Singleton
class TtpService @Inject() (ttpConnector: TtpConnector)(implicit ec: ExecutionContext) {

  def determineEligibility(journey: Journey with HasTaxId)(implicit request: RequestHeader): Future[EligibilityCheckResult] = {

    val eligibilityRequest: CallEligibilityApiRequest = journey match {
      case j: Journey.Epaye =>
        CallEligibilityApiRequest(
          idType           = "SSTTP", // Is this always SSTTP? - Yes
          idNumber         = j.taxId match {
            case aor: Aor => aor.value //Hmm, will it compile, theoretically it can't be Vrn ...
          },
          regimeType       = "PAYE",
          returnFinancials = true // This is always SSTTP? - Yes
        )
    }
    ttpConnector.callEligibilityApi(eligibilityRequest)
  }
}

