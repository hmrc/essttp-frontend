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

import com.google.inject.{Inject, Singleton}
import essttp.journey.model.ttp.EligibilityCheckResult
import play.api.mvc.RequestHeader
import requests.RequestSupport._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TtpConnector @Inject() (config: TtpConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  /**
   * Eligibility Api implemented by Ttp service.
   * https://confluence.tools.tax.service.gov.uk/display/DTDT/Eligibility+API
   */
  def callEligibilityApi(eligibilityRequest: CallEligibilityApiRequest)(implicit request: RequestHeader): Future[EligibilityCheckResult] = {
    val url: String = config.baseUrl + "/time-to-pay/self-serve/eligibility"
    httpClient.POST[CallEligibilityApiRequest, EligibilityCheckResult](url, eligibilityRequest)
  }
}

final case class TtpConfig(baseUrl: String) {
  @Inject()
  def this(config: ServicesConfig) {
    this(
      baseUrl = config.baseUrl("ttp")
    )
  }
}
