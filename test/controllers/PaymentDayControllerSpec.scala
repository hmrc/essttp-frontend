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
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.TdRequest.FakeRequestOps
import testsupport.testdata.TdAll
import uk.gov.hmrc.http.SessionKeys
import org.scalatest.prop.TableDrivenPropertyChecks._

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, collectionAsScalaIterableConverter}

class PaymentDayControllerSpec extends ItSpec {
  private val controller: PaymentDayController = app.injector.instanceOf[PaymentDayController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1: String = "Which day do you want to pay each month?"
  private val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"

  "GET /which-day-do-you-want-to-pay-each-month" - {
    "should return the 200 and the what day do you want to pay page" in {
      AuthStub.authorise()
      EssttpBackend.MonthlyPaymentAmount.findJourneyAfterUpdateMonthlyPaymentAmount()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.paymentDay(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitle
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount().url

      doc.select("#PaymentDay").size() shouldBe 1
      doc.select("#PaymentDay").attr("value") shouldBe "28"
      doc.select("#PaymentDay-2").size() shouldBe 1
      val radioLabels = doc.select(".govuk-radios__label").asScala.toSeq
      radioLabels(0).text() should include("28th or next working day")
      radioLabels(1).text() should include("A different day")
      doc.select("#conditional-PaymentDay-2 > div > label").text() shouldBe "Enter a day between 1 and 28"
      doc.select("#DifferentDay").size() shouldBe 1
      doc.select(".govuk-button").text().trim shouldBe "Continue"
    }

    "should prepopulate the form when user navigates back and they have a chosen day of month in their journey" in {
      AuthStub.authorise()
      EssttpBackend.DayOfMonth.findJourneyAfterUpdateDayOfMonth()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.paymentDay(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val doc: Document = Jsoup.parse(contentAsString(result))
      doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "28"
    }
  }

  "POST /which-day-do-you-want-to-pay-each-month" - {
    "should update journey with dayOfMonth and redirect to instalment page when 28th selected" in {
      AuthStub.authorise()
      EssttpBackend.MonthlyPaymentAmount.findJourneyAfterUpdateMonthlyPaymentAmount()
      EssttpBackend.DayOfMonth.updateDayOfMonth(TdAll.journeyId)
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/which-day-do-you-want-to-pay-each-month"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("PaymentDay", "28"))
      val result: Future[Result] = controller.paymentDaySubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/instalment-options")
      EssttpBackend.DayOfMonth.verifyUpdateDayOfMonthRequest(TdAll.journeyId)
    }
    "should update journey with dayOfMonth and redirect to instalment page when other day selected" in {
      AuthStub.authorise()
      EssttpBackend.MonthlyPaymentAmount.findJourneyAfterUpdateMonthlyPaymentAmount()
      EssttpBackend.DayOfMonth.updateDayOfMonth(TdAll.journeyId)
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
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/instalment-options")
      EssttpBackend.DayOfMonth.verifyUpdateDayOfMonthRequest(TdAll.journeyId, TdAll.dayOfMonth(1))
    }
    "should update journey with dayOfMonth and redirect to instalment page when other day selected and 28 entered" in {
      AuthStub.authorise()
      EssttpBackend.MonthlyPaymentAmount.findJourneyAfterUpdateMonthlyPaymentAmount()
      EssttpBackend.DayOfMonth.updateDayOfMonth(TdAll.journeyId)
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
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/instalment-options")
      EssttpBackend.DayOfMonth.verifyUpdateDayOfMonthRequest(TdAll.journeyId)
    }

    forAll(Table(
      ("Input Scenario", "inputValue", "expected error message"),
      ("No option selected", "", "Enter the day you want to pay each month"),
      ("Non number", "first", "The day you enter must be a number"),
      ("Less than 1", "0", "The day you enter must be between 1 and 28"),
      ("Greater than 28", "29", "The day you enter must be between 1 and 28"),
      ("Decimal", "1.8", "The day you enter must be a number"),
    )) {
      (scenario: String, inputValue: String, expectedErrorMessage: String) =>
        s"When input is: [ $scenario: [ $inputValue ]] error message should be $expectedErrorMessage" in {
          AuthStub.authorise()
          EssttpBackend.MonthlyPaymentAmount.findJourneyAfterUpdateMonthlyPaymentAmount()
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
          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)
          doc.title() shouldBe s"Error $expectedPageTitle"
          doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1
          doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
          doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
          doc.select("#back").attr("href") shouldBe routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount().url
          doc.select("#PaymentDay").size() shouldBe 1
          doc.select("#PaymentDay").attr("value") shouldBe "28"
          doc.select("#PaymentDay-2").size() shouldBe 1
          val radioLabels = doc.select(".govuk-radios__label").asScala.toSeq
          radioLabels(0).text() should include("28th or next working day")
          radioLabels(1).text() should include("A different day")
          doc.select("#conditional-PaymentDay-2 > div > label").text() shouldBe "Enter a day between 1 and 28"
          doc.select("#DifferentDay").size() shouldBe 1
          doc.select(".govuk-button").text().trim shouldBe "Continue"

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe expectedErrorMessage
          errorLink.attr("href") shouldBe "#DifferentDay"
          EssttpBackend.MonthlyPaymentAmount.verifyNoneUpdateMonthlyAmountRequest(TdAll.journeyId)

        }
    }
  }
}
