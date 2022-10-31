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
import essttp.journey.model.Origins
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.JourneyJsonTemplates
import uk.gov.hmrc.http.SessionKeys

import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class NotEnrolledControllerSpec extends ItSpec {
  private val controller = app.injector.instanceOf[NotEnrolledController]
  "GET /not-enrolled should" - {
    "return the not enrolled page" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notEnrolled(fakeRequest)
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "You are not enrolled",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        regimeBeingTested       = None
      )

      page.select(".govuk-body").asScala.toList(0).text() shouldBe "You are not eligible for an online payment plan because you need to enrol for PAYE Online. Find out how to enrol."
      page.select("#how-to-enrol-link").attr("href") shouldBe "https://www.gov.uk/paye-online/enrol"

      val commonEligibilityWrapper = page.select(".common-eligibility")
      val govukBodyElements = commonEligibilityWrapper.select(".govuk-body").asScala.toList
      govukBodyElements(0).text() shouldBe "If you need to speak to an adviser call us on 0300 200 3835 at the Business Support Service to talk about your payment options."

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
  }

  "GET /not-vat-registered should" - {
    "return the not vat registered page" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney(jsonBody = JourneyJsonTemplates.Started(Origins.Vat.GovUk))

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notVatRegistered(fakeRequest)
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "You are not registered",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None
      )

      page.select(".govuk-body").asScala.toList(0).text() shouldBe "You are not eligible for an online payment plan because you need to register for VAT Online. Find out how to register."
      page.select("#how-to-enrol-link").attr("href") shouldBe "https://www.gov.uk/register-for-vat"

      val commonEligibilityWrapper = page.select(".common-eligibility")
      val govukBodyElements = commonEligibilityWrapper.select(".govuk-body").asScala.toList
      govukBodyElements(0).text() shouldBe "If you need to speak to an adviser call us on 0300 200 3835 at the Business Support Service to talk about your payment options."

      val detailsReveal = commonEligibilityWrapper.select(".govuk-details")
      detailsReveal.select(".govuk-details__summary-text").text() shouldBe "If you cannot use speech recognition software"
      val detailsRevealText = detailsReveal.select(".govuk-details__text").select(".govuk-body").asScala.toList
      detailsRevealText(0).html() shouldBe "Find out how to <a href=\"https://www.gov.uk/get-help-hmrc-extra-support\" class=\"govuk-link\">deal with HMRC if you need extra support</a>."
      detailsRevealText(1).html() shouldBe "You can also use <a href=\"https://www.relayuk.bt.com/\" class=\"govuk-link\">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."
      detailsRevealText(2).html() shouldBe "If you are outside the UK: <strong>+44 2890 538 192</strong>"
      govukBodyElements(4).text() shouldBe "Before you call, make sure you have:"

      val bulletLists = commonEligibilityWrapper.select(".govuk-list").asScala.toList
      val beforeYouCallList = bulletLists(0).select("li").asScala.toList
      beforeYouCallList(0).text() shouldBe "your VAT number. This is 9 characters, for example, 1233456789"
      beforeYouCallList(1).text() shouldBe "your bank details"

      govukBodyElements(5).text() shouldBe "We’re likely to ask:"
      val likelyToAskList = bulletLists(1).select("li").asScala.toList
      likelyToAskList(0).text() shouldBe "what you’ve done to try to pay the bill"
      likelyToAskList(1).text() shouldBe "if you can pay some of the bill now"

      govukBodyElements(6).text() shouldBe "Our opening times are Monday to Friday: 8am to 6pm (we are closed on bank holidays)"
    }
  }
}
