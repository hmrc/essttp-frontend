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

package services

import actionsmodel.EligibleJourneyRequest
import cats.syntax.eq._
import cats.instances.int._
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import connectors.EmailVerificationConnector
import controllers.routes
import essttp.emailverification.EmailVerificationStatus
import essttp.rootmodel.Email
import messages.Messages
import models.emailverification.RequestEmailVerificationRequest.EmailDetails
import models.emailverification.{RequestEmailVerificationRequest, RequestEmailVerificationResponse}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, UNAUTHORIZED}
import play.api.libs.json.{Json, Reads}
import requests.RequestSupport
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.HttpResponseUtils.HttpResponseOps

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (
    connector:             EmailVerificationConnector,
    appConfig:             AppConfig,
    contactFrontendConfig: ContactFrontendConfig,
    requestSupport:        RequestSupport
)(implicit ec: ExecutionContext) {

  import requestSupport._

  private implicit val requestEmailVerificationResponseSuccessReads: Reads[RequestEmailVerificationResponse.Success] = Json.reads

  private val isLocal: Boolean = appConfig.BaseUrl.platformHost.isEmpty

  def requestEmailVerification(emailAddress: Email)(implicit r: EligibleJourneyRequest[_], hc: HeaderCarrier): Future[RequestEmailVerificationResponse] =
    connector.requestEmailVerification(emailVerificationRequest(emailAddress)).map{ response =>
      if (response.status === CREATED) {
        response.parseJSON[RequestEmailVerificationResponse.Success]
          .fold(
            msg => throw UpstreamErrorResponse(msg, INTERNAL_SERVER_ERROR),
            success =>
              if (isLocal && !URI.create(success.redirectUri).isAbsolute) {
                RequestEmailVerificationResponse.Success(s"${appConfig.BaseUrl.emailVerificationFrontendUrl}${success.redirectUri}")
              } else {
                success
              }
          )
      } else {
        throw UpstreamErrorResponse(s"Call to request email verification came back with unexpected status ${response.status}", response.status)
      }
    }.recover {
      case u: UpstreamErrorResponse if u.statusCode === UNAUTHORIZED => RequestEmailVerificationResponse.LockedOut
    }

  def getVerificationStatus(emailAddress: Email)(implicit r: EligibleJourneyRequest[_], hc: HeaderCarrier): Future[EmailVerificationStatus] =
    connector.getVerificationStatus(r.ggCredId).map{ statusResponse =>
      statusResponse.emails.find(_.emailAddress === emailAddress.value.decryptedValue) match {
        case None =>
          throw UpstreamErrorResponse("Verification status not found for email address", NOT_FOUND)

        case Some(status) =>
          (status.verified, status.locked) match {
            case (true, false) =>
              EmailVerificationStatus.Verified
            case (false, true) =>
              EmailVerificationStatus.Locked
            case _ =>
              throw UpstreamErrorResponse(s"Got unexpected combination of verified=${status.verified} and " +
                s"locked=${status.locked} in email verification status response", INTERNAL_SERVER_ERROR)
          }
      }

    }

  private def emailVerificationRequest(emailAddress: Email)(implicit r: EligibleJourneyRequest[_]): RequestEmailVerificationRequest = {
    val lang = language(r.request)

    RequestEmailVerificationRequest(
      r.ggCredId,
      RequestEmailVerification.continueUrl,
      RequestEmailVerification.origin,
      RequestEmailVerification.deskproServiceName,
      RequestEmailVerification.accessibilityStatementUrl,
      Messages.ServicePhase.serviceName(r.journey.taxRegime).show(language),
      RequestEmailVerification.whichEmailUrl,
      EmailDetails(emailAddress, RequestEmailVerification.whichEmailUrl),
      lang
    )
  }

  object RequestEmailVerification {
    private def url(s: String): String = if (isLocal) s"${appConfig.BaseUrl.essttpFrontend}$s" else s

    val continueUrl: String = url(routes.EmailController.emailCallback.url)
    val origin: String = appConfig.appName
    val deskproServiceName: String = contactFrontendConfig.serviceId.getOrElse(sys.error("Could not find contact frontend serviceId"))
    val accessibilityStatementUrl: String = {
      val u = s"/accessibility-statement${appConfig.accessibilityStatementPath}"
      if (isLocal) s"${appConfig.BaseUrl.accessibilityStatementFrontendUrl}$u" else u
    }
    val whichEmailUrl: String = url(routes.EmailController.whichEmailDoYouWantToUse.url)
  }

}
