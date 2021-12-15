/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  object BaseUrl {
    val essttpFrontend: String = config.get[String]("baseUrl.essttp-frontend")
    val contactFrontend = config.get[String]("baseUrl.contact-frontend")
    val feedbackFrontend: String = config.get[String]("baseUrl.feedback-frontend")
    val caFrontend: String = config.get[String]("baseUrl.ca-frontend")
    val gg: String = config.get[String]("baseUrl.gg")
  }

  object Urls {
    val loginUrl: String = BaseUrl.gg
    val signOutUrl = BaseUrl.caFrontend + "/gg/sign-out"
    val landingPage = BaseUrl.essttpFrontend + controllers.routes.IndexController.onPageLoad.path()
    val reportAProblemPartialUrl: String = s"${BaseUrl.contactFrontend}/contact/problem_reports_ajax?service=$appName"
    val reportAProblemNonJSUrl: String = s"${BaseUrl.contactFrontend}/contact/problem_reports_nonjs?service=$appName"
    def betaFeedbackUrl(implicit request: RequestHeader): String =
      s"${BaseUrl.contactFrontend}/contact/beta-feedback?" +
        s"service=$appName&" +
        s"backUrl=${SafeRedirectUrl(BaseUrl.essttpFrontend + request.uri).encodedUrl}"
    val exitSurveyUrl: String = s"${BaseUrl.feedbackFrontend}/feedback/$appName"

    val cookiesUrl: String = config.get[String]("govUkUrls.cookiesUrl")
    val termsAndConditionsUrl: String = config.get[String]("govUkUrls.termsAndConditionsUrl")
    val helpUsingGovUkUrl: String = config.get[String]("govUkUrls.helpUsingGovUkUrl")
  }

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy"))

  val timeout: Int = config.get[Int]("timeout-dialog.timeout")
  val countdown: Int = config.get[Int]("timeout-dialog.countdown")
  val cacheTtl: Int = config.get[Int]("mongodb.timeToLiveInSeconds")

  val analyticsToken: String = config.get[String](s"google-analytics.token")
  val analyticsHost: String = config.get[String](s"google-analytics.host")

}
