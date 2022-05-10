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

import actionsmodel.JourneyRequest
import connectors.TtpConnector
import essttp.journey.model.ttp.{EligibilityCheckResult, EligibilityRequest}
import essttp.rootmodel.{Aor, TaxRegime}
import services.TtpService.deriveRegimeTypeFromRegime
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Time To Pay (Ttp) Service.
 */
@Singleton
class TtpService @Inject() (ttpConnector: TtpConnector)(implicit ec: ExecutionContext) {

  //todo change this from epaye aor:Aor to common abstracted taxIdentifier or something
  def determineEligibility(aor: Aor)(implicit request: JourneyRequest[_], headerCarrier: HeaderCarrier): Future[EligibilityCheckResult] = {
    val eligibilityRequest: EligibilityRequest = EligibilityRequest(
      idType           = "SSTTP", // is this always SSTTP?
      idNumber         = aor.value,
      regimeType       = deriveRegimeTypeFromRegime(request.journey.taxRegime),
      returnFinancials = true // does this need to be true always?
    )
    for {
      eligibilityResult: EligibilityCheckResult <- ttpConnector.retrieveEligibilityData(eligibilityRequest)
    } yield eligibilityResult
  }
}

object TtpService {

  def deriveRegimeTypeFromRegime: TaxRegime => String = {
    case essttp.rootmodel.TaxRegime.Epaye => "PAYE"
    case essttp.rootmodel.TaxRegime.Vat   => "VAT"
  }

}
