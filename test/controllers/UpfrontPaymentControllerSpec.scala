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
  private val expectedServiceName: String = "Set up an Employers’ PAYE payment plan"
  private val expectedH1: String = "Can you make an upfront payment?"
  private val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
  private val expectedPageHint: String =
    "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 7 working days."

  "GET /can-you-make-an-upfront-payment" - {
    "return 200 and the can you make an upfront payment page" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitle
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.YourBillController.yourBill().url
      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHint
    }
  }

  "POST /can-you-make-an-upfront-payment [Yes] redirects to upfront-payment-amount" in {
    AuthStub.authorise()
    EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck
    EssttpBackend.CanPayUpfront.updateCanPayUpfront(TdAll.journeyId, canPayUpfrontScenario = true)

    val fakeRequest = FakeRequest(
      method = "POST",
      path   = "/can-you-make-an-upfront-payment"
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
    EssttpBackend.CanPayUpfront.updateCanPayUpfront(TdAll.journeyId, canPayUpfrontScenario = false)

    val fakeRequest = FakeRequest(
      method = "POST",
      path   = "/can-you-make-an-upfront-payment"
    ).withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "No"))

    val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
    status(result) shouldBe Status.SEE_OTHER
    redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/monthly-payment-amount")
  }

  "POST /can-you-make-an-upfront-payment [No option selected] returns /can-you-make-an-upfront-payment with error summary" in {
    AuthStub.authorise()
    EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck

    val fakeRequest = FakeRequest(
      method = "POST",
      path   = "/can-you-make-an-upfront-payment"
    ).withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)

    status(result) shouldBe Status.OK
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")

    val pageContent: String = contentAsString(result)
    val doc: Document = Jsoup.parse(pageContent)

    doc.title() shouldBe s"Error $expectedPageTitle"
    doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1
    doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
    doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
    doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHint
    val errorSummary = doc.select(".govuk-error-summary")
    val errorLink = errorSummary.select("a")
    errorLink.text() shouldBe "Select yes if you can make an upfront payment"
    errorLink.attr("href") shouldBe "#CanYouMakeAnUpFrontPayment"
  }
}
