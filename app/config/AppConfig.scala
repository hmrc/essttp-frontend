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

package config

import essttp.rootmodel.AmountInPence

import javax.inject.{Inject, Singleton}
import play.api.{ConfigLoader, Configuration}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")
  val emailJourneyEnabled: Boolean = config.get[Boolean]("features.email-journey")
  val vatEnabled: Boolean = config.get[Boolean]("features.vat")
  val authTimeoutSeconds: Int = config.get[FiniteDuration]("timeout-dialog.timeout").toSeconds.toInt
  val authTimeoutCountdownSeconds: Int = config.get[FiniteDuration]("timeout-dialog.countdown").toSeconds.toInt
  val accessibilityStatementPath: String = config.get[String]("accessibility-statement.service-path")

  object BaseUrl {
    val platformHost: Option[String] = config.getOptional[String]("platform.frontend.host")
    val essttpBackendUrl: String = servicesConfig.baseUrl("essttp-backend")
    val essttpFrontend: String = platformHost.getOrElse(config.get[String]("baseUrl.essttp-frontend"))
    val essttpFrontendHost: String = new URL(essttpFrontend).getHost
    val contactFrontend: String = platformHost.getOrElse(config.get[String]("baseUrl.contact-frontend"))
    val feedbackFrontend: String = platformHost.getOrElse(config.get[String]("baseUrl.feedback-frontend"))
    val gg: String = config.get[String]("baseUrl.gg")
    val businessTaxAccountFrontend: String = platformHost.getOrElse(config.get[String]("baseUrl.business-tax-account-frontend"))
    val timeToPayUrl: String = servicesConfig.baseUrl("time-to-pay")
    val timeToPayEligibilityUrl: String = servicesConfig.baseUrl("time-to-pay-eligibility")
    val barsUrl: String = servicesConfig.baseUrl("bank-account-reputation")
    val emailVerificationUrl: String = servicesConfig.baseUrl("email-verification")
    val accessibilityStatementFrontendUrl: String = platformHost.getOrElse(config.get[String]("baseUrl.accessibility-statement-frontend"))
  }

  object Urls {
    val loginUrl: String = BaseUrl.gg
    val signOutUrl: String = config.get[String]("baseUrl.sign-out")

    def betaFeedbackUrl(implicit request: RequestHeader): String =
      s"${BaseUrl.contactFrontend}/contact/beta-feedback?" +
        s"service=$appName&" +
        s"backUrl=${SafeRedirectUrl(BaseUrl.essttpFrontend + request.uri).encodedUrl}"

    val govUkUrl: String = config.get[String]("govUkUrls.govUk")
    val enrolForPayeUrl: String = config.get[String]("govUkUrls.enrolPayeUrl")
    val enrolForVatUrl: String = config.get[String]("govUkUrls.enrolVatUrl")
    val extraSupportUrl: String = config.get[String]("govUkUrls.extraSupportUrl")
    val relayUrl: String = config.get[String]("govUkUrls.relayUrl")
    val businessTaxAccountUrl: String = s"${BaseUrl.businessTaxAccountFrontend}/business-account"
  }

  object TtpHeaders {
    val correlationId: String = config.get[String]("ttp.headers.correlationId")
  }

  object ExitSurvey {
    private val baseUrl: String = s"${BaseUrl.feedbackFrontend}/feedback"

    val payeExitSurveyUrl: String = s"$baseUrl/eSSTTP-PAYE"

    val vatExitSurveyUrl: String = s"$baseUrl/eSSTTP-VAT"
  }

  object Crypto {
    val aesGcmCryptoKey: String = config.get[String]("crypto.encryption-key")
  }

  object PolicyParameters {

    val minimumUpfrontPaymentAmountInPence: AmountInPence = AmountInPence(config.get[Long]("policy-parameters.minimumUpfrontPaymentAmountInPence"))

    object InterestRates {
      val baseRate: BigDecimal = config.get[Double]("policy-parameters.interest-rates.base-rate")
      val hmrcRate: BigDecimal = config.get[Double]("policy-parameters.interest-rates.hmrc-additional-rate")
    }

    object EPAYE {

      private def getParam[A: ConfigLoader](path: String): A = config.get[A](s"policy-parameters.epaye.$path")

      val maxAmountOfDebt: AmountInPence = AmountInPence(getParam[Long]("max-amount-of-debt-in-pounds") * 100L)

      val maxPlanDurationInMonths: Int = getParam[Int]("max-plan-duration-in-months")

      val maxAgeOfDebtInDays: Int = getParam[Int]("max-age-of-debt-in-days")
    }

    object VAT {
      private def getParam[A: ConfigLoader](path: String): A = config.get[A](s"policy-parameters.vat.$path")

      val maxAmountOfDebt: AmountInPence = AmountInPence(getParam[Long]("max-amount-of-debt-in-pounds") * 100L)

      val maxPlanDurationInMonths: Int = getParam[Int]("max-plan-duration-in-months")

      val maxAgeOfDebtInDays: Int = getParam[Int]("max-age-of-debt-in-days")
    }
  }

}
