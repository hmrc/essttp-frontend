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

import org.jsoup.Jsoup
import play.api.mvc.Request
import play.api.test.FakeRequest
import testsupport.ItSpec

class ErrorHandlerSpec extends ItSpec {

  val errorHandler = app.injector.instanceOf[ErrorHandler]

  "The standard error template" - {

    "must not have a back link" in {
      implicit val request: Request[_] = FakeRequest()
      val html = errorHandler.standardErrorTemplate("title", "heading", "message")

      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-back-link").isEmpty shouldBe true
    }

  }

}
