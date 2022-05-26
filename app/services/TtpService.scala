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
import essttp.journey.model.Journey.Stages.ComputedTaxId
import essttp.journey.model.ttp.EligibilityCheckResult
import essttp.rootmodel.EmpRef
import play.api.mvc.RequestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
/**
 * Time To Pay (Ttp) Service.
 */
@Singleton
class TtpService @Inject() (ttpConnector: TtpConnector) {

  def determineEligibility(journey: ComputedTaxId)(implicit request: RequestHeader): Future[EligibilityCheckResult] = {

    val eligibilityRequest: CallEligibilityApiRequest = journey match {
      case j: Journey.Epaye =>
        CallEligibilityApiRequest(
          idType           = "SSTTP",
          idNumber         = j.taxId match {
            case empRef: EmpRef => empRef.value //Hmm, will it compile, theoretically it can't be Vrn ...
            case other          => sys.error(s"Expected EmpRef but found ${other.getClass.getSimpleName}")
          },
          regimeType       = "PAYE",
          returnFinancials = true
        )
    }
    ttpConnector.callEligibilityApi(eligibilityRequest)
  }
}

