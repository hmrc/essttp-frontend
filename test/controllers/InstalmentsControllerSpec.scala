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
import testsupport.TdRequest.FakeRequestOps
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.reusableassertions.RequestAssertions
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.{PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, collectionAsScalaIterableConverter}

class InstalmentsControllerSpec extends ItSpec {
  private val controller: InstalmentsController = app.injector.instanceOf[InstalmentsController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1: String = "How many months do you want to pay over?"
  private val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
  private val progressiveRevealContent: String = "How we calculate interest"
  private val progressiveRevealInnerContent1: String = "We only charge interest on overdue amounts."
  private val progressiveRevealInnerContent2: String = "We charge the Bank of England base rate plus 2.5%, calculated as simple interest."
  private val progressiveRevealInnerContent3: String =
    "If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan."

  "GET /how-many-months-do-you-want-to-pay-over should" - {
    "return 200 and the instalment selection page" in {
      AuthStub.authorise()
      EssttpBackend.AffordableQuotes.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.instalmentOptions(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitle
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.PaymentDayController.paymentDay.url

      val radioButtonGroup = doc.select(".govuk-radios")
      val individualButtons = radioButtonGroup.select(".govuk-radios__item").asScala.toSeq
      individualButtons.size shouldBe 3
      individualButtons(0).select(".govuk-radios__input").`val`() shouldBe "2"
      individualButtons(0).select(".govuk-radios__label").text() shouldBe "2 months at ??555.73"
      individualButtons(0).select(".govuk-radios__hint").text() shouldBe "Estimated total interest of ??0.06"
      individualButtons(1).select(".govuk-radios__input").`val`() shouldBe "3"
      individualButtons(1).select(".govuk-radios__label").text() shouldBe "3 months at ??370.50"
      individualButtons(1).select(".govuk-radios__hint").text() shouldBe "Estimated total interest of ??0.09"
      individualButtons(2).select(".govuk-radios__input").`val`() shouldBe "4"
      individualButtons(2).select(".govuk-radios__label").text() shouldBe "4 months at ??277.88"
      individualButtons(2).select(".govuk-radios__hint").text() shouldBe "Estimated total interest of ??0.12"

      doc.select(".govuk-details__summary-text").text() shouldBe progressiveRevealContent
      val progressiveRevealSubContent = doc.select(".govuk-details__text").select(".govuk-body").asScala.toSeq
      progressiveRevealSubContent(0).text() shouldBe progressiveRevealInnerContent1
      progressiveRevealSubContent(1).text() shouldBe progressiveRevealInnerContent2
      progressiveRevealSubContent(2).text() shouldBe progressiveRevealInnerContent3
      doc.select(".govuk-button").text().trim shouldBe "Continue"
    }
    "pre pop the selected radio option when user has navigated back and they have a chosen month in their journey" in {
      AuthStub.authorise()
      EssttpBackend.SelectedPaymentPlan.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.instalmentOptions(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))
      doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "2"
    }
  }

  "POST /how-many-months-do-you-want-to-pay-over should" - {
    "redirect to instalment summary page when form is valid" in {
      AuthStub.authorise()
      EssttpBackend.AffordableQuotes.findJourney()
      EssttpBackend.SelectedPaymentPlan.updateSelectedPlan(TdAll.journeyId)
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-many-months-do-you-want-to-pay-over"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("Instalments", "2"))
      val result: Future[Result] = controller.instalmentOptionsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.instalmentScheduleUrl)
      EssttpBackend.SelectedPaymentPlan.verifyUpdateSelectedPlanRequest(TdAll.journeyId)
    }
    "display correct error message when form is submitted with no value" in {
      AuthStub.authorise()
      EssttpBackend.AffordableQuotes.findJourney()
      EssttpBackend.SelectedPaymentPlan.updateSelectedPlan(TdAll.journeyId)
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-many-months-do-you-want-to-pay-over"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.instalmentOptionsSubmit(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))
      doc.title() shouldBe s"Error $expectedPageTitle"
      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "Select how many months you want to pay over"
      errorLink.attr("href") shouldBe "#Instalments"
      EssttpBackend.SelectedPaymentPlan.verifyNoneUpdateSelectedPlanRequest(TdAll.journeyId)
    }
  }

}
