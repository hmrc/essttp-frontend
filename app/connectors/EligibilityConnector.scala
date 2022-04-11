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

package connectors

import config.AppConfig
import connectors.EligibilityConnector.EligibilityRequest
import essttp.rootmodel.{ Aor, TaxId, TaxRegime, Vrn }
import models.ttp.TtpEligibilityData
import play.api.libs.json.{ Format, Json }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import EligibilityConnector._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EligibilityStubConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig) {

  def eligibilityData(idType: String, regime: TaxRegime, id: TaxId, showFinancials: Boolean)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[TtpEligibilityData] = {

    httpClient
      .POST[EligibilityRequest, TtpEligibilityData](
        url = ttpUrl,
        body = EligibilityRequest(idType, valueOf(id), regime.entryName, showFinancials))

  }

  def ttpUrl = s"${appConfig.ttpBaseUrl}/time-to-pay/self-serve/eligibility"

}

object EligibilityConnector {

  case class EligibilityRequest(idType: String, idNumber: String, regimeType: String, returnFinancials: Boolean)

  object EligibilityRequest {
    implicit val fmt: Format[EligibilityRequest] = Json.format[EligibilityRequest]
  }

  def valueOf(id: TaxId): String = id match {
    case Aor(value) => value
    case Vrn(value) => value
  }

}

