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
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.ContentAssertions
import testsupport.stubs.EssttpBackend
import testsupport.testdata.JourneyJsonTemplates
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class IneligibleControllerSpec extends ItSpec {

  private val controller: IneligibleController = app.injector.instanceOf[IneligibleController]
  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

  def pageContentAsDoc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  def ineligiblePageLeadingContent(page: Document, expectedH1: String, leadingP1: String): Assertion = {
    page.select(".govuk-heading-xl").text() shouldBe expectedH1
    page.select(".govuk-body").asScala.toList(0).text() shouldBe leadingP1
  }

  def assertCommonEligibilityContent(page: Document): Assertion = {
    ContentAssertions.languageToggleExists(page)
    val commonEligibilityWrapper = page.select(".common-eligibility")
    val govukBodyElements = commonEligibilityWrapper.select(".govuk-body").asScala.toList
    govukBodyElements(0).text() shouldBe "For further support you can contact the Payment Support Service on 0300 200 3835 to speak to an advisor."

    val detailsReveal = commonEligibilityWrapper.select(".govuk-details")
    detailsReveal.select(".govuk-details__summary-text").text() shouldBe "If you cannot use speech recognition software"
    val detailsRevealText = detailsReveal.select(".govuk-details__text").select(".govuk-body").asScala.toList
    detailsRevealText(0).html() shouldBe "Find out how to <a href=\"https://www.gov.uk/get-help-hmrc-extra-support\" class=\"govuk-link\">deal with HMRC if you need extra support</a>."
    detailsRevealText(1).html() shouldBe "You can also use <a href=\"https://www.relayuk.bt.com/\" class=\"govuk-link\">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."
    detailsRevealText(2).html() shouldBe "If you are outside the UK: <strong>+44 2890 538 192</strong>"

    govukBodyElements(4).text() shouldBe "Before you call, make sure you have:"
    val bulletLists = commonEligibilityWrapper.select(".govuk-list").asScala.toList
    val beforeYouCallList = bulletLists(0).select("li").asScala.toList
    beforeYouCallList(0).text() shouldBe "your Accounts Office reference. This is 13 characters, for example, 123PX00123456"
    beforeYouCallList(1).text() shouldBe "your bank details"

    govukBodyElements(5).text() shouldBe "We’re likely to ask:"
    val likelyToAskList = bulletLists(1).select("li").asScala.toList
    likelyToAskList(0).text() shouldBe "what you’ve done to try to pay the bill"
    likelyToAskList(1).text() shouldBe "if you can pay some of the bill now"

    govukBodyElements(6).text() shouldBe "Our opening times are Monday to Friday: 8am to 6pm (we are closed on bank holidays)"
  }

  "IneligibleController should display" - {
    "Generic not eligible page correctly" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney(JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`)
      val result: Future[Result] = controller.genericIneligiblePage(fakeRequest)
      val page = pageContentAsDoc(result)
      ineligiblePageLeadingContent(
        page       = page,
        expectedH1 = "Call us",
        leadingP1  = "You are not eligible for an online payment plan. You may still be able to set up a payment plan over the phone."
      )
      assertCommonEligibilityContent(page)
    }
    "Debt too large ineligible page correctly" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney(JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`)
      val result: Future[Result] = controller.debtTooLargePage(fakeRequest)
      val page = pageContentAsDoc(result)
      ineligiblePageLeadingContent(
        page       = page,
        expectedH1 = "Call us",
        leadingP1  = "You must owe £15,000 or less to be eligible for a payment plan online. You may still be able to set up a plan over the phone."
      )
      assertCommonEligibilityContent(page)
    }
    "Existing ttp ineligible page correctly" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`)
      val result: Future[Result] = controller.alreadyHaveAPaymentPlanPage(fakeRequest)
      val page = pageContentAsDoc(result)
      ineligiblePageLeadingContent(
        page       = page,
        expectedH1 = "You already have a payment plan with HMRC",
        leadingP1  = "You can only have one payment plan at a time."
      )
      assertCommonEligibilityContent(page)
    }
    "Debt too old ineligible page correctly" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`)
      val result: Future[Result] = controller.debtTooOldPage(fakeRequest)
      val page = pageContentAsDoc(result)
      ineligiblePageLeadingContent(
        page       = page,
        expectedH1 = "Call us",
        leadingP1  = "Your overdue amount must have a due date that is less than 35 days ago for you to be eligible for a payment plan online. You may still be able to set up a plan over the phone."
      )
      assertCommonEligibilityContent(page)
    }
    "Returns not up to date ineligible page correctly" in {
      stubActionDefaults()
      EssttpBackend.EligibilityCheck.findJourney(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`)
      val result: Future[Result] = controller.fileYourReturnPage(fakeRequest)
      val page = pageContentAsDoc(result)
      ineligiblePageLeadingContent(
        page       = page,
        expectedH1 = "File your return to use this service",
        leadingP1  = "To be eligible for a payment plan online, you need to be up to date with your PAYE for Employers returns. Once you have done this, you can return to this service."
      )
      assertCommonEligibilityContent(page)
      page.select(".govuk-body").asScala.toList(1).text() shouldBe "Go to your tax account to file your tax return."
      page.select("#bta-link").attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"
    }
  }
}
