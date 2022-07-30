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
import config.AppConfig
import play.api.mvc.RequestHeader
import requests.RequestSupport._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._
import models.bars.BarsModel._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  /**
   * The Validate Bank Details endpoint combines several functions to provide an aggregated validation result
   */
  private val validateUrl: String = appConfig.BaseUrl.barsUrl + "/validate/bank-details"

  def validateBankDetails(barsValidateRequest: BarsValidateRequest)(implicit requestHeader: RequestHeader): Future[BarsResponse] = {
    httpClient.POST[BarsValidateRequest, BarsResponse](validateUrl, barsValidateRequest)
  }
}
