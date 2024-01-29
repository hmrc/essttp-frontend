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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.journey.model.Origins
import essttp.rootmodel.{Email, TaxRegime}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.prop.TableDrivenPropertyChecks._
import paymentsEmailVerification.models.{EmailVerificationResult, EmailVerificationState}
import paymentsEmailVerification.models.api.StartEmailVerificationJourneyResponse
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EmailVerificationStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}

import java.time.LocalDateTime
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}

class EmailControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[EmailController]

  override lazy val configOverrides: Map[String, Any] = Map("features.email-journey" -> true)

  List(
    TaxRegime.Epaye -> Origins.Epaye.Bta,
    TaxRegime.Vat -> Origins.Vat.Bta,
    TaxRegime.Sa -> Origins.Sa.Bta
  ).foreach {
      case (taxRegime, origin) =>

        def requiresEmailAddressVerifiedBehaviour(action: Action[AnyContent]): Unit = {
            val email: Email = Email(SensitiveString("email@test.com"))

            "not allow journey when" - {

                def test(
                    journeyStubMapping:       () => StubMapping,
                    expectedRedirectLocation: Call
                ) = {
                  stubCommonActions()
                  journeyStubMapping()

                  val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
                  val result = action(fakeRequest)

                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(expectedRedirectLocation.url)
                }

              "an email verification result has not been obtained yet" in {
                test(
                  () => EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)(),
                  routes.EmailController.requestVerification
                )
              }

              "an arrangement has already been submitted" in {
                test(
                  () => EssttpBackend.SubmitArrangement.findJourney(origin, testCrypto)(),
                  taxRegime match {
                    case TaxRegime.Epaye => routes.PaymentPlanSetUpController.epayePaymentPlanSetUp
                    case TaxRegime.Vat   => routes.PaymentPlanSetUpController.vatPaymentPlanSetUp
                    case TaxRegime.Sa    => routes.PaymentPlanSetUpController.saPaymentPlanSetUp
                  }
                )
              }

              "an email verification result has been obtained but it is locked" in {
                test(
                  () => EssttpBackend.EmailVerificationResult.findJourney(email.value.decryptedValue, EmailVerificationResult.Locked, testCrypto, origin)(),
                  routes.EmailController.tooManyPasscodeAttempts
                )
              }

            }
          }

        s"[taxRegime: ${taxRegime.toString}] GET /which-email-do-you-want-to-use" - {

          "should return the which email do you want to use page" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin = origin, etmpEmail = Some(TdAll.etmpEmail))()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.whichEmailDoYouWantToUse(fakeRequest)
            val doc: Document = Jsoup.parse(contentAsString(result))

            RequestAssertions.assertGetRequestOk(result)
            ContentAssertions.commonPageChecks(
              doc,
              expectedH1              = "Which email do you want to use?",
              shouldBackLinkBePresent = true,
              expectedSubmitUrl       = Some(routes.EmailController.whichEmailDoYouWantToUse.url),
              regimeBeingTested       = Some(taxRegime),
              backLinkUrlOverride     = Some(routes.TermsAndConditionsController.termsAndConditions.url)
            )

            doc.select(".govuk-hint").first().html shouldBe "We will use this email address to send you information about your payment plan. " +
              "It may take <strong>up to 24 hours</strong> to receive notifications after you set up your plan."

            val radios: Elements = doc.select(".govuk-radios__item")
            radios.size() shouldBe 2

            val radioButtons: List[Element] = radios.select(".govuk-radios__input").asScala.toList
            radioButtons(0).`val` shouldBe "bobross@joyofpainting.com"
            radioButtons(1).`val` shouldBe "new"

            val radioLabels = radios.select(".govuk-radios__label").asScala.toList
            radioLabels(0).text() shouldBe "bobross@joyofpainting.com"
            radioLabels(1).text() shouldBe "A new email address"

            doc.select(".govuk-radios__conditional .govuk-label").text() shouldBe "Email address"
            doc.select("#newEmailInput-hint").text() shouldBe "For example, myname@sample.com"

            doc.select("#newEmailInput").attr("type") shouldBe "email"
          }

          "should prepopulate the form correctly" - {
            "existing email" in {
              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney("bobross@joyofpainting.com", testCrypto, origin)()

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
              val result: Future[Result] = controller.whichEmailDoYouWantToUse(fakeRequest)

              val doc: Document = Jsoup.parse(contentAsString(result))
              RequestAssertions.assertGetRequestOk(result)
              val radioInputs = doc.select(".govuk-radios__input").iterator().asScala.toList
              radioInputs.size shouldBe 2
              radioInputs(0).hasAttr("checked") shouldBe true
              radioInputs(1).hasAttr("checked") shouldBe false
              doc.select("#newEmailInput").text() shouldBe ""
            }

            "new email" in {
              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney("somenewemail@newemail.com", testCrypto, origin)()

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
              val result: Future[Result] = controller.whichEmailDoYouWantToUse(fakeRequest)

              val doc: Document = Jsoup.parse(contentAsString(result))
              RequestAssertions.assertGetRequestOk(result)
              val radioInputs = doc.select(".govuk-radios__input").iterator().asScala.toList
              radioInputs.size shouldBe 2
              radioInputs(0).hasAttr("checked") shouldBe false
              radioInputs(1).hasAttr("checked") shouldBe true
              doc.select("#newEmailInput").`val` shouldBe "somenewemail@newemail.com"
            }
          }

          "throw an error if an email address cannot be found in the eligibility check response" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = None)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val error = intercept[Exception](controller.whichEmailDoYouWantToUse(fakeRequest).futureValue)
            error.getMessage should endWith("Could not find email address in eligibility response.")
          }

        }

        s"[taxRegime: ${taxRegime.toString}] POST /which-email-do-you-want-to-use should" - {

          "update backend with existing email" in {
            val email: Email = Email(SensitiveString("bobross@joyofpainting.com"))

            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()
            EssttpBackend.SelectEmail.stubUpdateSelectedEmail(
              TdAll.journeyId,
              JourneyJsonTemplates.`Selected email to be verified`(email.value.decryptedValue, origin)
            )

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/which-email-do-you-want-to-use"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")
              .withFormUrlEncodedBody(
                ("selectAnEmailToUseRadio", email.value.decryptedValue),
                ("newEmailInput", "")
              )

            val result: Future[Result] = controller.whichEmailDoYouWantToUseSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.requestEmailVerificationUrl)
            EssttpBackend.SelectEmail.verifyUpdateSelectedEmailRequest(TdAll.journeyId, email)(testOperationCryptoFormat)
          }

          "update backend with new email" in {
            val email: Email = Email(SensitiveString("somenewemail@newemail.com"))

            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()
            EssttpBackend.SelectEmail.stubUpdateSelectedEmail(
              TdAll.journeyId,
              JourneyJsonTemplates.`Selected email to be verified`(email.value.decryptedValue, origin)
            )

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/which-email-do-you-want-to-use"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")
              .withFormUrlEncodedBody(
                ("selectAnEmailToUseRadio", "new"),
                ("newEmailInput", email.value.decryptedValue)
              )

            val result: Future[Result] = controller.whichEmailDoYouWantToUseSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.requestEmailVerificationUrl)
            EssttpBackend.SelectEmail.verifyUpdateSelectedEmailRequest(TdAll.journeyId, email)(testOperationCryptoFormat)
          }

          forAll(Table(
            ("Input Scenario", "inputValue", "expected error message", "errorTarget"),
            ("No option selected", List(("newEmailInput", "")), "Select which email address you want to use", "#selectAnEmailToUseRadio"),
            ("Empty for new email", List(("selectAnEmailToUseRadio", "new"), ("newEmailInput", "")), "Enter your email address in the correct format, like name@example.com", "#newEmailInput"),
            ("Invalid email format", List(("selectAnEmailToUseRadio", "new"), ("newEmailInput", "abc")), "Enter your email address in the correct format, like name@example.com", "#newEmailInput"),
            ("Email too long (> 256 characters)", List(("selectAnEmailToUseRadio", "new"), ("newEmailInput", "a" * 257)), "Enter an email address with 256 characters or less", "#newEmailInput")
          )) {
            (scenario: String, inputValue: List[(String, String)], expectedErrorMessage: String, expectedErrorTarget: String) =>
              s"When input is: [ $scenario: [ ${inputValue.toString} ]] error message should be $expectedErrorMessage" in {
                stubCommonActions()
                EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()

                val fakeRequest = FakeRequest(
                  method = "POST",
                  path   = "/which-day-do-you-want-to-pay-each-month"
                ).withAuthToken()
                  .withSession(SessionKeys.sessionId -> "IamATestSessionId")
                  .withFormUrlEncodedBody(inputValue: _*)

                val result: Future[Result] = controller.whichEmailDoYouWantToUseSubmit(fakeRequest)
                val pageContent: String = contentAsString(result)
                val doc: Document = Jsoup.parse(pageContent)

                RequestAssertions.assertGetRequestOk(result)
                ContentAssertions.commonPageChecks(
                  doc,
                  expectedH1              = "Which email do you want to use?",
                  shouldBackLinkBePresent = true,
                  expectedSubmitUrl       = Some(routes.EmailController.whichEmailDoYouWantToUse.url),
                  hasFormError            = true,
                  regimeBeingTested       = Some(taxRegime),
                  backLinkUrlOverride     = Some(routes.TermsAndConditionsController.termsAndConditions.url)
                )

                val errorSummary = doc.select(".govuk-error-summary")
                val errorLink = errorSummary.select("a")
                errorLink.text() shouldBe expectedErrorMessage
                errorLink.attr("href") shouldBe expectedErrorTarget
                EssttpBackend.SelectEmail.verifyNoneUpdateSelectedEmailRequest(TdAll.journeyId)
              }
          }

          "throw an error if an email address cannot be found in the eligibility check response" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = None)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val error = intercept[Exception](controller.whichEmailDoYouWantToUseSubmit(fakeRequest).futureValue)
            error.getMessage should endWith("Could not find email address in eligibility response.")
          }

        }

        s"[taxRegime: ${taxRegime.toString}] GET /enter-your-email-address" - {

          "should return the enter email page" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin = origin, etmpEmail = None)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.enterEmail(fakeRequest)
            val doc: Document = Jsoup.parse(contentAsString(result))

            RequestAssertions.assertGetRequestOk(result)
            ContentAssertions.commonPageChecks(
              doc,
              expectedH1              = "Enter your email address",
              shouldBackLinkBePresent = true,
              expectedSubmitUrl       = Some(routes.EmailController.enterEmailSubmit.url),
              regimeBeingTested       = Some(taxRegime),
              backLinkUrlOverride     = Some(routes.TermsAndConditionsController.termsAndConditions.url)
            )

            doc.select(".govuk-body").html shouldBe "We will use this email address to send you information about your payment plan. " +
              "It may take <strong>up to 24 hours</strong> to receive notifications after you set up your plan."

            doc.select("#newEmailInput-hint").text() shouldBe "For example, myname@sample.com"
            doc.select("#newEmailInput").attr("type") shouldBe "email"
          }

          "should prepopulate the form correctly" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney("somenewemail@newemail.com", testCrypto, origin, etmpEmail = None)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.enterEmail(fakeRequest)

            val doc: Document = Jsoup.parse(contentAsString(result))
            RequestAssertions.assertGetRequestOk(result)

            doc.select("#newEmailInput").`val` shouldBe "somenewemail@newemail.com"
          }
        }

        s"[taxRegime: ${taxRegime.toString}] POST /enter-your-email-address should" - {

          "update backend with given email" in {
            val email: Email = Email(SensitiveString("somenewemail@newemail.com"))

            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = None)()
            EssttpBackend.SelectEmail.stubUpdateSelectedEmail(
              TdAll.journeyId,
              JourneyJsonTemplates.`Selected email to be verified`(email.value.decryptedValue, origin, etmpEmail = None)
            )

            val fakeRequest = FakeRequest(
              method = "POST",
              path   = "/which-email-do-you-want-to-use"
            ).withAuthToken()
              .withSession(SessionKeys.sessionId -> "IamATestSessionId")
              .withFormUrlEncodedBody(("newEmailInput", email.value.decryptedValue))

            val result: Future[Result] = controller.enterEmailSubmit(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.requestEmailVerificationUrl)
            EssttpBackend.SelectEmail.verifyUpdateSelectedEmailRequest(TdAll.journeyId, email)(testOperationCryptoFormat)
          }

          forAll(Table(
            ("Input Scenario", "inputValue", "expected error message", "errorTarget"),
            ("Empty email", "newEmailInput" -> "", "Enter your email address in the correct format, like name@example.com", "#newEmailInput"),
            ("Invalid email format", "newEmailInput" -> "abc", "Enter your email address in the correct format, like name@example.com", "#newEmailInput"),
            ("Email too long (> 256 characters)", "newEmailInput" -> "a" * 257, "Enter an email address with 256 characters or less", "#newEmailInput")
          )) {
            (scenario: String, inputValue: (String, String), expectedErrorMessage: String, expectedErrorTarget: String) =>
              s"When input is: [ $scenario: [ ${inputValue.toString} ]] error message should be $expectedErrorMessage" in {
                stubCommonActions()
                EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, testCrypto, origin, etmpEmail = None)()

                val fakeRequest = FakeRequest(
                  method = "POST",
                  path   = "/which-day-do-you-want-to-pay-each-month"
                ).withAuthToken()
                  .withSession(SessionKeys.sessionId -> "IamATestSessionId")
                  .withFormUrlEncodedBody(inputValue)

                val result: Future[Result] = controller.enterEmailSubmit(fakeRequest)
                val pageContent: String = contentAsString(result)
                val doc: Document = Jsoup.parse(pageContent)

                RequestAssertions.assertGetRequestOk(result)
                ContentAssertions.commonPageChecks(
                  doc,
                  expectedH1              = "Enter your email address",
                  shouldBackLinkBePresent = true,
                  expectedSubmitUrl       = Some(routes.EmailController.enterEmailSubmit.url),
                  hasFormError            = true,
                  regimeBeingTested       = Some(taxRegime),
                  backLinkUrlOverride     = Some(routes.TermsAndConditionsController.termsAndConditions.url)
                )

                val errorSummary = doc.select(".govuk-error-summary")
                val errorLink = errorSummary.select("a")
                errorLink.text() shouldBe expectedErrorMessage
                errorLink.attr("href") shouldBe expectedErrorTarget
                EssttpBackend.SelectEmail.verifyNoneUpdateSelectedEmailRequest(TdAll.journeyId)
              }
          }

        }

        s"[taxRegime: ${taxRegime.toString}] GET /email-verification should" - {

          val email: Email = Email(SensitiveString("email@domain.com"))
          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
          val urlPrefix = "http://localhost:9215"

          val expectedPageTitle = taxRegime match {
            case TaxRegime.Epaye => "Set up an Employers’ PAYE payment plan"
            case TaxRegime.Vat   => "Set up a VAT payment plan"
            case TaxRegime.Sa    => "Set up a Self Assessment payment plan"
          }
          val expectedPageTitleWelsh = taxRegime match {
            case TaxRegime.Epaye => "Trefnu cynllun talu ar gyfer TWE Cyflogwyr"
            case TaxRegime.Vat   => "Trefnu cynllun talu TAW"
            case TaxRegime.Sa    => "Sefydlu cynllun talu ar gyfer Hunanasesiad"
          }

          "not allow journeys where an email has not been selected" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()

            val result = controller.requestVerification(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.whichEmailDoYouWantToUse.url)
            AuditConnectorStub.verifyNoAuditEvent()
          }

          "redirect to the given redirectUri if the call to request email verification is successful " +
            "when there is an ETMP email present" in {
              val redirectUri: String = "/redirect"

              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin, Some(email.value.decryptedValue))()
              EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Success(redirectUri))

              val result = controller.requestVerification(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(redirectUri)

              EmailVerificationStub.verifyRequestEmailVerification(
                email,
                "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
                expectedPageTitle,
                "en",
                urlPrefix,
                PageUrls.whichEmailDoYouWantToUseUrl
              )

              AuditConnectorStub.verifyEventAudited(
                "EmailVerificationRequested",
                Json.parse(
                  s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "ETMP",
                   |  "result" : "Started"
                   |}
                   |""".stripMargin
                ).as[JsObject]
              )
            }

          "redirect to the given redirectUri if the call to request email verification is successful " +
            "when there isn't an ETMP email present" in {
              val redirectUri: String = "/redirect"

              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin, etmpEmail = None)()
              EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Success(redirectUri))

              val result = controller.requestVerification(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(redirectUri)

              EmailVerificationStub.verifyRequestEmailVerification(
                email,
                "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
                expectedPageTitle,
                "en",
                urlPrefix,
                PageUrls.enterEmailAddressUrl
              )

              AuditConnectorStub.verifyEventAudited(
                "EmailVerificationRequested",
                Json.parse(
                  s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "Started"
                   |}
                   |""".stripMargin
                ).as[JsObject]
              )
            }

          "handle Welsh correctly" in {
            val redirectUri: String = "http:///host:12345/redirect"
            val fakeRequest =
              FakeRequest()
                .withAuthToken()
                .withSession(SessionKeys.sessionId -> "IamATestSessionId")
                .withCookies(Cookie("PLAY_LANG", "cy"))

            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Success(redirectUri))

            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUri)

            EmailVerificationStub.verifyRequestEmailVerification(
              email,
              "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
              expectedPageTitleWelsh,
              "cy",
              urlPrefix,
              PageUrls.whichEmailDoYouWantToUseUrl
            )
          }

          "redirect to the too-many-emails page if a TooManyDifferentEmailAddresses response is given" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Error(EmailVerificationState.TooManyDifferentEmailAddresses))

            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.tooManyEmailAddressesEnteredUrl)

            EmailVerificationStub.verifyRequestEmailVerification(
              email,
              "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
              expectedPageTitle,
              "en",
              urlPrefix,
              PageUrls.whichEmailDoYouWantToUseUrl
            )

            AuditConnectorStub.verifyEventAudited(
              "EmailVerificationRequested",
              Json.parse(
                s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "${EmailVerificationState.TooManyDifferentEmailAddresses.entryName}"
                   |}
                   |""".stripMargin
              ).as[JsObject]
            )
          }

          s"redirect to ${routes.EmailController.tooManyPasscodeJourneysStarted.url} when emailVerificationState is TooManyPasscodeJourneysStarted" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Error(EmailVerificationState.TooManyPasscodeJourneysStarted))
            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.tooManyPasscodeJourneysStartedUrl)

            AuditConnectorStub.verifyEventAudited(
              "EmailVerificationRequested",
              Json.parse(
                s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "${EmailVerificationState.TooManyPasscodeJourneysStarted.entryName}"
                   |}
                   |""".stripMargin
              ).as[JsObject]
            )
          }

          s"redirect to ${routes.EmailController.tooManyDifferentEmailAddresses.url} when emailVerificationState is TooManyPasscodeAttempts" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Error(EmailVerificationState.TooManyPasscodeAttempts))
            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.tooManyPasscodeAttemptsUrl)

            AuditConnectorStub.verifyEventAudited(
              "EmailVerificationRequested",
              Json.parse(
                s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "${EmailVerificationState.TooManyPasscodeAttempts.entryName}"
                   |}
                   |""".stripMargin
              ).as[JsObject]
            )
          }

          "redirect to email-address-confirmed when the email address has already been confirmed" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Error(EmailVerificationState.AlreadyVerified))
            EssttpBackend.EmailVerificationResult.stubUpdateEmailVerificationResult(
              TdAll.journeyId,
              JourneyJsonTemplates.`Email verification complete`(email.value.decryptedValue, EmailVerificationResult.Verified, origin)
            )

            val result: Future[Result] = controller.requestVerification(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.emailAddressConfirmedUrl)

            EssttpBackend.EmailVerificationResult.verifyUpdateEmailVerificationResultRequest(TdAll.journeyId, EmailVerificationResult.Verified)

            AuditConnectorStub.verifyEventAudited(
              "EmailVerificationRequested",
              Json.parse(
                s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "${EmailVerificationState.AlreadyVerified.entryName}"
                   |}
                   |""".stripMargin
              ).as[JsObject]
            )

          }
        }

        s"[taxRegime: ${taxRegime.toString}]GET /email-callback should" - {

          val email: Email = Email(SensitiveString("email@domain.com"))
          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

          "not allow journeys where an email has not been selected" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()

            val result = controller.emailCallback(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.whichEmailDoYouWantToUse.url)
            AuditConnectorStub.verifyNoAuditEvent()
          }

          "redirect to the email address confirmed page if the email address has successfully been verified" in {
            stubCommonActions()

            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.getVerificationStatus(EmailVerificationResult.Verified)
            EssttpBackend.EmailVerificationResult.stubUpdateEmailVerificationResult(
              TdAll.journeyId,
              JourneyJsonTemplates.`Email verification complete`(email.value.decryptedValue, EmailVerificationResult.Verified, origin)
            )

            val result = controller.emailCallback(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.emailAddressConfirmed.url)

            EssttpBackend.EmailVerificationResult.verifyUpdateEmailVerificationResultRequest(
              TdAll.journeyId, EmailVerificationResult.Verified
            )
            EmailVerificationStub.verifyGetEmailVerificationResult(email)
            AuditConnectorStub.verifyEventAudited(
              "EmailVerificationResult",
              Json.parse(
                s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "Success",
                   |  "authProviderId": "authId-999"
                   |}
                   |""".stripMargin
              ).as[JsObject]
            )
          }

          "redirect to the too many passcodes page if the email address has been locked" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.getVerificationStatus(EmailVerificationResult.Locked)
            EssttpBackend.EmailVerificationResult.stubUpdateEmailVerificationResult(
              TdAll.journeyId,
              JourneyJsonTemplates.`Email verification complete`(email.value.decryptedValue, EmailVerificationResult.Locked, origin)
            )

            val result = controller.emailCallback(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.tooManyPasscodeAttempts.url)

            EmailVerificationStub.verifyGetEmailVerificationResult(email)
            EssttpBackend.EmailVerificationResult.verifyUpdateEmailVerificationResultRequest(
              TdAll.journeyId, EmailVerificationResult.Locked
            )
            AuditConnectorStub.verifyEventAudited(
              "EmailVerificationResult",
              Json.parse(
                s"""
                   |{
                   |  "origin" : "Bta",
                   |  "taxType" : "${taxRegime.entryName}",
                   |  "taxDetail" : {
                   |    ${TdAll.taxDetailForAuditEvent(taxRegime)}
                   |  },
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "emailAddress" : "${email.value.decryptedValue}",
                   |  "emailSource" : "TEMP",
                   |  "result" : "Failed",
                   |  "failureReason" : "${EmailVerificationState.TooManyPasscodeAttempts.entryName}",
                   |  "authProviderId": "authId-999"
                   |}
                   |""".stripMargin
              ).as[JsObject]
            )
          }

        }

        s"[taxRegime: ${taxRegime.toString}] GET /email-address-confirmed should" - {

          behave like requiresEmailAddressVerifiedBehaviour(controller.emailAddressConfirmed)

          "display the page when the email address has successfully been verified" in {
            val email: Email = Email(SensitiveString("email@test.com"))

            stubCommonActions()
            EssttpBackend.EmailVerificationResult.findJourney(email.value.decryptedValue, EmailVerificationResult.Verified, testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = controller.emailAddressConfirmed(fakeRequest)
            status(result) shouldBe OK

            val doc = Jsoup.parse(contentAsString(result))

            ContentAssertions.commonPageChecks(
              doc,
              "Email address confirmed",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = Some(routes.EmailController.emailAddressConfirmedSubmit.url),
              regimeBeingTested       = Some(taxRegime)
            )

            val paragraphs = doc.select(".govuk-body").asScala.toList
            paragraphs.size shouldBe 3
            paragraphs(0).html() shouldBe s"The email address <strong>${email.value.decryptedValue}</strong> has been confirmed."

            ContentAssertions.formSubmitShouldDisableSubmitButton(doc)
          }

        }

        s"[taxRegime: ${taxRegime.toString}]  POST /email-address-confirmed should" - {

          behave like requiresEmailAddressVerifiedBehaviour(controller.emailAddressConfirmedSubmit)

          Seq[(String, String)](
            ("bobross@joyofpainting.com", "ETMP"),
            ("email@test.com", "TEMP")
          ).foreach {
              case (emailAddress, emailSource) =>
                s"redirect to submitArrangement with emailSource: $emailSource, when email address has successfully been verified" in {
                  val email: Email = Email(SensitiveString(emailAddress))

                  stubCommonActions()
                  EssttpBackend.EmailVerificationResult.findJourney(email.value.decryptedValue, EmailVerificationResult.Verified, testCrypto, origin)()

                  val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
                  val result = controller.emailAddressConfirmedSubmit(fakeRequest)
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(routes.SubmitArrangementController.submitArrangement.url)
                }
            }
        }

        s"[taxRegime: ${taxRegime.toString}] GET /email-verification-too-many-passcodes should" - {

          Seq(
            (routes.EmailController.whichEmailDoYouWantToUse.url, Some(TdAll.etmpEmail)),
            (routes.EmailController.enterEmail.url, None)
          ).foreach {
              case (expectedLink, emailInEtmp) =>
                s"display the page with email link to $expectedLink when email is ${emailInEtmp.toString}" in {
                  stubCommonActions()
                  EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, origin, emailInEtmp)()

                  val result = controller.tooManyPasscodeJourneysStarted(FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId"))
                  status(result) shouldBe OK

                  val doc = Jsoup.parse(contentAsString(result))

                  ContentAssertions.commonPageChecks(
                    doc,
                    "You have tried to verify an email address too many times",
                    shouldBackLinkBePresent = false,
                    expectedSubmitUrl       = None,
                    regimeBeingTested       = Some(taxRegime)
                  )

                  val paragraphs = doc.select("p.govuk-body").asScala.toList

                  paragraphs.size shouldBe 2

                  paragraphs(0).html() shouldBe "You have tried to verify <strong>email@test.com</strong> too many times."
                  paragraphs(1).html() shouldBe s"""You will need to <a href="$expectedLink" class="govuk-link">verify a different email address</a>."""
                }
            }

        }

        s"[taxRegime: ${taxRegime.toString}] GET /email-verification-code-entered-too-many-times should" - {

          "display the page when" - {

              def test(stubActions: () => Unit)(expectedEmailEntryUrl: String): Unit = {
                stubActions()

                val result = controller.tooManyPasscodeAttempts(FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId"))
                status(result) shouldBe OK

                val doc = Jsoup.parse(contentAsString(result))

                ContentAssertions.commonPageChecks(
                  doc,
                  "Email verification code entered too many times",
                  shouldBackLinkBePresent = false,
                  expectedSubmitUrl       = None,
                  regimeBeingTested       = Some(taxRegime)
                )

                val paragraphs = doc.select("p.govuk-body").asScala.toList

                paragraphs.size shouldBe 2

                paragraphs(0).text() shouldBe "You have entered an email verification code too many times."
                paragraphs(1).text() shouldBe "You can go back to enter a new email address."
                paragraphs(1).select("a").attr("href") shouldBe expectedEmailEntryUrl
                ()
              }

            "there is an ETMP email" in {
              test{ () =>
                stubCommonActions()
                EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, origin)()
                ()
              }(routes.EmailController.whichEmailDoYouWantToUse.url)
            }

            "there is no ETMP email" in {
              test{ () =>
                stubCommonActions()
                EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, origin, etmpEmail = None)()
                ()
              }(routes.EmailController.enterEmail.url)
            }
          }

        }

        s"[taxRegime: ${taxRegime.toString}] GET /email-verification-too-many-addresses should" - {

          "display the page" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, origin)()
            EmailVerificationStub.getLockoutCreatedAt(Some(LocalDateTime.of(2023, 1, 7, 11, 13)))

            val result = controller.tooManyDifferentEmailAddresses(FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId"))
            status(result) shouldBe OK

            val doc = Jsoup.parse(contentAsString(result))

            ContentAssertions.commonPageChecks(
              doc,
              "You have tried to verify too many email addresses",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val paragraphs = doc.select("p.govuk-body").asScala.toList

            paragraphs.size shouldBe 2

            paragraphs(0).html() shouldBe "You have been locked out because you have tried to verify too many email addresses. Please try again on <strong>8 January 2023 at 11:13am</strong>."
            paragraphs(1).select("a").text() shouldBe "Sign out"
            paragraphs(1).select("a").attr("href") shouldBe routes.SignOutController.signOut.url
          }

          "throw an error if no lockoutCreatedAt found/returned from payments-email-verification" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, origin)()
            EmailVerificationStub.getLockoutCreatedAt(None)
            val error: UpstreamErrorResponse = intercept[UpstreamErrorResponse](
              await(controller.tooManyDifferentEmailAddresses(FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")))
            )
            error.statusCode shouldBe INTERNAL_SERVER_ERROR
          }
        }

    }

}

class EmailNotEnabledControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[EmailController]

  override lazy val configOverrides: Map[String, Any] = Map(
    "features.email-journey" -> false
  )

  "A 501 NotImplemented response should be returned by" - {

    "GET /which-email-do-you-want-to-use" in {
      status(controller.whichEmailDoYouWantToUse(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "POST /which-email-do-you-want-to-use" in {
      status(controller.whichEmailDoYouWantToUseSubmit(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "GET /email-verification" in {
      status(controller.requestVerification(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "GET /email-callback should" in {
      status(controller.emailCallback(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "GET /email-address-confirmed" in {
      status(controller.emailAddressConfirmed(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "GET /email-verification-too-many-passcodes" in {
      status(controller.tooManyPasscodeJourneysStarted(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "GET /email-verification-too-many-addresses" in {
      status(controller.tooManyDifferentEmailAddresses(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "GET /email-verification-code-entered-too-many-times" in {
      status(controller.tooManyPasscodeAttempts(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

  }

}

class EmailNonLocalControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[EmailController]

  override lazy val configOverrides: Map[String, Any] = Map(
    "platform.frontend.host" -> "https://platform-host",
    "features.email-journey" -> true
  )

  "GET /email-verification should" - {

    "redirect to the given redirectUri if the call to request email verification is successful" in {
      val redirectUri: String = "/redirect"
      val email: Email = Email(SensitiveString("email@domain.com"))
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, Origins.Epaye.Bta)()
      EmailVerificationStub.requestEmailVerification(StartEmailVerificationJourneyResponse.Success(redirectUri))

      val result = controller.requestVerification(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectUri)

      EmailVerificationStub.verifyRequestEmailVerification(
        email,
        "/accessibility-statement/set-up-a-payment-plan",
        "Set up an Employers’ PAYE payment plan",
        "en",
        "",
        PageUrls.whichEmailDoYouWantToUseUrl
      )

    }

  }

}
