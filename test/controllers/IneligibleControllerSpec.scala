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
import scala.jdk.CollectionConverters.IterableHasAsScala

class IneligibleControllerSpec extends ItSpec {

  private val controller: IneligibleController = app.injector.instanceOf[IneligibleController]
  private val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

  def pageContentAsDoc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  def assertIneligiblePageLeadingP1(page: Document, leadingP1: String): Assertion =
    page.select(".govuk-body").asScala.toList(0).text() shouldBe leadingP1

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
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime)
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
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime)
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
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime)
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
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime)
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

            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime)
            page.select(".govuk-body").asScala.toList(1).text() shouldBe "Go to your tax account to file your tax return."
            if (taxRegime === TaxRegime.Vat) {
              page.select(".govuk-body").asScala.toList(2).text() shouldBe "If you have recently filed your return, your account may take up to 72 hours to be updated before you can set up a payment plan."
            }
            page.select("#bta-link").attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"
          }
      }
  }
}
