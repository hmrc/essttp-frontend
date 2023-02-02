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
import essttp.rootmodel.ttp.affordablequotes.{AmountDue, PaymentPlan}
import models.InstalmentOption
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}

class InstalmentsControllerSpec extends ItSpec {
  private val controller: InstalmentsController = app.injector.instanceOf[InstalmentsController]
  private val expectedH1: String = "How many months do you want to pay over?"
  private def expectedPageTitle(taxRegime: TaxRegime): String = taxRegime match {
    case TaxRegime.Epaye => s"$expectedH1 - ${TdAll.expectedServiceNamePayeEn} - GOV.UK"
    case TaxRegime.Vat   => s"$expectedH1 - ${TdAll.expectedServiceNameVatEn} - GOV.UK"
  }
  private val progressiveRevealContent: String = "How we calculate interest"
  private val progressiveRevealInnerContent1: String = "We only charge interest on overdue amounts."
  private val progressiveRevealInnerContent2: String = "We charge the Bank of England base rate plus 2.5%, calculated as simple interest."
  private val progressiveRevealInnerContent3: String =
    "If the interest rate changes during your plan, your monthly payments will not change. If we need to, we’ll settle the difference at the end of the plan."

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat)
  ).foreach {
      case (regime, origin, taxRegime) =>

        "GET /how-many-months-do-you-want-to-pay-over should" - {
          s"[$regime journey] return 200 and the instalment selection page" in {
            stubCommonActions()
            EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val result: Future[Result] = controller.instalmentOptions(fakeRequest)
            val pageContent: String = contentAsString(result)
            val doc: Document = Jsoup.parse(pageContent)

            RequestAssertions.assertGetRequestOk(result)
            ContentAssertions.commonPageChecks(
              doc,
              expectedH1              = expectedH1,
              shouldBackLinkBePresent = true,
              expectedSubmitUrl       = Some(routes.InstalmentsController.instalmentOptionsSubmit.url),
              regimeBeingTested       = Some(taxRegime)
            )

            val radioButtonGroup = doc.select(".govuk-radios")
            val individualButtons = radioButtonGroup.select(".govuk-radios__item").asScala.toSeq
            individualButtons.size shouldBe 3
            individualButtons(0).select(".govuk-radios__input").`val`() shouldBe "2"
            individualButtons(0).select(".govuk-radios__label").text() shouldBe "2 months at £555.73"
            individualButtons(0).select(".govuk-radios__hint").text() shouldBe "Estimated total interest of £0.06"
            individualButtons(1).select(".govuk-radios__input").`val`() shouldBe "3"
            individualButtons(1).select(".govuk-radios__label").text() shouldBe "3 months at £370.50"
            individualButtons(1).select(".govuk-radios__hint").text() shouldBe "Estimated total interest of £0.09"
            individualButtons(2).select(".govuk-radios__input").`val`() shouldBe "4"
            individualButtons(2).select(".govuk-radios__label").text() shouldBe "4 months at £277.88"
            individualButtons(2).select(".govuk-radios__hint").text() shouldBe "Estimated total interest of £0.12"

            doc.select(".govuk-details__summary-text").text() shouldBe progressiveRevealContent
            val progressiveRevealSubContent = doc.select(".govuk-details__text").select(".govuk-body").asScala.toSeq
            progressiveRevealSubContent(0).text() shouldBe progressiveRevealInnerContent1
            progressiveRevealSubContent(1).text() shouldBe progressiveRevealInnerContent2
            progressiveRevealSubContent(2).text() shouldBe progressiveRevealInnerContent3
            doc.select(".govuk-button").text().trim shouldBe "Continue"
          }

          s"[$regime journey] pre pop the selected radio option when user has navigated back and they have a chosen month in their journey" in {
            stubCommonActions()
            EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.instalmentOptions(fakeRequest)
            val doc: Document = Jsoup.parse(contentAsString(result))

            RequestAssertions.assertGetRequestOk(result)
            doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "2"
          }

        }

        "POST /how-many-months-do-you-want-to-pay-over should" - {

          s"[$regime journey] redirect to instalment summary page when form is valid" in {
            stubCommonActions()
            EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)()
            EssttpBackend.SelectedPaymentPlan.stubUpdateSelectedPlan(
              TdAll.journeyId,
              JourneyJsonTemplates.`Chosen Payment Plan`(origin = origin)
            )

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/how-many-months-do-you-want-to-pay-over"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")
              .withFormUrlEncodedBody(("Instalments", "2"))

            val result: Future[Result] = controller.instalmentOptionsSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.checkPaymentPlanUrl)
            EssttpBackend.SelectedPaymentPlan.verifyUpdateSelectedPlanRequest(TdAll.journeyId)
          }

          s"[$regime journey] display correct error message when form is submitted with no value" in {
            stubCommonActions()
            EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/how-many-months-do-you-want-to-pay-over"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val result: Future[Result] = controller.instalmentOptionsSubmit(fakeRequest)
            val doc: Document = Jsoup.parse(contentAsString(result))

            RequestAssertions.assertGetRequestOk(result)

            doc.title() shouldBe s"Error: ${expectedPageTitle(taxRegime)}"
            val errorSummary = doc.select(".govuk-error-summary")
            val errorLink = errorSummary.select("a")
            errorLink.text() shouldBe "Select how many months you want to pay over"
            errorLink.attr("href") shouldBe "#Instalments"

            EssttpBackend.SelectedPaymentPlan.verifyNoneUpdateSelectedPlanRequest(TdAll.journeyId)
          }

        }
    }

  "InstalmentsController.retrieveInstalmentOptions" - {

    "should filter out any instalments that are less than £1" in {
      val plansIncludingOneLessThanAPound: List[PaymentPlan] = List(
        TdAll.paymentPlan(1, amountDue = AmountDue(AmountInPence(1))),
        TdAll.paymentPlan(2, amountDue = AmountDue(AmountInPence(99))),
        TdAll.paymentPlan(3, amountDue = AmountDue(AmountInPence(100))),
        TdAll.paymentPlan(4, amountDue = AmountDue(AmountInPence(101))),
        TdAll.paymentPlan(5, amountDue = AmountDue(AmountInPence(999)))
      )
      val result: List[InstalmentOption] = InstalmentsController.retrieveInstalmentOptions(plansIncludingOneLessThanAPound)
      result.length shouldBe 3
      result.foreach(instalmentOption => (instalmentOption.amountToPayEachMonth.value >= 100) shouldBe true)
    }

  }

}
