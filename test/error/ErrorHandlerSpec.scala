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

package error

import essttp.journey.model.Origins
import org.jsoup.Jsoup
import play.api.mvc.{CookieHeaderEncoding, Request, Session, SessionCookieBaker}
import play.api.test.FakeRequest
import testsupport.ItSpec
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

class ErrorHandlerSpec extends ItSpec {

  val errorHandler = app.injector.instanceOf[ErrorHandler]

  "The standard error template" - {

    "must not have a back link" in {
      given Request[?] = FakeRequest()
      whenReady(errorHandler.standardErrorTemplate("title", "heading", "message")) { html =>

        val doc = Jsoup.parse(html.body)
        doc.select(".govuk-back-link").isEmpty shouldBe true
        doc.select("span.govuk-service-navigation__service-name").text shouldBe "Set up a payment plan"
      }
    }

    "must include the correct service name if a tax regime can be found from a journey" in {
      val sessionCookieCrypto  = app.injector.instanceOf[SessionCookieCrypto]
      val cookieBaker          = app.injector.instanceOf[SessionCookieBaker]
      val cookieHeaderEncoding = app.injector.instanceOf[CookieHeaderEncoding]

      val cookie          = cookieBaker.encodeAsCookie(
        Session(Map("authToken" -> "Bearer 123", "sessionId" -> "session-ff5d52ad-f14c-4c72-b553-f197633a91ad"))
      )
      val encryptedCookie = cookie.copy(value = sessionCookieCrypto.crypto.encrypt(PlainText(cookie.value)).value)

      given Request[?] =
        FakeRequest().withHeaders("Cookie" -> cookieHeaderEncoding.encodeCookieHeader(Seq(encryptedCookie)))

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Pta)()

      whenReady(errorHandler.standardErrorTemplate("title", "heading", "message")) { html =>
        val doc = Jsoup.parse(html.body)
        doc.select(".govuk-back-link").isEmpty shouldBe true
        doc
          .select("span.govuk-service-navigation__service-name")
          .text shouldBe "Set up a Simple Assessment payment plan"
      }
    }
  }

}
