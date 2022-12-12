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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.emailverification.EmailVerificationStatus
import essttp.journey.model.Origins
import essttp.rootmodel.{Email, TaxRegime}
import models.GGCredId
import models.emailverification.EmailVerificationStatusResponse.EmailStatus
import models.emailverification.RequestEmailVerificationResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Call, Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{EmailVerificationStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}

class EmailControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[EmailController]

  override lazy val configOverrides: Map[String, Any] = Map("features.email-journey" -> true)

  List(
    TaxRegime.Epaye -> Origins.Epaye.Bta,
    TaxRegime.Vat -> Origins.Vat.Bta
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
                  }
                )
              }

              "an email verification result has been obtained but it is locked" in {
                test(
                  () => EssttpBackend.EmailVerificationStatus.findJourney(email.value.decryptedValue, EmailVerificationStatus.Locked, testCrypto, origin)(),
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
              regimeBeingTested       = Some(taxRegime)
            )

            doc.select(".govuk-hint").first().html shouldBe "We will use this email address to send you information about your payment plan. " +
              "It may take up to <strong>24 hours</strong> to receive notifications after you set up your plan."

            val radios: Elements = doc.select(".govuk-radios__item")
            radios.size() shouldBe 2

            val radioButtons: List[Element] = radios.select(".govuk-radios__input").asScala.toList
            radioButtons(0).`val` shouldBe "bobross@joyofpainting.com"
            radioButtons(1).`val` shouldBe "new"

            val radioLabels = radios.select(".govuk-radios__label").asScala.toList
            radioLabels(0).text() shouldBe "bobross@joyofpainting.com"
            radioLabels(1).text() shouldBe "A new email address"

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
                  regimeBeingTested       = Some(taxRegime)
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
              regimeBeingTested       = Some(taxRegime)
            )

            doc.select(".govuk-body").html shouldBe "We will use this email address to send you information about your payment plan. " +
              "It may take up to <strong>24 hours</strong> to receive notifications after you set up your plan."

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
                  regimeBeingTested       = Some(taxRegime)
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
          }
          val expectedPageTitleWelsh = taxRegime match {
            case TaxRegime.Epaye => "Trefnu cynllun talu ar gyfer TWE Cyflogwyr"
            case TaxRegime.Vat   => "Trefnu cynllun talu TAW"
          }

          "not allow journeys where an email has not been selected" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()

            val result = controller.requestVerification(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.whichEmailDoYouWantToUse.url)
          }

          "redirect to the given redirectUri if the call to request email verification is successful " +
            "when there is an ETMP email present" in {
              val redirectUri: String = "/redirect"

              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
              EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

              val result = controller.requestVerification(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"http://localhost:9890$redirectUri")

              EmailVerificationStub.verifyRequestEmailVerification(
                email,
                GGCredId("authId-999"),
                "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
                expectedPageTitle,
                "en",
                urlPrefix,
                PageUrls.whichEmailDoYouWantToUseUrl
              )
            }

          "redirect to the given redirectUri if the call to request email verification is successful " +
            "when there isn't an ETMP email present" in {
              val redirectUri: String = "/redirect"

              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin, etmpEmail = None)()
              EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

              val result = controller.requestVerification(fakeRequest)
              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"http://localhost:9890$redirectUri")

              EmailVerificationStub.verifyRequestEmailVerification(
                email,
                GGCredId("authId-999"),
                "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
                expectedPageTitle,
                "en",
                urlPrefix,
                PageUrls.enterEmailAddressUrl
              )
            }

          "maintain the redirectUri in the email verification response if the environment is local and the uri is absolute" in {
            val redirectUri: String = "http:///host:12345/redirect"

            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUri)

            EmailVerificationStub.verifyRequestEmailVerification(
              email,
              GGCredId("authId-999"),
              "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
              expectedPageTitle,
              "en",
              urlPrefix,
              PageUrls.whichEmailDoYouWantToUseUrl
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
            EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUri)

            EmailVerificationStub.verifyRequestEmailVerification(
              email,
              GGCredId("authId-999"),
              "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
              expectedPageTitleWelsh,
              "cy",
              urlPrefix,
              PageUrls.whichEmailDoYouWantToUseUrl
            )
          }

          "redirect to the too-many-emails page if a 401 (UNAUTHORIZED) response is given by email-verification" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.requestEmailVerification(Left(UNAUTHORIZED))

            val result = controller.requestVerification(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.tooManyEmailAddressesUrl)

            EmailVerificationStub.verifyRequestEmailVerification(
              email,
              GGCredId("authId-999"),
              "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
              expectedPageTitle,
              "en",
              urlPrefix,
              PageUrls.whichEmailDoYouWantToUseUrl
            )
          }

        }

        s"[taxRegime: ${taxRegime.toString}]GET /email-callback should" - {

          val email: Email = Email(SensitiveString("email@domain.com"))

          val ggCredId: GGCredId = GGCredId("authId-999")

          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

          "not allow journeys where an email has not been selected" in {
            stubCommonActions()
            EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))()

            val result = controller.emailCallback(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.whichEmailDoYouWantToUse.url)
          }

          "redirect to the email address confirmed page if the email address has successfully been verified" in {
            stubCommonActions()

            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.getVerificationStatus(
              ggCredId,
              Right(List(EmailStatus(email.value.decryptedValue, verified = true, locked = false)))
            )
            EssttpBackend.EmailVerificationStatus.stubEmailVerificationStatus(
              TdAll.journeyId,
              JourneyJsonTemplates.`Email verification complete`(email.value.decryptedValue, EmailVerificationStatus.Verified, origin)
            )

            val result = controller.emailCallback(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.emailAddressConfirmed.url)

            EssttpBackend.EmailVerificationStatus.verifyEmailVerificationStatusRequest(
              TdAll.journeyId, EmailVerificationStatus.Verified
            )
          }

          "redirect to the too many passcodes page if the email address has been locked" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
            EmailVerificationStub.getVerificationStatus(
              ggCredId,
              Right(List(EmailStatus(email.value.decryptedValue, verified = false, locked = true)))
            )
            EssttpBackend.EmailVerificationStatus.stubEmailVerificationStatus(
              TdAll.journeyId,
              JourneyJsonTemplates.`Email verification complete`(email.value.decryptedValue, EmailVerificationStatus.Locked, origin)
            )

            val result = controller.emailCallback(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.EmailController.tooManyPasscodeAttempts.url)

            EssttpBackend.EmailVerificationStatus.verifyEmailVerificationStatusRequest(
              TdAll.journeyId, EmailVerificationStatus.Locked
            )
          }

          "show an error page when" - {

            "a 404 response is given by the email-verification service indicating that no records could be found" in {
              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
              EmailVerificationStub.getVerificationStatus(
                ggCredId,
                Left(NOT_FOUND)
              )

              an[Exception] shouldBe thrownBy(controller.emailCallback(fakeRequest).futureValue)
            }

            "an invalid combination of 'verified' and 'locked' is found in the email-verification response" in {
              List(
                true -> true,
                false -> false
              ).foreach {
                  case (verified, locked) =>
                    withClue(s"For verified=${verified.toString} and locked=${locked.toString}: ") {
                      stubCommonActions()
                      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
                      EmailVerificationStub.getVerificationStatus(
                        ggCredId,
                        Right(List(EmailStatus(email.value.decryptedValue, verified = verified, locked = locked)))
                      )

                      an[Exception] shouldBe thrownBy(controller.emailCallback(fakeRequest).futureValue)
                    }
                }
            }

            "the email address the user has selected can't be found in the email-verification response" in {
              stubCommonActions()
              EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto, origin)()
              EmailVerificationStub.getVerificationStatus(
                ggCredId,
                Right(List(EmailStatus("another@email.com", verified = true, locked = false)))
              )

              an[Exception] shouldBe thrownBy(controller.emailCallback(fakeRequest).futureValue)

            }

          }

        }

        s"[taxRegime: ${taxRegime.toString}] GET /email-address-confirmed should" - {

          behave like requiresEmailAddressVerifiedBehaviour(controller.emailAddressConfirmed)

          "display the page when the email address has successfully been verified" in {
            val email: Email = Email(SensitiveString("email@test.com"))

            stubCommonActions()
            EssttpBackend.EmailVerificationStatus.findJourney(email.value.decryptedValue, EmailVerificationStatus.Verified, testCrypto, origin)()

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

          "display the page when the email address has successfully been verified" in {
            val email: Email = Email(SensitiveString("email@test.com"))

            stubCommonActions()
            EssttpBackend.EmailVerificationStatus.findJourney(email.value.decryptedValue, EmailVerificationStatus.Verified, testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = controller.emailAddressConfirmedSubmit(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.SubmitArrangementController.submitArrangement.url)
          }
        }

        s"[taxRegime: ${taxRegime.toString}] GET /tried-to-confirm-email-too-many-times should" - {

          "display the page" in {
            stubCommonActions()
            EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, origin)()

            val result = controller.tooManyEmailAddresses(FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId"))
            status(result) shouldBe OK

            val doc = Jsoup.parse(contentAsString(result))

            ContentAssertions.commonPageChecks(
              doc,
              "You have tried to confirm an email too many times",
              shouldBackLinkBePresent = false,
              expectedSubmitUrl       = None,
              regimeBeingTested       = Some(taxRegime)
            )

            val paragraphs = doc.select("p.govuk-body").asScala.toList

            paragraphs.size shouldBe 2

            paragraphs(0).text() shouldBe "You have made too many attempts to confirm an email address."
            paragraphs(1).select("a").text() shouldBe "Sign out"
            paragraphs(1).select("a").attr("href") shouldBe routes.SignOutController.signOut.url
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

    "GET /tried-to-confirm-email-too-many-times" in {
      status(controller.tooManyEmailAddresses(FakeRequest())) shouldBe NOT_IMPLEMENTED
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
      EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

      val result = controller.requestVerification(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectUri)

      EmailVerificationStub.verifyRequestEmailVerification(
        email,
        GGCredId("authId-999"),
        "/accessibility-statement/set-up-a-payment-plan",
        "Set up an Employers’ PAYE payment plan",
        "en",
        "",
        PageUrls.whichEmailDoYouWantToUseUrl
      )
    }

  }

}
