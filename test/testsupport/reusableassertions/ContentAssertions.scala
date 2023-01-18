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

package testsupport.reusableassertions

import controllers.routes
import essttp.rootmodel.TaxRegime
import models.{Language, Languages}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import testsupport.RichMatchers
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import testsupport.testdata.TdAll

import scala.annotation.nowarn
import scala.jdk.CollectionConverters.IterableHasAsScala

object ContentAssertions extends RichMatchers {

  def assertListOfContent(elements: Elements)(expectedContent: List[String]) = {
    elements.asScala.toList.zip(expectedContent)
      .map { case (element, expectedText) => element.text() shouldBe expectedText }
  }

  //used for summary lists
  def assertKeyAndValue(element: Element, keyValue: (String, String)): Assertion = {
    element.select(".govuk-summary-list__key").text() shouldBe keyValue._1
    element.select(".govuk-summary-list__value").text() shouldBe keyValue._2
  }

  def languageToggleExists(document: Document, selectedLanguage: Language): Assertion = {
    val langToggleItems: List[Element] = document.select(".hmrc-language-select__list-item").asScala.toList
    langToggleItems.size shouldBe 2

    val englishOption = langToggleItems(0)
    val welshOption = langToggleItems(1)

    selectedLanguage match {
      case Languages.English =>
        englishOption.text() shouldBe "English"

        welshOption.select("a").attr("hreflang") shouldBe "cy"
        welshOption.select("span.govuk-visually-hidden").text() shouldBe "Newid yr iaith ir Gymraeg"
        welshOption.select("span[aria-hidden=true]").text() shouldBe "Cymraeg"

      case Languages.Welsh =>
        englishOption.select("a").attr("hreflang") shouldBe "en"
        englishOption.select("span.govuk-visually-hidden").text() shouldBe "Change the language to English"
        englishOption.select("span[aria-hidden=true]").text() shouldBe "English"

        welshOption.text() shouldBe "Cymraeg"

    }

  }

  @nowarn
  def commonPageChecks(
      page:                        Document,
      expectedH1:                  String,
      shouldBackLinkBePresent:     Boolean,
      expectedSubmitUrl:           Option[String],
      signedIn:                    Boolean           = true,
      hasFormError:                Boolean           = false,
      shouldH1BeSameAsServiceName: Boolean           = false,
      regimeBeingTested:           Option[TaxRegime] = Some(TaxRegime.Epaye),
      language:                    Language          = Languages.English,
      shouldServiceNameBeInHeader: Boolean           = true
  ): Unit = {
    val titlePrefix = if (hasFormError) {
      language match {
        case Languages.English => "Error: "
        case Languages.Welsh   => "Gwall: "
      }
    } else ""

    val regimeServiceName =
      language match {
        case Languages.English =>
          regimeBeingTested match {
            case Some(TaxRegime.Epaye) => TdAll.expectedServiceNamePayeEn
            case Some(TaxRegime.Vat)   => TdAll.expectedServiceNameVatEn
            case None                  => TdAll.expectedServiceNameGenericEn
          }
        case Languages.Welsh =>
          regimeBeingTested match {
            case Some(TaxRegime.Epaye) => TdAll.expectedServiceNamePayeCy
            case Some(TaxRegime.Vat)   => TdAll.expectedServiceNameVatCy
            case None                  => TdAll.expectedServiceNameGenericCy
          }
      }

    if (shouldH1BeSameAsServiceName) {
      expectedH1 shouldBe regimeServiceName
      page.title() shouldBe s"$titlePrefix$expectedH1 - GOV.UK"
    } else {
      expectedH1 shouldNot be(regimeServiceName)
      page.title() shouldBe s"$titlePrefix$expectedH1 - $regimeServiceName - GOV.UK"
    }

    page.select(".hmrc-header__service-name").text() shouldBe (if (shouldServiceNameBeInHeader) regimeServiceName else "")

    page.select("h1").text() shouldBe expectedH1
    ContentAssertions.languageToggleExists(page, language)

    val signOutLink = page.select(".hmrc-sign-out-nav__link")
    if (signedIn) signOutLink.attr("href") shouldBe routes.SignOutController.signOut.url
    else signOutLink.isEmpty shouldBe true

    val backLink = page.select(".govuk-back-link")
    if (shouldBackLinkBePresent) backLink.hasClass("js-visible") shouldBe true
    else backLink.isEmpty shouldBe true

    if (hasFormError) {
      val expectedText = language match {
        case Languages.English => "Error:"
        case Languages.Welsh   => "Gwall:"
      }
      page.select(".govuk-error-message > .govuk-visually-hidden").text shouldBe expectedText
    }

    val form = page.select("form")
    expectedSubmitUrl match {
      case None         => form.isEmpty shouldBe true
      case Some(submit) => form.attr("action") shouldBe submit
    }

    val footerLinks = page.select(".govuk-footer__link").asScala.toList
    footerLinks(1).attr("href") should startWith("http://localhost:12346/accessibility-statement/set-up-a-payment-plan")
  }

  def formSubmitShouldDisableSubmitButton(doc: Document): Unit = {
    doc.select("form").hasClass("prevent-multiple-submits") shouldBe true

    val button = doc.select("form > .govuk-button")
    button.hasClass("disable-on-click") shouldBe true
    button.attr("data-prevent-double-click") shouldBe "true"
    ()
  }

  def commonIneligibilityTextCheck(doc: Document, taxRegime: TaxRegime) = {
    val commonEligibilityWrapper = doc.select("#common-eligibility")
    val govukBodyElements = commonEligibilityWrapper.select(".govuk-body").asScala.toList
    govukBodyElements(0).text() shouldBe "For further support you can contact the Payment Support Service on 0300 200 3835 to speak to an advisor."

    val subheadings = commonEligibilityWrapper.select("h2").asScala.toList

    subheadings(0).text shouldBe "If you need extra support"
    govukBodyElements(1).html() shouldBe "Find out the different ways to <a href=\"https://www.gov.uk/get-help-hmrc-extra-support\" class=\"govuk-link\">deal with HMRC if you need some help</a>."
    govukBodyElements(2).html() shouldBe "You can also use <a href=\"https://www.relayuk.bt.com/\" class=\"govuk-link\">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."
    govukBodyElements(3).html() shouldBe "If you are outside the UK: <strong>+44 2890 538 192</strong>"

    subheadings(1).text shouldBe "Before you call, make sure you have:"
    val bulletLists = commonEligibilityWrapper.select(".govuk-list").asScala.toList
    val beforeYouCallList = bulletLists(0).select("li").asScala.toList
    beforeYouCallList(0).text() shouldBe (
      taxRegime match {
        case TaxRegime.Epaye => "your Accounts Office reference. This is 13 characters, for example, 123PX00123456"
        case TaxRegime.Vat   => "your VAT number. This is 9 characters, for example, 123456789"
      }
    )
    beforeYouCallList(1).text() shouldBe "your bank details"

    subheadings(2).text() shouldBe "We’re likely to ask:"
    val likelyToAskList = bulletLists(1).select("li").asScala.toList
    likelyToAskList(0).text() shouldBe "what you’ve done to try to pay the bill"
    likelyToAskList(1).text() shouldBe "if you can pay some of the bill now"

    govukBodyElements(4).text() shouldBe "Our opening times are Monday to Friday: 8am to 6pm (we are closed on bank holidays)"
  }

}
