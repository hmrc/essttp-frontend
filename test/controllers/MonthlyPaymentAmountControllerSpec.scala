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
import essttp.rootmodel.{AmountInPence, MonthlyPaymentAmount, TaxRegime}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
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
import scala.jdk.CollectionConverters.IterableHasAsScala

class MonthlyPaymentAmountControllerSpec extends ItSpec {

  private val controller: MonthlyPaymentAmountController = app.injector.instanceOf[MonthlyPaymentAmountController]
  private val expectedH1: String = "How much can you afford to pay each month?"
  private val expectedPageHint: String = "Enter an amount between £300 and £880"
  private val progressiveRevealContent: String = "I can’t afford the minimum payment"
  private val progressiveRevealInnerContent1: String =
    "You may still be able to set up a payment plan over the phone, but you are not eligible for an online payment plan."
  private val progressiveRevealInnerContent2: String =
    "We recommend you speak to an adviser on 0300 200 3835 at the Payment Support Service to talk about your payment options."

  def testMonthlyPaymentAmountContent(doc: Document): Unit = {
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
    ()
  }

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat)
  ).foreach {
      case (regime, origin, taxRegime) =>
        "GET /how-much-can-you-pay-each-month" - {
          s"[$regime journey] should return 200 and the how much can you pay a month page" in {
            stubCommonActions()
            EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.displayMonthlyPaymentAmount(fakeRequest)
            val pageContent: String = contentAsString(result)
            val doc: Document = Jsoup.parse(pageContent)

            RequestAssertions.assertGetRequestOk(result)
            ContentAssertions.commonPageChecks(
              doc,
              expectedH1              = expectedH1,
              shouldBackLinkBePresent = true,
              expectedSubmitUrl       = Some(routes.MonthlyPaymentAmountController.monthlyPaymentAmountSubmit.url),
              regimeBeingTested       = Some(taxRegime)
            )

            testMonthlyPaymentAmountContent(doc)
          }

          s"[$regime journey] should prepopulate the form when user navigates back and they have a monthly payment amount in their journey" in {
            stubCommonActions()
            EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.displayMonthlyPaymentAmount(fakeRequest)

            RequestAssertions.assertGetRequestOk(result)

            val doc: Document = Jsoup.parse(contentAsString(result))
            doc.select("#MonthlyPaymentAmount").`val`() shouldBe "300"
          }

          s"[$regime journey] should display the minimum amount as £1 if the minimum amount is less than £1" in {
            stubCommonActions()
            EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)(JourneyJsonTemplates.`Retrieved Affordability`(origin, 1))

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.displayMonthlyPaymentAmount(fakeRequest)

            RequestAssertions.assertGetRequestOk(result)
            val doc: Document = Jsoup.parse(contentAsString(result))
            doc.select("#MonthlyPaymentAmount-hint").text() shouldBe "Enter an amount between £1 and £880"
          }
        }

        "POST /how-much-can-you-pay-each-month should" - {
          s"[$regime journey] redirect to what day do you want to pay on when form is valid" in {
            stubCommonActions()
            EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)()
            EssttpBackend.MonthlyPaymentAmount.stubUpdateMonthlyPaymentAmount(
              TdAll.journeyId,
              JourneyJsonTemplates.`Entered Monthly Payment Amount`(origin)
            )

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/how-much-can-you-pay-each-month"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")
              .withFormUrlEncodedBody(("MonthlyPaymentAmount", "300"))

            val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.whichDayDoYouWantToPayUrl)
            EssttpBackend.MonthlyPaymentAmount.verifyUpdateMonthlyPaymentAmountRequest(TdAll.journeyId, TdAll.monthlyPaymentAmount)
          }

          s"[$regime journey] should redirect to the specified url in session if the user came from a change link and did not change their answer" in {
            val changeOriginUrl = "/abc"

            stubCommonActions()
            EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto, origin)()
            EssttpBackend.MonthlyPaymentAmount.stubUpdateMonthlyPaymentAmount(
              TdAll.journeyId,
              JourneyJsonTemplates.`Entered Monthly Payment Amount`(origin)
            )

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/how-much-can-you-pay-each-month"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId", Routing.clickedChangeFromSessionKey -> changeOriginUrl)
              .withFormUrlEncodedBody(("MonthlyPaymentAmount", "300"))

            val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(changeOriginUrl)
            session(result).get(Routing.clickedChangeFromSessionKey) shouldBe None
            EssttpBackend.MonthlyPaymentAmount.verifyUpdateMonthlyPaymentAmountRequest(TdAll.journeyId, TdAll.monthlyPaymentAmount)
          }

          forAll(
            Table(
              ("Scenario flavour", "form input", "expected amount of money"),
              ("one decimal place", "300.1", AmountInPence(30010)),
              ("two decimal places", "300.11", AmountInPence(30011)),
              ("spaces", " 3 00 . 1  1  ", AmountInPence(30011)),
              ("commas", "3,00", AmountInPence(30000)),
              ("'£' symbols", "300", AmountInPence(30000))
            )
          ) { (sf: String, formInput: String, expectedAmount: AmountInPence) =>
              s"[$regime journey] should allow for $sf" in {
                stubCommonActions()
                EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)()
                EssttpBackend.MonthlyPaymentAmount.stubUpdateMonthlyPaymentAmount(
                  TdAll.journeyId,
                  JourneyJsonTemplates.`Entered Monthly Payment Amount`(origin)
                )

                val fakeRequest = FakeRequest(
                  method = "POST",
                  path   = "/how-much-can-you-pay-each-month"
                ).withAuthToken()
                  .withSession(SessionKeys.sessionId -> "IamATestSessionId")
                  .withFormUrlEncodedBody(("MonthlyPaymentAmount", formInput))

                val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)
                status(result) shouldBe Status.SEE_OTHER
                redirectLocation(result) shouldBe Some(PageUrls.whichDayDoYouWantToPayUrl)
                EssttpBackend.MonthlyPaymentAmount.verifyUpdateMonthlyPaymentAmountRequest(TdAll.journeyId, MonthlyPaymentAmount(expectedAmount))
              }
            }

          forAll(
            Table(
              ("Scenario flavour", "form input", "expected error message"),
              ("x > maximum value", "880.01", "How much you can afford to pay each month must be between £300 and £880"),
              ("x < minimum value", "299.99", "How much you can afford to pay each month must be between £300 and £880"),
              ("x = NaN", "one", "How much you can afford to pay each month must be an amount of money"),
              ("x = null", "", "Enter how much you can afford to pay each month"),
              ("scientific notation", "1e2", "How much you can afford to pay each month must be an amount of money"),
              ("more than one decimal place", "1.123", "How much you can afford to pay each month must be an amount of money")
            )
          ) { (sf: String, formInput: String, errorMessage: String) =>
              s"[$regime journey] [$sf] should show the page with the correct error message when $formInput is submitted" in {
                stubCommonActions()
                EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, origin)()

                val fakeRequest = FakeRequest(
                  method = "POST",
                  path   = "/how-much-can-you-pay-each-month"
                ).withAuthToken()
                  .withSession(SessionKeys.sessionId -> "IamATestSessionId")
                  .withFormUrlEncodedBody(("MonthlyPaymentAmount", formInput))

                val result: Future[Result] = controller.monthlyPaymentAmountSubmit(fakeRequest)
                val pageContent: String = contentAsString(result)
                val doc: Document = Jsoup.parse(pageContent)

                RequestAssertions.assertGetRequestOk(result)
                ContentAssertions.commonPageChecks(
                  doc,
                  expectedH1              = expectedH1,
                  shouldBackLinkBePresent = true,
                  expectedSubmitUrl       = Some(routes.MonthlyPaymentAmountController.monthlyPaymentAmountSubmit.url),
                  hasFormError            = true,
                  regimeBeingTested       = Some(taxRegime)
                )
                testMonthlyPaymentAmountContent(doc)

                val errorSummary = doc.select(".govuk-error-summary")
                val errorLink = errorSummary.select("a")
                errorLink.text() shouldBe errorMessage
                errorLink.attr("href") shouldBe "#MonthlyPaymentAmount"
                EssttpBackend.MonthlyPaymentAmount.verifyNoneUpdateMonthlyAmountRequest(TdAll.journeyId)
              }
            }

        }
    }

}
