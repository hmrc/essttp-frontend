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

import cats.data.EitherT
import config.AppConfig
import play.api.libs.json.{ Format, Json }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, UpstreamErrorResponse }
import SsttpConnector._
import uk.gov.hmrc.http.HttpReads.Implicits._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class SsttpConnector @Inject() (client: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) {

  def handleUpstreamError(error: UpstreamErrorResponse): SsttpConnector.Error = error match {
    case UpstreamErrorResponse(msg, code, _, _) => StubResponseError(msg, code): SsttpConnector.Error
  }

  def startJourneyEpayeFromBta(implicit ec: ExecutionContext): CR[String] = {
    val body: String = ???
    // val response = client.POST[String, Either[UpstreamErrorResponse, Unit]](url = "the uurl", body = "")

    // EitherT(response).leftMap(handleUpstreamError)
    ???
  }

  def startJourneyEpayeFromGovUk(implicit ec: ExecutionContext): CR[String] = ???

  def startJourneyEpayeFromDetachedUrl(implicit hc: HeaderCarrier, ec: ExecutionContext): CR[String] = ???

  def makeEligibility(eligibility: Eligibility)(implicit hc: HeaderCarrier, ec: ExecutionContext): CR[Unit] = {
    val response = client.POST[Eligibility, Either[UpstreamErrorResponse, Unit]](
      url = "the uurl",
      body = eligibility)

    EitherT(response).leftMap(handleUpstreamError)
  }

}

object SsttpConnector {

  type CR[A] = EitherT[Future, SsttpConnector.Error, A]

  sealed trait Error

  case class StubResponseError(msg: String, code: Int) extends Error

  case class Eligibility(id: String, desc: String)

  object Eligibility {
    val Empty = Eligibility("emptyId", "this eligibility is empty")
    implicit val fmt: Format[Eligibility] = Json.format[Eligibility]
  }

}
