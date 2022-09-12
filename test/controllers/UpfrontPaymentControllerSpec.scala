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

//import essttp.rootmodel.AmountInPence
import essttp.rootmodel.AmountInPence
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.asScalaIteratorConverter

class UpfrontPaymentControllerSpec extends ItSpec {

  private val controller: UpfrontPaymentController = app.injector.instanceOf[UpfrontPaymentController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1CanYouPayUpfrontPage: String = "Can you make an upfront payment?"
  private val expectedPageTitleCanYouPayUpfrontPage: String = s"$expectedH1CanYouPayUpfrontPage - $expectedServiceName - GOV.UK"
  private val expectedPageHintCanPayUpfrontPage: String =
    "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 10 working days."
  private val expectedH1HowMuchCanYouPayUpfrontPage: String = "How much can you pay upfront?"
  private val expectedPageTitleHowMuchCanYouPayUpfrontPage: String = s"$expectedH1HowMuchCanYouPayUpfrontPage - $expectedServiceName - GOV.UK"
  private val expectedH1UpfrontSummaryPage: String = "Payment summary"
  private val expectedPageTitleUpfrontSummaryPage: String = s"$expectedH1UpfrontSummaryPage - $expectedServiceName - GOV.UK"

  "GET /can-you-make-an-upfront-payment" - {
    "should return 200 and the can you make an upfront payment page" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitleCanYouPayUpfrontPage
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1CanYouPayUpfrontPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.YourBillController.yourBill.url
      ContentAssertions.languageToggleExists(doc)
      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHintCanPayUpfrontPage
    }
    "should prepopulate the form when user navigates back and they have a chosen way to pay in their journey" in {
      stubActionDefaults()
      EssttpBackend.CanPayUpfront.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val doc: Document = Jsoup.parse(contentAsString(result))
      doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "Yes"
    }
  }

