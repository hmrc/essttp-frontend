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

import cats.syntax.either._
import com.google.inject.{Inject, Singleton}
import essttp.crypto.CryptoFormat.OperationalCryptoFormat
import essttp.journey.model.{Journey, JourneyId}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.dates.extremedates.{ExtremeDatesRequest, ExtremeDatesResponse}
import essttp.rootmodel.dates.startdates.{StartDatesRequest, StartDatesResponse}
import essttp.rootmodel.pega.{GetCaseResponse, StartCaseResponse}
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.libs.ws.writeableOf_JsValue
import requests.RequestSupport.hc
import uk.gov.hmrc.http.HttpReadsInstances._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EssttpBackendConnector @Inject() (
  config:     EssttpBackendConfig,
  httpClient: HttpClientV2
)(using ExecutionContext, OperationalCryptoFormat) {

  private val startDatesUrl: String = config.baseUrl + "/essttp-backend/start-dates"

  private val extremeDatesUrl: String = config.baseUrl + "/essttp-backend/extreme-dates"

  private val pegaCaseUrl: String = config.baseUrl + "/essttp-backend/pega/case"

  private val saveJourneyForPegaUrl: String = config.baseUrl + "/essttp-backend/pega/journey"

  private val recreatedSessionUrl: String = config.baseUrl + "/essttp-backend/pega/recreate-session"

  def startDates(startDatesRequest: StartDatesRequest)(using RequestHeader): Future[StartDatesResponse] =
    httpClient
      .post(url"$startDatesUrl")
      .withBody(Json.toJson(startDatesRequest))
      .execute[StartDatesResponse]

  def extremeDates(
    extremeDatesRequest: ExtremeDatesRequest
  )(using RequestHeader): Future[ExtremeDatesResponse] =
    httpClient
      .post(url"$extremeDatesUrl")
      .withBody(Json.toJson(extremeDatesRequest))
      .execute[ExtremeDatesResponse]

  def startPegaCase(journeyId: JourneyId, recalculationNeeded: Boolean)(using
    RequestHeader
  ): Future[StartCaseResponse] = {
    val url = s"$pegaCaseUrl/${journeyId.value}?recalculationNeeded=${recalculationNeeded.toString}"
    httpClient
      .post(url"$url")
      .execute[StartCaseResponse]
  }

  def getPegaCase(journeyId: JourneyId)(using RequestHeader): Future[GetCaseResponse] =
    httpClient
      .get(url"$pegaCaseUrl/${journeyId.value}")
      .execute[GetCaseResponse]

  def saveJourneyForPega(journeyId: JourneyId)(using RequestHeader): Future[Unit] =
    httpClient
      .post(url"$saveJourneyForPegaUrl/${journeyId.value}")
      .execute[Either[UpstreamErrorResponse, Unit]]
      .map(_.leftMap(throw _).merge)

  def recreateSession(taxRegime: TaxRegime)(using RequestHeader): Future[Option[Journey]] =
    httpClient
      .get(url"$recreatedSessionUrl/${TaxRegime.pathBindable.unbind("taxRegime", taxRegime)}")
      .execute[Option[Journey]]

}

final case class EssttpBackendConfig(baseUrl: String) {
  @Inject()
  def this(config: ServicesConfig) =
    this(
      baseUrl = config.baseUrl("essttp-backend")
    )
}
