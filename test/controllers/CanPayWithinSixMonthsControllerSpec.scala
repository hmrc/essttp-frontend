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
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.TaxRegime.Epaye
import models.Languages
import models.Languages.{English, Welsh}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.reusableassertions.{ContentAssertions, PegaRecreateSessionAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class CanPayWithinSixMonthsControllerSpec extends ItSpec, PegaRecreateSessionAssertions {

  val controller = app.injector.instanceOf[CanPayWithinSixMonthsController]
  private def pageContentAsDoc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  "GET /paying-within-six-months should" - {

    def testPageIsDisplayed(result: Future[Result], expectedPreselectedOption: Option[Boolean]): Unit = {
      RequestAssertions.assertGetRequestOk(result)

      val doc = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        "Paying within 6 months",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.CanPayWithinSixMonthsController.canPayWithinSixMonthsSubmit.url)
      )

      doc
        .select("p.govuk-body")
        .text() shouldBe "If you can afford to pay within 6 months, you’ll pay less interest than on a longer plan."

      val summaryList                 = doc.select(".govuk-summary-list")
      val summaryListRows             = summaryList.select(".govuk-summary-list__row").asScala.toList
      val summaryListRowKeysAndValues = summaryListRows.map(row =>
        row.select(".govuk-summary-list__key").text() -> row.select(".govuk-summary-list__value").text()
      )

      summaryListRowKeysAndValues shouldBe List("Remaining amount to pay" -> "£2,998")

      doc.select(".govuk-form-group > .govuk-fieldset > legend").text() shouldBe "Can you pay within 6 months?"

      val radioItems = doc.select(".govuk-radios__item").asScala.toList
      radioItems.map(r =>
        (
          r.select(".govuk-radios__input").attr("value"),
          r.select(".govuk-radios__label").text(),
          r.select(".govuk-radios__input").hasAttr("checked")
        )
      ) shouldBe List(
        ("Yes", "Yes, I can pay within 6 months", expectedPreselectedOption.contains(true)),
        ("No", "No, I need a longer plan", expectedPreselectedOption.contains(false))
      )
      ()
    }

    "not show the page if the affordability result has not been obtained yet" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()

      val result = controller.canPayWithinSixMonths(Epaye, None)(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineAffordabilityController.determineAffordability.url)
    }

    "display the page when options have not been previously selected" in {
      stubCommonActions()
      EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)()

      val result = controller.canPayWithinSixMonths(Epaye, None)(fakeRequest)
      testPageIsDisplayed(result, None)
    }

    "display the page when 'yes' has been previously selected" in {
      stubCommonActions()
      EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(Origins.Epaye.Bta)(using testCrypto)
      )

      val result = controller.canPayWithinSixMonths(Epaye, None)(fakeRequest)
      testPageIsDisplayed(result, Some(true))
    }

    "display the page when 'no' has been previously selected" in {
      stubCommonActions()
      EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
      )

      val result = controller.canPayWithinSixMonths(Epaye, None)(fakeRequest)
      testPageIsDisplayed(result, Some(false))
    }

    "change the language cookie to english if lang=en is supplied as a query parameter" in {
      stubCommonActions()
      EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
      )

      val request = fakeRequest.withLangWelsh().withHeaders(HeaderNames.REFERER -> "blah")
      val result  = controller.canPayWithinSixMonths(Epaye, Some(English))(request)
      cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("en")
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(Epaye, None).url
      )
    }

    "change the language cookie to welsh if lang=cy is supplied as a query parameter" in {
      stubCommonActions()
      EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
      )

      val request = fakeRequest.withLangEnglish().withHeaders(HeaderNames.REFERER -> "bluh")
      val result  = controller.canPayWithinSixMonths(Epaye, Some(Welsh))(request)
      cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("cy")
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(Epaye, None).url
      )
    }

    "not change the language cookie if no lang is supplied as a query parameter" in {
      Languages.values.foreach { lang =>
        stubCommonActions()
        EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
          JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
        )

        val result = controller.canPayWithinSixMonths(Epaye, None)(fakeRequest.withLang(lang))
        cookies(result).get("PLAY_LANG").map(_.value) shouldBe None
        status(result) shouldBe OK
      }
    }

    "display the page in Welsh" in {
      stubCommonActions()
      EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)()

      val result = controller.canPayWithinSixMonths(Epaye, None)(fakeRequest.withLangWelsh())
      RequestAssertions.assertGetRequestOk(result)

      val doc = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        "Talu cyn pen 6 mis",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.CanPayWithinSixMonthsController.canPayWithinSixMonthsSubmit.url),
        language = Welsh
      )

      doc
        .select("p.govuk-body")
        .text() shouldBe "Os gallwch fforddio talu cyn pen 6 mis, byddwch yn talu llai o log nag ar gynllun hirach."

      val summaryList                 = doc.select(".govuk-summary-list")
      val summaryListRows             = summaryList.select(".govuk-summary-list__row").asScala.toList
      val summaryListRowKeysAndValues = summaryListRows.map(row =>
        row.select(".govuk-summary-list__key").text() -> row.select(".govuk-summary-list__value").text()
      )

      summaryListRowKeysAndValues shouldBe List("Swm sy’n weddill i’w dalu" -> "£2,998")

      doc.select(".govuk-form-group > .govuk-fieldset > legend").text() shouldBe "A allwch dalu cyn pen 6 mis?"

      val radioItems = doc.select(".govuk-radios__item").asScala.toList
      radioItems.map(r =>
        (
          r.select(".govuk-radios__input").attr("value"),
          r.select(".govuk-radios__label").text(),
          r.select(".govuk-radios__input").hasAttr("checked")
        )
      ) shouldBe List(
        ("Yes", "Iawn, gallaf dalu cyn pen 6 mis", false),
        ("No", "Na, bydd angen cynllun hirach arnaf ar gyfer talu", false)
      )
    }

    "have the query parameters in the url" in {
      routes.CanPayWithinSixMonthsController
        .canPayWithinSixMonths(
          TaxRegime.Sa,
          Some(Languages.English)
        )
        .url shouldBe "/set-up-a-payment-plan/paying-within-six-months?regime=sa&lang=en"

      routes.CanPayWithinSixMonthsController
        .canPayWithinSixMonths(
          TaxRegime.Vat,
          Some(Languages.Welsh)
        )
        .url shouldBe "/set-up-a-payment-plan/paying-within-six-months?regime=vat&lang=cy"
    }
  }

  List(TaxRegime.Epaye, TaxRegime.Vat, TaxRegime.Sa)
    .foreach(regime =>
      s"[${regime.entryName} journey] GET ${routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(regime, None).url}" - {

        behave like recreateSessionErrorBehaviour(controller.canPayWithinSixMonths(_, None)(_))

        "be able to show the page correctly when no session is found but is successfully recreated" in {
          stubCommonActions()
          EssttpBackend.findByLatestSessionNotFound()
          EssttpBackend.Pega.stubRecreateSession(
            regime,
            Right(Json.parse(JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta)(using testCrypto)))
          )

          val request =
            FakeRequest("GET", s"/p?regime=${regime.toString}")
              .withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")

          val result = controller.canPayWithinSixMonths(regime, None)(request)

          status(result) shouldBe OK

          val pageH1 = pageContentAsDoc(result).getElementsByClass("govuk-heading-xl").text()
          pageH1 shouldBe "Paying within 6 months"

          EssttpBackend.verifyFindByLatestSessionId()
          EssttpBackend.Pega.verifyRecreateSessionCalled(regime)
        }
      }
    )

  "POST /paying-within-six-months should" - {

    "return a form error when" - {

      def testFormError(formData: (String, String)*)(expectedError: String): Unit = {
        stubCommonActions()
        EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()

        val request = fakeRequest.withFormUrlEncodedBody(formData*).withMethod("POST")
        val result  = controller.canPayWithinSixMonthsSubmit(request)
        val doc     = Jsoup.parse(contentAsString(result))

        ContentAssertions.commonPageChecks(
          doc,
          "Paying within 6 months",
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.CanPayWithinSixMonthsController.canPayWithinSixMonthsSubmit.url),
          hasFormError = true
        )

        val errorSummary = doc.select(".govuk-error-summary")
        val errorLink    = errorSummary.select("a")
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

    "redirect to the 'monthly payment amount' page when the user submits 'yes' and audit event" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.CanPayWithinSixMonths.stubUpdateCanPayWithinSixMonths(
        TdAll.journeyId,
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(Origins.Epaye.Bta)(using testCrypto)
      )

      val request = fakeRequest.withFormUrlEncodedBody("CanPayWithinSixMonths" -> "Yes").withMethod("POST")
      val result  = controller.canPayWithinSixMonthsSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount.url)

      EssttpBackend.CanPayWithinSixMonths.verifyUpdateCanPayWithinSixMonthsRequest(
        TdAll.journeyId,
        CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(value = true)
      )

      AuditConnectorStub.verifyEventAudited(
        auditType = "CanUserPayInSixMonths",
        auditEvent = Json
          .parse(
            s"""
             |{
             |  "regime" : "Epaye",
             |  "taxIdentifier" : "864FZ00049",
             |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
             |   "userEnteredDetails" : {
             |     "unableToPayReason": ["WaitingForRefund", "NoMoneySetAside"],
             |     "payUpfront" : true,
             |     "upfrontPaymentAmount" : 2,
             |     "canPayInSixMonths" : true
             |   }
             |}
            """.stripMargin
          )
          .as[JsObject]
      )

    }

    "redirect to the PEGA start endpoint when the user submits 'no' and no audit event" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.CanPayWithinSixMonths.stubUpdateCanPayWithinSixMonths(
        TdAll.journeyId,
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
      )

      val request = fakeRequest.withFormUrlEncodedBody("CanPayWithinSixMonths" -> "No").withMethod("POST")
      val result  = controller.canPayWithinSixMonthsSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.PegaController.startPegaJourney.url)

      EssttpBackend.CanPayWithinSixMonths.verifyUpdateCanPayWithinSixMonthsRequest(
        TdAll.journeyId,
        CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(value = false)
      )

      AuditConnectorStub.verifyNoAuditEvent()
    }
    "not redirect to the PEGA start endpoint if the user has not changed their answer after the pega journey has been started" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.CanPayWithinSixMonths.stubUpdateCanPayWithinSixMonths(
        TdAll.journeyId,
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
      )
      EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(
          Origins.Epaye.Bta
        )(using testCrypto)
      )

      val request = fakeRequest.withFormUrlEncodedBody("CanPayWithinSixMonths" -> "No").withMethod("POST")
      val result  = controller.canPayWithinSixMonthsSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/test-only/pega/start?regime=epaye")

      EssttpBackend.CanPayWithinSixMonths.verifyUpdateCanPayWithinSixMonthsRequest(
        TdAll.journeyId,
        CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(value = false)
      )
    }
  }
}
