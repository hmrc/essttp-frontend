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
import scala.collection.JavaConverters._

class UpfrontPaymentControllerSpec extends ItSpec {

  private val controller: UpfrontPaymentController = app.injector.instanceOf[UpfrontPaymentController]
  private val expectedH1CanYouPayUpfrontPage: String = "Can you make an upfront payment?"
  private val expectedPageHintCanPayUpfrontPage: String =
    "Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 10 working days."
  private val expectedH1HowMuchCanYouPayUpfrontPage: String = "How much can you pay upfront?"
  private val expectedH1UpfrontSummaryPage: String = "Payment summary"

  "GET /can-you-make-an-upfront-payment" - {

    "should return 200 and the can you make an upfront payment page" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = expectedH1CanYouPayUpfrontPage,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPaymentSubmit.url)
      )
      val radioContent = doc.select(".govuk-radios__label").asScala.toList
      radioContent(0).text() shouldBe "Yes"
      radioContent(1).text() shouldBe "No"

      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHintCanPayUpfrontPage
    }

    "should prepopulate the form when user navigates back and they have a chosen way to pay in their journey" in {
      stubCommonActions()
      EssttpBackend.CanPayUpfront.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)
      val doc: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "Yes"
    }
  }

  "POST /can-you-make-an-upfront-payment" - {
    "should redirect to /how-much-can-you-pay-upfront when user chooses yes" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()
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
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()
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
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/can-you-make-an-upfront-payment"
      ).withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = expectedH1CanYouPayUpfrontPage,
        expectedSubmitUrl       = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPaymentSubmit.url),
        shouldBackLinkBePresent = true,
        hasFormError            = true
      )

      doc.select("#CanYouMakeAnUpFrontPayment-hint").text() shouldBe expectedPageHintCanPayUpfrontPage
      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "Select yes if you can make an upfront payment"
      errorLink.attr("href") shouldBe "#CanYouMakeAnUpFrontPayment"
      EssttpBackend.CanPayUpfront.verifyNoneUpdateCanPayUpfrontRequest(TdAll.journeyId)
    }
  }

  "GET /how-much-can-you-pay-upfront" - {
    "should return 200 and the how much can you pay upfront page" in {
      stubCommonActions()
      EssttpBackend.CanPayUpfront.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = expectedH1HowMuchCanYouPayUpfrontPage,
        expectedSubmitUrl       = Some(routes.UpfrontPaymentController.upfrontPaymentAmountSubmit.url),
        shouldBackLinkBePresent = true
      )

      doc.select("#UpfrontPaymentAmount").size() shouldBe 1
      val poundSymbol = doc.select(".govuk-input__prefix")
      poundSymbol.size() shouldBe 1
      poundSymbol.text() shouldBe "£"

      doc.select(".govuk-hint").text shouldBe "Your monthly payments will be lower if you make an upfront payment. " +
        "This payment will be taken from your bank account within 10 working days."

      doc.select(".govuk-details__summary").text shouldBe "How we calculate interest"
    }

    "should route the user to /retrieve-extreme-dates when they try to force browse without selecting 'Yes' on the previous page" in {
      stubCommonActions()
      EssttpBackend.CanPayUpfront.findJourney(testCrypto)(JourneyJsonTemplates.`Answered Can Pay Upfront - No`(testCrypto))

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.retrievedExtremeDatesUrl)
    }

    "should prepopulate the form when user navigates back and they have an upfront payment amount in their journey" in {
      stubCommonActions()
      EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val doc: Document = Jsoup.parse(contentAsString(result))
      doc.select("#UpfrontPaymentAmount").`val`() shouldBe "10"
    }
  }

  "POST /how-much-can-you-pay-upfront" - {
    "should redirect to /upfront-payment-summary when user enters a positive number, less than their total debt" in {
      stubCommonActions()
      EssttpBackend.CanPayUpfront.findJourney(testCrypto)()
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
      stubCommonActions()
      EssttpBackend.CanPayUpfront.findJourney(testCrypto)()
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
          stubCommonActions()
          EssttpBackend.CanPayUpfront.findJourney(testCrypto)()
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
          stubCommonActions()
          EssttpBackend.CanPayUpfront.findJourney(testCrypto)()

          val fakeRequest = FakeRequest(
            method = "POST",
            path   = "/how-much-can-you-pay-upfront"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(("UpfrontPaymentAmount", formInput))

          val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1              = expectedH1HowMuchCanYouPayUpfrontPage,
            expectedSubmitUrl       = Some(routes.UpfrontPaymentController.upfrontPaymentAmountSubmit.url),
            shouldBackLinkBePresent = true,
            hasFormError            = true
          )

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe errorMessage
          errorLink.attr("href") shouldBe "#UpfrontPaymentAmount"
          EssttpBackend.UpfrontPaymentAmount.verifyNoneUpdateUpfrontPaymentAmountRequest(TdAll.journeyId)
        }
      }
  }

  "GET /upfront-payment-summary" - {

    "should return 200 and the upfront payment summary page" - {

        def test(
            stubActions:                        () => Unit,
            expectedUpfrontPaymentAmountString: String,
            expectedRemainingAmountString:      String
        ) = {
          stubActions()
          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

          val result: Future[Result] = controller.upfrontPaymentSummary(fakeRequest)
          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1              = expectedH1UpfrontSummaryPage,
            expectedSubmitUrl       = None,
            shouldBackLinkBePresent = true
          )

            def question(row: Element) = row.select(".govuk-summary-list__key").text()
            def answer(row: Element) = row.select(".govuk-summary-list__value").text()
            def changeUrl(row: Element) = row.select(".govuk-link").attr("href")

          val rows = doc.select(".govuk-summary-list__row").iterator().asScala.toList
          question(rows(0)) shouldBe "Can you make an upfront payment?"
          question(rows(1)) shouldBe "Upfront payment Taken within 10 working days"
          question(rows(2)) shouldBe "Remaining amount to pay"
          answer(rows(0)) shouldBe "Yes"
          answer(rows(1)) shouldBe expectedUpfrontPaymentAmountString
          answer(rows(2)) shouldBe expectedRemainingAmountString
          changeUrl(rows(0)) shouldBe PageUrls.canYouMakeAnUpfrontPaymentUrl
          changeUrl(rows(1)) shouldBe PageUrls.howMuchCanYouPayUpfrontUrl

          val continueCta = doc.select("#continue")
          continueCta.text() shouldBe "Continue"
          continueCta.attr("href") shouldBe PageUrls.retrievedExtremeDatesUrl

        }

      "when they have just given an upfront payment amount" in {
        test(
          { () =>
            stubCommonActions()
            EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto)()
            ()
          },
          expectedUpfrontPaymentAmountString = "£10",
          expectedRemainingAmountString      = "£2,990 (interest may be added to this amount)"
        )
      }

      "when they have confirmed they upfront payment answers and they have said " +
        "they can make an upfront payment" in {
          test(
            { () =>
              stubCommonActions()
              EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto)()
              ()
            },
            expectedUpfrontPaymentAmountString = "£2",
            expectedRemainingAmountString      = "£2,998 (interest may be added to this amount)"
          )
        }

    }

    "should redirect to the missing info page" - {

        def test(stubActions: () => Unit) = {
          stubActions()
          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

          val result: Future[Result] = controller.upfrontPaymentSummary(fakeRequest)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.MissingInfoController.missingInfo.url)
        }

      "when the user has not given an upfront payment amount yet" in {
        test({ () =>
          stubCommonActions()
          EssttpBackend.CanPayUpfront.findJourney(testCrypto)()
          ()
        })
      }

      "when the user has given upfront payment answers but they have said they can't " +
        "make an upfront payment" in {
          test({ () =>
            stubCommonActions()
            EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto)(
              JourneyJsonTemplates.`Retrieved Affordability no upfront payment`(29997, testCrypto)
            )
            ()
          })
        }

    }

  }
}
