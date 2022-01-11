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

package login

import config.AppConfig
import play.api.Environment
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrlPolicy.Id
import uk.gov.hmrc.play.bootstrap.binders._

import java.net.URL

object LoginSupport {

  def getLoginUrlStr(frontendReturn: Call)(implicit vc: AppConfig, env: Environment): String = {
    assert("GET".equalsIgnoreCase(frontendReturn.method))

    val relativeReturnUrl: String = frontendReturn.url
    val absoluteReturnUrl: String = vc.BaseUrl.essttpFrontend + relativeReturnUrl

    getLoginUrlStr(RedirectUrl(absoluteReturnUrl))
  }

  def getLoginUrlStr(returnUrl: RedirectUrl)(implicit vc: AppConfig, env: Environment): String = {
    loginUrl(returnUrl).fold(
      errorMsg => throw new IllegalArgumentException(errorMsg),
      goodUrl => goodUrl.toString)
  }

  def loginUrl(returnUrl: RedirectUrl)(implicit vc: AppConfig, env: Environment): Either[String, URL] = {
    import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._
    val policy: RedirectUrlPolicy[Id] = AbsoluteWithHostnameFromAllowlist(vc.BaseUrl.essttpFrontendHost) | PermitAllOnDev(env)

    returnUrl.getEither(policy).map { (safeUrl: SafeRedirectUrl) =>
      new URL(s"${vc.BaseUrl.gg}?continue=${safeUrl.encodedUrl}&origin=pay-online")
    }
  }
}
