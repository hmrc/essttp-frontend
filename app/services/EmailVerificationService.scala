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

package services

import actionsmodel.EligibleJourneyRequest
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import connectors.EmailVerificationConnector
import controllers.routes
import essttp.emailverification._
import essttp.rootmodel.Email
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import messages.Messages
import requests.RequestSupport
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class EmailVerificationService @Inject() (
    connector:             EmailVerificationConnector,
    appConfig:             AppConfig,
    contactFrontendConfig: ContactFrontendConfig,
    requestSupport:        RequestSupport
) {

  import requestSupport._

  private val isLocal: Boolean = appConfig.BaseUrl.platformHost.isEmpty

  def requestEmailVerification(emailAddress: Email)(implicit r: EligibleJourneyRequest[_], hc: HeaderCarrier): Future[StartEmailVerificationJourneyResponse] =
    connector.startEmailVerificationJourney(emailVerificationRequest(emailAddress))

  def getEmailVerificationResult(emailAddress: Email)(implicit r: EligibleJourneyRequest[_], hc: HeaderCarrier): Future[EmailVerificationResult] =
    connector.getEmailVerificationResult(GetEmailVerificationResultRequest(r.ggCredId, emailAddress)) //todo jake return result instead of state

  private def emailVerificationRequest(emailAddress: Email)(implicit r: EligibleJourneyRequest[_]): StartEmailVerificationJourneyRequest = {
    val lang = language(r.request)

    StartEmailVerificationJourneyRequest(
      r.ggCredId,
      RequestEmailVerification.continueUrl,
      RequestEmailVerification.origin,
      RequestEmailVerification.deskproServiceName,
      RequestEmailVerification.accessibilityStatementUrl,
      Messages.ServicePhase.serviceName(r.journey.taxRegime).show(language),
      RequestEmailVerification.emailEntryUrl(r.eligibilityCheckResult),
      RequestEmailVerification.emailEntryUrl(r.eligibilityCheckResult),
      emailAddress,
      lang.code,
      isLocal

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
    def emailEntryUrl(eligibilityCheckResult: EligibilityCheckResult): String =
      url(
        eligibilityCheckResult.email.fold(
          routes.EmailController.enterEmail.url
        )(_ =>
            routes.EmailController.whichEmailDoYouWantToUse.url)
      )
  }

}
