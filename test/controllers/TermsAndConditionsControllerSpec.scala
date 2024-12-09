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
import essttp.rootmodel.{IsEmailAddressRequired, TaxRegime}
import models.Languages.Welsh
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import testsupport.{ItSpec, JsonUtils}

import scala.concurrent.Future

class TermsAndConditionsControllerSpec extends ItSpec {

  private val controller: TermsAndConditionsController = app.injector.instanceOf[TermsAndConditionsController]

  object TermsAndConditionsPage {
    val expectedH1: String = "Terms and conditions"
    val expectedH1Welsh: String = "Telerau ac amodau"
  }

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat),
    ("SA", Origins.Sa.Bta, TaxRegime.Sa),
    ("SIMP", Origins.Simp.Pta, TaxRegime.Simp)
  ).foreach {
      case (regime, origin, taxRegime) =>

        "GET /terms-and-conditions should" - {

            def test(stubActions: () => Unit)(extraContentChecks: Document => Unit): Unit = {
              stubActions()

              val result: Future[Result] = controller.termsAndConditions(fakeRequest)
              val pageContent: String = contentAsString(result)
              val doc: Document = Jsoup.parse(pageContent)

              RequestAssertions.assertGetRequestOk(result)

              ContentAssertions.commonPageChecks(
                doc,
                expectedH1              = TermsAndConditionsPage.expectedH1,
                shouldBackLinkBePresent = true,
                expectedSubmitUrl       = Some(routes.TermsAndConditionsController.termsAndConditionsSubmit.url),
                regimeBeingTested       = Some(taxRegime)
              )

              val taxRegimeAddress: String = taxRegime match {
                case TaxRegime.Epaye => "DM PAYE HM Revenue and Customs BX9 1EW United Kingdom"
                case TaxRegime.Vat   => "HMRC Direct Debit Support Team VAT 2 DMB 612 BX5 5AB United Kingdom"
                case TaxRegime.Sa    => "Debt Management Self Assessment HM Revenue and Customs BX9 1AS United Kingdom"
                case TaxRegime.Simp  => "Debt Management Simple Assessment HM Revenue and Customs BX9 1AS United Kingdom"
              }

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-body")
              )(
                  expectedContent = List(
                    "We can cancel this agreement if you:",
                    "Contact HMRC straight away if you have a problem. If we cancel this agreement, you’ll need to pay the total amount you owe. We can also use any refunds you are due to pay off your bill.",
                    "You should also tell us if your circumstances change, and you can pay more or pay in full.",
                    "You can write to us about your Direct Debit:",
                    taxRegimeAddress,
                    "Telephone: 0300 123 1813",
                    "Outside UK: +44 2890 538 192",
                    "Our phone line opening hours are:",
                    "Monday to Friday: 8am to 6pm",
                    "Closed weekends and bank holidays.",
                    "Use Relay UK if you cannot hear or speak on the telephone, dial 18001 then 0345 300 3900. Find out more on the Relay UK website (opens in new tab).",
                    "If a health condition or personal circumstances make it difficult to contact us",
                    "Our guidance Get help from HMRC if you need extra support (opens in new tab) explains how we can support you.",
                    "Declaration",
                    "I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt."
                  )
                )

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-list--bullet").select("li")
              )(
                  expectedContent = List(
                    "pay late or miss a payment",
                    "pay another tax bill late",
                    "do not submit your future tax returns on time"
                  )
                )

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-heading-m")
              )(
                  expectedContent = List(
                    "Call the debt management helpline",
                    "Text service"
                  )
                )

              ContentAssertions.assertListOfLinks(
                elements = doc.select("p.govuk-body").select("a")
              ) (
                  expectedContent = List(
                    "https://www.relayuk.bt.com/",
                    "https://www.gov.uk/get-help-hmrc-extra-support"
                  )
                )

              doc.select(".govuk-button").text() shouldBe "Agree and continue"

              extraContentChecks(doc)
            }

          s"[$regime journey] return 200 and the terms and conditions page when an email address is required" in {
            test { () =>
              stubCommonActions()
              EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, origin)()
              ()
            }{ doc =>
              doc.select("form").hasClass("prevent-multiple-submits") shouldBe false

              val button = doc.select("form > .govuk-button")
              button.hasClass("disable-on-click") shouldBe false
              button.hasAttr("data-prevent-double-click") shouldBe false
              ()
            }
          }

          s"[$regime journey] return 200 and the terms and conditions page when an email address is not required" in {
            test { () =>
              stubCommonActions()
              EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, origin)(
                JsonUtils.replace(
                  List("ConfirmedDirectDebitDetails", "eligibilityCheckResult", "regimeDigitalCorrespondence"),
                  JsBoolean(false)
                )(
                    Json.parse(JourneyJsonTemplates.`Confirmed Direct Debit Details`(origin)).as[JsObject]
                  ).toString
              )
              ()
            }(ContentAssertions.formSubmitShouldDisableSubmitButton)
          }

        }

        "GET /terms-and-conditions should in Welsh" - {

            def test(stubActions: () => Unit)(extraContentChecks: Document => Unit): Unit = {
              stubActions()

              val result: Future[Result] = controller.termsAndConditions(fakeRequest.withLangWelsh())
              val pageContent: String = contentAsString(result)
              val doc: Document = Jsoup.parse(pageContent)

              RequestAssertions.assertGetRequestOk(result)

              ContentAssertions.commonPageChecks(
                doc,
                expectedH1              = TermsAndConditionsPage.expectedH1Welsh,
                shouldBackLinkBePresent = true,
                expectedSubmitUrl       = Some(routes.TermsAndConditionsController.termsAndConditionsSubmit.url),
                regimeBeingTested       = Some(taxRegime),
                language                = Welsh
              )

              val taxRegimeAddress: String = taxRegime match {
                case TaxRegime.Epaye => "Gwasanaeth Cwsmeriaid Cymraeg CThEF HMRC BX9 1ST"
                case TaxRegime.Vat   => "Gwasanaeth Cwsmeriaid Cymraeg CThEF HMRC BX9 1ST"
                case TaxRegime.Sa    => "Rheolaeth Dyledion Hunanasesiad Gwasanaeth Cwsmeriaid Cymraeg CThEF HMRC BX9 1ST"
                case TaxRegime.Simp  => "Rheolaeth Dyledion Asesiad Syml Gwasanaeth Cwsmeriaid Cymraeg CThEF HMRC BX9 1ST"
              }

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-body")
              )(
                  expectedContent = List(
                    "Gallwn ganslo’r cytundeb hwn os:",
                    "Cysylltwch â CThEF ar unwaith os oes gennych broblem. Os byddwn yn canslo’r cytundeb hwn, bydd angen i chi dalu’r cyfanswm sydd arnoch. Gallwn hefyd ddefnyddio unrhyw ad-daliadau rydych sydd arnoch i dalu’ch bil.",
                    "Dylech hefyd roi gwybod i ni os bydd eich amgylchiadau’n newid, a gallwch dalu mwy neu dalu’n llawn.",
                    "Gallwch ysgrifennu atom ynglŷn â’ch Debyd Uniongyrchol:",
                    taxRegimeAddress,
                    "Ffôn: 0300 200 1900",
                    "Oriau agor ein llinell ffôn yw:",
                    "Dydd Llun i ddydd Gwener: 8:30 i 17:00",
                    "Ar gau ar benwythnosau a gwyliau banc.",
                    "Defnyddiwch wasanaeth Text Relay UK os na allwch glywed na siarad dros y ffôn. Deialwch 18001 ac yna 0345 300 3900. Dysgwch ragor am hyn ar wefan Text Relay UK (yn agor tab newydd).",
                    "Os yw cyflwr iechyd neu amgylchiadau personol yn ei gwneud hi’n anodd i chi gysylltu â ni",
                    "Bydd ein harweiniad ynghylch ‘Cael help gan CThEF os oes angen cymorth ychwanegol arnoch’ (yn agor tab newydd) yn esbonio sut y gallwn eich helpu.",
                    "Datganiad",
                    "Cytunaf â thelerau ac amodau’r cynllun talu hwn. Cadarnhaf mai dyma’r cynharaf y gallaf setlo’r ddyled hon."
                  )
                )

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-list--bullet").select("li")
              )(
                  expectedContent = List(
                    "ydych yn talu’n hwyr neu’n methu taliad",
                    "ydych yn talu bil treth arall yn hwyr",
                    "nad ydych yn cyflwyno’ch Ffurflenni Treth yn y dyfodol mewn pryd"
                  )
                )

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-heading-m")
              )(
                  expectedContent = List(
                    "Ffoniwch y llinell gymorth rheoli dyledion",
                    "Gwasanaeth Text Relay"
                  )
                )

              ContentAssertions.assertListOfLinks(
                elements = doc.select("p.govuk-body").select("a")
              ) (
                  expectedContent = List(
                    "https://www.relayuk.bt.com/",
                    "https://www.gov.uk/get-help-hmrc-extra-support"
                  )
                )

              doc.select(".govuk-button").text() shouldBe "Cytuno ac yn eich blaen"

              extraContentChecks(doc)
            }

          s"[$regime journey] return 200 and the terms and conditions page" in {
            test { () =>
              stubCommonActions()
              EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, origin)()
              ()
            }{ doc =>
              doc.select("form").hasClass("prevent-multiple-submits") shouldBe false

              val button = doc.select("form > .govuk-button")
              button.hasClass("disable-on-click") shouldBe false
              button.hasAttr("data-prevent-double-click") shouldBe false
              ()
            }
          }
        }

        "POST /terms-and-conditions should" - {

          s"[$regime journey] redirect the user to submit arrangement if regimeDigitalCorrespondence is false and update backend" in {
            stubCommonActions()
            EssttpBackend.ConfirmedDirectDebitDetails
              .findJourney(testCrypto, origin)(JourneyJsonTemplates.`Confirmed Direct Debit Details - regimeDigitalCorrespondence flag`(origin, regimeDigitalCorrespondence = false)(testCrypto))
            EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
              TdAll.journeyId,
              JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = false, origin, Some(TdAll.etmpEmail))
            )

            val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.submitArrangementUrl)
            EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(value = false))
          }

          s"[$regime journey] redirect the user to the email journey if regimeDigitalCorrespondence is enabled and update backend" +
            s" and there is an email address in the eligibility check result" in {
              stubCommonActions()
              EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, origin)()
              EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
                TdAll.journeyId,
                JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = true, origin, Some(TdAll.etmpEmail))
              )

              val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(PageUrls.whichEmailDoYouWantToUseUrl)
              EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(value = true))
            }

          s"[$regime journey] redirect the user to the email journey if regimeDigitalCorrespondence is enabled and update backend" +
            s" and there is no an email address in the eligibility check result" in {
              stubCommonActions()
              EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, origin)()
              EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
                TdAll.journeyId,
                JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = true, origin, None)
              )

              val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(PageUrls.enterEmailAddressUrl)
              EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(value = true))
            }
        }
    }

}

class TermsAndConditionsControllerEmailDisabledSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.email-journey" -> false)

  val controller: TermsAndConditionsController = app.injector.instanceOf[TermsAndConditionsController]

  Seq[(String, Origin)](
    ("EPAYE", Origins.Epaye.Bta),
    ("VAT", Origins.Vat.Bta),
    ("SA", Origins.Sa.Bta),
    ("SIMP", Origins.Simp.Pta)
  ).foreach {
      case (regime, origin) =>

        "POST /terms-and-conditions should" - {

          s"[$regime journey] redirect the user to the submit arrangement endpoint if the our email journey feature flag is false and update backend" in {
            stubCommonActions()
            EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, origin)()
            EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
              TdAll.journeyId,
              JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = false, origin = origin, etmpEmail = Some(TdAll.etmpEmail))
            )

            val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.submitArrangementUrl)
            EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(value = false))
          }
        }
    }

}
