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