  "POST /can-you-make-an-upfront-payment" - {
    "should redirect to /how-much-can-you-pay-upfront when user chooses yes" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney()
      EssttpBackend.CanPayUpfront.stubUpdateCanPayUpfront(TdAll.journeyId, canPayUpfrontScenario = true)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/can-you-make-an-upfront-payment"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "Yes"))

      val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.howMuchCanYouPayUpfrontUrl)
      EssttpBackend.CanPayUpfront.verifyUpdateCanPayUpfrontRequest(TdAll.journeyId, TdAll.canPayUpfront)
    }

    "should redirect to /can-you-make-an-upfront-payment when user chooses no" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney()
      EssttpBackend.CanPayUpfront.stubUpdateCanPayUpfront(TdAll.journeyId, canPayUpfrontScenario = false)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/can-you-make-an-upfront-payment"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "No"))

      val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.retrievedExtremeDatesUrl)
      EssttpBackend.CanPayUpfront.verifyUpdateCanPayUpfrontRequest(TdAll.journeyId, TdAll.canNotPayUpfront)
    }

    "should redirect to /can-you-make-an-upfront-payment with error summary when no option is selected" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney()

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/can-you-make-an-upfront-payment"
      ).withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe s"Error: $expectedPageTitleCanYouPayUpfrontPage"
      doc.select(".govuk-fieldset__heading").text() shouldBe expectedH1CanYouPayUpfrontPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHintCanPayUpfrontPage
      doc.select("#back").attr("href") shouldBe routes.YourBillController.yourBill.url
      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "Select yes if you can make an upfront payment"
      errorLink.attr("href") shouldBe "#CanYouMakeAnUpFrontPayment"
      EssttpBackend.CanPayUpfront.verifyNoneUpdateCanPayUpfrontRequest(TdAll.journeyId)
    }
  }

  "GET /how-much-can-you-pay-upfront" - {
    "should return 200 and the how much can you pay upfront page" in {
      stubActionDefaults()
      EssttpBackend.CanPayUpfront.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitleHowMuchCanYouPayUpfrontPage
      doc.select(".govuk-label-wrapper").text() shouldBe expectedH1HowMuchCanYouPayUpfrontPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url
      ContentAssertions.languageToggleExists(doc)
      doc.select("#UpfrontPaymentAmount").size() shouldBe 1
      val poundSymbol = doc.select(".govuk-input__prefix")
      poundSymbol.size() shouldBe 1
      poundSymbol.text() shouldBe "£"
    }

    "should route the user to /can-you-make-an-upfront-payment when they try to force browse without selecting 'Yes' on the previous page" in {
      stubActionDefaults()
      EssttpBackend.CanPayUpfront.findJourney(JourneyJsonTemplates.`Answered Can Pay Upfront - No`)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)
    }

    "should prepopulate the form when user navigates back and they have an upfront payment amount in their journey" in {
      stubActionDefaults()
      EssttpBackend.UpfrontPaymentAmount.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val doc: Document = Jsoup.parse(contentAsString(result))
      doc.select("#UpfrontPaymentAmount").`val`() shouldBe "10"
    }
  }

  "POST /how-much-can-you-pay-upfront" - {
    "should redirect to /upfront-payment-summary when user enters a positive number, less than their total debt" in {
      stubActionDefaults()
      EssttpBackend.CanPayUpfront.findJourney()
      EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-upfront"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("UpfrontPaymentAmount", "1"))

      val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.upfrontPaymentSummaryUrl)
      EssttpBackend.UpfrontPaymentAmount.verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId, TdAll.upfrontPaymentAmount(100))
    }

    "should redirect to /upfront-payment-summary when user enters a positive number, at the upper limit" in {
      stubActionDefaults()
      EssttpBackend.CanPayUpfront.findJourney()
      EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(TdAll.journeyId)

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/how-much-can-you-pay-upfront"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(("UpfrontPaymentAmount", "2999"))

      val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.upfrontPaymentSummaryUrl)
      EssttpBackend.UpfrontPaymentAmount.verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId, TdAll.upfrontPaymentAmount(299900))
    }

    forAll(
      Table(
        ("Scenario flavour", "form input", "expected amount of money"),
        ("one decimal place", "1.1", AmountInPence(110)),
        ("two decimal places", "1.11", AmountInPence(111)),
        ("spaces", " 1 . 1  1  ", AmountInPence(111)),
        ("commas", "1,234", AmountInPence(123400)),
        ("'£' symbols", "£1234", AmountInPence(123400))
      )
    ) { (sf: String, formInput: String, expectedAmount: AmountInPence) =>
        s"should allow for $sf" in {
          stubActionDefaults()
          EssttpBackend.CanPayUpfront.findJourney()
          EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(TdAll.journeyId)

          val fakeRequest = FakeRequest(
            method = "POST",
            path   = "/how-much-can-you-pay-upfront"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(("UpfrontPaymentAmount", formInput))

          val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.upfrontPaymentSummaryUrl)
          EssttpBackend.UpfrontPaymentAmount.verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId, TdAll.upfrontPaymentAmount(expectedAmount.value))
        }
      }

    forAll(
      Table(
        ("Scenario flavour", "form input", "expected error message"),
        ("x > maximum debt", "30001", "Your upfront payment must be between £1 and £2,999"),
        ("x < 1", "0.99", "Your upfront payment must be between £1 and £2,999"),
        ("x < 0", "-1", "Your upfront payment must be between £1 and £2,999"),
        ("x = 0", "0", "Your upfront payment must be between £1 and £2,999"),
        ("x = NaN", "one", "How much you can pay upfront must be an amount of money"),
        ("x = null", "", "Enter your upfront payment"),
        ("scientific notation", "1e2", "How much you can pay upfront must be an amount of money"),
        ("more than one decimal place", "1.123", "How much you can pay upfront must be an amount of money")
      )
    ) { (sf: String, formInput: String, errorMessage: String) =>
        s"[$sf] should redirect to /how-much-can-you-pay-upfront with correct error summary when $formInput is submitted" in {
          stubActionDefaults()
          EssttpBackend.CanPayUpfront.findJourney()

          val fakeRequest = FakeRequest(
            method = "POST",
            path   = "/how-much-can-you-pay-upfront"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(("UpfrontPaymentAmount", formInput))

          val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)

          RequestAssertions.assertGetRequestOk(result)

          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)

          doc.title() shouldBe s"Error: $expectedPageTitleHowMuchCanYouPayUpfrontPage"
          doc.select(".govuk-label--xl").text() shouldBe expectedH1HowMuchCanYouPayUpfrontPage
          doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
          doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe errorMessage
          errorLink.attr("href") shouldBe "#UpfrontPaymentAmount"
          doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url
          EssttpBackend.UpfrontPaymentAmount.verifyNoneUpdateUpfrontPaymentAmountRequest(TdAll.journeyId)
        }
      }
  }

  "GET /upfront-payment-summary" - {
    "should return 200 and the upfront payment summary page" in {
      stubActionDefaults()
      EssttpBackend.UpfrontPaymentAmount.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentSummary(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitleUpfrontSummaryPage
      doc.select(".govuk-heading-xl").text() shouldBe expectedH1UpfrontSummaryPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select("#back").attr("href") shouldBe routes.UpfrontPaymentController.upfrontPaymentAmount.url
      ContentAssertions.languageToggleExists(doc)
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"

        def question(row: Element) = row.select(".govuk-summary-list__key").text()
        def answer(row: Element) = row.select(".govuk-summary-list__value").text()
        def changeUrl(row: Element) = row.select(".govuk-link").attr("href")
      val rows = doc.select(".govuk-summary-list__row").iterator().asScala.toList
      question(rows(0)) shouldBe "Can you make an upfront payment?"
      question(rows(1)) shouldBe "Upfront payment Taken within 10 working days"
      question(rows(2)) shouldBe "Remaining amount to pay"
      answer(rows(0)) shouldBe "Yes"
      answer(rows(1)) shouldBe "£10"
      answer(rows(2)) shouldBe "£2,990 (interest may be added to this amount)"
      changeUrl(rows(0)) shouldBe PageUrls.canYouMakeAnUpfrontPaymentUrl
      changeUrl(rows(1)) shouldBe PageUrls.howMuchCanYouPayUpfrontUrl

      val continueCta = doc.select("#continue")
      continueCta.text() shouldBe "Continue"
      continueCta.attr("href") shouldBe PageUrls.retrievedExtremeDatesUrl
    }
  }
}
