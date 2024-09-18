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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.{AmountInPence, TaxRegime}
import essttp.rootmodel.ttp.affordablequotes.{AmountDue, PaymentPlan}
import models.{InstalmentOption, Languages}
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
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}

class InstalmentsControllerSpec extends ItSpec {
  private val controller: InstalmentsController = app.injector.instanceOf[InstalmentsController]

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat),
    ("SA", Origins.Sa.Bta, TaxRegime.Sa),
    ("SIA", Origins.Sia.Pta, TaxRegime.Sia)
  ).foreach {
      case (regime, origin, taxRegime) =>

        "GET /how-many-months-do-you-want-to-pay-over should" - {

          s"[regime $regime] return an error when" - {

            "the journey is in state" - {

              "AfterStartedPegaCase" in {
                stubCommonActions()
                EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)()

                val exception = intercept[UpstreamErrorResponse](await(controller.instalmentOptions(fakeRequest)))

                exception.statusCode shouldBe INTERNAL_SERVER_ERROR
                exception.message shouldBe "Not expecting to select payment plan option when started PEGA case"
              }

              "AfterCheckedPaymentPlan on an affordability journey" in {
                stubCommonActions()
                EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)()

                val exception = intercept[UpstreamErrorResponse](await(controller.instalmentOptions(fakeRequest)))

                exception.statusCode shouldBe INTERNAL_SERVER_ERROR
                exception.message shouldBe "Not expecting to select payment plan option when payment plan has been checked on affordability journey"
              }

            }

          }

          s"[$regime journey] return 200 and the instalment selection page when" - {

              def test(stubFindJourney: () => StubMapping): Unit = {
                stubCommonActions()
                stubFindJourney()

                val result: Future[Result] = controller.instalmentOptions(fakeRequest)
                val pageContent: String = contentAsString(result)
                val doc: Document = Jsoup.parse(pageContent)

                RequestAssertions.assertGetRequestOk(result)
                ContentAssertions.commonPageChecks(
                  doc,
                  expectedH1              = "Select a payment plan",
                  shouldBackLinkBePresent = true,
                  expectedSubmitUrl       = Some(routes.InstalmentsController.instalmentOptionsSubmit.url),
                  regimeBeingTested       = Some(taxRegime)
                )

                doc.select("p.govuk-body").first().text() shouldBe "Based on what you can pay each month, you can now select a payment plan."

                val details = doc.select(".govuk-details")
                details.select(".govuk-details__summary-text").text() shouldBe "How we calculate interest"

                val detailsParagraphs = details.select("p.govuk-body").asScala.toList
                detailsParagraphs.size shouldBe 3

                detailsParagraphs(0).text() shouldBe "We charge interest on all overdue amounts."
                detailsParagraphs(1).text() shouldBe "We charge the Bank of England base rate plus 2.5% per year."
                detailsParagraphs(2).text() shouldBe "If the interest rate changes during your payment plan, you may need to settle any difference at the end. " +
                  "We will contact you if this is the case."

                doc.select(".govuk-fieldset__legend").text() shouldBe "How many months do you want to pay over?"

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

                doc.select(".govuk-button").text().trim shouldBe "Continue"
                ()
              }

            "the user has not checked their payment plan yet" in {
              test(() =>
                EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)())
            }

            "the user has checked their payment plan yet on a non-affordability journey" in {
              test(() =>
                EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)())
            }

          }

          s"[$regime journey] return 200 and the instalment selection page in Welsh" in {
            stubCommonActions()
            EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)()

            val result: Future[Result] = controller.instalmentOptions(fakeRequest.withLangWelsh())
            val pageContent: String = contentAsString(result)
            val doc: Document = Jsoup.parse(pageContent)

            RequestAssertions.assertGetRequestOk(result)
            ContentAssertions.commonPageChecks(
              doc,
              expectedH1              = "Dewiswch gynllun talu",
              shouldBackLinkBePresent = true,
              expectedSubmitUrl       = Some(routes.InstalmentsController.instalmentOptionsSubmit.url),
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )

            doc.select("p.govuk-body").first().text() shouldBe "Yn seiliedig at yr hyn y gallwch ei dalu bob mis, gallwch nawr ddewis gynllun talu."

            val details = doc.select(".govuk-details")
            details.select(".govuk-details__summary-text").text() shouldBe "Sut rydym yn cyfrifo llog"

            val detailsParagraphs = details.select("p.govuk-body").asScala.toList
            detailsParagraphs.size shouldBe 3

            detailsParagraphs(0).text() shouldBe "Rydym yn codi llog ar bob swm sy’n hwyr."
            detailsParagraphs(1).text() shouldBe "Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5% y flwyddyn."
            detailsParagraphs(2).text() shouldBe "Os bydd y gyfradd llog yn newid yn ystod eich cynllun talu, efallai bydd yn rhaid i chi setlo unrhyw wahaniaeth ar y diwedd. " +
              "Byddwn yn cysylltu â chi os yw hyn yn wir."

            doc.select(".govuk-fieldset__legend").text() shouldBe "Dros sawl mis yr hoffech dalu?"

            val radioButtonGroup = doc.select(".govuk-radios")
            val individualButtons = radioButtonGroup.select(".govuk-radios__item").asScala.toSeq
            individualButtons.size shouldBe 3
            individualButtons(0).select(".govuk-radios__input").`val`() shouldBe "2"
            individualButtons(0).select(".govuk-radios__label").text() shouldBe "2 mis ar £555.73"
            individualButtons(0).select(".govuk-radios__hint").text() shouldBe "Cyfanswm llog amcangyfrifedig o £0.06"
            individualButtons(1).select(".govuk-radios__input").`val`() shouldBe "3"
            individualButtons(1).select(".govuk-radios__label").text() shouldBe "3 mis ar £370.50"
            individualButtons(1).select(".govuk-radios__hint").text() shouldBe "Cyfanswm llog amcangyfrifedig o £0.09"
            individualButtons(2).select(".govuk-radios__input").`val`() shouldBe "4"
            individualButtons(2).select(".govuk-radios__label").text() shouldBe "4 mis ar £277.88"
            individualButtons(2).select(".govuk-radios__hint").text() shouldBe "Cyfanswm llog amcangyfrifedig o £0.12"

            doc.select(".govuk-button").text().trim shouldBe "Yn eich blaen"
          }

          s"[$regime journey] pre pop the selected radio option when user has navigated back and they have a chosen month in their journey" in {
            stubCommonActions()
            EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

            val result: Future[Result] = controller.instalmentOptions(fakeRequest)
            val doc: Document = Jsoup.parse(contentAsString(result))

            RequestAssertions.assertGetRequestOk(result)
            doc.select(".govuk-radios__input[checked]").iterator().asScala.toList(0).`val`() shouldBe "2"
          }

        }

        "POST /how-many-months-do-you-want-to-pay-over should" - {

          s"[$regime journey] redirect to instalment summary page when form is valid and" - {

              def test(stubFindJourney: () => StubMapping): Unit = {
                stubCommonActions()
                stubFindJourney()
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
                ()
              }

            "the user has not checked their payment plan yet" in {
              test(() => EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)())
            }

            "the user has checked their payment plan on a non-affordability journey" in {
              test(() => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)())
            }
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
