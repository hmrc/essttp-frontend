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
import org.scalatest.prop.TableDrivenPropertyChecks._
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
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1CanYouPayUpfrontPage: String = "Can you make an upfront payment?"
  private val expectedPageTitleCanYouPayUpfrontPage: String = s"$expectedH1CanYouPayUpfrontPage - $expectedServiceName - GOV.UK"
  private val expectedPageHintCanPayUpfrontPage: String =
    "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 10 working days."
  private val expectedH1HowMuchCanYouPayUpfrontPage: String = "How much can you pay upfront?"
  private val expectedPageTitleHowMuchCanYouPayUpfrontPage: String = s"$expectedH1HowMuchCanYouPayUpfrontPage - $expectedServiceName - GOV.UK"

  "GET /can-you-make-an-upfront-payment" - {
    "should return 200 and the can you make an upfront payment page" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitleCanYouPayUpfrontPage
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1CanYouPayUpfrontPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.YourBillController.yourBill().url
      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHintCanPayUpfrontPage
    }
  }

  "POST /can-you-make-an-upfront-payment" - {
    "should redirect to /how-much-can-you-pay-upfront when user chooses yes" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck()
      EssttpBackend.CanPayUpfront.updateCanPayUpfront(TdAll.journeyId, canPayUpfrontScenario = true)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/can-you-make-an-upfront-payment"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "Yes"))

      val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/how-much-can-you-pay-upfront")
      EssttpBackend.CanPayUpfront.verifyUpdateCanPayUpfrontRequest(TdAll.journeyId)
    }

    "should redirect to /can-you-make-an-upfront-payment when user chooses no" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck()
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
      EssttpBackend.CanPayUpfront.verifyUpdateCanPayUpfrontRequest(TdAll.journeyId)
    }

    "should redirect to /can-you-make-an-upfront-payment with error summary when no option is selected" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourneyAfterEligibilityCheck()

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

      doc.title() shouldBe s"Error $expectedPageTitleCanYouPayUpfrontPage"
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1CanYouPayUpfrontPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHintCanPayUpfrontPage
      doc.select("#back").attr("href") shouldBe routes.YourBillController.yourBill().url
      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "Select yes if you can make an upfront payment"
      errorLink.attr("href") shouldBe "#CanYouMakeAnUpFrontPayment"
      EssttpBackend.CanPayUpfront.verifyNoneUpdateCanPayUpfrontRequest(TdAll.journeyId)
    }
  }

  "GET /how-much-can-you-pay-upfront" - {
    "should return 200 and the how much can you pay upfront page" in {
      AuthStub.authorise()
      EssttpBackend.CanPayUpfront.findJourneyAfterUpdateCanPayUpfront(canPayUpfront = TdAll.canPayUpfront)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitleHowMuchCanYouPayUpfrontPage
      doc.select(".govuk-label-wrapper").text() shouldBe expectedH1HowMuchCanYouPayUpfrontPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url
      doc.select("#UpfrontPaymentAmount").size() shouldBe 1
      val poundSymbol = doc.select(".govuk-input__prefix")
      poundSymbol.size() shouldBe 1
      poundSymbol.text() shouldBe "£"
    }

    "should route the user to /can-you-make-an-upfront-payment when they try to force browse without selecting 'Yes' on the previous page" in {
      AuthStub.authorise()
      EssttpBackend.CanPayUpfront.findJourneyAfterUpdateCanPayUpfront(TdAll.canNotPayUpfront)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/can-you-make-an-upfront-payment")
    }
  }

  "POST /how-much-can-you-pay-upfront" - {
    "should redirect to /upfront-payment-summary when user enters a positive number, less than their total debt" in {
      AuthStub.authorise()
      EssttpBackend.CanPayUpfront.findJourneyAfterUpdateCanPayUpfront(TdAll.canPayUpfront)
      EssttpBackend.UpfrontPaymentAmount.updateUpfrontPaymentAmount(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-upfront"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("UpfrontPaymentAmount", "1"))

      val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/upfront-payment-summary")
      EssttpBackend.UpfrontPaymentAmount.verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId)
    }

    "should allow for decimal numbers if they are within the amount bounds" in {
      AuthStub.authorise()
      EssttpBackend.CanPayUpfront.findJourneyAfterUpdateCanPayUpfront(TdAll.canPayUpfront)
      EssttpBackend.UpfrontPaymentAmount.updateUpfrontPaymentAmount(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-upfront"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("UpfrontPaymentAmount", "1.1"))

      val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/upfront-payment-summary")
      EssttpBackend.UpfrontPaymentAmount.verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId)
    }

    forAll(
      Table(
        ("Scenario flavour", "form input", "expected error message"),
        ("x > maximum debt", "30001", "How much you can pay upfront must be £3,000.00 or less"),
        ("x < 1", "0.99", "How much you can pay upfront must be £1.00 or more"),
        ("x < 0", "-1", "How much you can pay upfront must be £1.00 or more"),
        ("x = 0", "0", "How much you can pay upfront must be £1.00 or more"),
        ("x = NaN", "one", "How much you can pay upfront must be an amount of money"),
        ("x = null", "", "Enter your upfront payment")
      )
    ) { (sf: String, formInput: String, errorMessage: String) =>
        s"[$sf] should redirect to /how-much-can-you-pay-upfront with correct error summary when $formInput is submitted" in {
          AuthStub.authorise()
          EssttpBackend.CanPayUpfront.findJourneyAfterUpdateCanPayUpfront(TdAll.canPayUpfront)

          val fakeRequest = FakeRequest(
            method = "POST",
            path   = "/how-much-can-you-pay-upfront"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(("UpfrontPaymentAmount", formInput))

          val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)

          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")

          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)

          doc.title() shouldBe s"Error $expectedPageTitleHowMuchCanYouPayUpfrontPage"
          doc.select(".govuk-label--xl").text() shouldBe expectedH1HowMuchCanYouPayUpfrontPage
          doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
          doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe errorMessage
          errorLink.attr("href") shouldBe "#UpfrontPaymentAmount"
          doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url
          EssttpBackend.UpfrontPaymentAmount.verifyNoneUpdateUpfrontPaymentAmountRequest(TdAll.journeyId)
        }
      }
  }
}
