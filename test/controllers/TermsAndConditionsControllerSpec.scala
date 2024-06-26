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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.{ItSpec, JsonUtils}
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class TermsAndConditionsControllerSpec extends ItSpec {

  private val controller: TermsAndConditionsController = app.injector.instanceOf[TermsAndConditionsController]

  object TermsAndConditionsPage {
    val expectedH1: String = "Terms and conditions"
  }

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat),
    ("SA", Origins.Sa.Bta, TaxRegime.Sa)
  ).foreach {
      case (regime, origin, taxRegime) =>

        "GET /terms-and-conditions should" - {

            def test(stubActions: () => Unit)(extraContentChecks: Document => Unit): Unit = {
              stubActions()

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
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
              }

              ContentAssertions.assertListOfContent(
                elements = doc.select(".govuk-body")
              )(
                  expectedContent = List(
                    "We can cancel this agreement if you:",
                    "If we cancel this agreement, you will need to pay the total amount you owe straight away.",
                    "We can use any refunds you might get to pay off your tax charges.",
                    "Contact HMRC on 0300 123 1813 if anything changes that you think affects your payment plan.",
                    "You can write to us about your Direct Debit:",
                    taxRegimeAddress,
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

              doc.select(".govuk-heading-m").text() shouldBe "Declaration"
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

        "POST /terms-and-conditions should" - {

          s"[$regime journey] redirect the user to submit arrangement if regimeDigitalCorrespondence is false and update backend" in {
            stubCommonActions()
            EssttpBackend.ConfirmedDirectDebitDetails
              .findJourney(testCrypto, origin)(JourneyJsonTemplates.`Confirmed Direct Debit Details - regimeDigitalCorrespondence flag`(origin, regimeDigitalCorrespondence = false)(testCrypto))
            EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
              TdAll.journeyId,
              JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = false, origin, Some(TdAll.etmpEmail))
            )

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

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

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

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

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

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
    ("SA", Origins.Sa.Bta)
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

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.submitArrangementUrl)
            EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(value = false))
          }
        }
    }

}
