/*
 * Copyright 2022 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.ContentAssertions.assertKeyAndValue
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

class PaymentPlanSetUpControllerSpec extends ItSpec {

  private val controller: PaymentPlanSetUpController = app.injector.instanceOf[PaymentPlanSetUpController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1PaymentPlanSetUpPage: String = "Your payment plan is set up"
  private val expectedPageTitlePaymentPlanSetUpPage: String = s"$expectedH1PaymentPlanSetUpPage - $expectedServiceName - GOV.UK"
  private val expectedH1PaymentPlanPrintPage: String = "Your payment plan"
  private val expectedPageTitlePaymentPlanPrintPage: String = s"$expectedH1PaymentPlanPrintPage - $expectedServiceName - GOV.UK"

  "GET /payment-plan-set-up should" - {
    "return the confirmation page with correct content when there is an upfront payment" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.paymentPlanSetUp(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitlePaymentPlanSetUpPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").size() shouldBe 0
      ContentAssertions.languageToggleExists(doc)

      doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
      doc.select(".govuk-panel__body").text() shouldBe "Your payment reference is 123PA44545546"
      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      val paragraphs = doc.select(".govuk-body").asScala.toList
      subheadings(0).text() shouldBe "What happens next"
      paragraphs(0).text() shouldBe "HMRC will send you a letter within 5 working days with your payment dates."
      paragraphs(1).text() shouldBe "Your upfront payment will be taken within 10 working days. Your next payment will be taken on 28th August 2022 or the next working day."
      paragraphs(2).text() shouldBe "Print your plan or save it as a PDF"
      doc.select("#print-plan-link").attr("href") shouldBe PageUrls.printPlanUrl
      subheadings(1).text() shouldBe "If you need to change your payment plan"
      paragraphs(3).text() shouldBe "Call the HMRC Helpline on 0300 123 1813, or write to us by post:"
      paragraphs(4).text() shouldBe "DM PAYE HM Revenue and Customs BX9 1EW United Kingdom"
    }

    "return the confirmation page with correct content when there is no upfront payment" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(JourneyJsonTemplates.`Arrangement Submitted - No upfront payment`)
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.paymentPlanSetUp(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitlePaymentPlanSetUpPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").size() shouldBe 0
      ContentAssertions.languageToggleExists(doc)

      doc.select(".govuk-panel__title").text() shouldBe "Your payment plan is set up"
      doc.select(".govuk-panel__body").text() shouldBe "Your payment reference is 123PA44545546"
      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      val paragraphs = doc.select(".govuk-body").asScala.toList
      subheadings(0).text() shouldBe "What happens next"
      paragraphs(0).text() shouldBe "HMRC will send you a letter within 5 working days with your payment dates."
      paragraphs(1).text() shouldBe "Your next payment will be taken on 28th August 2022 or the next working day."
      paragraphs(2).text() shouldBe "Print your plan or save it as a PDF"
      doc.select("#print-plan-link").attr("href") shouldBe PageUrls.printPlanUrl
      subheadings(1).text() shouldBe "If you need to change your payment plan"
      paragraphs(3).text() shouldBe "Call the HMRC Helpline on 0300 123 1813, or write to us by post:"
      paragraphs(4).text() shouldBe "DM PAYE HM Revenue and Customs BX9 1EW United Kingdom"
    }
  }

  "GET /payment-plan-print-summary should" - {
    "return the print payment schedule page with correct content (with upfront payment)" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.printSummary(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitlePaymentPlanPrintPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.PaymentPlanSetUpController.paymentPlanSetUp.url
      doc.select(".govuk-heading-xl").text() shouldBe expectedH1PaymentPlanPrintPage

      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      subheadings.size shouldBe 2
      subheadings(0).text() shouldBe "Upfront payment"
      subheadings(1).text() shouldBe "Monthly payments"

      val allSummaryLists = doc.select(".govuk-summary-list").asScala.toList
      val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
      val upfrontPaymentSummaryListRows = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
      val monthlyPaymentSummaryListRows = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

      assertPaymentReference(paymentReferenceSummaryListRows)

      upfrontPaymentSummaryListRows.size shouldBe 2
      assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "Yes"))
      assertKeyAndValue(upfrontPaymentSummaryListRows(1), ("Taken within 10 working days", "£2"))

      assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
      assertPrintLink(doc)
    }

    "return the print payment schedule page with correct content (without upfront payment)" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(JourneyJsonTemplates.`Arrangement Submitted - No upfront payment`)
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.printSummary(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitlePaymentPlanPrintPage
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.PaymentPlanSetUpController.paymentPlanSetUp.url
      doc.select(".govuk-heading-xl").text() shouldBe expectedH1PaymentPlanPrintPage

      val subheadings = doc.select(".govuk-heading-m").asScala.toList
      subheadings.size shouldBe 2
      subheadings(0).text() shouldBe "Upfront payment"
      subheadings(1).text() shouldBe "Monthly payments"

      val allSummaryLists = doc.select(".govuk-summary-list").asScala.toList
      val paymentReferenceSummaryListRows = allSummaryLists(0).select(".govuk-summary-list__row").asScala.toList
      val upfrontPaymentSummaryListRows = allSummaryLists(1).select(".govuk-summary-list__row").asScala.toList
      val monthlyPaymentSummaryListRows = allSummaryLists(2).select(".govuk-summary-list__row").asScala.toList

      assertPaymentReference(paymentReferenceSummaryListRows)

      upfrontPaymentSummaryListRows.size shouldBe 1
      assertKeyAndValue(upfrontPaymentSummaryListRows(0), ("Can you make an upfront payment?", "No"))

      assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows)
      assertPrintLink(doc)
    }

      def assertPaymentReference(paymentReferenceSummaryListRows: List[Element]): Assertion =
        assertKeyAndValue(paymentReferenceSummaryListRows(0), ("Payment reference", "123PA44545546"))

      def assertMonthlyPaymentSummaryList(monthlyPaymentSummaryListRows: List[Element]): Assertion = {
        monthlyPaymentSummaryListRows.size shouldBe 4
        assertKeyAndValue(monthlyPaymentSummaryListRows(0), ("Payments collected on", "28th or next working day"))
        assertKeyAndValue(monthlyPaymentSummaryListRows(1), ("August 2022", "£555.73"))
        assertKeyAndValue(monthlyPaymentSummaryListRows(2), ("September 2022", "£555.73"))
        assertKeyAndValue(monthlyPaymentSummaryListRows(3), ("Total to pay", "£1,111.47"))
      }

      def assertPrintLink(doc: Document): Assertion = {
        val printlink = doc.select("#printLink")
        printlink.text() shouldBe "Print your plan or save it as a PDF"
        printlink.attr("href") shouldBe "#print-dialogue"
      }
  }
}
