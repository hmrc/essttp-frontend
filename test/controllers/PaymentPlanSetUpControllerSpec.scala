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

import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.*
import testsupport.reusableassertions.ContentAssertions.assertKeyAndValue
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls}
import testsupport.{ItSpec, JsonUtils}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IterableHasAsScala

class PaymentPlanSetUpControllerWithResearchBannerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Boolean] = Map("features.user-research-banner-enabled" -> true)
  private val controller: PaymentPlanSetUpController      = app.injector.instanceOf[PaymentPlanSetUpController]
  private val expectedH1PaymentPlanSetUpPage: String      = "Your payment plan is set up"
  private val expectedH1PaymentPlanPrintPage: String      = "Your payment plan"
  private val expectedH1PaymentPlanPrintPageSa: String    = "Confirmation of plan to pay £1,111.47"

  def testUserResearchBannerIsPresent(doc: Document): Unit = {
    val bannerContainer = doc.select("div.hmrc-user-research-banner > div.hmrc-user-research-banner__container")
    val bannerText      = bannerContainer.select("div.hmrc-user-research-banner__text")

    val title = bannerText.select(".hmrc-user-research-banner__title")
    title.text() shouldBe "Tell us what you think about this service"

    val link = bannerText.select("a.hmrc-user-research-banner__link")
    link.attr("href") shouldBe "https://s.userzoom.com/m/MSBDMTU1M1MxMDAw"
    link.attr("rel") shouldBe "noopener noreferrer"
    link.attr("target") shouldBe "_blank"
    link.text() shouldBe "Complete our short survey (opens in new tab)"

    ()
  }

  def testUserResearchBannerIsNotPresent(doc: Document): Unit = {
    doc.select("div.hmrc-user-research-banner").isEmpty shouldBe true
    ()
  }

  List(
    Origins.Epaye.Bta,
    Origins.Vat.Bta,
    Origins.Simp.Pta
  ).foreach { origin =>
    val taxRegime = origin.taxRegime

    s"[taxRegime: ${taxRegime.toString}] GET /payment-plan-set-up should" - {
      def test(stubActions: () => Unit): Unit = {
        stubActions()

        val result: Future[Result] = taxRegime match {
          case TaxRegime.Epaye => controller.epayePaymentPlanSetUp(fakeRequest)
          case TaxRegime.Vat   => controller.vatPaymentPlanSetUp(fakeRequest)
          case TaxRegime.Sa    => sys.error("Not expecting SA here")
          case TaxRegime.Simp  => controller.simpPaymentPlanSetUp(fakeRequest)
        }
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = expectedH1PaymentPlanSetUpPage,
          shouldBackLinkBePresent = false,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
        doc.select(".govuk-panel__body").text() shouldBe (taxRegime match {
          case TaxRegime.Epaye =>
            "Your payment reference is 123PA44545546"
          case TaxRegime.Vat   =>
            "Your payment reference is 101747001"
          case TaxRegime.Sa    =>
            sys.error("Not expecting SA here")
          case TaxRegime.Simp  =>
            "Your payment reference is QQ123456A"
        })

        val subheadings = doc.select(".govuk-heading-m").asScala.toList
        val paragraphs  = doc.select(".govuk-body").asScala.toList

        subheadings(0).text() shouldBe "What happens next"
        subheadings(0).tagName() shouldBe "h2"

        paragraphs(0).text() shouldBe "In the next 24 hours we’ll:"

        val list      = doc.select("ul.govuk-list").asScala.toList
        val listItems = list(0).select("li").asScala.toList
        listItems(0).text() shouldBe "update your tax account with your payment plan"
        listItems(1).text() shouldBe (
          if (taxRegime == TaxRegime.Simp) "send payment due dates to your personal tax account inbox"
          else "send payment due dates to your business tax account inbox"
        )

        paragraphs(1)
          .text() shouldBe "You’ll also receive a letter with your payment dates. We’ll send this out within 5 days."

        paragraphs(2)
          .text() shouldBe "If you’ve made an upfront payment, we’ll take it from your bank account within 6 working days."

        val viewPaymentPlanLink = paragraphs(3)
        viewPaymentPlanLink.text() shouldBe "View your payment plan"
        viewPaymentPlanLink.select("a").text() shouldBe "View your payment plan"
        viewPaymentPlanLink.select("a").attr("href") shouldBe (taxRegime match {
          case TaxRegime.Epaye => routes.PaymentPlanSetUpController.epayeVatPrintSummary.url
          case TaxRegime.Vat   => routes.PaymentPlanSetUpController.epayeVatPrintSummary.url
          case TaxRegime.Sa    => sys.error("Not expecting SA here")
          case TaxRegime.Simp  => routes.PaymentPlanSetUpController.simpPrintSummary.url
        })

        paragraphs(4)
          .text() shouldBe "You can call HMRC to update your payment plan. Make sure you have your payment reference number ready."

        subheadings(1).text() shouldBe "Call the debt management helpline"
        subheadings(1).tagName() shouldBe "h3"

        paragraphs(5).text() shouldBe "Telephone: 0300 123 1813"
        paragraphs(5).select("strong").text() shouldBe "0300 123 1813"

        paragraphs(6).text() shouldBe "Outside UK: +44 2890 538 192"
        paragraphs(6).select("strong").text() shouldBe "+44 2890 538 192"

        paragraphs(7).text() shouldBe "Our phone line opening hours are:"
        paragraphs(8).text() shouldBe "Monday to Friday: 8am to 6pm"
        paragraphs(9).text() shouldBe "Closed weekends and bank holidays."

        subheadings(2).text() shouldBe "Text service"
        subheadings(2).tagName() shouldBe "h3"

        paragraphs(10)
          .text() shouldBe "Use Relay UK if you cannot hear or speak on the telephone, dial 18001 then 0345 300 3900. Find out more on the Relay UK website (opens in new tab)."
        paragraphs(10).select("strong").asScala.toList.map(_.text()) shouldBe List("18001", "0345 300 3900")
        val relayLink = paragraphs(10).select("a")
        relayLink.text() shouldBe "Relay UK website (opens in new tab)"
        relayLink.attr("href") shouldBe "https://www.relayuk.bt.com/"
        relayLink.attr("rel") shouldBe "noreferrer noopener"
        relayLink.attr("target") shouldBe "_blank"

        subheadings(3).text() shouldBe "If a health condition or personal circumstances make it difficult to contact us"
        subheadings(3).tagName() shouldBe "h3"

        paragraphs(11)
          .text() shouldBe "Our guidance Get help from HMRC if you need extra support (opens in new tab) explains how we can support you."
        val extraSupportLink = paragraphs(11).select("a")
        extraSupportLink.text() shouldBe "Get help from HMRC if you need extra support (opens in new tab)"
        extraSupportLink.attr("href") shouldBe "https://www.gov.uk/get-help-hmrc-extra-support"
        extraSupportLink.attr("rel") shouldBe "noreferrer noopener"
        extraSupportLink.attr("target") shouldBe "_blank"

        val continueButton = doc.select(".govuk-button")
        continueButton.text() shouldBe "Go to tax account"
        continueButton.attr("role") shouldBe "button"
        continueButton.attr("href") shouldBe (taxRegime match {
          case TaxRegime.Epaye | TaxRegime.Vat => "http://localhost:9020/business-account"
          case TaxRegime.Sa                    => sys.error("Not expceting SA here")
          case TaxRegime.Simp                  => "http://localhost:9232/personal-account"
        })

        val surveyLink = doc.select(".govuk-body > .govuk-link").asScala.toList(3)
        surveyLink.parent().text() shouldBe "What did you think of this service? (takes 30 seconds)"
        surveyLink.attr("href") shouldBe (taxRegime match {
          case TaxRegime.Epaye => PageUrls.exitSurveyEpayeUrl
          case TaxRegime.Vat   => PageUrls.exitSurveyVatUrl
          case TaxRegime.Sa    => sys.error("Not expceting SA here")
          case TaxRegime.Simp  => PageUrls.exitSurveySimpUrl
        })
        ()
      }

      "return the confirmation page with correct content" in {
        test { () =>
          stubCommonActions()
          EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)()
          ()
        }
      }

      "show the user research banner if the user went through an affordability journey" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto, withAffordability = true)()

        val result: Future[Result] = taxRegime match {
          case TaxRegime.Epaye => controller.epayePaymentPlanSetUp(fakeRequest)
          case TaxRegime.Vat   => controller.vatPaymentPlanSetUp(fakeRequest)
          case TaxRegime.Sa    => sys.error("Not expecting SA here")
          case TaxRegime.Simp  => controller.simpPaymentPlanSetUp(fakeRequest)
        }
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
        testUserResearchBannerIsPresent(doc)
      }

      "not show the user research banner if the user did not go through an affordability journey" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto, withAffordability = false)()

        val result: Future[Result] = taxRegime match {
          case TaxRegime.Epaye => controller.epayePaymentPlanSetUp(fakeRequest)
          case TaxRegime.Vat   => controller.vatPaymentPlanSetUp(fakeRequest)
          case TaxRegime.Sa    => sys.error("Not expecting SA here")
          case TaxRegime.Simp  => controller.simpPaymentPlanSetUp(fakeRequest)
        }
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
        testUserResearchBannerIsNotPresent(doc)
      }

    }

    s"[taxRegime: ${taxRegime.toString}] GET /payment-plan-print-summary should" - {
      "return the print payment schedule page with correct content (with upfront payment)" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)()

        val result: Future[Result] = controller.epayeVatPrintSummary(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = expectedH1PaymentPlanPrintPage,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        val subheadings = doc.select(".govuk-heading-m").asScala.toList
        subheadings.size shouldBe 1
        subheadings(0).text() shouldBe "Monthly payments"

        val allSummaryLists                 = doc.select(".govuk-summary-list").asScala.toList
        val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
        val upfrontPaymentSummaryListRows   = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
        val monthlyPaymentSummaryListRows   = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

        assertPaymentReference(paymentReferenceSummaryListRows)

        upfrontPaymentSummaryListRows.size shouldBe 2
        assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "Yes"))
        assertKeyAndValue(upfrontPaymentSummaryListRows(1), ("Upfront payment Taken within 6 working days", "£2"))

        assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
        assertPrintLink(doc)
      }

      "return the print payment schedule page with correct content (without upfront payment)" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)(
          JourneyJsonTemplates.`Arrangement Submitted - No upfront payment`(origin)
        )

        val result: Future[Result] = controller.saPrintSummary(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = expectedH1PaymentPlanPrintPage,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        val subheadings = doc.select(".govuk-heading-m").asScala.toList
        subheadings.size shouldBe 1
        subheadings(0).text() shouldBe "Monthly payments"

        val allSummaryLists                 = doc.select(".govuk-summary-list").asScala.toList
        val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
        val upfrontPaymentSummaryListRows   = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
        val monthlyPaymentSummaryListRows   = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

        assertPaymentReference(paymentReferenceSummaryListRows)

        upfrontPaymentSummaryListRows.size shouldBe 1
        assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "No"))

        assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
        assertPrintLink(doc)
      }

      "return the print payment schedule page with correct content (affordability)" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto, withAffordability = true)()

        val result: Future[Result] = controller.saPrintSummary(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = expectedH1PaymentPlanPrintPage,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        val subheadings = doc.select(".govuk-heading-m").asScala.toList
        subheadings.size shouldBe 1
        subheadings(0).text() shouldBe "Monthly payments"

        val allSummaryLists                 = doc.select(".govuk-summary-list").asScala.toList
        val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
        val upfrontPaymentSummaryListRows   = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
        val monthlyPaymentSummaryListRows   = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

        assertPaymentReference(paymentReferenceSummaryListRows)

        upfrontPaymentSummaryListRows.size shouldBe 2
        assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "Yes"))
        assertKeyAndValue(upfrontPaymentSummaryListRows(1), ("Upfront payment Taken within 6 working days", "£2"))

        assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
        assertPrintLink(doc)
      }

      def assertPaymentReference(paymentReferenceSummaryListRows: List[Element]): Assertion = {
        val expectedPaymentReference = taxRegime match {
          case TaxRegime.Epaye => "123PA44545546"
          case TaxRegime.Vat   => "101747001"
          case TaxRegime.Sa    => "1234567895"
          case TaxRegime.Simp  => "QQ123456A"
        }
        assertKeyAndValue(paymentReferenceSummaryListRows(0), ("Payment reference", expectedPaymentReference))
      }

      def assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows: List[Element]): Assertion = {
        monthlyPaymentSummaryListRows.size shouldBe 4
        assertKeyAndValue(monthlyPaymentSummaryListRows(0), ("Payments collected on", "28th or next working day"))
        assertKeyAndValue(monthlyPaymentSummaryListRows(1), ("August 2022", "£555.73"))
        assertKeyAndValue(monthlyPaymentSummaryListRows(2), ("September 2022", "£555.73"))
        assertKeyAndValue(monthlyPaymentSummaryListRows(3), ("Total to pay", "£1,111.47"))
      }

      def assertPrintLink(doc: Document): Assertion = {
        val printLink = doc.select("#printLink")
        printLink.text() shouldBe "Print a copy of your payment plan"
        printLink.attr("href") shouldBe "#print-dialogue"
      }
    }
  }

  s"[taxRegime: ${Origins.Sa.Bta.taxRegime.toString}] GET /payment-plan-set-up should" - {

    def test(
      stubActions:            () => Unit,
      hasUpfrontPayment:      Boolean,
      isEmailAddressRequired: Boolean
    ): Unit = {
      stubActions()

      val result: Future[Result] = controller.saPaymentPlanSetUp(fakeRequest)

      val pageContent: String = contentAsString(result)
      val doc: Document       = Jsoup.parse(pageContent)
      val origin              = Origins.Sa.Bta
      val taxRegime           = origin.taxRegime

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = expectedH1PaymentPlanSetUpPage,
        shouldBackLinkBePresent = false,
        expectedSubmitUrl = None,
        regimeBeingTested = Some(taxRegime)
      )

      doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
      doc.select(".govuk-panel__body").text() shouldBe (taxRegime match {
        case TaxRegime.Epaye =>
          "Your payment reference is 123PA44545546"
        case TaxRegime.Vat   =>
          "Your payment reference is 101747001"
        case TaxRegime.Sa    =>
          "Your payment reference is 1234567895"
        case TaxRegime.Simp  =>
          "Your payment reference is QQ123456A"
      })

      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      val paragraphs  = doc.select(".govuk-body").asScala.toList

      subheadings(0).text() shouldBe "What you need to do next"
      paragraphs(0).text() shouldBe "View your payment plan where you will be able to print or save a copy."
      paragraphs(1)
        .text() shouldBe "We will not send a copy of your payment plan in the post. This is your only chance to access this information."

      if (isEmailAddressRequired) {
        val _ = paragraphs(3)
          .text() shouldBe "We will send a secure message with payment due dates to your personal tax account inbox within 24 hours."
      }

      val emailParagraphOffset = if (isEmailAddressRequired) 0 else -1

      if (hasUpfrontPayment) {
        paragraphs(2)
          .text() shouldBe "Your upfront payment will be taken within 6 working days. Your next payment will be taken on 28 August 2022 or the next working day."
      } else {
        paragraphs(2).text() shouldBe "Your next payment will be taken on 28 August 2022 or the next working day."
      }

      subheadings(1).text() shouldBe "About your payment plan"
      subheadings(2).text() shouldBe "Call the debt management helpline"
      paragraphs(emailParagraphOffset + 5).text() shouldBe "Telephone: 0300 123 1813"

      val continueButton = doc.select(".govuk-button")
      continueButton.text() shouldBe "Go to tax account"
      continueButton.attr("role") shouldBe "button"
      continueButton.attr("href") shouldBe "http://localhost:9232/personal-account"

      val surveyLink = doc.select(".govuk-body > .govuk-link").asScala.toList(3)
      surveyLink.parent().text() shouldBe "What did you think of this service? (takes 30 seconds)"
      surveyLink.attr("href") shouldBe PageUrls.exitSurveySaUrl
      ()
    }

    "return the confirmation page with correct content when there is an upfront payment" in {
      val origin = Origins.Sa.Bta
      test(
        { () =>
          stubCommonActions()
          EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)()
          ()
        },
        hasUpfrontPayment = true,
        isEmailAddressRequired = true
      )
    }

    "return the confirmation page with correct content when there is no upfront payment" in {
      val origin = Origins.Sa.Bta
      test(
        { () =>
          stubCommonActions()
          EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)(
            JourneyJsonTemplates.`Arrangement Submitted - No upfront payment`(origin)
          )
          ()
        },
        hasUpfrontPayment = false,
        isEmailAddressRequired = true
      )
    }

    "return the confirmation page with correct content when an email address wasn't required" in {
      val origin = Origins.Sa.Bta
      test(
        { () =>
          stubCommonActions()
          EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)(
            JsonUtils
              .replace(
                List("SubmittedArrangement", "eligibilityCheckResult", "regimeDigitalCorrespondence"),
                JsBoolean(false)
              )(
                Json.parse(JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`(origin)).as[JsObject]
              )
              .toString
          )
          ()
        },
        hasUpfrontPayment = true,
        isEmailAddressRequired = false
      )
    }

    "show the user research banner if the user went through an affordability journey" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Sa.Bta, testCrypto, withAffordability = true)()

      val result: Future[Result] = controller.saPaymentPlanSetUp(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
      testUserResearchBannerIsPresent(doc)
    }

    "not show the user research banner if the user did not go through an affordability journey" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Sa.Bta, testCrypto, withAffordability = false)()

      val result: Future[Result] = controller.saPaymentPlanSetUp(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
      testUserResearchBannerIsNotPresent(doc)
    }

  }

  s"[taxRegime: ${Origins.Sa.Bta.taxRegime.toString}] GET /payment-plan-print-summary should" - {
    "return the print payment schedule page with correct content (with upfront payment)" in {
      val origin    = Origins.Sa.Bta
      val taxRegime = origin.taxRegime
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)()

      val result: Future[Result] = controller.epayeVatPrintSummary(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = expectedH1PaymentPlanPrintPageSa,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = None,
        regimeBeingTested = Some(taxRegime)
      )

      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      subheadings(0).text() shouldBe "Your payment plan"
      subheadings(1).text() shouldBe "Monthly payments"
      subheadings(2).text() shouldBe "If you do not pay on time"
      subheadings(3).text() shouldBe "If you’re having difficulty paying"

      val allSummaryLists                 = doc.select(".govuk-summary-list").asScala.toList
      val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
      val upfrontPaymentSummaryListRows   = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
      val monthlyPaymentSummaryListRows   = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

      assertPaymentReference(paymentReferenceSummaryListRows)

      upfrontPaymentSummaryListRows.size shouldBe 2
      assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "Yes"))
      assertKeyAndValue(upfrontPaymentSummaryListRows(1), ("Upfront payment Taken within 6 working days", "£2"))

      assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
      assertPrintLink(doc)
    }

    "return the print payment schedule page with correct content (without upfront payment)" in {
      val origin    = Origins.Sa.Bta
      val taxRegime = origin.taxRegime
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)(
        JourneyJsonTemplates.`Arrangement Submitted - No upfront payment`(origin)
      )

      val result: Future[Result] = controller.saPrintSummary(fakeRequest)
      val pageContent: String    = contentAsString(result)
      val doc: Document          = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = expectedH1PaymentPlanPrintPageSa,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = None,
        regimeBeingTested = Some(taxRegime)
      )

      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      subheadings(0).text() shouldBe "Your payment plan"
      subheadings(1).text() shouldBe "Monthly payments"
      subheadings(2).text() shouldBe "If you do not pay on time"
      subheadings(3).text() shouldBe "If you’re having difficulty paying"

      val allSummaryLists                 = doc.select(".govuk-summary-list").asScala.toList
      val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
      val upfrontPaymentSummaryListRows   = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
      val monthlyPaymentSummaryListRows   = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

      assertPaymentReference(paymentReferenceSummaryListRows)

      upfrontPaymentSummaryListRows.size shouldBe 1
      assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "No"))

      assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
      assertPrintLink(doc)
    }

    def assertPaymentReference(paymentReferenceSummaryListRows: List[Element]): Assertion = {
      val origin                   = Origins.Sa.Bta
      val taxRegime                = origin.taxRegime
      val expectedPaymentReference = taxRegime match {
        case TaxRegime.Epaye => "123PA44545546"
        case TaxRegime.Vat   => "101747001"
        case TaxRegime.Sa    => "1234567895"
        case TaxRegime.Simp  => "QQ123456A"
      }
      assertKeyAndValue(paymentReferenceSummaryListRows(0), ("Payment reference", expectedPaymentReference))
    }

    def assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows: List[Element]): Assertion = {
      monthlyPaymentSummaryListRows.size shouldBe 5
      assertKeyAndValue(monthlyPaymentSummaryListRows(0), ("Payments collected on", "28th or next working day"))
      assertKeyAndValue(monthlyPaymentSummaryListRows(1), ("August 2022", "£555.73"))
      assertKeyAndValue(monthlyPaymentSummaryListRows(2), ("September 2022", "£555.73"))
      assertKeyAndValue(monthlyPaymentSummaryListRows(3), ("Estimated total interest Included in your plan", "£0.06"))
      assertKeyAndValue(monthlyPaymentSummaryListRows(4), ("Total to pay", "£1,111.47"))
    }

    def assertPrintLink(doc: Document): Assertion = {
      val printLink = doc.select("#printLink")
      printLink.text() shouldBe "Print or save a copy of your payment plan"
      printLink.attr("href") shouldBe "#print-dialogue"
    }
  }

}

class PaymentPlanSetUpControllerEmailDisabledSpec extends ItSpec {

  override lazy val configOverrides = Map("features.email-journey" -> false)

  private val controller: PaymentPlanSetUpController = app.injector.instanceOf[PaymentPlanSetUpController]

  "When email is disabled" - {

    s"[taxRegime: ${Origins.Sa.Bta.toString()}] GET /payment-plan-set-up should" - {

      def test(
        stubActions: () => Unit
      ): Unit = {
        stubActions()

        val result: Future[Result] = controller.saPaymentPlanSetUp(fakeRequest)

        val pageContent: String = contentAsString(result)
        val doc: Document       = Jsoup.parse(pageContent)
        val origin              = Origins.Sa.Bta
        val taxRegime           = origin.taxRegime

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = "Your payment plan is set up",
          shouldBackLinkBePresent = false,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        val subheadings = doc.select(".govuk-heading-m").asScala.toList
        val paragraphs  = doc.select(".govuk-body").asScala.toList

        subheadings(0).text() shouldBe "What you need to do next"
        paragraphs(1)
          .text() shouldBe "We will not send a copy of your payment plan in the post. This is your only chance to access this information."
        paragraphs(2)
          .text() shouldBe "Your upfront payment will be taken within 6 working days. Your next payment will be taken on 28 August 2022 or the next working day."
        ()
      }

      "not display the email text when regimeDigitalCorrespondence=true" in {
        val origin = Origins.Sa.Bta
        test { () =>
          stubCommonActions()
          EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)()
          ()
        }
      }

      "not display the email text when regimeDigitalCorrespondence=false" in {
        val origin = Origins.Sa.Bta
        test { () =>
          stubCommonActions()
          EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)(
            JsonUtils
              .replace(
                List("SubmittedArrangement", "eligibilityCheckResult", "regimeDigitalCorrespondence"),
                JsBoolean(false)
              )(
                Json.parse(JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`(origin)).as[JsObject]
              )
              .toString
          )
          ()
        }
      }

    }
  }
}

