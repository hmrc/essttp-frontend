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
import models.GGCredId
import models.emailverification.{EmailVerificationStatusResponse, RequestEmailVerificationRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  private val requestVerificationUrl: String = appConfig.BaseUrl.emailVerificationUrl + "/email-verification/verify-email"

  private def getVerificationStatusUrl(ggCredId: GGCredId): String =
    appConfig.BaseUrl.emailVerificationUrl + s"/email-verification/verification-status/${ggCredId.value}"

  def requestEmailVerification(emailVerificationRequest: RequestEmailVerificationRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.POST[RequestEmailVerificationRequest, HttpResponse](requestVerificationUrl, emailVerificationRequest)

  def getVerificationStatus(ggCredId: GGCredId)(implicit hc: HeaderCarrier): Future[EmailVerificationStatusResponse] =
    httpClient.GET[EmailVerificationStatusResponse](getVerificationStatusUrl(ggCredId))

}
