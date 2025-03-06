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
import models.bars.request.{BarsValidateRequest, BarsVerifyBusinessRequest, BarsVerifyPersonalRequest}
import models.bars.response.BarsVerifyResponse
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.libs.ws.writeableOf_JsValue
import requests.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(using ExecutionContext) {

  /** "The Validate Bank Details endpoint combines several functions to provide an aggregated validation result"
    */
  private val validateUrl: String = appConfig.BaseUrl.barsUrl + "/validate/bank-details"

  def validateBankDetails(
    barsValidateRequest: BarsValidateRequest
  )(implicit requestHeader: RequestHeader): Future[HttpResponse] =
    httpClient
      .post(url"$validateUrl")
      .withBody(Json.toJson(barsValidateRequest))
      .execute[HttpResponse]

  /** "This endpoint checks the likely correctness of a given personal bank account and it's likely connection to the
    * given account holder (aka the subject)"
    */
  private val verifyPersonalUrl: String = appConfig.BaseUrl.barsUrl + "/verify/personal"

  def verifyPersonal(
    barsVerifyPersonalRequest: BarsVerifyPersonalRequest
  )(implicit requestHeader: RequestHeader): Future[BarsVerifyResponse] =
    httpClient
      .post(url"$verifyPersonalUrl")
      .withBody(Json.toJson(barsVerifyPersonalRequest))
      .execute[BarsVerifyResponse]

  /** "This endpoint checks the likely correctness of a given business bank account and it's likely connection to the
    * given business"
    */
  private val verifyBusinessUrl: String = appConfig.BaseUrl.barsUrl + "/verify/business"

  def verifyBusiness(
    barsVerifyBusinessRequest: BarsVerifyBusinessRequest
  )(implicit requestHeader: RequestHeader): Future[BarsVerifyResponse] =
    httpClient
      .post(url"$verifyBusinessUrl")
      .withBody(Json.toJson(barsVerifyBusinessRequest))
      .execute[BarsVerifyResponse]
}
