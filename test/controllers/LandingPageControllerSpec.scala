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

package controllers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.testdata.TdAll
import testsupport.ItSpec
import testsupport.reusableassertions.RequestAssertions
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class LandingPageControllerSpec extends ItSpec {

  private val controller: LandingController = app.injector.instanceOf[LandingController]

  "GET /" - {
    "return 200 and the PAYE landing page" in {
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.landingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      val expectedH1 = "Set up an Employersâ€™ PAYE payment plan"
      val expectedServiceName: String = TdAll.expectedServiceNamePaye

      doc.title() shouldBe s"$expectedH1 - $expectedServiceName - GOV.UK"
      doc.select(".govuk-heading-xl").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName

    }
  }
}
