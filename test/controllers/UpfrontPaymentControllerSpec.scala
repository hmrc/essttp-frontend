/*
 * Copyright 2023 HM Revenue & Customs
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

import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.{AmountInPence, TaxRegime}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.{Call, Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.Givens.canEqualPlaySession
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions, UnchangedFromCYALinkAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala}

class UpfrontPaymentControllerSpec extends ItSpec, UnchangedFromCYALinkAssertions {

  private val controller: UpfrontPaymentController = app.injector.instanceOf[UpfrontPaymentController]

  private val expectedH1CanYouPayUpfrontPage: String        = "Upfront payment"
  private val expectedPageLegendCanPayUpfrontPage: String   = "Can you make an upfront payment?"
  private val expectedH1HowMuchCanYouPayUpfrontPage: String = "How much can you pay upfront?"
  private val expectedH1UpfrontSummaryPage: String          = "Payment summary"

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat),
    ("SA", Origins.Sa.Bta, TaxRegime.Sa),
    ("SIMP", Origins.Simp.Pta, TaxRegime.Simp)
  ).foreach { case (regime, origin, taxRegime) =>
    "GET /can-you-make-an-upfront-payment" - {

      s"[$regime journey] should return 200 and the can you make an upfront payment page when" - {

        "there is an interest bearing charge" in {
          stubCommonActions()
          EssttpBackend.WhyCannotPayInFull
            .findJourney(testCrypto, origin, maybeChargeIsInterestBearingCharge = Some(true))()

          val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)
          val pageContent: String    = contentAsString(result)
          val doc: Document          = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1 = expectedH1CanYouPayUpfrontPage,
            shouldBackLinkBePresent = true,
            expectedSubmitUrl = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPaymentSubmit.url),
            regimeBeingTested = Some(taxRegime)
          )
          val radioContent = doc.select(".govuk-radios__label").asScala.toList
          radioContent(0).text() shouldBe "Yes"
          radioContent(1).text() shouldBe "No"

          doc.select("legend").text() shouldBe expectedPageLegendCanPayUpfrontPage
          doc.select("#upfrontPayment-p1").text() shouldBe "If you pay some of your bill upfront, you’ll:"
          doc.select(".govuk-list > li").asScala.toList.map(_.text()) shouldBe List(
            "have a shorter payment plan",
            "pay less interest"
          )
          doc
            .select("#upfrontPayment-p2")
            .text() shouldBe "An upfront payment is separate to any recent payments you’ve made. We’ll take it from your bank account within 6 working days."
        }

        "there is no interest bearing charge" in {
          stubCommonActions()
          EssttpBackend.WhyCannotPayInFull
            .findJourney(testCrypto, origin, maybeChargeIsInterestBearingCharge = Some(false))()

          val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)
          val pageContent: String    = contentAsString(result)
          val doc: Document          = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1 = expectedH1CanYouPayUpfrontPage,
            shouldBackLinkBePresent = true,
            expectedSubmitUrl = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPaymentSubmit.url),
            regimeBeingTested = Some(taxRegime)
          )
          val radioContent = doc.select(".govuk-radios__label").asScala.toList
          radioContent(0).text() shouldBe "Yes"
          radioContent(1).text() shouldBe "No"

          doc.select("legend").text() shouldBe expectedPageLegendCanPayUpfrontPage
          doc
            .select("#upfrontPayment-p1")
            .text() shouldBe "If you pay some of your bill upfront, you’ll have a shorter payment plan."
          doc
            .select("#upfrontPayment-p2")
            .text() shouldBe "This payment will be taken from your bank account within 6 working days."
          doc
            .select("#upfrontPayment-p3")
            .text() shouldBe "An upfront payment is separate to any recent payments you have made."
        }
      }

      s"[$regime journey] should prepopulate the form when user navigates back and they have a chosen way to pay in their journey" in {
        stubCommonActions()
        EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.canYouMakeAnUpfrontPayment(fakeRequest)
        val doc: Document          = Jsoup.parse(contentAsString(result))

        RequestAssertions.assertGetRequestOk(result)
        doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "Yes"
      }
    }

    "POST /can-you-make-an-upfront-payment" - {

      s"[$regime journey] should redirect to /how-much-can-you-pay-upfront when user chooses yes" in {
        stubCommonActions()
        EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto, origin)()
        EssttpBackend.CanPayUpfront.stubUpdateCanPayUpfront(
          TdAll.journeyId,
          canPayUpfrontScenario = true,
          JourneyJsonTemplates.`Answered Can Pay Upfront - Yes`(origin)
        )

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/can-you-make-an-upfront-payment"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "Yes"))

        val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.howMuchCanYouPayUpfrontUrl)
        EssttpBackend.CanPayUpfront.verifyUpdateCanPayUpfrontRequest(TdAll.journeyId, TdAll.canPayUpfront)
      }

      s"[$regime journey] should redirect to /can-you-make-an-upfront-payment when user chooses no" in {
        stubCommonActions()
        EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto, origin)()
        EssttpBackend.CanPayUpfront.stubUpdateCanPayUpfront(
          TdAll.journeyId,
          canPayUpfrontScenario = false,
          JourneyJsonTemplates.`Answered Can Pay Upfront - No`(origin)
        )

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/can-you-make-an-upfront-payment"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(("CanYouMakeAnUpFrontPayment", "No"))

        val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.retrievedExtremeDatesUrl)
        EssttpBackend.CanPayUpfront.verifyUpdateCanPayUpfrontRequest(TdAll.journeyId, TdAll.canNotPayUpfront)
      }

      s"[$regime journey] should redirect to /can-you-make-an-upfront-payment with error summary when no option is selected" in {
        stubCommonActions()
        EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto, origin)()

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/can-you-make-an-upfront-payment"
        ).withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

        val result: Future[Result] = controller.canYouMakeAnUpfrontPaymentSubmit(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = expectedH1CanYouPayUpfrontPage,
          expectedSubmitUrl = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPaymentSubmit.url),
          shouldBackLinkBePresent = true,
          hasFormError = true,
          regimeBeingTested = Some(taxRegime)
        )

        doc.select("legend").text() shouldBe expectedPageLegendCanPayUpfrontPage
        val errorSummary = doc.select(".govuk-error-summary")
        val errorLink    = errorSummary.select("a")
        errorLink.text() shouldBe "Select yes if you can make an upfront payment"
        errorLink.attr("href") shouldBe "#CanYouMakeAnUpFrontPayment"
        EssttpBackend.CanPayUpfront.verifyNoneUpdateCanPayUpfrontRequest(TdAll.journeyId)
      }

      behave like unchangedAnswerAfterClickingCYAChangeBehaviuor(
        origin,
        controller.canYouMakeAnUpfrontPaymentSubmit,
        Seq(("CanYouMakeAnUpFrontPayment", "Yes")),
        EssttpBackend.CanPayUpfront.stubUpdateCanPayUpfront(
          TdAll.journeyId,
          canPayUpfrontScenario = true,
          _
        )
      )

    }

    "GET /how-much-can-you-pay-upfront" - {
      s"[$regime journey] should return 200 and the how much can you pay upfront page" in {
        stubCommonActions()
        EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = expectedH1HowMuchCanYouPayUpfrontPage,
          expectedSubmitUrl = Some(routes.UpfrontPaymentController.upfrontPaymentAmountSubmit.url),
          shouldBackLinkBePresent = true,
          regimeBeingTested = Some(taxRegime)
        )

        doc.select("#UpfrontPaymentAmount").size() shouldBe 1
        val poundSymbol = doc.select(".govuk-input__prefix")
        poundSymbol.size() shouldBe 1
        poundSymbol.text() shouldBe "£"

        val hint           = doc.select(".govuk-hint")
        val hintParagraphs = hint.select("p.govuk-body").asScala.toList
        hintParagraphs.size shouldBe 1
        hintParagraphs(0).text shouldBe "Enter an amount between £1 and £1,499"
      }

      s"[$regime journey] should route the user to /retrieve-extreme-dates when they try to force browse without selecting 'Yes' on the previous page" in {
        stubCommonActions()
        EssttpBackend.CanPayUpfront.findJourney(testCrypto)(
          JourneyJsonTemplates.`Answered Can Pay Upfront - No`(origin)
        )

        val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.retrievedExtremeDatesUrl)
      }

      s"[$regime journey] should prepopulate the form when user navigates back and they have an upfront payment amount in their journey" in {
        stubCommonActions()
        EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.upfrontPaymentAmount(fakeRequest)

        RequestAssertions.assertGetRequestOk(result)

        val doc: Document = Jsoup.parse(contentAsString(result))
        doc.select("#UpfrontPaymentAmount").`val`() shouldBe "10"
      }
    }

    "POST /how-much-can-you-pay-upfront" - {
      s"[$regime journey] should redirect to /upfront-payment-summary when user enters a positive number, less than their total debt" in {
        stubCommonActions()
        EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()
        EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(
          TdAll.journeyId,
          JourneyJsonTemplates.`Entered Upfront payment amount`(origin)
        )

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/how-much-can-you-pay-upfront"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(("UpfrontPaymentAmount", "1"))

        val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.upfrontPaymentSummaryUrl)
        EssttpBackend.UpfrontPaymentAmount
          .verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId, TdAll.upfrontPaymentAmount(100))
      }

      s"[$regime journey] should redirect to /upfront-payment-summary when user enters a positive number, at the upper limit" in {
        stubCommonActions()
        EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()
        EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(
          TdAll.journeyId,
          JourneyJsonTemplates.`Entered Upfront payment amount`(origin)
        )

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/how-much-can-you-pay-upfront"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(("UpfrontPaymentAmount", "1499"))

        val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.upfrontPaymentSummaryUrl)
        EssttpBackend.UpfrontPaymentAmount
          .verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId, TdAll.upfrontPaymentAmount(149900))
      }

      behave like unchangedAnswerAfterClickingCYAChangeBehaviuor(
        origin,
        controller.upfrontPaymentAmountSubmit,
        Seq(("UpfrontPaymentAmount", "2")),
        EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(
          TdAll.journeyId,
          _
        )
      )

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
        s"[$regime journey] should allow for $sf" in {
          stubCommonActions()
          EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()
          EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(
            TdAll.journeyId,
            JourneyJsonTemplates.`Entered Upfront payment amount`(origin)
          )

          val fakeRequest = FakeRequest(
            method = "POST",
            path = "/how-much-can-you-pay-upfront"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(("UpfrontPaymentAmount", formInput))

          val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.upfrontPaymentSummaryUrl)
          EssttpBackend.UpfrontPaymentAmount
            .verifyUpdateUpfrontPaymentAmountRequest(TdAll.journeyId, TdAll.upfrontPaymentAmount(expectedAmount.value))
        }
      }

      forAll(
        Table(
          ("Scenario flavour", "form input", "expected error message"),
          ("x > maximum debt", "1499.01", "Your upfront payment must be between £1 and £1,499"),
          ("x < 1", "0.99", "Your upfront payment must be between £1 and £1,499"),
          ("x < 0", "-1", "Your upfront payment must be between £1 and £1,499"),
          ("x = 0", "0", "Your upfront payment must be between £1 and £1,499"),
          ("x = NaN", "one", "How much you can pay upfront must be an amount of money"),
          ("x = null", "", "Enter your upfront payment"),
          ("scientific notation", "1e2", "How much you can pay upfront must be an amount of money"),
          ("more than one decimal place", "1.123", "How much you can pay upfront must be an amount of money")
        )
      ) { (sf: String, formInput: String, errorMessage: String) =>
        s"[$regime journey][$sf] should redirect to /how-much-can-you-pay-upfront with correct error summary when $formInput is submitted" in {
          stubCommonActions()
          EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()

          val fakeRequest = FakeRequest(
            method = "POST",
            path = "/how-much-can-you-pay-upfront"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(("UpfrontPaymentAmount", formInput))

          val result: Future[Result] = controller.upfrontPaymentAmountSubmit(fakeRequest)
          val pageContent: String    = contentAsString(result)
          val doc: Document          = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1 = expectedH1HowMuchCanYouPayUpfrontPage,
            expectedSubmitUrl = Some(routes.UpfrontPaymentController.upfrontPaymentAmountSubmit.url),
            shouldBackLinkBePresent = true,
            hasFormError = true,
            regimeBeingTested = Some(taxRegime)
          )

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink    = errorSummary.select("a")
          errorLink.text() shouldBe errorMessage
          errorLink.attr("href") shouldBe "#UpfrontPaymentAmount"
          EssttpBackend.UpfrontPaymentAmount.verifyNoneUpdateUpfrontPaymentAmountRequest(TdAll.journeyId)
        }
      }
    }

    "GET /upfront-payment-summary" - {

      s"[$regime journey] should return 200 and the upfront payment summary page" - {

        def test(
          stubActions:                        () => Unit,
          expectedUpfrontPaymentAmountString: String,
          expectedRemainingAmountString:      String
        ) = {
          stubActions()

          val result: Future[Result] = controller.upfrontPaymentSummary(fakeRequest)
          val pageContent: String    = contentAsString(result)
          val doc: Document          = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1 = expectedH1UpfrontSummaryPage,
            expectedSubmitUrl = None,
            shouldBackLinkBePresent = true,
            regimeBeingTested = Some(taxRegime)
          )

          def question(row: Element) = row.select(".govuk-summary-list__key").text()

          def answer(row: Element) = row.select(".govuk-summary-list__value").text()

          def changeUrl(row: Element) = row.select(".govuk-link").attr("href")

          val rows = doc.select(".govuk-summary-list__row").iterator().asScala.toList
          question(rows(0)) shouldBe "Can you make an upfront payment?"
          question(rows(1)) shouldBe "Upfront payment Taken within 6 working days"
          question(rows(2)) shouldBe "Remaining amount to pay"
          answer(rows(0)) shouldBe "Yes"
          answer(rows(1)) shouldBe expectedUpfrontPaymentAmountString
          answer(rows(2)) shouldBe expectedRemainingAmountString
          changeUrl(rows(0)) shouldBe PageUrls.upfrontPaymentSummaryChangeUrl("CanPayUpfront")
          changeUrl(rows(1)) shouldBe PageUrls.upfrontPaymentSummaryChangeUrl("UpfrontPaymentAmount")

          val continueCta = doc.select("#continue")
          continueCta.text() shouldBe "Continue"
          continueCta.attr("href") shouldBe PageUrls.retrievedExtremeDatesUrl

        }

        "when they have just given an upfront payment amount and there is an interest bearing charge" in {
          test(
            { () =>
              stubCommonActions()
              EssttpBackend.UpfrontPaymentAmount
                .findJourney(testCrypto, origin, maybeChargeIsInterestBearingCharge = Some(true))()
              ()
            },
            expectedUpfrontPaymentAmountString = "£10",
            expectedRemainingAmountString = "£2,990 (interest may be added to this amount)"
          )
        }

        "when they have just given an upfront payment amount and there is no interest bearing charge" in {
          test(
            { () =>
              stubCommonActions()
              EssttpBackend.UpfrontPaymentAmount
                .findJourney(testCrypto, origin, maybeChargeIsInterestBearingCharge = Some(false))()
              ()
            },
            expectedUpfrontPaymentAmountString = "£10",
            expectedRemainingAmountString = "£2,990"
          )
        }

        "when they have confirmed they upfront payment answers and they have said " +
          "they can make an upfront payment" in {
            test(
              { () =>
                stubCommonActions()
                EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)()
                ()
              },
              expectedUpfrontPaymentAmountString = "£2",
              expectedRemainingAmountString = "£2,998 (interest may be added to this amount)"
            )
          }

      }

      s"[$regime journey] should redirect to the missing info page" - {

        def test(stubActions: () => Unit) = {
          stubActions()

          val result: Future[Result] = controller.upfrontPaymentSummary(fakeRequest)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.MissingInfoController.missingInfo.url)
        }

        "when the user has not given an upfront payment amount yet" in {
          test { () =>
            stubCommonActions()
            EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()
            ()
          }
        }

        "when the user has given upfront payment answers but they have said they can't " +
          "make an upfront payment" in {
            test { () =>
              stubCommonActions()
              EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)(
                JourneyJsonTemplates
                  .`Retrieved Affordability no upfront payment`(origin = origin, minimumInstalmentAmount = 29997)
              )
              ()
            }
          }

      }

    }

    "GET /upfront-payment-summary/change" - {

      s"[$regime journey] should redirect to the correct page and update the cookie session with the pageId" - {

        def test(pageId: String, expectedRedirect: Call): Unit = {
          stubCommonActions()
          EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto, origin)()

          val expectedUpdatedSession = Session(
            fakeRequest.session.data
              .updated(Routing.clickedChangeFromSessionKey, "true")
          )
          val result                 = controller.changeFromUpfrontPaymentSummary(pageId)(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirect.url)
          session(result) shouldBe expectedUpdatedSession
          ()
        }

        "CanPayUpfront" in {
          test("CanPayUpfront", routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment)
        }

        "UpfrontPaymentAmount" in {
          test("UpfrontPaymentAmount", routes.UpfrontPaymentController.upfrontPaymentAmount)
        }

      }

      s"[$regime journey] should return an error when the pageId is not recognised" in {
        stubCommonActions()
        EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto, origin)()

        a[NoSuchElementException] shouldBe thrownBy(
          await(controller.changeFromUpfrontPaymentSummary("abc")(fakeRequest))
        )
      }

    }

  }
}

class UpfrontPaymentControllerPEGARedirectInConfigSpec extends ItSpec, UnchangedFromCYALinkAssertions {

  val pegaChangeLinkReturnUrl = "/abc"

  override protected lazy val configOverrides: Map[String, Any] = Map(
    "pega.change-link-return-url" -> pegaChangeLinkReturnUrl
  )

  val controller: UpfrontPaymentController = app.injector.instanceOf[UpfrontPaymentController]

  Seq(
    Origins.Epaye.Bta,
    Origins.Vat.Bta,
    Origins.Sa.Bta,
    Origins.Simp.Pta
  ).foreach { origin =>
    "When the PEGA change link return URL is defined in config" - {

      "POST /can-you-make-an-upfront-payment" - {

        behave like unchangedAnswerAfterClickingCYAChangeBehaviuor(
          origin,
          controller.canYouMakeAnUpfrontPaymentSubmit,
          Seq(("CanYouMakeAnUpFrontPayment", "Yes")),
          EssttpBackend.CanPayUpfront.stubUpdateCanPayUpfront(
            TdAll.journeyId,
            canPayUpfrontScenario = true,
            _
          ),
          pegaChangeLinkReturnUrl
        )

      }

      "POST /how-much-can-you-pay-upfront" - {

        behave like unchangedAnswerAfterClickingCYAChangeBehaviuor(
          origin,
          controller.upfrontPaymentAmountSubmit,
          Seq(("UpfrontPaymentAmount", "2")),
          EssttpBackend.UpfrontPaymentAmount.stubUpdateUpfrontPaymentAmount(
            TdAll.journeyId,
            _
          ),
          pegaChangeLinkReturnUrl
        )

      }
    }

  }

}
