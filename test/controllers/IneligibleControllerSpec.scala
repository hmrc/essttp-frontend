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

    Seq[(TaxRegime, Origin)](
      (TaxRegime.Epaye, Origins.Epaye.Bta),
      (TaxRegime.Vat, Origins.Vat.Bta),
      (TaxRegime.Sa, Origins.Sa.Bta)
    )
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
              case TaxRegime.Sa    => "You are not eligible to set up a Self Assessment payment plan online."
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
              case TaxRegime.Sa    => controller.saDebtTooLargePage(fakeRequest)
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
              case TaxRegime.Sa    => "You cannot set up a Self Assessment payment plan online because you owe more than £30,000."
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English)
          }

          s"${taxRegime.entryName} Debt too old ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooOldPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatDebtTooOldPage(fakeRequest)
              case TaxRegime.Sa    => controller.saDebtTooOldPage(fakeRequest)
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
              case TaxRegime.Sa    => "You cannot set up a Self Assessment payment plan online because your payment deadline was over 60 days ago."
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
              case TaxRegime.Sa    => controller.saDebtTooSmallPage(fakeRequest)
            }
            val page = pageContentAsDoc(result)

            val expectedH1 = taxRegime match {
              case TaxRegime.Epaye => "Pay your PAYE bill in full"
              case TaxRegime.Vat   => "Pay your VAT bill in full"
              case TaxRegime.Sa    => "Pay your Self Assessment tax bill in full"
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
              case TaxRegime.Sa =>
                "You cannot set up a Self Assessment payment plan online because your bill is too small." ->
                  "<a class=\"govuk-link\" href=\"https://www.gov.uk/pay-self-assessment-tax-bill\">Make a payment online</a> to cover your Self Assessment tax bill in full."

            }

            val callUsContentEnglish = "Call us on <strong>0300 123 1813</strong> if you are having difficulty making a payment online."

            val leadingParagraphs = page.select(".govuk-body").asScala.toList
            leadingParagraphs(0).html() shouldBe expectedParagraph1
            leadingParagraphs(1).html() shouldBe expectedParagraph2
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.English, callUsContentEnglish, showFullListPreparationTips = false)
          }

          s"${taxRegime.entryName} Existing ttp ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(origin))
            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeAlreadyHaveAPaymentPlanPage(fakeRequest)
              case TaxRegime.Vat   => controller.vatAlreadyHaveAPaymentPlanPage(fakeRequest)
              case TaxRegime.Sa    => controller.saAlreadyHaveAPaymentPlanPage(fakeRequest)
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
              case TaxRegime.Epaye => "You cannot set up an Employers’ PAYE payment plan online because you already have a payment plan with HMRC."
              case TaxRegime.Vat   => "You cannot set up a VAT payment plan online because you already have a payment plan with HMRC."
              case TaxRegime.Sa    => "You cannot set up a Self Assessment payment plan online because you already have a payment plan with HMRC."
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

            val (result, expectedH1) = taxRegime match {
              case TaxRegime.Epaye =>
                (controller.epayeFileYourReturnPage(fakeRequest), "File your return to use this service")
              case TaxRegime.Vat =>
                (controller.vatFileYourReturnPage(fakeRequest), "File your return to use this service")
              case TaxRegime.Sa =>
                (controller.saFileYourReturnPage(fakeRequest), "File your Self Assessment tax return to use this service")
            }

            val expectedCallUsContent = "Call us on <strong>0300 123 1813</strong> if you need to speak to an adviser."

            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = expectedH1,
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val expectedLeadingContent = taxRegime match {
              case TaxRegime.Epaye => "You must file your tax return before you can set up an Employers’ PAYE payment plan online."
              case TaxRegime.Vat   => "You must file your tax return before you can set up a VAT payment plan online."
              case TaxRegime.Sa    => "You must file your tax return before you can set up a Self Assessment payment plan online."
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingContent
            )

            ContentAssertions.commonIneligibilityTextCheck(
              page,
              taxRegime,
              Languages.English,
              expectCallPreparationHints = false,
              callUsContentEnglish       = expectedCallUsContent
            )

            val fileYourTaxReturnLink = page.select("p.govuk-body").first().select("a")
            fileYourTaxReturnLink.text() shouldBe "file your tax return"
            fileYourTaxReturnLink.attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"

            page.select(".govuk-body").asScala.toList(1).text() shouldBe "If you have recently filed your return, your account can take up to 3 days to update. Try again after 3 days."

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

    Seq[(TaxRegime, Origin)](
      (TaxRegime.Epaye, Origins.Epaye.Bta),
      (TaxRegime.Vat, Origins.Vat.Bta),
      (TaxRegime.Sa, Origins.Sa.Bta)
    )
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
              case TaxRegime.Sa    => controller.saGenericIneligiblePage(fakeRequest.withLangWelsh())
            }
            val page = pageContentAsDoc(result)
            val expectedLeadingP1 = taxRegime match {
              case TaxRegime.Epaye => "Nid ydych yn gymwys i drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein."
              case TaxRegime.Vat   => "Nid ydych yn gymwys i drefnu cynllun talu TAW ar-lein."
              case TaxRegime.Sa    => "Nid ydych yn gymwys i drefnu cynllun talu Hunanasesiad ar-lein."
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
              case TaxRegime.Sa    => controller.saDebtTooLargePage(fakeRequest.withLangWelsh())
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
              case TaxRegime.Sa    => "Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd mae arnoch dros £30,000."
            }

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingP1
            )
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh)
          }

          s"${taxRegime.entryName} Debt too old ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(origin))

            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeDebtTooOldPage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatDebtTooOldPage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => controller.saDebtTooOldPage(fakeRequest.withLangWelsh())
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
              case TaxRegime.Sa    => "Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros 60 diwrnod yn ôl."
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
              case TaxRegime.Sa    => controller.saDebtTooSmallPage(fakeRequest.withLangWelsh())
            }
            val page = pageContentAsDoc(result)

            val expectedH1 = taxRegime match {
              case TaxRegime.Epaye => "Talu’ch bil TWE yn llawn"
              case TaxRegime.Vat   => "Talu’ch bil TAW yn llawn"
              case TaxRegime.Sa    => "Talu’ch bil treth Hunanasesiad yn llawn"
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
              case TaxRegime.Sa =>
                "Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd bod eich bil yn rhy fach." ->
                  "<a class=\"govuk-link\" href=\"https://www.gov.uk/pay-self-assessment-tax-bill\">Gwnewch daliad ar-lein</a> i dalu’ch bil Hunanasesiad yn llawn."

            }

            val callUsContentWelsh = "Os ydych yn cael anawsterau wrth dalu ar-lein, ffoniwch ni ar <strong>0300 200 1900</strong>."

            val leadingParagraphs = page.select(".govuk-body").asScala.toList
            leadingParagraphs(0).html() shouldBe expectedParagraph1
            leadingParagraphs(1).html() shouldBe expectedParagraph2
            ContentAssertions.commonIneligibilityTextCheck(page, taxRegime, Languages.Welsh, callUsContentWelsh = callUsContentWelsh, showFullListPreparationTips = false)
          }

          s"${taxRegime.entryName} Existing ttp ineligible page correctly" in {
            stubCommonActions(authAllEnrolments = enrolments)
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(origin))
            val result: Future[Result] = taxRegime match {
              case TaxRegime.Epaye => controller.epayeAlreadyHaveAPaymentPlanPage(fakeRequest.withLangWelsh())
              case TaxRegime.Vat   => controller.vatAlreadyHaveAPaymentPlanPage(fakeRequest.withLangWelsh())
              case TaxRegime.Sa    => controller.saAlreadyHaveAPaymentPlanPage(fakeRequest.withLangWelsh())
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
              case TaxRegime.Epaye => "Ni allwch drefnu cynllun talu ar-lein ar gyfer TWE y Cyflogwr oherwydd bod gennych gynllun talu ar-lein gyda CThEF yn barod."
              case TaxRegime.Vat   => "Ni allwch drefnu cynllun talu ar-lein ar gyfer TAW oherwydd bod gennych gynllun talu gyda CThEF yn barod."
              case TaxRegime.Sa    => "Ni allwch drefnu cynllun talu ar-lein ar gyfer Hunanasesiad oherwydd bod gennych gynllun talu gyda CThEF yn barod."
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

            val (result, expectedH1) = taxRegime match {
              case TaxRegime.Epaye =>
                (controller.epayeFileYourReturnPage(fakeRequest.withLangWelsh()), "Cyflwynwch eich Ffurflen Dreth i ddefnyddio’r gwasanaeth hwn")
              case TaxRegime.Vat =>
                (controller.vatFileYourReturnPage(fakeRequest.withLangWelsh()), "Cyflwynwch eich Ffurflen Dreth i ddefnyddio’r gwasanaeth hwn")
              case TaxRegime.Sa =>
                (controller.saFileYourReturnPage(fakeRequest.withLangWelsh()), "Cyflwynwch eich Ffurflen Dreth Hunanasesiad er mwyn defnyddio’r gwasanaeth hwn")
            }
            val page = pageContentAsDoc(result)

            ContentAssertions.commonPageChecks(
              page,
              expectedH1              = expectedH1,
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime),
              language                = Languages.Welsh
            )

            val expectedLeadingContent = taxRegime match {
              case TaxRegime.Epaye => "Mae’n rhaid i chi gyflwyno’ch Ffurflen Dreth cyn i chi allu trefnu cynllun talu ar-lein ar gyfer TWE y Cyflogwr."
              case TaxRegime.Vat   => "Mae’n rhaid i chi gyflwyno’ch Ffurflen Dreth cyn i chi allu trefnu cynllun talu ar-lein ar gyfer TAW."
              case TaxRegime.Sa    => "Mae’n rhaid i chi gyflwyno’ch Ffurflen Dreth cyn i chi allu trefnu cynllun talu ar-lein ar gyfer Hunanasesiad ar-lein."
            }

            val expectedCallUsContent = "Ffoniwch ni ar <strong>0300 200 1900</strong> os oes angen i chi siarad ag ymgynghorydd."

            assertIneligiblePageLeadingP1(
              page      = page,
              leadingP1 = expectedLeadingContent
            )

            ContentAssertions.commonIneligibilityTextCheck(
              page,
              taxRegime,
              Languages.Welsh,
              expectCallPreparationHints = false,
              callUsContentWelsh         = expectedCallUsContent
            )

            val fileYourTaxReturnLink = page.select("p.govuk-body").first().select("a")
            fileYourTaxReturnLink.text() shouldBe "gyflwyno’ch Ffurflen Dreth"
            fileYourTaxReturnLink.attr("href") shouldBe "/set-up-a-payment-plan/test-only/bta-page?return-page"

            page.select(".govuk-body").asScala.toList(1).text() shouldBe "Os ydych wedi cyflwyno’ch Ffurflen Dreth yn ddiweddar, gall gymryd hyd at 3 diwrnod i ddiweddaru’ch cyfrif. Rhowch gynnig arall arni ar ôl 3 diwrnod."
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
