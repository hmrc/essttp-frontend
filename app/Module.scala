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

import com.google.inject.{AbstractModule, Provides, Singleton}
import config.AppConfig
import connectors.{AuthLoginStubConnector, AuthLoginStubConnectorImpl}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.WSClient
import play.api.mvc.{Session, SessionCookieBaker}
import services.{AuthLoginStubService, AuthLoginStubServiceImpl}
import services.AuthLoginStubService.LSR
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

import java.time.{Clock, ZoneOffset}
import scala.concurrent.ExecutionContext

class Module extends AbstractModule {

  override def configure(): Unit = {
    ()
  }

  @Provides
  @Singleton
  def authLoginStubConnector(config: AppConfig, ws: WSClient, ec: ExecutionContext): AuthLoginStubConnector = new AuthLoginStubConnectorImpl(config, ws)(ec)

  @Provides
  @Singleton
  def authLoginStubService(
      connector:           AuthLoginStubConnector,
      sessionCookieCrypto: SessionCookieCrypto,
      sessionCookieBaker:  SessionCookieBaker,
      ec:                  ExecutionContext
  ): AuthLoginStubService =
    new AuthLoginStubServiceImpl(connector, sessionCookieCrypto, sessionCookieBaker)(ec)

  @Provides
  @Singleton
  def clock(): Clock = Clock.systemDefaultZone.withZone(ZoneOffset.UTC)

  @Provides
  @Singleton
  def authorisedFunctions(ac: AuthConnector): AuthorisedFunctions = new AuthorisedFunctions {
    override def authConnector: AuthConnector = ac
  }

  @Provides
  @Singleton
  def i18nSupport(api: MessagesApi): I18nSupport = new I18nSupport {
    override def messagesApi: MessagesApi = api
  }
}
