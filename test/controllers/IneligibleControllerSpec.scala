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

import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.ContentAssertions
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class IneligibleControllerSpec extends ItSpec {

  private val controller: IneligibleController = app.injector.instanceOf[IneligibleController]
  private val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

  def pageContentAsDoc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  def assertIneligiblePageLeadingP1(page: Document, leadingP1: String): Assertion =
    page.select(".govuk-body").asScala.toList(0).text() shouldBe leadingP1

  def assertCommonEligibilityContent(page: Document, taxRegime: TaxRegime): Assertion = {

    val taxRegimeSpecificContent = taxRegime match {
      case TaxRegime.Epaye => "your Accounts Office reference. This is 13 characters, for example, 123PX00123456"
      case TaxRegime.Vat   => "your VAT number. This is 9 characters, for example, 1233456789"
    }

    val commonEligibilityWrapper = page.select("#common-eligibility")
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
    beforeYouCallList(0).text() shouldBe taxRegimeSpecificContent
    beforeYouCallList(1).text() shouldBe "your bank details"

    govukBodyElements(5).text() shouldBe "We’re likely to ask:"
    val likelyToAskList = bulletLists(1).select("li").asScala.toList
    likelyToAskList(0).text() shouldBe "what you’ve done to try to pay the bill"
    likelyToAskList(1).text() shouldBe "if you can pay some of the bill now"

    govukBodyElements(6).text() shouldBe "Our opening times are Monday to Friday: 8am to 6pm (we are closed on bank holidays)"
  }

  "IneligibleController should display" - {

    Seq[(TaxRegime, Origin)]((TaxRegime.Epaye, Origins.Epaye.Bta), (TaxRegime.Vat, Origins.Vat.Bta))
      .foreach {
        case (taxRegime, origin) =>
          s"${taxRegime.entryName} Generic not eligible page correctly" in {
            val enrolments = taxRegime match {
              case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
              case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            }
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.payeGenericIneligiblePage(fakeRequest)
              case TaxRegime.Vat   => controller.vatGenericIneligiblePage(fakeRequest)
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Call us",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = "You are not eligible for an online payment plan. You may still be able to set up a payment plan over the phone."
            )
            assertCommonEligibilityContent(page, taxRegime)
          }

          s"${taxRegime.entryName} Debt too large ineligible page correctly" in {
            val enrolments = taxRegime match {
              case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
              case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            }
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooLargePage(fakeRequest)
              case TaxRegime.Vat   => controller.vatDebtTooLargePage(fakeRequest)
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Call us",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val expectedAmount = taxRegime match {
              case TaxRegime.Epaye => "£15,000"
              case TaxRegime.Vat   => "£20,000"
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = s"You must owe $expectedAmount or less to be eligible for a payment plan online. You may still be able to set up a plan over the phone."
            )
            assertCommonEligibilityContent(page, taxRegime)
          }

          s"${taxRegime.entryName} Debt too old ineligible page correctly" in {
            val enrolments = taxRegime match {
              case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
              case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            }
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooOldPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatDebtTooOldPage(fakeRequest)
            }

            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Call us",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )
            val expectedNumberOfDays = taxRegime match {
              case TaxRegime.Epaye => "35"
              case TaxRegime.Vat   => "28"
            }
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = s"Your overdue amount must have a due date that is less than $expectedNumberOfDays days ago for you to be eligible for a payment plan online. You may still be able to set up a plan over the phone."
            )
            assertCommonEligibilityContent(page, taxRegime)
          }

          s"${taxRegime.entryName} Existing ttp ineligible page correctly" in {
            val enrolments = taxRegime match {
              case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
              case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            }
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(origin))
            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeAlreadyHaveAPaymentPlanPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatAlreadyHaveAPaymentPlanPage(fakeRequest)
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "You already have a payment plan with HMRC",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = "You can only have one payment plan at a time."
            )
            assertCommonEligibilityContent(page, taxRegime)
          }

          s"${taxRegime.entryName} Returns not up to date ineligible page correctly" in {
            val enrolments = taxRegime match {
              case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
              case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            }
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeFileYourReturnPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatFileYourReturnPage(fakeRequest)
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "File your return to use this service",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val expectedLeadingContent = taxRegime match {
              case TaxRegime.Epaye => "To be eligible for a payment plan online, you need to be up to date with your PAYE for Employers returns. Once you have done this, you can return to this service."
              case TaxRegime.Vat   => "To be eligible to set up a payment plan online, you need to be up to date with your returns. Once you have done this, you can return to the service."
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingContent
            )

            assertCommonEligibilityContent(page, taxRegime)
            page.select(".govuk-body").asScala.toList(1).text() shouldBe "Go to your tax account to file your tax return."
            if (taxRegime === TaxRegime.Vat) {
              page.select(".govuk-body").asScala.toList(2).text() shouldBe "If you have recently filed your return, your account may take up to 72 hours to be updated before you can set up a payment plan."
            }
            page.select("#bta-link").attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"
          }
      }
  }
}
