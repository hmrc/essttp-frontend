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
import essttp.rootmodel.TaxRegime
import models.Languages
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls}
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class YourBillControllerSpec extends ItSpec {

  private val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "GET /your-bill should" - {
    "return your bill page for EPAYE for interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your PAYE bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
    }

    "return your bill page for EPAYE for non-interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithNoInterestBearingCharges(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your PAYE bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000"
    }

    "return your bill page for VAT for interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your VAT bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested       = Some(TaxRegime.Vat)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
    }

    "return your bill page for VAT for non-interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithNoInterestBearingCharges(testCrypto, Origins.Vat.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your VAT bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested       = Some(TaxRegime.Vat)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000"
    }

    "return your bill page for SA" in {
      val origin = Origins.Sa.Bta
      val journeyJson = eligibleJsonWithChargeTypeAssessmentItems(
        chargeTypeAssessmentItemJson(
          taxPeriodFrom           = LocalDate.of(2020, 4, 4),
          taxPeriodTo             = LocalDate.of(2021, 4, 4),
          isInterestBearingCharge = true,
          dueDate                 = LocalDate.of(2020, 6, 15)
        ),
        chargeTypeAssessmentItemJson(
          taxPeriodFrom           = LocalDate.of(2021, 4, 4),
          taxPeriodTo             = LocalDate.of(2022, 4, 4),
          isInterestBearingCharge = false,
          dueDate                 = LocalDate.of(2021, 7, 13)
        )
      )(origin)

      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin)(journeyJson)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your Self Assessment tax bill is £20,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested       = Some(TaxRegime.Sa)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "Due 15 June 2020 Balancing payment for tax year 2020 to 2021"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£10,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "Due 13 July 2021 Balancing payment for tax year 2021 to 2022"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£10,000"
    }
  }

  "POST /your-bill should" - {
    "redirect to can you make an upfront payment question page" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)
    }

    "redirect to You already have a direct debit page when there is a ddInProgress" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.youAlreadyHaveDirectDebit)
    }
  }

  "GET /you-already-have-a-direct-debit should" - {
    "return You already have a direct debit page for charges with ddInProgress" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.youAlreadyHaveDirectDebit(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "You already have a Direct Debit",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
      )

      ContentAssertions.assertListOfContent(
        elements = doc.select(".govuk-body")
      )(
          expectedContent = List(
            "You already have a Direct Debit set up for Employers’ PAYE.",
            "If you set up a payment plan, the following charges could be collected twice.",
            "If you select ‘continue’ you understand that you may be charged twice if you do not contact your bank."
          )
        )

      ContentAssertions.assertListOfContent(
        elements = doc.select(".govuk-warning-text__text")
      )(
          expectedContent = List(
            "Warning Contact your bank to discuss your payment options before setting up a payment plan."
          )
        )

      ContentAssertions.assertListOfContent(
        elements = doc.select("#link")
      )(
          expectedContent = List(
            "I do not want to set up a payment plan"
          )
        )

      val backLink = doc.select("#kickout")
      backLink.attr("href") shouldBe routes.IneligibleController.epayeYouHaveChosenNotToSetUpPage.url

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
    }

    "return You already have a direct debit page for charges with ddInProgress in Welsh" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId").withLangWelsh()

      val result = controller.youAlreadyHaveDirectDebit(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Mae eisoes gennych drefniant Debyd Uniongyrchol",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url),
        language                = Languages.Welsh
      )

      ContentAssertions.assertListOfContent(
        elements = doc.select(".govuk-body")
      )(
          expectedContent = List(
            "Mae eisoes gennych drefniant Debyd Uniongyrchol er mwyn talu TWE y Cyflogwr.",
            "Os ydych yn trefnu cynllun talu, mae’n bosibl y gall y taliadau hyn gael eu casglu ddwywaith.",
            "Os dewiswch yr opsiwn i fynd yn eich blaen cyn cysylltu â’ch banc, rydych yn deall ei bod yn bosibl y gall taliadau gael eu casglu ddwywaith."
          )
        )

      ContentAssertions.assertListOfContent(
        elements = doc.select(".govuk-warning-text__text")
      )(
          expectedContent = List(
            "Rhybudd Dylech gysylltu â’ch banc i drafod eich opsiynau talu cyn i chi drefnu cynllun talu."
          )
        )

      ContentAssertions.assertListOfContent(
        elements = doc.select("#link")
      )(
          expectedContent = List(
            "Nid wyf am drefnu cynllun talu"
          )
        )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Gorff 2020 i 14 Gorff 2020 Bil yn ddyledus 7 Chwefror 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Awst 2020 i 14 Awst 2020 Bil yn ddyledus 7 Mawrth 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"
    }
  }

  "POST /you-already-have-a-direct-debit should" - {
    "redirect to can you make an upfront payment question page" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Vat.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.youAlreadyHaveDirectDebitSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)
    }
  }

  def eligibleJsonWithChargeTypeAssessmentItems(chargeTypeAssessmentItemJsons: String*)(origin: Origin): String = {
    val json = Json.parse(JourneyJsonTemplates.`Eligibility Checked - Eligible`(origin)(testCrypto)).as[JsObject]

    json.deepMerge(
      Json.parse(
        s"""{
           |  "EligibilityChecked": {
           |    "eligibilityCheckResult": {
           |      "chargeTypeAssessment": [ ${chargeTypeAssessmentItemJsons.mkString(", ")} ]
           |    }
           |  }
           |}
           |""".stripMargin
      ).as[JsObject]
    ).toString
  }

  def chargeTypeAssessmentItemJson(
      taxPeriodFrom:           LocalDate,
      taxPeriodTo:             LocalDate,
      isInterestBearingCharge: Boolean,
      dueDate:                 LocalDate
  ): String =
    s"""{
       |  "taxPeriodFrom" : "${DateTimeFormatter.ISO_DATE.format(taxPeriodFrom)}",
       |  "taxPeriodTo" : "${DateTimeFormatter.ISO_DATE.format(taxPeriodTo)}",
       |  "debtTotalAmount" : 1000000,
       |  "chargeReference" : "A00000000001",
       |  "charges" : [
       |    {
       |        "chargeType" : "InYearRTICharge-Tax",
       |        "mainType" : "InYearRTICharge(FPS)",
       |        "chargeReference" : "9000064909",
       |        "mainTrans" : "mainTrans",
       |        "subTrans" : "subTrans",
       |        "outstandingAmount" : 1000000,
       |        "interestStartDate" : "2017-03-07",
       |        "isInterestBearingCharge": ${isInterestBearingCharge.toString},
       |        "dueDate" : "${DateTimeFormatter.ISO_DATE.format(dueDate)}",
       |        "accruedInterest" : 0,
       |        "ineligibleChargeType" : false,
       |        "chargeOverMaxDebtAge" : false,
       |        "locks" : [
       |            {
       |                "lockType" : "Payment",
       |                "lockReason" : "Risk/Fraud",
       |                "disallowedChargeLockType" : false
       |            }
       |        ],
       |        "dueDateNotReached" : false
       |    }
       |  ]
       |}
       |""".stripMargin

}

