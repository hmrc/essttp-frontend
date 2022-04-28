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
import com.google.inject.Inject
import config.AppConfig
import connectors.AuthLoginStubConnector.{ACR, StubException, wrapException, wrapResponse}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSResponse}
import services.AuthLoginStubService.LoginData
import uk.gov.hmrc.http.HeaderCarrier
import util.StringUtils.StringOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait AuthLoginStubConnector {

  def login(loginData: LoginData)(implicit hc: HeaderCarrier): ACR[WSResponse]

}

class AuthLoginStubConnectorImpl @Inject() (config: AppConfig, ws: WSClient)(implicit ec: ExecutionContext) extends AuthLoginStubConnector {

  private val authLoginUrl = config.BaseUrl.gg

  private def requestFormBody(loginData: LoginData): Map[String, String] = {
    val enrolmentIdentifier = loginData.enrolment.flatMap(_.identifiers.headOption)

    List(
      "authorityId" -> Some(loginData.ggCredId.value),
      "redirectionUrl" -> Some(loginData.redirectUrl),
      "credentialStrength" -> Some("strong"),
      "confidenceLevel" -> Some(loginData.confidenceLevel.level.toString),
      "affinityGroup" -> Some(loginData.affinityGroup.toString),
      "credentialRole" -> Some("User"),
      "email" -> loginData.email.map(_.value),
      "nino" -> loginData.nino.map(_.value),
      "enrolment[0].name" -> loginData.enrolment.map(_.key),
      "enrolment[0].taxIdentifier[0].name" -> enrolmentIdentifier.map(_.key),
      "enrolment[0].taxIdentifier[0].value" -> enrolmentIdentifier.map(_.value),
      "enrolment[0].state" -> loginData.enrolment.map(_.state)
    ).collect { case (k, Some(v)) => k -> v }.toMap
  }

  override def login(loginData: LoginData)(implicit hc: HeaderCarrier): ACR[WSResponse] = {
    val formData =
      requestFormBody(loginData).map { case (k, v) => s"${k.urlEncode}=${v.urlEncode}" }.mkString("&")
    val result: Future[Either[StubException, WSResponse]] = ws.url(authLoginUrl)
      .withFollowRedirects(false)
      .withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(formData)
      .map(wrapResponse(_))
      .recover { case NonFatal(e) => wrapException(e) }

    EitherT(result)
  }

}

object AuthLoginStubConnector {

  def wrapResponse(response: WSResponse): Either[StubException, WSResponse] = Right(response)

  def wrapException(e: Throwable): Either[StubException, WSResponse] = Left(StubException(e))

  type ACR[A] = EitherT[Future, StubException, A]

  case class StubException(e: Throwable)

  private def requestFormBody(loginData: LoginData): Map[String, String] = {
    val enrolmentIdentifier = loginData.enrolment.flatMap(_.identifiers.headOption)

    List(
      "authorityId" -> Some(loginData.ggCredId.value),
      "redirectionUrl" -> Some(loginData.redirectUrl),
      "credentialStrength" -> Some("strong"),
      "confidenceLevel" -> Some(loginData.confidenceLevel.level.toString),
      "affinityGroup" -> Some(loginData.affinityGroup.toString),
      "credentialRole" -> Some("User"),
      "email" -> loginData.email.map(_.value),
      "nino" -> loginData.nino.map(_.value),
      "enrolment[0].name" -> loginData.enrolment.map(_.key),
      "enrolment[0].taxIdentifier[0].name" -> enrolmentIdentifier.map(_.key),
      "enrolment[0].taxIdentifier[0].value" -> enrolmentIdentifier.map(_.value),
      "enrolment[0].state" -> loginData.enrolment.map(_.state)
    ).collect { case (k, Some(v)) => k -> v }.toMap
  }

}
