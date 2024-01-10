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

  "IneligibleController should display in English" - {

    Seq[(TaxRegime, Origin)]((TaxRegime.Epaye, Origins.Epaye.Bta), (TaxRegime.Vat, Origins.Vat.Bta))
      .foreach {
        case (taxRegime, origin) =>

          val enrolments = taxRegime match {
            case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
            case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            case TaxRegime.Sa    => Some(Set(TdAll.saEnrolment))
          }

          s"${taxRegime.entryName} Generic not eligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.payeGenericIneligiblePage(fakeRequest)
              case TaxRegime.Vat   => controller.vatGenericIneligiblePage(fakeRequest)
              case TaxRegime.Sa    => controller.saGenericIneligiblePage(fakeRequest)
            }
            val page = pageContentAsDoc(result)
            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "You are not eligible to set up an Employers’ PAYE payment plan online."
              case TaxRegime.Vat   => "You are not eligible to set up a VAT payment plan online."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Call us about a payment plan",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English)
          }

          s"${taxRegime.entryName} Debt too large ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooLargePage(fakeRequest)
              case TaxRegime.Vat   => controller.vatDebtTooLargePage(fakeRequest)
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Call us about a payment plan",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "You cannot set up an Employers’ PAYE payment plan online because you owe more than £50,000."
              case TaxRegime.Vat   => "You cannot set up a VAT payment plan online because you owe more than £50,000."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English)
          }

          s"${taxRegime.entryName} Debt too small ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooSmallPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatDebtTooSmallPage(fakeRequest)
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            val expectedH1 = taxRegime match {
              case TaxRegime.Epaye => "Pay your PAYE bill in full"
              case TaxRegime.Vat   => "Pay your VAT bill in full"
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = expectedH1,
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val (expectedParagraph1, expectedParagraph2) = taxRegime match {
              case TaxRegime.Epaye =>
                "You cannot set up an Employers’ PAYE payment plan online because your bill is too small." ->
                  "<a class=\"govuk-link\" href=\"https://tax.service.gov.uk/business-account/epaye/overdue-payments\">Make a payment online</a> to cover your PAYE bill in full."
              case TaxRegime.Vat =>
                "You cannot set up a VAT payment plan online because your bill is too small." ->
                  "<a class=\"govuk-link\" href=\"https://tax.service.gov.uk/vat-through-software/what-you-owe\">Make a payment online</a> to cover your VAT bill in full."
              case TaxRegime.Sa => throw new NotImplementedError()
            }

            val callUsContentEnglish = "Call us on <strong>0300 123 1813</strong> if you are having difficulty making a payment online."

            val leadingParagraphs = page.select(".govuk-body").asScala.toList
            leadingParagraphs(0).html() shouldBe expectedParagraph1
            leadingParagraphs(1).html() shouldBe expectedParagraph2
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English, callUsContentEnglish)
          }

          s"${taxRegime.entryName} Debt too old ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooOldPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatDebtTooOldPage(fakeRequest)
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Call us about a payment plan",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )
            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "You cannot set up an Employers’ PAYE payment plan online because your payment deadline was over 5 years ago."
              case TaxRegime.Vat   => "You cannot set up a VAT payment plan online because your payment deadline was over 28 days ago."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English)
          }

          s"${taxRegime.entryName} Existing ttp ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(origin))
            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeAlreadyHaveAPaymentPlanPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatAlreadyHaveAPaymentPlanPage(fakeRequest)
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "You already have a payment plan with HMRC",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val expectedP1 = taxRegime match {
              case TaxRegime.Epaye => "You cannot set up an Employers’ PAYE payment plan online."
              case TaxRegime.Vat   => "You cannot set up a VAT payment plan online."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English)
          }

          s"${taxRegime.entryName} Returns not up to date ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeFileYourReturnPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatFileYourReturnPage(fakeRequest)
              case TaxRegime.Sa    => throw new NotImplementedError()
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
              case TaxRegime.Epaye => "To be eligible to set up a payment plan online, you need to be up to date with your Employers’ PAYE returns. Once you have done this, you can return to the service."
              case TaxRegime.Vat   => "To be eligible to set up a payment plan online, you need to have filed your VAT returns. Once you have done this, you can return to the service."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingContent
            )

            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English)
            page.select(".govuk-body").asScala.toList(1).text() shouldBe "Go to your tax account to file your tax return."
            page.select(".govuk-body").asScala.toList(2).text() shouldBe "If you have recently filed your return, your account may take up to 72 hours to be updated before you can set up a payment plan."
            page.select("#bta-link").attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"
          }
      }

    "Vat Debt charges before max accounting date ineligible page correctly" in {
      val enrolment = Some(Set(TdAll.vatEnrolment))

      stubCommonActions(authAllEnrolments = enrolment)
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - BeforeMaxAccountingDate`(Origins.Vat.Bta))

      val result: Future[Result] = controller.vatDebtBeforeAccountingDatePage(fakeRequest)

      val page = pageContentAsDoc(result)

      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Call us about a payment plan",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        regimeBeingTested       = Some(TaxRegime.Vat)
      )
      val expectedLeadingP1 = "You cannot set up a VAT payment plan online because your debt is for an accounting period that started before 1 January 2023."

      assertIneligiblePageLeadingP1(
        page      = page,
        leadingP1 = expectedLeadingP1
      )
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Vat, Languages.English)
    }
  }

  "IneligibleController should display in Welsh" - {

    Seq[(TaxRegime, Origin)]((TaxRegime.Epaye, Origins.Epaye.Bta), (TaxRegime.Vat, Origins.Vat.Bta))
      .foreach {
        case (taxRegime, origin) =>
          val enrolments = taxRegime match {
            case TaxRegime.Epaye => Some(Set(TdAll.payeEnrolment))
            case TaxRegime.Vat   => Some(Set(TdAll.vatEnrolment))
            case TaxRegime.Sa    => Some(Set(TdAll.saEnrolment))
          }

          s"${taxRegime.entryName} Generic not eligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.payeGenericIneligiblePage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatGenericIneligiblePage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)
            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "Nid ydych yn gymwys i drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein."
              case TaxRegime.Vat   => "Nid ydych yn gymwys i drefnu cynllun talu TAW ar-lein."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Ffoniwch ni ynghylch cynllun talu",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh)
          }

          s"${taxRegime.entryName} Debt too large ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooLargePage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatDebtTooLargePage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Ffoniwch ni ynghylch cynllun talu",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )

            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd mae arnoch dros £50,000."
              case TaxRegime.Vat   => "Ni allwch drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein oherwydd mae arnoch dros £50,000."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh)
          }

          s"${taxRegime.entryName} Debt too small ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooSmallPage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatDebtTooSmallPage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            val expectedH1 = taxRegime match {
              case TaxRegime.Epaye => "Talu’ch bil TWE yn llawn"
              case TaxRegime.Vat   => "Talu’ch bil TAW yn llawn"
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = expectedH1,
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )

            val (expectedParagraph1, expectedParagraph2) = taxRegime match {
              case TaxRegime.Epaye =>
                "Ni allwch drefnu cynllun talu ar gyfer TWE y Cyflogwr ar-lein oherwydd bod eich bil yn rhy fach." ->
                  "<a class=\"govuk-link\" href=\"https://tax.service.gov.uk/business-account/epaye/overdue-payments\">Gwnewch daliad ar-lein</a> i dalu’ch bil TWE yn llawn."
              case TaxRegime.Vat =>
                "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd bod eich bil yn rhy fach." ->
                  "<a class=\"govuk-link\" href=\"https://tax.service.gov.uk/vat-through-software/what-you-owe\">Gwnewch daliad ar-lein</a> i dalu’ch bil TAW yn llawn."
              case TaxRegime.Sa => throw new NotImplementedError()
            }

            val callUsContentWelsh = "Os ydych yn cael anawsterau wrth dalu ar-lein, ffoniwch ni ar <strong>0300 200 1900</strong>."

            val leadingParagraphs = page.select(".govuk-body").asScala.toList
            leadingParagraphs(0).html() shouldBe expectedParagraph1
            leadingParagraphs(1).html() shouldBe expectedParagraph2
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh, callUsContentWelsh = callUsContentWelsh)
          }

          s"${taxRegime.entryName} Debt too old ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooOldPage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatDebtTooOldPage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Ffoniwch ni ynghylch cynllun talu",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )
            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "Ni allwch drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros 5 mlynedd yn ôl."
              case TaxRegime.Vat   => "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros 28 wythnos yn ôl."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh)
          }

          s"${taxRegime.entryName} Existing ttp ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(origin))
            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeAlreadyHaveAPaymentPlanPage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatAlreadyHaveAPaymentPlanPage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Mae gennych chi gynllun talu gyda CThEF yn barod",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )

            val expectedP1 = taxRegime match {
              case TaxRegime.Epaye => "Ni allwch drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein."
              case TaxRegime.Vat   => "Ni allwch drefnu cynllun talu TAW ar-lein."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh)
          }

          s"${taxRegime.entryName} Returns not up to date ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeFileYourReturnPage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatFileYourReturnPage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => throw new NotImplementedError()
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = "Cyflwynwch eich Ffurflen Dreth i ddefnyddio’r gwasanaeth hwn",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )

            val expectedLeadingContent = taxRegime match {
              case TaxRegime.Epaye => "I fod yn gymwys i drefnu cynllun talu ar-lein, mae’n rhaid i chi fod wedi cyflwyno’ch Ffurflenni Treth TWE Cyflogwyr. Pan fyddwch wedi gwneud hyn, gallwch ddychwelyd i’r gwasanaeth."
              case TaxRegime.Vat   => "I fod yn gymwys i drefnu cynllun talu ar-lein, mae’n rhaid i chi fod wedi cyflwyno’ch Ffurflen TAW. Pan fyddwch wedi gwneud hyn, gallwch ddychwelyd i’r gwasanaeth."
              case TaxRegime.Sa    => throw new NotImplementedError()
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingContent
            )

            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh)
            page.select(".govuk-body").asScala.toList(1).text() shouldBe "Ewch i’ch cyfrif treth er mwyn cyflwyno’ch Ffurflen Dreth."
            page.select(".govuk-body").asScala.toList(2).text() shouldBe "Os ydych chi wedi cyflwyno’ch Ffurflen Dreth yn ddiweddar, gallai gymryd hyd at 72 awr i’ch cyfrif gael ei ddiweddaru cyn i chi allu trefnu cynllun talu."
            page.select("#bta-link").attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"
          }
      }

    s"Vat Debt charges before max accounting date ineligible page correctly" in {
      val enrolment = Some(Set(TdAll.vatEnrolment))

      stubCommonActions(authAllEnrolments = enrolment)
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - BeforeMaxAccountingDate`(Origins.Vat.Bta))

      val result: Future[Result] = controller.vatDebtBeforeAccountingDatePage(fakeRequest.withLangWelsh())

      val page = pageContentAsDoc(result)

      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Ffoniwch ni ynghylch cynllun talu",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        regimeBeingTested       = Some(TaxRegime.Vat),
        language                = Languages.Welsh
      )
      val expectedLeadingP1 = "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd bod eich dyled am gyfnod cyfrifyddu a ddechreuodd cyn 1 Ionawr 2023."

      assertIneligiblePageLeadingP1(
        page      = page,
        leadingP1 = expectedLeadingP1
      )
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Vat, Languages.Welsh)
    }
  }
}
