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
import essttp.rootmodel.ttp.eligibility.{AssessmentCategory, MainTrans}
import messages.ChargeTypeMessages.chargeFromMTrans
import models.Languages
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.must
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Result}
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

  private def mockFindJourney(
    eligibilityAssessmentCategories: Seq[AssessmentCategory],
    assessmentCategory:              Option[AssessmentCategory],
    origin:                          Origin = Origins.Epaye.Bta,
    affordabilityEnabled:            Boolean = false,
    ddInProgress:                    Option[Boolean] = None
  ) =
    assessmentCategory.fold(
      EssttpBackend.EligibilityCheck
        .findJourney(
          testCrypto,
          origin,
          assessmentCategories = eligibilityAssessmentCategories,
          affordabilityEnabled = affordabilityEnabled,
          maybeDdInProgress = ddInProgress
        )()
    )(a =>
      EssttpBackend.DetermineAssessmentCategory.findJourney(
        testCrypto,
        origin,
        assessmentCategory = a,
        eligibilityResultAssessmentCategories = eligibilityAssessmentCategories,
        affordabilityEnabled = affordabilityEnabled,
        maybeDdInProgress = ddInProgress
      )()
    )

  def computeNextPageBehaviour(
    action:                          Action[AnyContent],
    eligibilityAssessmentCategories: Seq[AssessmentCategory],
    assessmentCategory:              AssessmentCategory
  ) = {
    "redirect to the 'can you make an upfront payment' page when affordability is not enabled in the journey" in {
      stubCommonActions()
      mockFindJourney(eligibilityAssessmentCategories, Some(assessmentCategory))
      EssttpBackend.WhyCannotPayInFull.stubUpdateWhyCannotPayInFull(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.AnswerNotRequired,
        JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Vat.Bta)(using testCrypto)
      )

      val result = action(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)

      EssttpBackend.WhyCannotPayInFull
        .verifyUpdateWhyCannotPayInFullRequest(TdAll.journeyId, WhyCannotPayInFullAnswers.AnswerNotRequired)
    }

    "redirect to the 'why can't you pay in full' page when affordability is enabled in the journey" in {
      stubCommonActions()
      mockFindJourney(eligibilityAssessmentCategories, Some(assessmentCategory), affordabilityEnabled = true)

      val result = action(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.whyCannotPayInFull)
    }

    "redirect to You already have a direct debit page when there is a ddInProgress" in {
      stubCommonActions()
      stubCommonActions()
      mockFindJourney(eligibilityAssessmentCategories, Some(assessmentCategory), ddInProgress = Some(true))

      val result = action(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.youAlreadyHaveDirectDebit)
    }

  }

  "GET /your-bill should" - {

    Seq(
      (Seq(AssessmentCategory.Standard), Some(AssessmentCategory.Standard)),
      (Seq(AssessmentCategory.Debts), Some(AssessmentCategory.Debts)),
      (Seq(AssessmentCategory.Debts, AssessmentCategory.Liabilities, AssessmentCategory.DebtsAndLiabilities), None)
    ).foreach { (eligibilityAssessmentCategories, journeyAssessmentCategory) =>
      "return your bill page for EPAYE for interest bearing charges when the eligibility check result has assessment " +
        s"categories ${eligibilityAssessmentCategories.mkString(", ")} and the determined assessment category is $journeyAssessmentCategory" in {
          stubCommonActions()

          mockFindJourney(eligibilityAssessmentCategories, journeyAssessmentCategory)

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

          tableRows(1)
            .select(".govuk-summary-list__key")
            .text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
          tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"
        }
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
        .text() shouldBe "Here, you can view the total of all your Simple Assessment debts. In your Personal Tax Account, you can only view your debts from the last 4 tax years."
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

    "redirect to the 'your upcoming bill' page if the assessment category is liabilities only" in {
      stubCommonActions()
      EssttpBackend.DetermineAssessmentCategory
        .findJourney(
          testCrypto,
          Origins.Epaye.Bta,
          assessmentCategory = AssessmentCategory.Liabilities,
          eligibilityResultAssessmentCategories = Seq(AssessmentCategory.Liabilities)
        )()

      val result = controller.yourBill(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.YourBillController.yourUpcomingBill.url)
    }
  }

  "POST /your-bill should" - {
    behave like computeNextPageBehaviour(
      controller.yourBillSubmit,
      Seq(AssessmentCategory.Standard),
      AssessmentCategory.Standard
    )
  }

  "GET /your-upcoming-tax-bill should" - {

    "return your bill page for SIMP" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck
        .findJourney(testCrypto, Origins.Simp.Pta, assessmentCategories = Seq(AssessmentCategory.Liabilities))()

      val result: Future[Result] = controller.yourUpcomingBill(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your upcoming Simple Assessment tax bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourUpcomingBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Simp)
      )

      val para = doc.select("p.govuk-body")
      para.text shouldBe "You have no overdue payments. Instead, you can set up a payment plan for an upcoming tax " +
        "bill, allowing you to pay in advance instalments."

      val h2 = doc.select("h2.govuk-heading-s")
      h2.text shouldBe "Advance payments"

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"

    }

    "return your bill page for SIMP in welsh" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck
        .findJourney(testCrypto, Origins.Simp.Pta, assessmentCategories = Seq(AssessmentCategory.Liabilities))()

      val result: Future[Result] = controller.yourUpcomingBill(fakeRequest.withLangWelsh())
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Eich bil treth Asesiad Syml sydd i ddod yw £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourUpcomingBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Simp),
        language = Languages.Welsh
      )

      val para = doc.select("p.govuk-body")
      para.text shouldBe "Nid oes gennych daliadau sy’n hwyr. Yn lle hynny, gallwch sefydlu cynllun talu ar gyfer bil " +
        "treth sydd i ddod, a gallwch ei dalu fesul rhandaliad."

      val h2 = doc.select("h2.govuk-heading-s")
      h2.text shouldBe "Taliadau ymlaen llaw"

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

    Seq(
      Seq(AssessmentCategory.Standard),
      Seq(AssessmentCategory.Debts),
      Seq(AssessmentCategory.DebtsAndLiabilities, AssessmentCategory.Debts, AssessmentCategory.Liabilities)
    ).foreach { assessmentCategories =>
      s"redirect to the determine assessment categories endpoint if the assessment categories are (${assessmentCategories
          .map(_.entryName)
          .mkString(", ")})" in {
        stubCommonActions()
        EssttpBackend.EligibilityCheck
          .findJourney(testCrypto, Origins.Epaye.Bta, assessmentCategories = assessmentCategories)()

        val result = controller.yourUpcomingBill(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(
          routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
        )
      }
    }

  }

  "POST /your-upcoming-bill should" - {
    behave like computeNextPageBehaviour(
      controller.yourUpcomingBillSubmit,
      Seq(AssessmentCategory.Liabilities),
      AssessmentCategory.Liabilities
    )
  }

  "GET /your-bill-combined should" - {

    "return your bill page for SIMP" in {
      stubCommonActions()
      EssttpBackend.DetermineAssessmentCategory
        .findJourney(
          testCrypto,
          Origins.Simp.Pta,
          assessmentCategory = AssessmentCategory.DebtsAndLiabilities,
          eligibilityResultAssessmentCategories =
            Seq(AssessmentCategory.Debts, AssessmentCategory.Liabilities, AssessmentCategory.DebtsAndLiabilities)
        )()

      val result: Future[Result] = controller.yourBillCombined(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your total Simple Assessment tax bill is £6,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillCombinedSubmit.url),
        regimeBeingTested = Some(TaxRegime.Simp)
      )

      val h1 = doc.selectFirst("h1.govuk-heading-l")

      val overduePaymentsH2 = h1.nextElementSibling()
      overduePaymentsH2.is("h2.govuk-heading-m") shouldBe true
      overduePaymentsH2.text() shouldBe "Overdue payments"

      val overduePaymentsTable = overduePaymentsH2.nextElementSibling()
      overduePaymentsTable.is("dl.govuk-summary-list") shouldBe true

      val overduePaymentsTableRows = overduePaymentsTable.select(".govuk-summary-list__row").asScala.toList
      overduePaymentsTableRows.size shouldBe 2

      overduePaymentsTableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      overduePaymentsTableRows(0)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£2,000 (includes interest added to date)"

      overduePaymentsTableRows(1)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      overduePaymentsTableRows(1)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£1,000 (includes interest added to date)"

      val advancePaymentsH2 = overduePaymentsTable.nextElementSibling()
      advancePaymentsH2.is("h2.govuk-heading-m") shouldBe true
      advancePaymentsH2.text() shouldBe "Advance payments"

      val advancePaymentsTable = advancePaymentsH2.nextElementSibling()
      advancePaymentsTable.is("dl.govuk-summary-list") shouldBe true

      val advancePaymentsTableRows = advancePaymentsTable.select(".govuk-summary-list__row").asScala.toList
      advancePaymentsTableRows.size shouldBe 2

      advancePaymentsTableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      advancePaymentsTableRows(0)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£2,000 (includes interest added to date)"

      advancePaymentsTableRows(1)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      advancePaymentsTableRows(1)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£1,000 (includes interest added to date)"

      val buttonGroup = doc.selectFirst("div.govuk-button-group")
      buttonGroup.child(0).text() shouldBe "Continue"

      buttonGroup.child(1).is("a.govuk-link") shouldBe true
      buttonGroup.child(1).attr("href") shouldBe routes.YourBillController.removeAdvancePayments.url
      buttonGroup.child(1).text shouldBe "Remove advance payments"
    }

    "return your bill page for SIMP in welsh" in {
      stubCommonActions()
      EssttpBackend.DetermineAssessmentCategory
        .findJourney(
          testCrypto,
          Origins.Simp.Pta,
          assessmentCategory = AssessmentCategory.DebtsAndLiabilities,
          eligibilityResultAssessmentCategories =
            Seq(AssessmentCategory.Debts, AssessmentCategory.Liabilities, AssessmentCategory.DebtsAndLiabilities)
        )()

      val result: Future[Result] = controller.yourBillCombined(fakeRequest.withLangWelsh())
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Cyfanswm eich bil treth Asesiad Syml yw £6,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillCombinedSubmit.url),
        regimeBeingTested = Some(TaxRegime.Simp),
        language = Languages.Welsh
      )

      val h1 = doc.selectFirst("h1.govuk-heading-l")

      val overduePaymentsH2 = h1.nextElementSibling()
      overduePaymentsH2.is("h2.govuk-heading-m") shouldBe true
      overduePaymentsH2.text() shouldBe "Taliadau sy’n hwyr"

      val overduePaymentsTable = overduePaymentsH2.nextElementSibling()
      overduePaymentsTable.is("dl.govuk-summary-list") shouldBe true

      val overduePaymentsTableRows = overduePaymentsTable.select(".govuk-summary-list__row").asScala.toList
      overduePaymentsTableRows.size shouldBe 2

      overduePaymentsTableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Gorff 2020 i 14 Gorff 2020 Bil yn ddyledus 7 Chwefror 2017"
      overduePaymentsTableRows(0)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£2,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"

      overduePaymentsTableRows(1)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Awst 2020 i 14 Awst 2020 Bil yn ddyledus 7 Mawrth 2017"
      overduePaymentsTableRows(1)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£1,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"

      val advancePaymentsH2 = overduePaymentsTable.nextElementSibling()
      advancePaymentsH2.is("h2.govuk-heading-m") shouldBe true
      advancePaymentsH2.text() shouldBe "Taliadau ymlaen llaw"

      val advancePaymentsTable = advancePaymentsH2.nextElementSibling()
      advancePaymentsTable.is("dl.govuk-summary-list") shouldBe true

      val advancePaymentsTableRows = advancePaymentsTable.select(".govuk-summary-list__row").asScala.toList
      advancePaymentsTableRows.size shouldBe 2

      advancePaymentsTableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Gorff 2020 i 14 Gorff 2020 Bil yn ddyledus 7 Chwefror 2017"
      advancePaymentsTableRows(0)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£2,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"

      advancePaymentsTableRows(1)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Awst 2020 i 14 Awst 2020 Bil yn ddyledus 7 Mawrth 2017"
      advancePaymentsTableRows(1)
        .select(".govuk-summary-list__value")
        .text() shouldBe "£1,000 (yn cynnwys llog a ychwanegwyd hyd yn hyn)"

      val buttonGroup = doc.selectFirst("div.govuk-button-group")
      buttonGroup.child(0).text() shouldBe "Yn eich blaen"

      buttonGroup.child(1).is("a.govuk-link") shouldBe true
      buttonGroup.child(1).attr("href") shouldBe routes.YourBillController.removeAdvancePayments.url
      buttonGroup.child(1).text shouldBe "Tynnu taliadau ymlaen llaw"

    }

    Seq(
      Seq(AssessmentCategory.Standard),
      Seq(AssessmentCategory.Debts),
      Seq(AssessmentCategory.DebtsAndLiabilities, AssessmentCategory.Debts, AssessmentCategory.Liabilities)
    ).foreach { assessmentCategories =>
      s"redirect to the determine assessment categories endpoint if the assessment categories are (${assessmentCategories
          .map(_.entryName)
          .mkString(", ")})" in {
        stubCommonActions()
        EssttpBackend.EligibilityCheck
          .findJourney(testCrypto, Origins.Epaye.Bta, assessmentCategories = assessmentCategories)()

        val result = controller.yourUpcomingBill(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(
          routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
        )
      }
    }

  }

  "POST /your-bill-combined should" - {
    behave like computeNextPageBehaviour(
      controller.yourBillCombinedSubmit,
      Seq(AssessmentCategory.DebtsAndLiabilities, AssessmentCategory.Debts, AssessmentCategory.Liabilities),
      AssessmentCategory.DebtsAndLiabilities
    )
  }

  "GET /advance-payments should" - {

    def testPage(checkedOption: Option[Boolean]) = {
      val result = controller.advancePayment(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val doc = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        "Advance payments",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.advancePaymentSubmit.url)
      )

      doc
        .select("p.govuk-body")
        .text() shouldBe "You can add an upcoming tax bill to your payment plan if you want to pay in advance instalments."

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0)
        .select(".govuk-summary-list__key")
        .text() shouldBe "13 Jul 2020 to 14 Jul 2020 Bill due 7 February 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Aug 2020 to 14 Aug 2020 Bill due 7 March 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"

      doc
        .select(".govuk-form-group > .govuk-fieldset > legend")
        .text() shouldBe "Do you want to add this upcoming tax bill to your payment plan?"

      val radioItems = doc.select(".govuk-radios__item").asScala.toList
      radioItems.map(r =>
        (
          r.select(".govuk-radios__input").attr("value"),
          r.select(".govuk-radios__label").text(),
          r.select(".govuk-radios__input").hasAttr("checked")
        )
      ) shouldBe List(
        ("Yes", "Yes", checkedOption.contains(true)),
        ("No", "No", checkedOption.contains(false))
      )
    }

    "display the page when the eligibility check result has debts and liabilities" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(
        testCrypto,
        Origins.Epaye.Bta,
        assessmentCategories =
          Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
      )()

      testPage(None)
    }

    "preselect 'yes' if the assessment category in the journey has been determined and is 'debts and liabilities'" in {
      stubCommonActions()
      EssttpBackend.DetermineAssessmentCategory.findJourney(
        testCrypto,
        Origins.Epaye.Bta,
        eligibilityResultAssessmentCategories =
          Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities),
        assessmentCategory = AssessmentCategory.DebtsAndLiabilities
      )()

      testPage(Some(true))
    }

    "preselect 'no' if the assessment category in the journey has been determined and is 'debts'" in {
      stubCommonActions()
      EssttpBackend.DetermineAssessmentCategory.findJourney(
        testCrypto,
        Origins.Epaye.Bta,
        eligibilityResultAssessmentCategories =
          Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities),
        assessmentCategory = AssessmentCategory.Debts
      )()

      testPage(Some(false))
    }

    Seq(
      Seq(AssessmentCategory.Standard),
      Seq(AssessmentCategory.Debts),
      Seq(AssessmentCategory.Liabilities)
    ).foreach { assessmentCategories =>
      s"redirect to the determine assessment categories endpoint if the assessment categories are (${assessmentCategories
          .map(_.entryName)
          .mkString(", ")})" in {
        stubCommonActions()
        EssttpBackend.EligibilityCheck
          .findJourney(testCrypto, Origins.Epaye.Bta, assessmentCategories = assessmentCategories)()

        val result = controller.advancePayment(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(
          routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
        )
      }
    }

  }

  "POST /advance-payments should" - {

    Languages.values.foreach { lang =>
      s"return a form error if nothing is submitted in ${lang.toString}" in {
        stubCommonActions()
        EssttpBackend.EligibilityCheck.findJourney(
          testCrypto,
          Origins.Simp.Pta,
          assessmentCategories =
            Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
        )()

        val result = controller.advancePaymentSubmit(
          fakeRequest.withFormUrlEncodedBody().withMethod("POST").withLang(lang)
        )
        status(result) shouldBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        ContentAssertions.commonPageChecks(
          doc,
          lang.fold("Advance payments", "Taliadau ymlaen llaw"),
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.YourBillController.advancePaymentSubmit.url),
          hasFormError = true,
          regimeBeingTested = Some(TaxRegime.Simp),
          language = lang
        )

        val errorSummary = doc.select(".govuk-error-summary")
        val errorLink    = errorSummary.select("a")
        errorLink.text() shouldBe lang.fold(
          "Select yes if you want to add this upcoming tax bill to your payment plan",
          "Dewiswch ‘Iawn’ os ydych am ychwanegu’r bil treth hwn sydd i ddod at eich cynllun talu"
        )
        errorLink.attr("href") shouldBe "#advancePayments"
        EssttpBackend.DetermineAssessmentCategory.verifyAssessmentCategoryUpdateNotCalled(TdAll.journeyId)
      }
    }

    Seq(
      ("Yes", AssessmentCategory.DebtsAndLiabilities, routes.YourBillController.yourBillCombined),
      ("No", AssessmentCategory.Debts, routes.YourBillController.yourBill)
    ).foreach { (formValue, expectedAssessmentCategory, expectedRedirect) =>
      s"update the journey and redirect to '${expectedRedirect.url}' if the user submits '$formValue'" in {
        stubCommonActions()
        EssttpBackend.DetermineAssessmentCategory.stubUpdateAssessmentCategory(
          TdAll.journeyId,
          JourneyJsonTemplates.`Assessment Category Determined`(
            Origins.Simp.Pta,
            assessmentCategory = expectedAssessmentCategory,
            eligibilityResultAssessmentCategories =
              Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
          )
        )
        EssttpBackend.EligibilityCheck.findJourney(
          testCrypto,
          Origins.Simp.Pta,
          assessmentCategories =
            Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
        )()

        val result = controller.advancePaymentSubmit(
          fakeRequest.withFormUrlEncodedBody("advancePayments" -> formValue).withMethod("POST")
        )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirect.url)
        EssttpBackend.DetermineAssessmentCategory.verifyAssessmentCategoryRequest(
          TdAll.journeyId,
          expectedAssessmentCategory
        )
      }
    }

  }

  "GET /remove-advance-payments should" - {

    "remove liabilities if debts and liabilities had previously been selected" in {
      stubCommonActions()

      EssttpBackend.DetermineAssessmentCategory.stubUpdateAssessmentCategory(
        TdAll.journeyId,
        JourneyJsonTemplates.`Assessment Category Determined`(
          Origins.Simp.Pta,
          assessmentCategory = AssessmentCategory.Debts,
          eligibilityResultAssessmentCategories =
            Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
        )
      )
      EssttpBackend.DetermineAssessmentCategory
        .findJourney(
          testCrypto,
          Origins.Simp.Pta,
          assessmentCategory = AssessmentCategory.DebtsAndLiabilities,
          eligibilityResultAssessmentCategories =
            Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
        )()

      val result = controller.removeAdvancePayments(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.YourBillController.yourBill.url)
      EssttpBackend.DetermineAssessmentCategory.verifyAssessmentCategoryRequest(
        TdAll.journeyId,
        AssessmentCategory.Debts
      )
    }

    "redirect to 'determine assessment category' if the assessment category has not been determined" in {
      stubCommonActions()

      EssttpBackend.EligibilityCheck.findJourney(
        testCrypto,
        Origins.Simp.Pta,
        assessmentCategories =
          Seq(AssessmentCategory.Liabilities, AssessmentCategory.Debts, AssessmentCategory.DebtsAndLiabilities)
      )()

      val result = controller.removeAdvancePayments(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.DetermineAssessmentCategoryController.determineAssessmentCategory.url
      )
    }

    Seq(
      (AssessmentCategory.Standard, routes.YourBillController.yourBill.url),
      (AssessmentCategory.Debts, routes.YourBillController.yourBill.url),
      (AssessmentCategory.Liabilities, routes.YourBillController.yourUpcomingBill.url)
    ).foreach { (assessmentCategory, expectedRedirect) =>
      s"redirect to the correct page if the assessment category is ${assessmentCategory.entryName}" in {
        stubCommonActions()
        EssttpBackend.DetermineAssessmentCategory
          .findJourney(testCrypto, Origins.Epaye.Bta, assessmentCategory = assessmentCategory)()

        val result = controller.removeAdvancePayments(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirect)
      }

    }

  }

  "GET /you-already-have-a-direct-debit should" - {
    "return You already have a direct debit page for charges with ddInProgress" in {
      stubCommonActions()
      EssttpBackend.DetermineAssessmentCategory
        .findJourney(testCrypto, Origins.Epaye.Bta, maybeDdInProgress = Some(true))()

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
      EssttpBackend.DetermineAssessmentCategory
        .findJourney(testCrypto, Origins.Epaye.Bta, maybeDdInProgress = Some(true))()

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
           |    "chargeTypeAssessments" : [ {
           |      "chargeTypeAssessment": [ ${chargeTypeAssessmentItemJsons.mkString(", ")} ],
           |      "assessmentEligibilityRules": {
           |         "isLessThanMinDebtAllowance" : false,
           |         "isMoreThanMaxDebtAllowance" : false,
           |         "disallowedChargeLockTypes" : false,
           |         "ineligibleChargeTypes" : false,
           |         "noDueDatesReached" : false,
           |         "chargesBeforeMaxAccountingDate": false,
           |         "chargesOverMaxDebtAge": false
           |       },
           |       "assessmentEligibilityStatus": true,
           |       "assessmentCategory": "standard"
           |      } ]
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
