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
import essttp.crypto.CryptoFormat.OperationalCryptoFormat
import essttp.emailverification._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (
    appConfig:  AppConfig,
    httpClient: HttpClient
)(implicit ec: ExecutionContext, cryptoFormat: OperationalCryptoFormat) {

  private val startEmailVerificationJourneyUrl: String = appConfig.BaseUrl.essttpBackendUrl + "/essttp-backend/email-verification/start"

  private val getVerificationResultUrl: String = appConfig.BaseUrl.essttpBackendUrl + "/essttp-backend/email-verification/result"

  private val getVerificationStateUrl: String = appConfig.BaseUrl.essttpBackendUrl + "/essttp-backend/email-verification/verification-state"

  private val updateVerificationStateUrl: String = appConfig.BaseUrl.essttpBackendUrl + "/essttp-backend/email-verification/verification-state/update"

  private val updateVerificationStateResultUrl: String = appConfig.BaseUrl.essttpBackendUrl + "/essttp-backend/email-verification/verification-state/result-update"

  def startEmailVerificationJourney(
      emailVerificationRequest: StartEmailVerificationJourneyRequest
  )(implicit hc: HeaderCarrier): Future[StartEmailVerificationJourneyResponse] =
    httpClient.POST[StartEmailVerificationJourneyRequest, StartEmailVerificationJourneyResponse](
      startEmailVerificationJourneyUrl, emailVerificationRequest
    )

  def getEmailVerificationResult(request: GetEmailVerificationResultRequest)(implicit hc: HeaderCarrier): Future[EmailVerificationResult] =
    httpClient.POST[GetEmailVerificationResultRequest, EmailVerificationResult](getVerificationResultUrl, request)

  def getEmailVerificationState(request: GetEmailVerificationResultRequest)(implicit hc: HeaderCarrier): Future[EmailVerificationState] =
    httpClient.POST[GetEmailVerificationResultRequest, EmailVerificationState](getVerificationStateUrl, request)

  def updateEmailVerificationState(request: GetEmailVerificationResultRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.POST[GetEmailVerificationResultRequest, Unit](updateVerificationStateUrl, request)

  def updateEmailVerificationStateWithResult(request: EmailVerificationStateResultRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.POST[EmailVerificationStateResultRequest, Unit](updateVerificationStateResultUrl, request)

}
