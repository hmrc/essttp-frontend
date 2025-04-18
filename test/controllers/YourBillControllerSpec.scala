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

import essttp.journey.model.{Origin, Origins, WhyCannotPayInFullAnswers}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.MainTrans
import messages.ChargeTypeMessages.chargeFromMTrans
import models.Languages
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.must
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.*
import testsupport.ItSpec
import testsupport.TdRequest.*
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.*

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

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your PAYE bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url)
      )

      doc.select("#simp-extra-para1").asScala.toList shouldBe empty
      doc.select("#simp-extra-para2").asScala.toList shouldBe empty

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
    }

    "return your bill page for EPAYE for non-interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithNoInterestBearingCharges(testCrypto, Origins.Epaye.Bta)()

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your PAYE bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url)
      )

      doc.select("#simp-extra-para1").asScala.toList shouldBe empty
      doc.select("#simp-extra-para2").asScala.toList shouldBe empty

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000"
    }

    "return your bill page for VAT for interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your VAT bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Vat)
      )

      doc.select("#simp-extra-para1").asScala.toList shouldBe empty
      doc.select("#simp-extra-para2").asScala.toList shouldBe empty

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
    }

    "return your bill page for VAT for non-interest bearing charges" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithNoInterestBearingCharges(testCrypto, Origins.Vat.Bta)()

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your VAT bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Vat)
      )

      doc.select("#simp-extra-para1").asScala.toList shouldBe empty
      doc.select("#simp-extra-para2").asScala.toList shouldBe empty

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000"
    }

    "return your bill page for SA for known MainTrans code" in {
      val mTransCodes = List(
        "5060",
        "4910",
        "5050",
        "4950",
        "4990",
        "5210",
        "4920",
        "4930",
        "5190",
        "4960",
        "4970",
        "5010",
        "5020",
        "6010",
        "5110",
        "5120",
        "5130",
        "5080",
        "5100",
        "5070",
        "5140",
        "4940",
        "5150",
        "5160",
        "4980",
        "5170",
        "5200",
        "5071",
        "5180",
        "5090",
        "5030",
        "5040",
        "5073",
        "4000",
        "4001",
        "4002",
        "4003",
        "4026"
      )

      mTransCodes.size shouldBe chargeFromMTrans.size

      for {
        code <- mTransCodes
      } {
        val origin      = Origins.Sa.Bta
        val journeyJson = eligibleJsonWithChargeTypeAssessmentItems(
          chargeTypeAssessmentItemJson(
            taxPeriodFrom = LocalDate.of(2020, 4, 4),
            taxPeriodTo = LocalDate.of(2021, 4, 4),
            isInterestBearingCharge = true,
            dueDate = LocalDate.of(2020, 6, 15),
            mainTrans = MainTrans(code)
          )
        )(origin)

        stubCommonActions()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin)(journeyJson)

        val result: Future[Result] = controller.yourBill(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = "Your Self Assessment tax bill is £10,000",
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url),
          regimeBeingTested = Some(TaxRegime.Sa)
        )

        doc.select("#extra-para1").asScala.toList shouldBe empty
        doc.select("#extra-para2").asScala.toList shouldBe empty

        val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
        tableRows.size shouldBe 1

        tableRows(0)
          .select(".govuk-summary-list__key")
          .text() shouldBe s"Due 15 June 2020 ${chargeFromMTrans(MainTrans(code)).english} for tax year 2020 to 2021"
        tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£10,000 (includes interest added to date)"
      }
    }

    "return your bill page for SIMP" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Simp.Pta)()

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your Simple Assessment tax bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Simp)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"

      val simpExtraPara1 = doc.select("#simp-extra-para1").asScala.toList
      simpExtraPara1.size shouldBe 1
      val simpExtraPara2 = doc.select("#simp-extra-para2").asScala.toList
      simpExtraPara2.size shouldBe 1
      simpExtraPara1(0)
        .text() shouldBe "The figures shown here are accurate but may differ from those showing in your Personal Tax Account."
      simpExtraPara2(0)
        .text() shouldBe "Here, you can view the total of all your Simple Assessment debts. In your Personal Tax Account, you can only view your debts from the last 2 tax years."
    }

    "return sa generic ineligible page" - {
      "for unknown MainTrans code" in {
        val origin      = Origins.Sa.Bta
        val journeyJson = eligibleJsonWithChargeTypeAssessmentItems(
          chargeTypeAssessmentItemJson(
            taxPeriodFrom = LocalDate.of(2020, 4, 4),
            taxPeriodTo = LocalDate.of(2021, 4, 4),
            isInterestBearingCharge = true,
            dueDate = LocalDate.of(2020, 6, 15),
            mainTrans = MainTrans("mainTransNotInTable")
          )
        )(origin)

        stubCommonActions()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin)(journeyJson)

        val result: Future[Result] = controller.yourBill(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IneligibleController.saGenericIneligiblePage.url)
      }

      "for ChargeTypeAssessment containing charges with different MainTrans" in {
        val origin      = Origins.Sa.Bta
        val journeyJson = eligibleJsonWithChargeTypeAssessmentItems(
          chargeTypeAssessmentWithMultipleChargesItemJson(
            taxPeriodFrom = LocalDate.of(2020, 4, 4),
            taxPeriodTo = LocalDate.of(2021, 4, 4),
            isInterestBearingCharge = true,
            dueDate = LocalDate.of(2020, 6, 15),
            mainTrans1 = MainTrans("4910"),
            mainTrans2 = MainTrans("4920")
          )
        )(origin)

        stubCommonActions()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin)(journeyJson)

        val result: Future[Result] = controller.yourBill(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IneligibleController.saGenericIneligiblePage.url)
      }
    }
  }

  "POST /your-bill should" - {
    "redirect to the 'can you make an upfront payment' page when affordability is not enabled in the journey" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()
      EssttpBackend.WhyCannotPayInFull.stubUpdateWhyCannotPayInFull(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.AnswerNotRequired,
        JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Vat.Bta)(using testCrypto)
      )

      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)

      EssttpBackend.WhyCannotPayInFull
        .verifyUpdateWhyCannotPayInFullRequest(TdAll.journeyId, WhyCannotPayInFullAnswers.AnswerNotRequired)
    }

    "redirect to the 'why can't you pay in full' page when affordability is enabled in the journey" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)(
        TdJsonBodies.createJourneyJson(
          stageInfo = StageInfo.eligibilityCheckedEligible,
          journeyInfo = JourneyInfo.eligibilityCheckedEligible(TaxRegime.Vat, testCrypto),
          Origins.Vat.Bta,
          affordabilityEnabled = true
        )
      )

      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.whyCannotPayInFull)
    }

    "redirect to You already have a direct debit page when there is a ddInProgress" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Epaye.Bta)()

      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.youAlreadyHaveDirectDebit)
    }
  }

  "GET /you-already-have-a-direct-debit should" - {
    "return You already have a direct debit page for charges with ddInProgress" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Epaye.Bta)()

      val result              = controller.youAlreadyHaveDirectDebit(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document       = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "You already have a Direct Debit",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.youAlreadyHaveDirectDebitSubmit.url)
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

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
    }

    "return You already have a direct debit page for charges with ddInProgress in Welsh" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Epaye.Bta)()

      val result              = controller.youAlreadyHaveDirectDebit(fakeRequest.withLangWelsh())
      val pageContent: String = contentAsString(result)
      val doc: Document       = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Mae eisoes gennych drefniant Debyd Uniongyrchol",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.youAlreadyHaveDirectDebitSubmit.url),
        language = Languages.Welsh
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

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Gorff 2020 i 14 Gorff 2020 Bil yn ddyledus 7 Chwefror 2017"
      tableRows(0)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£2,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"

      tableRows(1)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Awst 2020 i 14 Awst 2020 Bil yn ddyledus 7 Mawrth 2017"
      tableRows(1)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£1,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"
    }
  }

  "POST /you-already-have-a-direct-debit should" - {

    "redirect to can you make an upfront payment question page when affordability is not enabled in the journey" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourneyWithDdInProgress(testCrypto, Origins.Vat.Bta)()
      EssttpBackend.WhyCannotPayInFull.stubUpdateWhyCannotPayInFull(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.AnswerNotRequired,
        JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Vat.Bta)(using testCrypto)
      )

      val result = controller.youAlreadyHaveDirectDebitSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)

      val expectedTaxDetailsJson = """{ "vrn": "101747001" }"""

      AuditConnectorStub.verifyEventAudited(
        "DirectDebitInProgress",
        Json
          .parse(
            s"""
             |{
             |  "origin": "Bta",
             |  "taxType": "Vat",
             |  "taxDetail": $expectedTaxDetailsJson,
             |  "correlationId": "CorrelationId(8d89a98b-0b26-4ab2-8114-f7c7c81c3059)",
             |  "authProviderId": "GGCredId(authId-999)",
             |  "continueOrExit": "continue"
             |}
             |""".stripMargin
          )
          .as[JsObject]
      )
      EssttpBackend.WhyCannotPayInFull
        .verifyUpdateWhyCannotPayInFullRequest(TdAll.journeyId, WhyCannotPayInFullAnswers.AnswerNotRequired)
    }
  }

  def eligibleJsonWithChargeTypeAssessmentItems(chargeTypeAssessmentItemJsons: String*)(origin: Origin): String = {
    val json = Json.parse(JourneyJsonTemplates.`Eligibility Checked - Eligible`(origin)(using testCrypto)).as[JsObject]

    json
      .deepMerge(
        Json
          .parse(
            s"""{
           |  "EligibilityChecked": {
           |    "eligibilityCheckResult": {
           |      "chargeTypeAssessment": [ ${chargeTypeAssessmentItemJsons.mkString(", ")} ]
           |    }
           |  }
           |}
           |""".stripMargin
          )
          .as[JsObject]
      )
      .toString
  }

  def chargeTypeAssessmentItemJson(
    taxPeriodFrom:           LocalDate,
    taxPeriodTo:             LocalDate,
    isInterestBearingCharge: Boolean,
    dueDate:                 LocalDate,
    mainTrans:               MainTrans
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
       |        "mainTrans" : "${mainTrans.value}",
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

  def chargeTypeAssessmentWithMultipleChargesItemJson(
    taxPeriodFrom:           LocalDate,
    taxPeriodTo:             LocalDate,
    isInterestBearingCharge: Boolean,
    dueDate:                 LocalDate,
    mainTrans1:              MainTrans,
    mainTrans2:              MainTrans
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
         |        "mainTrans" : "${mainTrans1.value}",
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
         |    },
         |        {
         |        "chargeType" : "InYearRTICharge-Tax",
         |        "mainType" : "InYearRTICharge(FPS)",
         |        "chargeReference" : "9000064908",
         |        "mainTrans" : "${mainTrans2.value}",
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
