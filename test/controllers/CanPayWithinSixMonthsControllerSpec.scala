/*
 * Copyright 2024 HM Revenue & Customs
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

import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Origins}
import org.jsoup.Jsoup
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class CanPayWithinSixMonthsControllerSpec extends ItSpec {

  val controller = app.injector.instanceOf[CanPayWithinSixMonthsController]

  val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

  "GET /paying-within-six-months should" - {

      def testPageIsDisplayed(result: Future[Result], expectedPreselectedOption: Option[Boolean]): Unit = {
        RequestAssertions.assertGetRequestOk(result)

        val doc = Jsoup.parse(contentAsString(result))

        ContentAssertions.commonPageChecks(
          doc,
          "Paying within 6 months",
          shouldBackLinkBePresent = true,
          expectedSubmitUrl       = Some(routes.CanPayWithinSixMonthsController.canPayWithinSixMonthsSubmit.url)
        )

        doc.select("p.govuk-body").text() shouldBe "If you can afford to pay within 6 months, you’ll pay less interest than on a longer plan."

        val summaryList = doc.select(".govuk-summary-list")
        val summaryListRows = summaryList.select(".govuk-summary-list__row").asScala.toList
        val summaryListRowKeysAndValues = summaryListRows.map(row => row.select(".govuk-summary-list__key").text() -> row.select(".govuk-summary-list__value").text())

        summaryListRowKeysAndValues shouldBe List("Remaining amount to pay" -> "£1,498")

        doc.select(".govuk-form-group > .govuk-fieldset > legend").text() shouldBe "Can you pay within 6 months?"

        val radioItems = doc.select(".govuk-radios__item").asScala.toList
        radioItems.map(r =>
          (
            r.select(".govuk-radios__input").attr("value"),
            r.select(".govuk-radios__label").text(),
            r.select(".govuk-radios__input").hasAttr("checked")
          )) shouldBe List(
          ("Yes", "Yes, I can pay within 6 months", expectedPreselectedOption.contains(true)),
          ("No", "No, I need a longer plan", expectedPreselectedOption.contains(false))
        )
        ()
      }

    "not show the page if the affordability result has not been obtained yet" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()

      val result = controller.canPayWithinSixMonths(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineAffordabilityController.determineAffordability.url)
    }

    "display the page when options have not been previously selected" in {
      stubCommonActions()
      EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)()

      val result = controller.canPayWithinSixMonths(fakeRequest)
      testPageIsDisplayed(result, None)
    }

    "display the page when 'yes' has been previously selected" in {
      stubCommonActions()
      EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(Origins.Epaye.Bta)(testCrypto)
      )

      val result = controller.canPayWithinSixMonths(fakeRequest)
      testPageIsDisplayed(result, Some(true))
    }

    "display the page when 'no' has been previously selected" in {
      stubCommonActions()
      EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
      )

      val result = controller.canPayWithinSixMonths(fakeRequest)
      testPageIsDisplayed(result, Some(false))
    }

  }

  "POST /paying-within-six-months should" - {

    "return a form error when" - {

        def testFormError(formData: (String, String)*)(expectedError: String): Unit = {
          stubCommonActions()
          EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()

          val request = fakeRequest.withFormUrlEncodedBody(formData: _*).withMethod("POST")
          val result = controller.canPayWithinSixMonthsSubmit(request)
          val doc = Jsoup.parse(contentAsString(result))

          ContentAssertions.commonPageChecks(
            doc,
            "Paying within 6 months",
            shouldBackLinkBePresent = true,
            expectedSubmitUrl       = Some(routes.CanPayWithinSixMonthsController.canPayWithinSixMonthsSubmit.url),
            hasFormError            = true
          )

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe expectedError
          errorLink.attr("href") shouldBe "#CanPayWithinSixMonths"
          EssttpBackend.CanPayWithinSixMonths.verifyNoneUpdateCanPayWithinSixMonthsRequest(TdAll.journeyId)
        }

      "nothing is submitted" in {
        testFormError()("Select yes if you can pay within 6 months")
      }

      "an unrecognised option is submitted" in {
        testFormError("CanPayWithinSixMonths" -> "Unknown")("Select yes if you can pay within 6 months")
      }

    }

    "redirect to the 'monthly payment amount' page when the user submits 'yes'" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.CanPayWithinSixMonths.stubUpdateCanPayWithinSixMonths(
        TdAll.journeyId,
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(Origins.Epaye.Bta)(testCrypto)
      )

      val request = fakeRequest.withFormUrlEncodedBody("CanPayWithinSixMonths" -> "Yes").withMethod("POST")
      val result = controller.canPayWithinSixMonthsSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount.url)

      EssttpBackend.CanPayWithinSixMonths.verifyUpdateCanPayWithinSixMonthsRequest(
        TdAll.journeyId,
        CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(value = true)
      )
    }

    "redirect to the PEGA start endpoint when the user submits 'no'" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.CanPayWithinSixMonths.stubUpdateCanPayWithinSixMonths(
        TdAll.journeyId,
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
      )

      val request = fakeRequest.withFormUrlEncodedBody("CanPayWithinSixMonths" -> "No").withMethod("POST")
      val result = controller.canPayWithinSixMonthsSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.PegaController.startPegaJourney.url)

      EssttpBackend.CanPayWithinSixMonths.verifyUpdateCanPayWithinSixMonthsRequest(
        TdAll.journeyId,
        CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(value = false)
      )
    }

  }
}
