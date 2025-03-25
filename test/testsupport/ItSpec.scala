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

package testsupport

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.crypto.CryptoFormat.OperationalCryptoFormat
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.{DefaultTestServerFactory, FakeRequest, RunningServer}
import play.api.{Application, Mode}
import play.core.server.ServerConfig
import testsupport.TdRequest._
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.crypto.{AesCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.{HttpReadsInstances, SessionKeys}

import java.time.Instant

class ItSpec extends UnitSpec, GuiceOneServerPerSuite, WireMockSupport, HttpReadsInstances {

  val testPort: Int    = 19001
  val baseUrl: BaseUrl = BaseUrl(s"http://localhost:${testPort.toString}")

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(2000, Millis)), interval = scaled(Span(2, Seconds)))

  protected lazy val configOverrides: Map[String, Any] = Map()

  given testCrypto: (Encrypter & Decrypter) = new AesCrypto {
    override protected val encryptionKey: String = "P5xsJ9Nt+quxGZzB4DeLfw=="
  }

  val testOperationCryptoFormat: OperationalCryptoFormat = OperationalCryptoFormat(testCrypto)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  protected lazy val configMap: Map[String, Any] = Map[String, Any](
    "microservice.services.auth.port"                        -> WireMockSupport.port,
    "microservice.services.essttp-backend.port"              -> WireMockSupport.port,
    "microservice.services.time-to-pay.port"                 -> WireMockSupport.port,
    "microservice.services.time-to-pay-eligibility.port"     -> WireMockSupport.port,
    "microservice.services.essttp-dates.port"                -> WireMockSupport.port,
    "microservice.services.bank-account-reputation.port"     -> WireMockSupport.port,
    "microservice.services.email-verification.port"          -> WireMockSupport.port,
    "microservice.services.payments-email-verification.port" -> WireMockSupport.port,
    "auditing.consumer.baseUri.port"                         -> WireMockSupport.port,
    "journeyVariables.minimumUpfrontPaymentAmountInPence"    -> 100L,
    "auditing.enabled"                                       -> true,
    "auditing.traceRequests"                                 -> false
  ) ++ configOverrides

  lazy val modules: List[GuiceableModule] =
    List(
      bind[OperationalCryptoFormat].toInstance(testOperationCryptoFormat)
    )

  // in tests use `app`
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(modules*)
    .disable(classOf[essttp.module.CryptoModule])
    .configure(configMap)
    .build()

  val frozenDateString: String = "2019-11-25"
  val frozenTimeString: String = s"${frozenDateString}T16:33:51.880"

  override def beforeEach(): Unit = {
    super.beforeEach()
    ()
  }

  // defaults are suitable for most tests
  def stubCommonActions(
    authAllEnrolments: Option[Set[Enrolment]] = Some(Set(TdAll.payeEnrolment)),
    authCredentials:   Option[Credentials] = Some(Credentials("authId-999", "GovernmentGateway")),
    barsLockoutExpiry: Option[Instant] = None,
    authNino:          Option[String] = None
  ): StubMapping = {
    // stub Authenticated action
    AuthStub.authorise(authAllEnrolments, authCredentials, authNino)
    // stub Bars lockout filter
    barsLockoutExpiry.fold(EssttpBackend.BarsVerifyStatusStub.statusUnlocked()) { expiry =>
      EssttpBackend.BarsVerifyStatusStub.statusLocked(expiry)
    }
  }

  override implicit protected lazy val runningServer: RunningServer =
    TestServerFactory.start(app)

  object TestServerFactory extends DefaultTestServerFactory {
    override protected def serverConfig(app: Application): ServerConfig = {
      val sc = ServerConfig(port = Some(testPort), sslPort = None, mode = Mode.Test, rootDir = app.path)
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))
    }
  }

  val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

  def fakeRequestWithPath(path: String) =
    FakeRequest("GET", path).withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

}
