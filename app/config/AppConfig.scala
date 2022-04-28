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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")
  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)
  val authTimeoutSeconds: Int = config.get[FiniteDuration]("timeout-dialog.timeout").toSeconds.toInt
  val authTimeoutCountdownSeconds: Int = config.get[FiniteDuration]("timeout-dialog.countdown").toSeconds.toInt
  val mongoTimeToLiveInSeconds: Int = config.get[Int]("mongodb.timeToLiveInSeconds")

  val ttpBaseUrl: String = s"${servicesConfig.baseUrl("ttp")}"

  object BaseUrl {
    val essttpFrontend: String = config.get[String]("baseUrl.essttp-frontend")
    val essttpFrontendHost: String = new URL(essttpFrontend).getHost
    val contactFrontend: String = config.get[String]("baseUrl.contact-frontend")
    val feedbackFrontend: String = config.get[String]("baseUrl.feedback-frontend")
    val caFrontend: String = config.get[String]("baseUrl.ca-frontend")
    val gg: String = config.get[String]("baseUrl.gg")
    val businessTaxAccountFrontend: String = config.get[String]("baseUrl.business-tax-account-frontend")
  }

  object Urls {
    val loginUrl: String = BaseUrl.gg
    val signOutUrl: String = config.get[String]("baseUrl.sign-out")

    def betaFeedbackUrl(implicit request: RequestHeader): String =
      s"${BaseUrl.contactFrontend}/contact/beta-feedback?" +
        s"service=$appName&" +
        s"backUrl=${SafeRedirectUrl(BaseUrl.essttpFrontend + request.uri).encodedUrl}"

    val exitSurveyUrl: String = s"${BaseUrl.feedbackFrontend}/feedback/$appName"
    val firstPageBackUrl: String = "https://gov.uk"
    val enrolForPayeUrl: String = config.get[String]("govUkUrls.enrolPayeUrl")
    val extraSupportUrl: String = config.get[String]("govUkUrls.extraSupportUrl")
    val relayUrl: String = config.get[String]("govUkUrls.relayUrl")
  }

  object InterestRates {
    val baseRate: BigDecimal = 0.25
    val hmrcRate: BigDecimal = 2.5
  }
}
