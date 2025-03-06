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

package testOnly

import cats.syntax.eq._
import com.google.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.JsObject
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.Session
import testOnly.models.testusermodel.{AuthToken, TestUser}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Clock
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

/** Test Login Service.
  */
@Singleton
class AuthLoginApiService @Inject() (
  httpClient:     HttpClientV2,
  servicesConfig: ServicesConfig
)(using ExecutionContext) {

  def logIn(testUser: TestUser): Future[Session] = for {
    authToken: AuthToken <- callAuthLoginApi(LoginRequestMaker.makeLoginRequestBody(testUser))
  } yield buildAuthenticatedSession(authToken)

  private def callAuthLoginApi(requestBody: JsObject): Future[AuthToken] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    httpClient
      .post(url"$authLoginApiUrl/government-gateway/session/login")
      .withBody(requestBody)
      .execute[HttpResponse]
      .map(r =>
        if (r.status === 201) {
          AuthToken(
            r
              .header(HeaderNames.AUTHORIZATION)
              .getOrElse(throw new RuntimeException(s"missing 'AUTHORIZATION' header: ${r.toString()}"))
          )
        } else {
          throw UpstreamErrorResponse(s"Got response with status ${r.status.toString} and body ${r.body}", 500)
        }
      )
  }

  private def buildAuthenticatedSession(authToken: AuthToken) =
    Session(
      Map(
        SessionKeys.sessionId            -> s"session-${randomUUID.toString}",
        SessionKeys.authToken            -> authToken.value,
        SessionKeys.lastRequestTimestamp -> realClock.millis().toString
      )
    )

  private val realClock: Clock = Clock.systemUTC()
  private val authLoginApiUrl  = servicesConfig.baseUrl("auth-login-api")
}
