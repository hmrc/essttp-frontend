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

package connectors

import com.google.inject.{Inject, Singleton}
import config.AppConfig
import essttp.crypto.CryptoFormat
import essttp.journey.model.CorrelationId
import essttp.rootmodel.ttp.affordability.{InstalmentAmountRequest, InstalmentAmounts}
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesRequest, AffordableQuotesResponse}
import essttp.rootmodel.ttp.arrangement.{ArrangementRequest, ArrangementResponse}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.libs.ws.writeableOf_JsValue
import requests.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TtpConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(using ExecutionContext) {

  import appConfig.eligibilityReqIdentificationFlag
  implicit val cryptoFormat: CryptoFormat = CryptoFormat.NoOpCryptoFormat

  private val correlationIdHeaderKey: String = appConfig.TtpHeaders.correlationId

  /** Eligibility Api implemented by Ttp service.
    * https://confluence.tools.tax.service.gov.uk/display/DTDT/Eligibility+API
    */
  private val eligibilityUrl: String = appConfig.BaseUrl.timeToPayEligibilityUrl + "/debts/time-to-pay/eligibility"

  def callEligibilityApi(eligibilityRequest: CallEligibilityApiRequest, correlationId: CorrelationId)(implicit
    requestHeader: RequestHeader
  ): Future[EligibilityCheckResult] =
    httpClient
      .post(url"$eligibilityUrl")
      .withBody(Json.toJson(eligibilityRequest))
      .setHeader((correlationIdHeaderKey, correlationId.value.toString))
      .execute[EligibilityCheckResult]

  /** Affordability Api (min/max) implemented by Ttp service.
    * https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?pageId=433455297
    */
  private val affordabilityUrl: String = appConfig.BaseUrl.timeToPayUrl + "/debts/time-to-pay/self-serve/affordability"

  def callAffordabilityApi(instalmentAmountRequest: InstalmentAmountRequest, correlationId: CorrelationId)(implicit
    requestHeader: RequestHeader
  ): Future[InstalmentAmounts] =
    httpClient
      .post(url"$affordabilityUrl")
      .withBody(Json.toJson(instalmentAmountRequest))
      .setHeader((correlationIdHeaderKey, correlationId.value.toString))
      .execute[InstalmentAmounts]

  /** Affordable Quotes API (for instalments) implemented by ttp service.
    * https://confluence.tools.tax.service.gov.uk/display/DTDT/Affordable+quotes+API
    */
  private val affordableQuotesUrl: String =
    appConfig.BaseUrl.timeToPayUrl + "/debts/time-to-pay/affordability/affordable-quotes"

  def callAffordableQuotesApi(affordableQuotesRequest: AffordableQuotesRequest, correlationId: CorrelationId)(implicit
    requestHeader: RequestHeader
  ): Future[AffordableQuotesResponse] =
    httpClient
      .post(url"$affordableQuotesUrl")
      .withBody(Json.toJson(affordableQuotesRequest))
      .setHeader((correlationIdHeaderKey, correlationId.value.toString))
      .execute[AffordableQuotesResponse]

  /** Enact arrangement API (for setting up the arrangement) implemented by ttp service.
    * https://confluence.tools.tax.service.gov.uk/display/DTDT/Enact+arrangement+API
    */
  private val arrangementUrl: String = appConfig.BaseUrl.timeToPayUrl + "/debts/time-to-pay/self-serve/arrangement"

  def callArrangementApi(arrangementRequest: ArrangementRequest, correlationId: CorrelationId)(implicit
    requestHeader: RequestHeader
  ): Future[ArrangementResponse] =
    httpClient
      .post(url"$arrangementUrl")
      .withBody(Json.toJson(arrangementRequest))
      .setHeader((correlationIdHeaderKey, correlationId.value.toString))
      .execute[ArrangementResponse]
}
