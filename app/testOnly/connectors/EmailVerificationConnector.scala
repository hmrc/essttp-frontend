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

package testOnly.connectors

import cats.syntax.eq._
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import play.api.http.Status.NOT_FOUND
import testOnly.models.EmailVerificationPasscodes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  private val getPasscodesUrl: String = appConfig.BaseUrl.emailVerificationUrl + "/test-only/passcodes"

  def requestEmailVerification()(implicit hc: HeaderCarrier): Future[EmailVerificationPasscodes] =
    httpClient.GET[EmailVerificationPasscodes](getPasscodesUrl)
      .recover{
        case e: UpstreamErrorResponse if e.statusCode === NOT_FOUND => EmailVerificationPasscodes(List.empty)
      }

}
