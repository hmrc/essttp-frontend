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

package testsupport

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.google.inject.AbstractModule
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.{DefaultTestServerFactory, RunningServer}
import play.api.{Application, Mode}
import play.core.server.ServerConfig
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HttpReadsInstances

import java.time.Instant

class ItSpec
  extends UnitSpec
  with GuiceOneServerPerSuite
  with WireMockSupport
  with HttpReadsInstances {

  val testPort: Int = 19001
  val baseUrl: BaseUrl = BaseUrl(s"http://localhost:$testPort")

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout  = scaled(Span(300, Millis)), interval = scaled(Span(2, Seconds)))

  protected lazy val configOverrides: Map[String, Any] = Map()

  @SuppressWarnings(Array("org.wartremover.Warts.Any"))
  protected lazy val configMap: Map[String, Any] = Map[String, Any](
    "microservice.services.auth.port" -> WireMockSupport.port,
    "microservice.services.essttp-backend.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-eligibility.port" -> WireMockSupport.port,
    "microservice.services.essttp-dates.port" -> WireMockSupport.port,
    "microservice.services.bank-account-reputation.port" -> WireMockSupport.port,
    "auditing.consumer.baseUri.port" -> WireMockSupport.port,
    "journeyVariables.minimumUpfrontPaymentAmountInPence" -> 100L,
    "auditing.enabled" -> true,
    "auditing.traceRequests" -> false
  ) ++ configOverrides

  //in tests use `app`
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(GuiceableModule.fromGuiceModules(Seq(module)))
    .configure(configMap)
    .build()

  val frozenDateString: String = "2019-11-25"
  val frozenTimeString: String = s"${frozenDateString}T16:33:51.880"

  lazy val module: AbstractModule = new AbstractModule {
    override def configure(): Unit = ()
  }

  implicit lazy val webDriver: HtmlUnitDriver = {
    val wd = new HtmlUnitDriver(true)
    wd.setJavascriptEnabled(false)
    wd
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    webDriver.manage().deleteAllCookies()
  }

  // suitable for most tests
  def stubActionDefaults(): StubMapping = {
    stubCommonActionsWith(
      authAllEnrolments = Some(Set(TdAll.payeEnrolment)),
      authCredentials   = Some(Credentials("authId-999", "GovernmentGateway")),
      barsLockoutExpiry = None
    )
  }
  // custom action setup
  def stubCommonActionsWith(
      authAllEnrolments: Option[Set[Enrolment]] = Some(Set(TdAll.payeEnrolment)),
      authCredentials:   Option[Credentials]    = Some(Credentials("authId-999", "GovernmentGateway")),
      barsLockoutExpiry: Option[Instant]        = None
  ): StubMapping = {
    // stub Authenticated action
    AuthStub.authorise(authAllEnrolments, authCredentials)
    // stub Bars lockout filter
    barsLockoutExpiry.fold(EssttpBackend.BarsVerifyStatusStub.statusUnlocked()) { expiry =>
      EssttpBackend.BarsVerifyStatusStub.statusLocked(expiry)
    }
  }

  override implicit protected lazy val runningServer: RunningServer =
    TestServerFactory.start(app)

  object TestServerFactory extends DefaultTestServerFactory {
    override protected def serverConfig(app: Application): ServerConfig = {
      val sc = ServerConfig(port    = Some(testPort), sslPort = Some(0), mode = Mode.Test, rootDir = app.path)
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))
    }
  }

}
