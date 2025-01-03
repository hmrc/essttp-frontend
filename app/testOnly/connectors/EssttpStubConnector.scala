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

package testOnly.connectors

import essttp.crypto.CryptoFormat
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EssttpStubConnector @Inject() (httpClient: HttpClientV2, config: Configuration)(implicit ex: ExecutionContext) extends ServicesConfig(config) {

  implicit val cryptoFormat: CryptoFormat = CryptoFormat.NoOpCryptoFormat

  val stubsBaseUrl: String = baseUrl("essttp-stubs")

  val insertEligibilityDataUrl: String = s"$stubsBaseUrl/debts/time-to-pay/eligibility/insert"

  def storePegaGetCaseResponseUrl(caseId: String): String = s"$stubsBaseUrl/pega/case/$caseId"

  def primeStubs(response: EligibilityCheckResult)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.post(url"$insertEligibilityDataUrl")
      .withBody(Json.toJson(response))
      .execute[Unit]

  def storePegaGetCaseResponse(caseId: String, getCaseResponse: JsValue)(implicit rh: HeaderCarrier): Future[Unit] =
    httpClient.post(url"${storePegaGetCaseResponseUrl(caseId)}")
      .withBody(getCaseResponse)
      .execute[Unit]

}
