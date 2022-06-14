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
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class MonthlyPaymentAmountControllerSpec extends ItSpec {
  private val controller: MonthlyPaymentAmountController = app.injector.instanceOf[MonthlyPaymentAmountController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1: String = "How much can you afford to pay each month?"
  private val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
  private val expectedPageHint: String = "Enter an amount between £299.97 and £879.44"
  private val progressiveRevealContent: String = "I can’t afford the minimum payment"
  private val progressiveRevealInnerContent1: String =
    "You may still be able to set up a payment plan over the phone, but you are not eligible for an online payment plan."
  private val progressiveRevealInnerContent2: String =
    "We recommend you speak to an adviser on 0300 200 3835 at the Payment Support Service to talk about your payment options."

  "GET /how-much-can-you-pay-each-month" - {
    "should return 200 and the how much can you pay a month page" in {
      AuthStub.authorise()
      EssttpBackend.AffordabilityMinMaxApi.findJourneyAfterUpdateAffordability()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.displayMonthlyPaymentAmount(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitle
      doc.select(".govuk-label--xl").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url // todo update this, it depends on journey
      doc.select("#MonthlyPaymentAmount-hint").text() shouldBe expectedPageHint
      doc.select("#MonthlyPaymentAmount").size() shouldBe 1
      val poundSymbol = doc.select(".govuk-input__prefix")
      poundSymbol.size() shouldBe 1
      poundSymbol.text() shouldBe "£"
      doc.select(".govuk-details__summary-text").text() shouldBe progressiveRevealContent
      val progressiveRevealSubContent = doc.select(".govuk-details__text").select(".govuk-body").asScala.toSeq
      progressiveRevealSubContent(0).text() shouldBe progressiveRevealInnerContent1
      progressiveRevealSubContent(1).text() shouldBe progressiveRevealInnerContent2
      doc.select("#continue").text() should include("Continue")
    }
  }

  "POST /how-much-can-you-pay-each-month should" - {
    "redirect to what day do you want to pay on when form is valid" in {
      AuthStub.authorise()
      EssttpBackend.AffordabilityMinMaxApi.findJourneyAfterUpdateAffordability()
      EssttpBackend.MonthlyPaymentAmount.updateMonthlyPaymentAmount(TdAll.journeyId)
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("MonthlyPaymentAmount", "300"))
      val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/payment-day")
      EssttpBackend.MonthlyPaymentAmount.verifyUpdateMonthlyPaymentAmountRequest(TdAll.journeyId)
    }

    "display correct error message when form is submitted with value outside of bounds" in {
      AuthStub.authorise()
      EssttpBackend.AffordabilityMinMaxApi.findJourneyAfterUpdateAffordability()
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("MonthlyPaymentAmount", "100"))
      val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe s"Error $expectedPageTitle"
      doc.select(".govuk-label--xl").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url // todo update this, it depends on journey
      doc.select("#MonthlyPaymentAmount-hint").text() shouldBe expectedPageHint
      doc.select("#MonthlyPaymentAmount").size() shouldBe 1
      val poundSymbol = doc.select(".govuk-input__prefix")
      poundSymbol.size() shouldBe 1
      poundSymbol.text() shouldBe "£"

      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "How much you can afford to pay each month must be between £299.97 and £879.44"
      errorLink.attr("href") shouldBe "#MonthlyPaymentAmount"
      EssttpBackend.MonthlyPaymentAmount.verifyNoneUpdateMonthlyAmountRequest(TdAll.journeyId)
    }

    "display correct error message when empty form is submitted" in {
      AuthStub.authorise()
      EssttpBackend.AffordabilityMinMaxApi.findJourneyAfterUpdateAffordability()
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("MonthlyPaymentAmount", ""))
      val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe s"Error $expectedPageTitle"
      doc.select(".govuk-label--xl").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url // todo update this, it depends on journey
      doc.select("#MonthlyPaymentAmount-hint").text() shouldBe expectedPageHint
      doc.select("#MonthlyPaymentAmount").size() shouldBe 1
      val poundSymbol = doc.select(".govuk-input__prefix")
      poundSymbol.size() shouldBe 1
      poundSymbol.text() shouldBe "£"

      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "Enter how much you can afford to pay each month"
      errorLink.attr("href") shouldBe "#MonthlyPaymentAmount"
      EssttpBackend.MonthlyPaymentAmount.verifyNoneUpdateMonthlyAmountRequest(TdAll.journeyId)
    }
  }
}
