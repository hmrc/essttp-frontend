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
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatest.Assertion
import testsupport.RichMatchers

import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter
import org.jsoup.nodes.Document
import testsupport.testdata.TdAll

import scala.annotation.nowarn

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
      page:              Document,
      expectedH1:        String,
      expectedBack:      Option[String],
      expectedSubmitUrl: Option[String],
      signedIn:          Boolean        = true,
      hasFormError:      Boolean        = false
  ): Unit = {
    val expectedServiceName: String = TdAll.expectedServiceNamePaye
    val titlePrefix = if (hasFormError) "Error: " else ""
    page.title() shouldBe s"$titlePrefix$expectedH1 - $expectedServiceName - GOV.UK"
    page.select(".hmrc-header__service-name").text() shouldBe expectedServiceName

    page.select("h1").text() shouldBe expectedH1
    ContentAssertions.languageToggleExists(page)

    val signOutLink = page.select(".hmrc-sign-out-nav__link")
    if (signedIn) signOutLink.attr("href") shouldBe routes.SignOutController.signOut.url
    else signOutLink.isEmpty shouldBe true

    val backLink = page.select("#back")

    expectedBack match {
      case None       => backLink.isEmpty shouldBe true
      case Some(back) => backLink.attr("href") shouldBe back
    }

    val form = page.select("form")
    expectedSubmitUrl match {
      case None         => form.isEmpty shouldBe true
      case Some(submit) => form.attr("action") shouldBe submit
    }

  }
}
