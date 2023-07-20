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

package controllers

import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class SignOutControllerSpec extends ItSpec {

  private val controller: SignOutController = app.injector.instanceOf[SignOutController]

  "signOutFromTimeout should" - {

    "return the timed out page" in {
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.signOutFromTimeout(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "For your security, we signed you out",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        signedIn                = false,
        regimeBeingTested       = None
      )
    }
  }

  "exitSurveyPaye should" - {
    "redirect to feedback frontend with eSSTTP-PAYE as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.exitSurveyPaye(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-PAYE")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "exitSurveyVat should" - {
    "redirect to feedback frontend with eSSTTP-VAT as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.exitSurveyVat(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-VAT")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "signOut should" - {
    TaxRegime.values.foreach { taxRegime =>
      s"[taxRegime = ${taxRegime.toString}] redirect to the tax regime specific exist survey route with no sessionId" in {
        val (origin, expectedRedirectLocation) = taxRegime match {
          case TaxRegime.Epaye => Origins.Epaye.Bta -> "/set-up-a-payment-plan/exit-survey/paye"
          case TaxRegime.Vat   => Origins.Vat.Bta -> "/set-up-a-payment-plan/exit-survey/vat"
        }

        stubCommonActions()
        EssttpBackend.StartJourney.findJourney(origin)

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
        val result: Future[Result] = controller.signOut(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectLocation)
        session(result) shouldBe Session(Map.empty)
      }
    }
  }

}
