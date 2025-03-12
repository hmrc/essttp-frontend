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
import controllers.routes
import essttp.rootmodel.Email
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import messages.Messages
import paymentsEmailVerification.connectors.PaymentsEmailVerificationConnector
import paymentsEmailVerification.models.EmailVerificationResult
import paymentsEmailVerification.models.api.{GetEarliestCreatedAtTimeResponse, GetEmailVerificationResultRequest, StartEmailVerificationJourneyRequest, StartEmailVerificationJourneyResponse}
import requests.RequestSupport
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (
  paymentsEmailVerificationConnector: PaymentsEmailVerificationConnector,
  appConfig:                          AppConfig,
  contactFrontendConfig:              ContactFrontendConfig,
  requestSupport:                     RequestSupport,
  auditService:                       AuditService
)(using ExecutionContext) {

  import requestSupport.languageFromRequest

  private val isLocal: Boolean = appConfig.BaseUrl.platformHost.isEmpty

  def requestEmailVerification(emailAddress: Email)(using
    r:  EligibleJourneyRequest[?],
    hc: HeaderCarrier
  ): Future[StartEmailVerificationJourneyResponse] =
    for {
      startResponse <- paymentsEmailVerificationConnector.startEmailVerification(emailVerificationRequest(emailAddress))
      _              = auditService.auditEmailVerificationRequested(
                         journey = r.journey,
                         ggCredId = r.ggCredId,
                         email = emailAddress,
                         result = auditResultFromStartEmailVerificationJourneyResponse(startResponse)
                       )
    } yield startResponse

  def getEmailVerificationResult(emailAddress: Email)(using
    r:  EligibleJourneyRequest[?],
    hc: HeaderCarrier
  ): Future[EmailVerificationResult] =
    for {
      verificationResult <-
        paymentsEmailVerificationConnector.getEmailVerificationResult(
          GetEmailVerificationResultRequest(paymentsEmailVerification.models.Email(emailAddress.value.decryptedValue))
        )
      _                   = auditService.auditEmailVerificationResult(
                              journey = r.journey,
                              ggCredId = r.ggCredId,
                              email = emailAddress,
                              verificationResult
                            )
    } yield verificationResult

  def getLockoutCreatedAt()(using HeaderCarrier): Future[GetEarliestCreatedAtTimeResponse] =
    paymentsEmailVerificationConnector.getEarliestCreatedAtTime()

  private def emailVerificationRequest(
    emailAddress: Email
  )(using r: EligibleJourneyRequest[?]): StartEmailVerificationJourneyRequest = {
    val lang = languageFromRequest(using r.request)

    StartEmailVerificationJourneyRequest(
      continueUrl = RequestEmailVerification.continueUrl,
      origin = RequestEmailVerification.origin,
      deskproServiceName = RequestEmailVerification.deskproServiceName,
      accessibilityStatementUrl = RequestEmailVerification.accessibilityStatementUrl,
      pageTitle = Messages.ServicePhase.serviceName(r.journey.taxRegime).show(using languageFromRequest),
      backUrl = RequestEmailVerification.emailEntryUrl(r.eligibilityCheckResult),
      enterEmailUrl = RequestEmailVerification.emailEntryUrl(r.eligibilityCheckResult),
      email = paymentsEmailVerification.models.Email(emailAddress.value.decryptedValue),
      lang = lang.code
    )
  }

  object RequestEmailVerification {
    private def url(s: String): String = if (isLocal) s"${appConfig.BaseUrl.essttpFrontend}$s" else s

    val continueUrl: String               = url(routes.EmailController.emailCallback.url)
    val origin: String                    = appConfig.appName
    val deskproServiceName: String        =
      contactFrontendConfig.serviceId.getOrElse(sys.error("Could not find contact frontend serviceId"))
    val accessibilityStatementUrl: String = {
      val u = s"/accessibility-statement${appConfig.accessibilityStatementPath}"
      if (isLocal) s"${appConfig.BaseUrl.accessibilityStatementFrontendUrl}$u" else u
    }

    def emailEntryUrl(eligibilityCheckResult: EligibilityCheckResult): String =
      url(
        eligibilityCheckResult.email.fold(routes.EmailController.enterEmail.url)(_ =>
          routes.EmailController.whichEmailDoYouWantToUse.url
        )
      )
  }

  private def auditResultFromStartEmailVerificationJourneyResponse(
    startEmailVerificationJourneyResponse: StartEmailVerificationJourneyResponse
  ): String =
    startEmailVerificationJourneyResponse match {
      case StartEmailVerificationJourneyResponse.Success(_)    => "Started"
      case StartEmailVerificationJourneyResponse.Error(reason) => reason.entryName
    }

}
