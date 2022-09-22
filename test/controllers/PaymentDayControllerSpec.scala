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

import essttp.rootmodel.DayOfMonth
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, collectionAsScalaIterableConverter}

class PaymentDayControllerSpec extends ItSpec {

  private val controller: PaymentDayController = app.injector.instanceOf[PaymentDayController]
  private val expectedH1: String = "Which day do you want to pay each month?"

  def assertPaymentDayPageContent(doc: Document): Unit = {
    doc.select("#PaymentDay").size() shouldBe 1
    doc.select("#PaymentDay").attr("value") shouldBe "28"
    doc.select("#PaymentDay-2").size() shouldBe 1

    val radioLabels = doc.select(".govuk-radios__label").asScala.toSeq
    radioLabels(0).text() should include("28th or next working day")
    radioLabels(1).text() should include("A different day")

    doc.select("#conditional-PaymentDay-2 > div > label").text() shouldBe "Enter a day between 1 and 28"
    doc.select("#DifferentDay").size() shouldBe 1
    doc.select(".govuk-button").text().trim shouldBe "Continue"
    ()
  }

  "GET /which-day-do-you-want-to-pay-each-month" - {

    "should return the 200 and the what day do you want to pay page" in {
      stubCommonActions()
      EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.paymentDay(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1        = expectedH1,
        expectedBack      = Some(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount.url),
        expectedSubmitUrl = Some(routes.PaymentDayController.paymentDaySubmit.url)
      )

      assertPaymentDayPageContent(doc)
    }

    "should prepopulate the form" - {

      "when user navigates back and they have a chosen the 28th of each month in their journey" in {
        stubCommonActions()
        EssttpBackend.DayOfMonth.findJourney(DayOfMonth(28), testCrypto)()

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

        val result: Future[Result] = controller.paymentDay(fakeRequest)
        val doc: Document = Jsoup.parse(contentAsString(result))

        RequestAssertions.assertGetRequestOk(result)

        val radioInputs = doc.select(".govuk-radios__input").iterator().asScala.toList
        radioInputs.size shouldBe 2
        radioInputs(0).select("[checked]").`val`() shouldBe "28"
        radioInputs(1).select("[checked]").isEmpty shouldBe true
      }

      "when user navigates back and they have a chosen a day different from the 28th of each month in their journey" in {
        stubCommonActions()
        EssttpBackend.DayOfMonth.findJourney(DayOfMonth(5), testCrypto)()

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

        val result: Future[Result] = controller.paymentDay(fakeRequest)
        val doc: Document = Jsoup.parse(contentAsString(result))

        RequestAssertions.assertGetRequestOk(result)

        val radioInputs = doc.select(".govuk-radios__input").iterator().asScala.toList
        radioInputs.size shouldBe 2
        radioInputs(0).select("[checked]").isEmpty shouldBe true
        radioInputs(1).select("[checked]").isEmpty shouldBe false

        doc.select(".govuk-radios__conditional > .govuk-form-group > .govuk-input").`val`() shouldBe "5"

      }

    }
  }

  "POST /which-day-do-you-want-to-pay-each-month" - {

    "should update journey with dayOfMonth and redirect to instalment page when 28th selected" in {
      stubCommonActions()
      EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto)()
      EssttpBackend.DayOfMonth.stubUpdateDayOfMonth(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/which-day-do-you-want-to-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("PaymentDay", "28"))

      val result: Future[Result] = controller.paymentDaySubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.retrieveStartDatesUrl)
      EssttpBackend.DayOfMonth.verifyUpdateDayOfMonthRequest(TdAll.journeyId)
    }

    "should update journey with dayOfMonth and redirect to instalment page when other day selected" in {
      stubCommonActions()
      EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto)()
      EssttpBackend.DayOfMonth.stubUpdateDayOfMonth(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/which-day-do-you-want-to-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("PaymentDay", "other"),
          ("DifferentDay", "1")
        )

      val result: Future[Result] = controller.paymentDaySubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.retrieveStartDatesUrl)
      EssttpBackend.DayOfMonth.verifyUpdateDayOfMonthRequest(TdAll.journeyId, TdAll.dayOfMonth(1))
    }

    "should update journey with dayOfMonth and redirect to instalment page when other day selected and 28 entered" in {
      stubCommonActions()
      EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto)()
      EssttpBackend.DayOfMonth.stubUpdateDayOfMonth(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/which-day-do-you-want-to-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("PaymentDay", "other"),
          ("DifferentDay", "28")
        )

      val result: Future[Result] = controller.paymentDaySubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.retrieveStartDatesUrl)
      EssttpBackend.DayOfMonth.verifyUpdateDayOfMonthRequest(TdAll.journeyId)
    }

    forAll(Table(
      ("Input Scenario", "inputValue", "expected error message"),
      ("No option selected", "", "Enter the day you want to pay each month"),
      ("Non number", "first", "The day you want to pay must be a number"),
      ("Less than 1", "0", "The day you want to pay must be between 1 and 28"),
      ("Greater than 28", "29", "The day you want to pay must be between 1 and 28"),
      ("Decimal", "1.8", "The day you want to pay must be a number"),
    )) {
      (scenario: String, inputValue: String, expectedErrorMessage: String) =>
        s"When input is: [ $scenario: [ $inputValue ]] error message should be $expectedErrorMessage" in {
          stubCommonActions()
          EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto)()

          val fakeRequest = FakeRequest(
            method = "POST",
            path   = "/which-day-do-you-want-to-pay-each-month"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(
              ("PaymentDay", "other"),
              ("DifferentDay", inputValue)
            )

          val result: Future[Result] = controller.paymentDaySubmit(fakeRequest)
          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1        = expectedH1,
            expectedBack      = Some(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount.url),
            expectedSubmitUrl = Some(routes.PaymentDayController.paymentDaySubmit.url),
            hasFormError      = true
          )

          assertPaymentDayPageContent(doc)

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe expectedErrorMessage
          errorLink.attr("href") shouldBe "#DifferentDay"
          EssttpBackend.MonthlyPaymentAmount.verifyNoneUpdateMonthlyAmountRequest(TdAll.journeyId)
        }
    }
  }
}
