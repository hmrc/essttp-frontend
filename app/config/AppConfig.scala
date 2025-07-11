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

import essttp.rootmodel.{AmountInPence, TaxRegime}
import models.{EligibilityReqIdentificationFlag, Language}
import play.api.mvc.RequestHeader
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String                                                      = config.get[String]("appName")
  val emailJourneyEnabled: Boolean                                         = config.get[Boolean]("features.email-journey")
  val saEnabled: Boolean                                                   = config.get[Boolean]("features.sa")
  val simpEnabled: Boolean                                                 = config.get[Boolean]("features.simp")
  val authTimeoutSeconds: Int                                              = config.get[FiniteDuration]("timeout-dialog.timeout").toSeconds.toInt
  val authTimeoutCountdownSeconds: Int                                     = config.get[FiniteDuration]("timeout-dialog.countdown").toSeconds.toInt
  val accessibilityStatementPath: String                                   = config.get[String]("accessibility-statement.service-path")
  val shutteredTaxRegimes: List[TaxRegime]                                 =
    config.get[Seq[String]]("shuttering.shuttered-tax-regimes").map(TaxRegime.withNameInsensitive).toList
  given eligibilityReqIdentificationFlag: EligibilityReqIdentificationFlag = EligibilityReqIdentificationFlag(
    config.get[Boolean]("features.eligibilityReqIdentificationFlag")
  )
  val userResearchBannerEnabled: Boolean                                   = config.get[Boolean]("features.user-research-banner-enabled")

  object BaseUrl {
    val platformHost: Option[String]              = config.getOptional[String]("platform.frontend.host")
    val essttpBackendUrl: String                  = servicesConfig.baseUrl("essttp-backend")
    val essttpFrontend: String                    = platformHost.getOrElse(config.get[String]("baseUrl.essttp-frontend"))
    val essttpFrontendHost: String                = new URI(essttpFrontend).toURL.getHost
    val contactFrontend: String                   = platformHost.getOrElse(config.get[String]("baseUrl.contact-frontend"))
    val feedbackFrontend: String                  = platformHost.getOrElse(config.get[String]("baseUrl.feedback-frontend"))
    val gg: String                                = config.get[String]("baseUrl.gg")
    val businessTaxAccountFrontend: String        =
      platformHost.getOrElse(config.get[String]("baseUrl.business-tax-account-frontend"))
    val personalTaxAccountFrontend: String        = platformHost.getOrElse(config.get[String]("baseUrl.pertax-frontend"))
    val timeToPayUrl: String                      = servicesConfig.baseUrl("time-to-pay")
    val timeToPayEligibilityUrl: String           = servicesConfig.baseUrl("time-to-pay-eligibility")
    val barsUrl: String                           = servicesConfig.baseUrl("bank-account-reputation")
    val emailVerificationUrl: String              = servicesConfig.baseUrl("email-verification")
    val accessibilityStatementFrontendUrl: String =
      platformHost.getOrElse(config.get[String]("baseUrl.accessibility-statement-frontend"))
    val paymentsEmailVerificationUrl: String      = servicesConfig.baseUrl("payments-email-verification")
  }

  object Urls {
    val loginUrl: String = BaseUrl.gg

    def betaFeedbackUrl(using request: RequestHeader): String = {
      import uk.gov.hmrc.http.StringContextOps
      s"${BaseUrl.contactFrontend}/contact/beta-feedback?" +
        s"service=$appName&" +
        s"backUrl=${url"${BaseUrl.essttpFrontend + request.uri}".toString}"
    }

    val govUkUrl: String                               = config.get[String]("govUkUrls.govUk")
    val enrolForPayeUrl: String                        = config.get[String]("govUkUrls.enrolPayeUrl")
    val enrolForVatUrl: String                         = config.get[String]("govUkUrls.enrolVatUrl")
    val enrolForSaUrl: String                          = config.get[String]("govUkUrls.enrolSaUrl")
    val signUpForMtdUrl: String                        = config.get[String]("govUkUrls.signUpMtdUrl")
    val extraSupportUrl: String                        = config.get[String]("govUkUrls.extraSupportUrl")
    val relayUrl: String                               = config.get[String]("govUkUrls.relayUrl")
    val businessTaxAccountUrl: String                  = s"${BaseUrl.businessTaxAccountFrontend}/business-account"
    val personalTaxAccountUrl: String                  = s"${BaseUrl.personalTaxAccountFrontend}/personal-account"
    val businessPaymentSupportService: String          = config.get[String]("govUkUrls.businessPaymentSupportService")
    val welshLanguageHelplineForDebtManagement: String =
      config.get[String]("govUkUrls.welshLanguageHelplineForDebtManagement")
    val saSuppUrl: String                              = {
      val baseUrl = BaseUrl.platformHost.getOrElse("http://localhost:9063")
      s"$baseUrl/pay-what-you-owe-in-instalments"
    }
    val fileSaReturnUrl: String                        = config.get[String]("govUkUrls.fileSaReturnUrl")
    val tellHMRCChangeDetailsUrl: String               = config.get[String]("govUkUrls.changeDetails")
    val userResearchBannerLink: String                 = config.get[String]("govUkUrls.userResearchBannerLink")
    val signOutUrl: String                             = {
      val basGatewayBaseUrl = BaseUrl.platformHost.getOrElse(config.get[String]("baseUrl.bas-gateway-frontend"))
      s"$basGatewayBaseUrl/bas-gateway/sign-out-without-state"
    }
  }

  object TtpHeaders {
    val correlationId: String = config.get[String]("ttp.headers.correlationId")
  }

  object ExitSurvey {
    private val baseUrl: String = s"${BaseUrl.feedbackFrontend}/feedback"

    val payeExitSurveyUrl: String = s"$baseUrl/eSSTTP-PAYE"

    val vatExitSurveyUrl: String = s"$baseUrl/eSSTTP-VAT"

    val saExitSurveyUrl: String = s"$baseUrl/eSSTTP-SA"

    val simpExitSurveyUrl: String = s"$baseUrl/eSSTTP-SIMP"
  }

  object Crypto {
    val aesGcmCryptoKey: String = config.get[String]("crypto.encryption-key")
  }

  object PolicyParameters {

    val minimumUpfrontPaymentAmountInPence: AmountInPence = AmountInPence(
      config.get[Long]("policy-parameters.minimumUpfrontPaymentAmountInPence")
    )

    object InterestRates {
      val baseRate: BigDecimal = config.get[Double]("policy-parameters.interest-rates.base-rate")
      val hmrcRate: BigDecimal = config.get[Double]("policy-parameters.interest-rates.hmrc-additional-rate")
    }

    object EPAYE {
      private def getParam[A: ConfigLoader](path: String): A = config.get[A](s"policy-parameters.epaye.$path")

      val maxAmountOfDebt: AmountInPence = AmountInPence(getParam[Long]("max-amount-of-debt-in-pounds") * 100L)
      val maxPlanDurationInMonths: Int   = getParam[Int]("max-plan-duration-in-months")
      val maxAgeOfDebtInYears: Int       = getParam[Int]("max-age-of-debt-in-years")
      val payOnlineLink: String          = getParam[String]("pay-online-link")
    }

    object VAT {
      private def getParam[A: ConfigLoader](path: String): A = config.get[A](s"policy-parameters.vat.$path")

      val maxAmountOfDebt: AmountInPence      = AmountInPence(getParam[Long]("max-amount-of-debt-in-pounds") * 100L)
      val maxPlanDurationInMonths: Int        = getParam[Int]("max-plan-duration-in-months")
      val maxAgeOfDebtInDays: Int             = getParam[Int]("max-age-of-debt-in-days")
      val payOnlineLink: String               = getParam[String]("pay-online-link")
      val vatAccountingPeriodStart: LocalDate = {
        val string    = getParam[String]("vat-accounting-period-start")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        LocalDate.parse(string, formatter)
      }
    }

    object SA {
      private def getParam[A: ConfigLoader](path: String): A = config.get[A](s"policy-parameters.sa.$path")

      val maxAmountOfDebt: AmountInPence = AmountInPence(getParam[Long]("max-amount-of-debt-in-pounds") * 100L)
      val maxPlanDurationInMonths: Int   = getParam[Int]("max-plan-duration-in-months")
      val maxAgeOfDebtInDays: Int        = getParam[Int]("max-age-of-debt-in-days")
      val payOnlineLink: String          = getParam[String]("pay-online-link")
      val payNowUrl: String              = getParam[String]("pay-now-url")
    }

    object SIMP {
      private def getParam[A: ConfigLoader](path: String): A = config.get[A](s"policy-parameters.simp.$path")

      val maxAmountOfDebt: AmountInPence = AmountInPence(getParam[Long]("max-amount-of-debt-in-pounds") * 100L)
      val maxPlanDurationInMonths: Int   = getParam[Int]("max-plan-duration-in-months")
      val payOnlineLink: String          = getParam[String]("pay-online-link")
    }
  }

  def pegaStartRedirectUrl(taxRegime: TaxRegime, lang: Language): String =
    Some(config.get[String]("pega.start-redirect-url"))
      .filter(_.nonEmpty)
      .map(_ + toPegaQueryParamString(taxRegime, lang))
      .getOrElse(testOnly.controllers.routes.PegaController.start(taxRegime).url)

  def pegaChangeLinkReturnUrl(taxRegime: TaxRegime, lang: Language): String =
    Some(config.get[String]("pega.change-link-return-url"))
      .filter(_.nonEmpty)
      .map(_ + toPegaQueryParamString(taxRegime, lang))
      .getOrElse(testOnly.controllers.routes.PegaController.start(taxRegime).url)

  private def toPegaQueryParamString(taxRegime: TaxRegime, lang: Language): String = {
    val regimeValue = taxRegime match {
      case TaxRegime.Epaye => "PAYE"
      case TaxRegime.Vat   => "VAT"
      case TaxRegime.Sa    => "SA"
      case TaxRegime.Simp  => sys.error("Should not be trying to construct PEGA redirect URL's for SIMP")
    }

    s"?regime=$regimeValue&lang=${lang.code}"
  }

}
