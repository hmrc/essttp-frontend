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
import essttp.rootmodel.dates.extremedates.{ExtremeDatesRequest, ExtremeDatesResponse}
import essttp.rootmodel.dates.startdates.{StartDatesRequest, StartDatesResponse}
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import requests.RequestSupport._
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DatesApiConnector @Inject() (config: DatesApiConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val startDatesUrl: String = config.baseUrl + "/essttp-backend/start-dates"

  def startDates(startDatesRequest: StartDatesRequest)(implicit request: RequestHeader): Future[StartDatesResponse] = {
    httpClient.post(url"$startDatesUrl")
      .withBody(Json.toJson(startDatesRequest))
      .execute[StartDatesResponse]
  }

  private val extremeDatesUrl: String = config.baseUrl + "/essttp-backend/extreme-dates"

  def extremeDates(extremeDatesRequest: ExtremeDatesRequest)(implicit request: RequestHeader): Future[ExtremeDatesResponse] = {
    httpClient.post(url"$extremeDatesUrl")
      .withBody(Json.toJson(extremeDatesRequest))
      .execute[ExtremeDatesResponse]
  }
}

final case class DatesApiConfig(baseUrl: String) {
  @Inject()
  def this(config: ServicesConfig) = {
    this(
      baseUrl = config.baseUrl("essttp-backend")
    )
  }
}
