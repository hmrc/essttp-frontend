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

package actionrefiners

import config.AppConfig
import controllers.{YourBillController, routes}
import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import models.{Language, Languages}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.reusableassertions.ContentAssertions
import testsupport.stubs.{AuthStub, EssttpBackend}

trait ShutteringSpec { this: ItSpec =>

  lazy val appConfig = app.injector.instanceOf[AppConfig]

  def assertShutteringPageContent(
    doc:       Document,
    taxRegime: Option[TaxRegime],
    language:  Language
  ): Unit = {

    val (expectedH1, expectedLink) = language match {
      case Languages.English =>
        ("Sorry, the service is unavailable", appConfig.Urls.businessPaymentSupportService)
      case Languages.Welsh   =>
        ("Mae’n ddrwg gennym – nid yw’r gwasanaeth ar gael", appConfig.Urls.welshLanguageHelplineForDebtManagement)
    }

    ContentAssertions.commonPageChecks(
      doc,
      expectedH1,
      shouldBackLinkBePresent = false,
      expectedSubmitUrl = None,
      regimeBeingTested = taxRegime,
      language = language
    )

    val link = doc.select("p.govuk-body > a.govuk-link")
    link.attr("href") shouldBe expectedLink
    ()
  }

}

class EpayeShutteringSpec extends ItSpec, ShutteringSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "shuttering.shuttered-tax-regimes" -> List("epaye")
  )

  lazy val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "When EPAYE is shuttered" - {
    "the shutter page should be shown when the tax regime in the journey is EPAYE" - {

      "in english" in {
        AuthStub.authorise()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.EpayeService)()

        val result = controller.yourBill(fakeRequest)
        status(result) shouldBe Status.OK
        val doc    = Jsoup.parse(contentAsString(result))

        assertShutteringPageContent(doc, Some(TaxRegime.Epaye), Languages.English)
      }

      "in welsh" in {
        AuthStub.authorise()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.GovUk)()

        val result = controller.yourBill(fakeRequest.withLang(Languages.Welsh))
        status(result) shouldBe Status.OK
        val doc    = Jsoup.parse(contentAsString(result))

        assertShutteringPageContent(doc, Some(TaxRegime.Epaye), Languages.Welsh)
      }

    }

    "the shutter page should not be shown if the tax regime in the journey is not EPAYE" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.GovUk)()

      val result = controller.yourBill(fakeRequest)
      status(result) shouldBe Status.OK
      val doc    = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your VAT bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Vat)
      )
    }

  }
}

class VatShutteringSpec extends ItSpec, ShutteringSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "shuttering.shuttered-tax-regimes" -> List("VAT")
  )

  lazy val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "When VAT is shuttered" - {
    "the shutter page should be shown when the tax regime in the journey is EPAYE" - {

      "in english" in {
        AuthStub.authorise()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()

        val result = controller.yourBill(fakeRequest)
        status(result) shouldBe Status.OK
        val doc    = Jsoup.parse(contentAsString(result))

        assertShutteringPageContent(doc, Some(TaxRegime.Vat), Languages.English)
      }

      "in welsh" in {
        AuthStub.authorise()
        EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.VatService)()

        val result = controller.yourBill(fakeRequest.withLang(Languages.Welsh))
        status(result) shouldBe Status.OK
        val doc    = Jsoup.parse(contentAsString(result))

        assertShutteringPageContent(doc, Some(TaxRegime.Vat), Languages.Welsh)
      }

    }

    "the shutter page should not be shown if the tax regime in the journey is not VAT" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.GovUk)()

      val result = controller.yourBill(fakeRequest)
      status(result) shouldBe Status.OK
      val doc    = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1 = "Your PAYE bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested = Some(TaxRegime.Epaye)
      )
    }

  }
}
