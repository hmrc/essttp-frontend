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
import play.api.http.Status
import play.api.mvc.{Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.RequestAssertions
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class SignOutControllerSpec extends ItSpec {
  private val controller: SignOutController = app.injector.instanceOf[SignOutController]

  "signOutFromTimeout should" - {
    "return the timed out page" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.signOutFromTimeout(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)
      doc.title() shouldBe "For your security, we signed you out - Set up an Employers’ PAYE payment plan - GOV.UK"
      doc.select(".govuk-heading-xl").text() shouldBe "For your security, we signed you out"
      doc.select(".hmrc-header__service-name").text() shouldBe "Set up an Employers’ PAYE payment plan"
    }
  }

  "exitSurveyPaye should" - {
    "redirect to feedback frontend with eSSTTP-PAYE as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(testCrypto)()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.exitSurveyPaye(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-PAYE")
      session(result) shouldBe Session(Map.empty)
    }
  }

}
