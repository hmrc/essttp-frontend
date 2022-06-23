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
import essttp.journey.model.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.journey.model.ttp.affordablequotes.{AffordableQuotesRequest, AffordableQuotesResponse}
import play.api.mvc.RequestHeader
import requests.RequestSupport._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits.{readUnit => _}
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TtpConnector @Inject() (config: TtpConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  /**
   * Eligibility Api implemented by Ttp service.
   * https://confluence.tools.tax.service.gov.uk/display/DTDT/Eligibility+API
   */
  private val eligibilityUrl: String = config.baseUrl + "/time-to-pay/self-serve/eligibility"

  def callEligibilityApi(eligibilityRequest: CallEligibilityApiRequest)(implicit requestHeader: RequestHeader): Future[EligibilityCheckResult] = {
    httpClient.POST[CallEligibilityApiRequest, EligibilityCheckResult](eligibilityUrl, eligibilityRequest)
  }

  /**
   * Affordability Api (min/max) implemented by Ttp service.
   * https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?pageId=433455297
   */
  private val affordabilityUrl: String = config.baseUrl + "/time-to-pay/self-serve/affordability"

  def callAffordabilityApi(instalmentAmountRequest: InstalmentAmountRequest)(implicit requestHeader: RequestHeader): Future[InstalmentAmounts] = {
    httpClient.POST[InstalmentAmountRequest, InstalmentAmounts](affordabilityUrl, instalmentAmountRequest)
  }

  /**
   * Affordable Quotes API (for instalments) implemented by ttp service.
   * https://confluence.tools.tax.service.gov.uk/display/DTDT/Affordable+quotes+API
   */
  private val affordableQuotesUrl: String = config.baseUrl + "/time-to-pay/self-serve/affordable-quotes"

  def callAffordableQuotesApi(affordableQuotesRequest: AffordableQuotesRequest)(implicit requestHeader: RequestHeader): Future[AffordableQuotesResponse] = {
    httpClient.POST[AffordableQuotesRequest, AffordableQuotesResponse](affordableQuotesUrl, affordableQuotesRequest)
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
