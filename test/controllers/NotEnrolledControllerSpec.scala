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
import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import models.Languages
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

    "return the not enrolled page in English" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notEnrolled(fakeRequest)
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Enrol for PAYE Online to use this service",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None
      )

      page.select(".govuk-body").asScala.toList(0).html() shouldBe s"""You must <a href="https://www.gov.uk/paye-online/enrol" class="govuk-link">enrol for PAYE Online</a> before you can set up an Employers’ PAYE payment plan."""
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Epaye, Languages.English)
    }

    "return the not enrolled page in Welsh" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notEnrolled(fakeRequest.withLangWelsh())
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Ymrestru ar gyfer TWE Ar-lein er mwyn defnyddio’r gwasanaeth hwn",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        language                = Languages.Welsh
      )

      page.select(".govuk-body").asScala.toList(0).html() shouldBe s"""Mae’n rhaid i chi <a href="https://www.gov.uk/paye-online/enrol" class="govuk-link">ymrestru ar gyfer TWE Ar-lein</a> cyn i chi allu trefnu cynllun talu ar gyfer TWE y Cyflogwr."""
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Epaye, Languages.Welsh)
    }

  }

  "GET /not-vat-registered should" - {

    "return the not vat registered page in English" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notVatRegistered(fakeRequest)
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Register for VAT online to use this service",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        regimeBeingTested       = Some(TaxRegime.Vat)
      )

      page.select(".govuk-body").asScala.toList(0).html() shouldBe s"""You must <a href="https://www.gov.uk/register-for-vat" class="govuk-link">register for VAT online</a> before you can set up a VAT payment plan."""
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Vat, Languages.English)
    }

    "return the not vat registered page in Welsh" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notVatRegistered(fakeRequest.withLangWelsh())
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Cofrestru ar gyfer TAW ar-lein er mwyn defnyddio’r gwasanaeth hwn",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        regimeBeingTested       = Some(TaxRegime.Vat),
        language                = Languages.Welsh
      )

      page.select(".govuk-body").asScala.toList(0).html() shouldBe s"""Mae’n rhaid i chi <a href="https://www.gov.uk/register-for-vat" class="govuk-link">gofrestru ar gyfer TAW ar-lein</a> cyn i chi allu trefnu cynllun talu ar gyfer TAW."""
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Vat, Languages.Welsh)
    }

  }

  "GET /request-access-to-self-assessment should" - {

    "return the not enrolled page in English" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notSaEnrolled(fakeRequest)
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Request access to Self Assessment to use this service",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None
      )

      page.select(".govuk-body").asScala.toList(0).html() shouldBe (
        "You must " +
        """<a href="https://www.tax.service.gov.uk/business-account/add-tax/self-assessment/enter-sa-utr?origin=ssttp-sa" class="govuk-link">request access to Self Assessment</a> """ +
        "before you can set up a Self Assessment payment plan."
      )
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Sa, Languages.English)
    }

    "return the not enrolled page in Welsh" in {
      stubCommonActions(authAllEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.notSaEnrolled(fakeRequest.withLangWelsh())
      val page: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        page,
        expectedH1              = "Gwneud cais i gael mynediad at eich cyfrif Hunanasesiad er mwyn defnyddio’r gwasanaeth hwn",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        language                = Languages.Welsh
      )

      page.select(".govuk-body").asScala.toList(0).html() shouldBe (
        "Mae’n rhaid i chi " +
        """<a href="https://www.tax.service.gov.uk/business-account/add-tax/self-assessment/enter-sa-utr?origin=ssttp-sa" class="govuk-link">wneud cais i gael mynediad at eich cyfrif Hunanasesiad</a> """ +
        "cyn i chi allu trefnu cynllun talu Hunanasesiad ar-lein."
      )
      ContentAssertions.commonIneligibilityTextCheck(page, TaxRegime.Sa, Languages.Welsh)
    }

  }
}
