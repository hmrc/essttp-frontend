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

package configs

import config.AppConfig
import essttp.rootmodel.AmountInPence
import testsupport.ItSpec

class ConfigSpec extends ItSpec {

  //use default config from application.conf
  override lazy val configMap: Map[String, Any] = Map()

  "Config should all be loaded in correctly" - {
    val config: AppConfig = app.injector.instanceOf[AppConfig]
    val configsToTest = List(
      ("appName", config.appName, "essttp-frontend"),
      ("welshLanguageSupportEnabled", config.welshLanguageSupportEnabled, true),
      ("authTimeoutSeconds", config.authTimeoutSeconds, 900),
      ("authTimeoutCountdownSeconds", config.authTimeoutCountdownSeconds, 120),

      ("baseUrl.essttpFrontend", config.BaseUrl.essttpFrontend, "http://localhost:9215"),
      ("baseUrl.essttpFrontendHost", config.BaseUrl.essttpFrontendHost, "localhost"),
      ("baseUrl.contact-frontend", config.BaseUrl.contactFrontend, "http://localhost:9250"),
      ("baseUrl.feedback-frontend", config.BaseUrl.feedbackFrontend, "http://localhost:9514"),
      ("baseUrl.gg", config.BaseUrl.gg, "http://localhost:9949/auth-login-stub/gg-sign-in"),
      ("baseUrl.business-tax-account-frontend", config.BaseUrl.businessTaxAccountFrontend, "http://localhost:9020"),
      ("baseUrl.timeToPayUrl", config.BaseUrl.timeToPayUrl, "http://localhost:9218"),
      ("baseUrl.timeToPayEligibilityUrl", config.BaseUrl.timeToPayEligibilityUrl, "http://localhost:9218"),

      ("Urls.loginUrl", config.Urls.loginUrl, "http://localhost:9949/auth-login-stub/gg-sign-in"),
      ("Urls.signOutUrl", config.Urls.signOutUrl, "http://localhost:9949/auth-login-stub/session/logout"),
      ("Urls.firstPageBackUrl", config.Urls.govUkUrl, "https://www.gov.uk"),
      ("Urls.enrolForPayeUrl", config.Urls.enrolForPayeUrl, "https://www.gov.uk/paye-online/enrol"),
      ("Urls.extraSupportUrl", config.Urls.extraSupportUrl, "https://www.gov.uk/get-help-hmrc-extra-support"),
      ("Urls.relayUrl", config.Urls.relayUrl, "https://www.relayuk.bt.com/"),

      ("InterestRates.baseRate", config.InterestRates.baseRate, 1.0),
      ("InterestRates.hmrcRate", config.InterestRates.hmrcRate, 2.5),

      ("JourneyVariables.minimumUpfrontPaymentAmountInPence", config.JourneyVariables.minimumUpfrontPaymentAmountInPence, AmountInPence(100L)),
      ("Ttp.headers.correlationId", config.TtpHeaders.correlationId, "CorrelationId"),

      ("ExitSurvey.payeExitSurveyUrl", config.ExitSurvey.payeExitSurveyUrl, "http://localhost:9514/feedback/eSSTTP-PAYE")

    )
    configsToTest.foreach { (configData: (String, Any, Any)) =>
      val (configTestName, readInConfigValue, expectedValue) = (configData._1, configData._2, configData._3)
      s"Config: [$configTestName]" in {
        readInConfigValue shouldBe expectedValue withClue s"For config: $configTestName"
      }
    }
  }

}