class PaymentPlanSetUpControllerNoResearchBannerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Boolean] = Map("features.user-research-banner-enabled" -> false)
  private val controller: PaymentPlanSetUpController      = app.injector.instanceOf[PaymentPlanSetUpController]

  def testUserResearchBannerIsPresent(doc: Document): Unit = {
    val bannerContainer = doc.select("div.hmrc-user-research-banner > div.hmrc-user-research-banner__container")
    val bannerText      = bannerContainer.select("div.hmrc-user-research-banner__text")

    val title = bannerText.select(".hmrc-user-research-banner__title")
    title.text() shouldBe "Tell us what you think about this service"

    val link = bannerText.select("a.hmrc-user-research-banner__link")
    link.attr("href") shouldBe "https://s.userzoom.com/m/MSBDMTU1M1MxMDAw"
    link.attr("rel") shouldBe "noopener noreferrer"
    link.attr("target") shouldBe "_blank"
    link.text() shouldBe "Complete our short survey (opens in new tab)"
    ()
  }

  def testUserResearchBannerIsNotPresent(doc: Document): Unit = {
    doc.select("div.hmrc-user-research-banner").isEmpty shouldBe true
    ()
  }

  List(
    Origins.Epaye.Bta,
    Origins.Vat.Bta,
    Origins.Simp.Pta
  ).foreach { origin =>
    val taxRegime = origin.taxRegime

    s"[taxRegime: ${taxRegime.toString}] GET /payment-plan-set-up should" - {

      "do not show the user research banner even if the user went through an affordability journey" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto, withAffordability = true)()

        val result: Future[Result] = taxRegime match {
          case TaxRegime.Epaye => controller.epayePaymentPlanSetUp(fakeRequest)
          case TaxRegime.Vat   => controller.vatPaymentPlanSetUp(fakeRequest)
          case TaxRegime.Sa    => sys.error("Not expecting SA here")
          case TaxRegime.Simp  => controller.simpPaymentPlanSetUp(fakeRequest)
        }
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
        testUserResearchBannerIsNotPresent(doc)
      }

      "do not show the user research banner if the user did not go through an affordability journey" in {
        stubCommonActions()
        EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)()

        val result: Future[Result] = taxRegime match {
          case TaxRegime.Epaye => controller.epayePaymentPlanSetUp(fakeRequest)
          case TaxRegime.Vat   => controller.vatPaymentPlanSetUp(fakeRequest)
          case TaxRegime.Sa    => sys.error("Not expecting SA here")
          case TaxRegime.Simp  => controller.simpPaymentPlanSetUp(fakeRequest)
        }
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
        testUserResearchBannerIsNotPresent(doc)
      }
    }
  }
}
