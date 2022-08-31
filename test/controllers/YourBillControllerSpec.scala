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
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.PageUrls
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate
import scala.concurrent.Future

class YourBillControllerSpec extends ItSpec {
  private val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "GET /your-bill should" - {
    "return you bill page" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.yourBill(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.languageToggleExists(Jsoup.parse(contentAsString(result)))
    }
  }

  "POST /your-bill should" - {
    "redirect to can you make an upfront payment question page" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)
    }
  }

  "YourBillController.monthNumberInTaxYear should return correct tax month" in {
    forAll(Table(
      ("Scenario", "Date", "Expected result"),
      ("January", "2022-01-10", 10),
      ("February", "2022-02-10", 11),
      ("March", "2022-03-10", 12),
      ("April", "2022-04-10", 1),
      ("May", "2022-05-10", 2),
      ("June", "2022-06-10", 3),
      ("July", "2022-07-10", 4),
      ("August", "2022-08-10", 5),
      ("September", "2022-09-10", 6),
      ("October", "2022-10-10", 7),
      ("November", "2022-11-10", 8),
      ("December", "2022-12-10", 9),
      ("April 5th", "2022-04-05", 12),
      ("April 6th", "2022-04-06", 1),
      ("March 5th", "2022-03-05", 11),
      ("March 6th", "2022-03-06", 12)
    )) {
      (scenario: String, date: String, expectedResult: Int) =>
        {
          YourBillController.monthNumberInTaxYear(LocalDate.parse(date)) shouldBe expectedResult withClue s"monthNumberInTaxYear failed for $scenario"
        }
    }
  }
}
