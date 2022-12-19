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
import essttp.rootmodel.TaxRegime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.jdk.CollectionConverters.IterableHasAsScala

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
        expectedSubmitUrl       = None
      )

      page.select(".govuk-body").asScala.toList(0).text() shouldBe "You are not eligible for an online payment plan because you need to enrol for PAYE Online. Find out how to enrol."
      page.select(".govuk-body").asScala.toList(1).text() shouldBe "If you need to speak to an adviser call us on 0300 200 3835 at the Business Support Service to talk about your payment options."
      page.select("#how-to-enrol-link").attr("href") shouldBe "https://www.gov.uk/paye-online/enrol"

      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Epaye)
    }
  }

  "GET /not-vat-registered should" - {
    "return the not vat registered page" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notVatRegistered(fakeRequest)
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "You are not registered",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        regimeBeingTested       = Some(TaxRegime.Vat)
      )

      page.select(".govuk-body").asScala.toList(0).text() shouldBe "You are not eligible for an online payment plan because you need to register for VAT Online. Find out how to register."
      page.select(".govuk-body").asScala.toList(1).text() shouldBe "If you need to speak to an adviser call us on 0300 200 3835 at the Business Support Service to talk about your payment options."
      page.select("#how-to-enrol-link").attr("href") shouldBe "https://www.gov.uk/register-for-vat"

      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Vat)
    }
  }
}
