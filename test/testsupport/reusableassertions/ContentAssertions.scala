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

package testsupport.reusableassertions

import controllers.routes
import essttp.rootmodel.TaxRegime
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatest.Assertion
import testsupport.RichMatchers
import org.jsoup.nodes.Document
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

  def languageToggleExists(document: Document): Assertion = {
    val langToggleItems: List[Element] = document.select(".hmrc-language-select__list-item").asScala.toList
    langToggleItems.size shouldBe 2
    langToggleItems.headOption.map(someToggleItem => someToggleItem.text()) shouldBe Some("English")

    val welshOption = langToggleItems.drop(1).headOption
    welshOption.map(_.select("a").attr("hreflang")) shouldBe Some("cy")
    welshOption.map(_.select("span.govuk-visually-hidden").text()) shouldBe Some("Newid yr iaith ir Gymraeg")
    welshOption.map(_.select("span[aria-hidden=true]").text()) shouldBe Some("Cymraeg")
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
      regimeBeingTested:           Option[TaxRegime] = Some(TaxRegime.Epaye)
  ): Unit = {
    val titlePrefix = if (hasFormError) "Error: " else ""

    val regimeServiceName = regimeBeingTested match {
      case Some(TaxRegime.Epaye) => TdAll.expectedServiceNamePaye
      case Some(TaxRegime.Vat)   => TdAll.expectedServiceNameVat
      case None                  => TdAll.expectedServiceNameGeneric
    }

    if (shouldH1BeSameAsServiceName) {
      expectedH1 shouldBe regimeServiceName
      page.title() shouldBe s"$titlePrefix$expectedH1 - GOV.UK"
    } else {
      expectedH1 shouldNot be(regimeServiceName)
      page.title() shouldBe s"$titlePrefix$expectedH1 - $regimeServiceName - GOV.UK"
    }

    page.select(".hmrc-header__service-name").text() shouldBe regimeServiceName

    page.select("h1").text() shouldBe expectedH1
    ContentAssertions.languageToggleExists(page)

    val signOutLink = page.select(".hmrc-sign-out-nav__link")
    if (signedIn) signOutLink.attr("href") shouldBe routes.SignOutController.signOut.url
    else signOutLink.isEmpty shouldBe true

    val backLink = page.select(".govuk-back-link")
    if (shouldBackLinkBePresent) backLink.hasClass("js-visible") shouldBe true
    else backLink.isEmpty shouldBe true

    if (hasFormError)
      page.select(".govuk-error-message > .govuk-visually-hidden").text shouldBe "Error:"

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

    val subheadings = commonEligibilityWrapper.select("h2").asScala.toList

    subheadings(0).text shouldBe "If you need extra support"
    govukBodyElements(0).html() shouldBe "Find out the different ways to <a href=\"https://www.gov.uk/get-help-hmrc-extra-support\" class=\"govuk-link\">deal with HMRC if you need some help</a>."
    govukBodyElements(1).html() shouldBe "You can also use <a href=\"https://www.relayuk.bt.com/\" class=\"govuk-link\">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."
    govukBodyElements(2).html() shouldBe "If you are outside the UK: <strong>+44 2890 538 192</strong>"

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

    govukBodyElements(3).text() shouldBe "Our opening times are Monday to Friday: 8am to 6pm (we are closed on bank holidays)"
  }

}
