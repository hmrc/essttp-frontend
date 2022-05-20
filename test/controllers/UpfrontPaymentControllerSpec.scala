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
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.TdAll
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class UpfrontPaymentControllerSpec extends ItSpec {

  private val controller: UpfrontPaymentController = app.injector.instanceOf[UpfrontPaymentController]

  "GET /can-you-make-an-upfront-payment" - {
    "return 200 and the can you make an upfront payment page" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) should include("Can you make an upfront payment?")
      contentAsString(result) should include(
        "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 7 working days."
      )
    }
  }

  "POST /can-you-make-an-upfront-payment [Yes] redirects to upfront-payment-amount" in {
    AuthStub.authorise()
    EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck
    EssttpBackend.CanPayUpfront.updateCanPayUpfront(TdAll.journeyId, true)

    val fakeRequest = FakeRequest(
      method = "POST",
      path = "/can-you-make-an-upfront-payment"
    ).withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "Yes"))

    val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/upfront-payment-amount")
  }

  "POST /can-you-make-an-upfront-payment [No] redirects to monthly payment amount" in {
    AuthStub.authorise()
    EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck
    EssttpBackend.CanPayUpfront.updateCanPayUpfront(TdAll.journeyId, false)

    val fakeRequest = FakeRequest(
      method = "POST",
      path = "/can-you-make-an-upfront-payment"
    ).withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "No"))

    val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/monthly-payment-amount")
  }
}
