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
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Assertion
import testsupport.RichMatchers
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
      shouldServiceNameBeInHeader: Boolean           = true,
      backLinkUrlOverride:         Option[String]    = None
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
            case Some(TaxRegime.Sa)    => TdAll.expectedServiceNameSaEn
            case None                  => TdAll.expectedServiceNameGenericEn
          }
        case Languages.Welsh =>
          regimeBeingTested match {
            case Some(TaxRegime.Epaye) => TdAll.expectedServiceNamePayeCy
            case Some(TaxRegime.Vat)   => TdAll.expectedServiceNameVatCy
            case Some(TaxRegime.Sa)    => TdAll.expectedServiceNameSaCy
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

    val serviceName = page.select(".govuk-header__service-name")

    serviceName.is("a") shouldBe regimeBeingTested.isDefined && shouldServiceNameBeInHeader
    serviceName.text() shouldBe (if (shouldServiceNameBeInHeader) regimeServiceName else "")
    serviceName.attr("href") shouldBe (if (shouldServiceNameBeInHeader) regimeBeingTested match {
      case Some(TaxRegime.Epaye) => routes.LandingController.epayeLandingPage.url
      case Some(TaxRegime.Vat)   => routes.LandingController.vatLandingPage.url
      case Some(TaxRegime.Sa)    => routes.LandingController.saLandingPage.url
      case None                  => ""
    }
    else "")

    page.select("h1").text() shouldBe expectedH1
    ContentAssertions.languageToggleExists(page, language)

    val signOutLink = page.select(".hmrc-sign-out-nav__link")
    if (signedIn) signOutLink.attr("href") shouldBe routes.SignOutController.signOut.url
    else signOutLink.isEmpty shouldBe true

    val backLink = page.select(".govuk-back-link")
    if (shouldBackLinkBePresent) {
      backLinkUrlOverride match {
        case Some(url) =>
          backLink.hasClass("js-visible") shouldBe false
          backLink.attr("href") shouldBe url
        case None =>
          backLink.hasClass("js-visible") shouldBe true
          backLink.attr("href") shouldBe "#"
      }
    } else backLink.isEmpty shouldBe true

    val backLinkJavascript = page.select("script[src=\"/set-up-a-payment-plan/assets/javascripts/back-link.js\"]")
    if (shouldBackLinkBePresent && backLinkUrlOverride.isEmpty) backLinkJavascript.isEmpty shouldBe false
    else backLinkJavascript.isEmpty shouldBe true

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

  def welshOrEnglish(language: Language, expectedEnglish: String, expectedWelsh: String): String = {
    language match {
      case Languages.English => expectedEnglish
      case Languages.Welsh   => expectedWelsh
    }
  }

  val (defaultCallUsContentEnglish, defaultCallUsContentWelsh) =
    "Call us on <strong>0300 123 1813</strong> as you may be able to set up a plan over the phone." ->
      "Ffoniwch ni ar <strong>0300 200 1900</strong> oherwydd mae’n bosibl y gallwch drefnu cynllun dros y ffôn."

  def commonIneligibilityTextCheck(
      doc:                         Document,
      taxRegime:                   TaxRegime,
      language:                    Language,
      callUsContentEnglish:        String    = ContentAssertions.defaultCallUsContentEnglish,
      callUsContentWelsh:          String    = ContentAssertions.defaultCallUsContentWelsh,
      expectCallPreparationHints:  Boolean   = true,
      showFullListPreparationTips: Boolean   = true
  ): Unit = {

    val callUsContentFromDoc = doc.select("#call-us-content")
    callUsContentFromDoc.html() shouldBe {
      language match {
        case Languages.English => callUsContentEnglish
        case Languages.Welsh   => callUsContentWelsh
      }
    }

    val commonEligibilityWrapper = doc.select("#common-eligibility")
    val govukBodyElements = commonEligibilityWrapper.select(".govuk-body").asScala.toList

    govukBodyElements(0).html() shouldBe {
      language match {
        case Languages.English => "Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays."
        case Languages.Welsh   => "Ein horiau agor yw Dydd Llun i Ddydd Gwener, 08:30 i 17:00. Rydym ar gau ar benwythnosau a gwyliau banc."
      }
    }

    val subheadings = commonEligibilityWrapper.select("h2").asScala.toList
    subheadings.size shouldBe {
      language match {
        case Languages.English => if (expectCallPreparationHints) 4 else 2
        case Languages.Welsh   => if (expectCallPreparationHints) 3 else 1
      }
    }

    if (expectCallPreparationHints) {
      subheadings(0).text shouldBe {
        language match {
          case Languages.English => "Before you call, make sure you have:"
          case Languages.Welsh   => "Cyn i chi ffonio, sicrhewch fod gennych y canlynol:"
        }
      }
      val bulletLists = commonEligibilityWrapper.select(".govuk-list").asScala.toList
      val beforeYouCallList = bulletLists(0).select("li").asScala.toList
      val expectedBeforeYouCallBullets = {
        language match {
          case Languages.English =>
            taxRegime match {
              case TaxRegime.Epaye => List(
                "your Accounts Office reference which is 13 characters long, like 123PX00123456",
                "your bank details"
              )
              case TaxRegime.Vat =>
                List(
                  "your VAT registration number which is 9 digits long, like 123456789",
                  "your bank details"
                )
              case TaxRegime.Sa => if (showFullListPreparationTips) {
                List(
                  "your Self Assessment Unique Taxpayer Reference (UTR) which can be 10 or 13 digits long",
                  "information on any savings or investments you have",
                  "your bank details",
                  "details of your income and spending"
                )
              } else {
                List(
                  "your Self Assessment Unique Taxpayer Reference (UTR) which can be 10 or 13 digits long",
                  "your bank details"
                )
              }
            }
          case Languages.Welsh =>
            taxRegime match {
              case TaxRegime.Epaye => List(
                "eich cyfeirnod Swyddfa Gyfrifon, sy’n 13 o gymeriadau o hyd, er enghraifft 123PX00123456",
                "eich manylion banc"
              )
              case TaxRegime.Vat =>
                List(
                  "eich rhif cofrestru TAW, sy’n 9 digid o hyd, er enghraifft 123456789",
                  "eich manylion banc"
                )
              case TaxRegime.Sa => if (showFullListPreparationTips) {
                List(
                  "eich Cyfeirnod Unigryw y Trethdalwr (UTR) ar gyfer Hunanasesiad a allai fod yn 10 neu 13 digid o hyd",
                  "gwybodaeth am unrhyw gynilion neu fuddsoddiadau sydd gennych",
                  "eich manylion banc",
                  "manylion eich incwm a’ch gwariant"
                )
              } else {
                List(
                  "eich Cyfeirnod Unigryw y Trethdalwr (UTR) ar gyfer Hunanasesiad a allai fod yn 10 neu 13 digid o hyd",
                  "eich manylion banc"
                )
              }
            }
        }
      }

      beforeYouCallList.map(_.text()) shouldBe expectedBeforeYouCallBullets

      subheadings(1).text() shouldBe {
        language match {
          case Languages.English => "We’re likely to ask:"
          case Languages.Welsh   => "Rydym yn debygol o ofyn:"
        }
      }
      val likelyToAskList = bulletLists(1).select("li").asScala.toList
      likelyToAskList(0).text() shouldBe {
        language match {
          case Languages.English => "what you’ve done to try to pay the bill"
          case Languages.Welsh   => "beth rydych wedi’i wneud i geisio talu’r bil"
        }
      }
      likelyToAskList(1).text() shouldBe {
        language match {
          case Languages.English => "if you can pay some of the bill now"
          case Languages.Welsh   => "a allwch dalu rhywfaint o’r bil nawr"
        }
      }
      ()

      subheadings(2).text() shouldBe {
        language match {
          case Languages.English => "If you need extra support"
          case Languages.Welsh   => "Os oes angen cymorth ychwanegol arnoch chi"
        }
      }
      govukBodyElements(1).html() shouldBe {
        language match {
          case Languages.English => """Find out the different ways to <a href="https://www.gov.uk/get-help-hmrc-extra-support" class="govuk-link">deal with HMRC if you need some help</a>."""
          case Languages.Welsh   => """Dysgwch am y ffyrdd gwahanol o <a href="https://www.gov.uk/get-help-hmrc-extra-support" class="govuk-link">ddelio â CThEF os oes angen help arnoch chi</a>."""
        }
      }
      govukBodyElements(2).html() shouldBe {
        language match {
          case Languages.English => """You can also use <a href="https://www.relayuk.bt.com/" class="govuk-link">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."""
          case Languages.Welsh   => """Gallwch hefyd ddefnyddio <a href="https://www.relayuk.bt.com/" class="govuk-link">Relay UK</a> os na allwch glywed na siarad dros y ffôn: deialwch <strong>18001</strong> ac yna <strong>0345 300 3900</strong>. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw."""
        }
      }
      ()

      if (language === Languages.English) {
        subheadings(3).text() shouldBe "If you’re calling from outside the UK"
        govukBodyElements(3).html() shouldBe "Call us on <strong>+44 2890 538 192</strong>."
        govukBodyElements(4).html() shouldBe "Our opening times are Monday to Friday, 8am to 6pm (UK time). We are closed on weekends and bank holidays."
        ()
      }
    }
  }

}
