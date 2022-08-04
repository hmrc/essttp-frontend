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

import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.reusableassertions.RequestAssertions
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.PageUrls
import uk.gov.hmrc.http.SessionKeys

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
}
