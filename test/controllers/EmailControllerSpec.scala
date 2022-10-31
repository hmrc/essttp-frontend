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
import essttp.rootmodel.Email
import models.GGCredId
import models.emailverification.EmailVerificationStatusResponse.EmailStatus
import models.emailverification.RequestEmailVerificationResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.{Call, Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{EmailVerificationStub, EssttpBackend}
import testsupport.testdata.{PageUrls, TdAll}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, iterableAsScalaIterableConverter}

class EmailControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[EmailController]

  override lazy val configOverrides: Map[String, Any] = Map("features.email-journey" -> true)

  "GET /which-email-do-you-want-to-use" - {

    "should return the which email do you want to use page" in {
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.whichEmailDoYouWantToUse(fakeRequest)
      val doc: Document = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Which email do you want to use?",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.EmailController.whichEmailDoYouWantToUse.url)
      )

      val radios: Elements = doc.select(".govuk-radios__item")
      radios.size() shouldBe 2

      val radioButtons: List[Element] = radios.select(".govuk-radios__input").asScala.toList
      radioButtons(0).`val` shouldBe "bobross@joyOfPainting.com"
      radioButtons(1).`val` shouldBe "new"

      val radioLabels = radios.select(".govuk-radios__label").asScala.toList
      radioLabels(0).text() shouldBe "bobross@joyOfPainting.com"
      radioLabels(1).text() shouldBe "A new email address"

      doc.select("#newEmailInput-hint").text() shouldBe "For example, myname@sample.com"
      doc.select("#newEmailInput").attr("type") shouldBe "email"
    }

    "should prepopulate the form correctly" - {
      "existing email" in {
        stubCommonActions()
        EssttpBackend.SelectEmail.findJourney("bobross@joyOfPainting.com", encrypter = testCrypto)()
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
        EssttpBackend.SelectEmail.findJourney("somenewemail@newemail.com", encrypter = testCrypto)()
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
  }

  "POST /which-email-do-you-want-to-use should" - {

    "update backend with existing email" in {
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto)()
      EssttpBackend.SelectEmail.stubUpdateSelectedEmail(TdAll.journeyId)

      val email: Email = Email(SensitiveString("bobross@joyOfPainting.com"))

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
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto)()
      EssttpBackend.SelectEmail.stubUpdateSelectedEmail(TdAll.journeyId)

      val email: Email = Email(SensitiveString("somenewemail@newemail.com"))

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
          EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, testCrypto)()

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
            hasFormError            = true
          )

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe expectedErrorMessage
          errorLink.attr("href") shouldBe expectedErrorTarget
          EssttpBackend.SelectEmail.verifyNoneUpdateSelectedEmailRequest(TdAll.journeyId)
        }
    }

  }

  "GET /email-verification should" - {

    val email: Email = Email(SensitiveString("email@domain.com"))
    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
    val urlPrefix = "http://localhost:9215"

    "not allow journeys where an email has not been selected" in {
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto)()

      val result = controller.requestVerification(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.EmailController.whichEmailDoYouWantToUse.url)
    }

    "redirect to the given redirectUri if the call to request email verification is successful" in {
      val redirectUri: String = "/redirect"

      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
      EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

      val result = controller.requestVerification(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9890$redirectUri")

      EmailVerificationStub.verifyRequestEmailVerification(
        email,
        GGCredId("authId-999"),
        "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
        "Set up an Employers’ PAYE payment plan",
        "en",
        urlPrefix
      )
    }

    "maintain the redirectUri in the email verification response if the environment is local and the uri is absolute" in {
      val redirectUri: String = "http:///host:12345/redirect"

      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
      EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

      val result = controller.requestVerification(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectUri)

      EmailVerificationStub.verifyRequestEmailVerification(
        email,
        GGCredId("authId-999"),
        "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
        "Set up an Employers’ PAYE payment plan",
        "en",
        urlPrefix
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
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
      EmailVerificationStub.requestEmailVerification(Right(RequestEmailVerificationResponse.Success(redirectUri)))

      val result = controller.requestVerification(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectUri)

      EmailVerificationStub.verifyRequestEmailVerification(
        email,
        GGCredId("authId-999"),
        "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
        "Trefnu cynllun talu ar gyfer TWE Cyflogwyr",
        "cy",
        urlPrefix
      )
    }

    "redirect to the too-many-emails page if a 401 (UNAUTHORIZED) response is given by email-verification" in {
      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
      EmailVerificationStub.requestEmailVerification(Left(UNAUTHORIZED))

      val result = controller.requestVerification(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.tooManyEmailAddressesUrl)

      EmailVerificationStub.verifyRequestEmailVerification(
        email,
        GGCredId("authId-999"),
        "http://localhost:12346/accessibility-statement/set-up-a-payment-plan",
        "Set up an Employers’ PAYE payment plan",
        "en",
        urlPrefix
      )
    }

  }

  "GET /email-callback should" - {

    val email: Email = Email(SensitiveString("email@domain.com"))

    val ggCredId: GGCredId = GGCredId("authId-999")

    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

    "not allow journeys where an email has not been selected" in {
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, encrypter = testCrypto)()

      val result = controller.emailCallback(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.EmailController.whichEmailDoYouWantToUse.url)
    }

    "redirect to the email address confirmed page if the email address has successfully been verified" in {
      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
      EmailVerificationStub.getVerificationStatus(
        ggCredId,
        Right(List(EmailStatus(email.value.decryptedValue, verified = true, locked = false)))
      )
      EssttpBackend.EmailVerificationStatus.stubEmailVerificationStatus(TdAll.journeyId)

      val result = controller.emailCallback(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.EmailController.emailAddressConfirmed.url)

      EssttpBackend.EmailVerificationStatus.verifyEmailVerificationStatusRequest(
        TdAll.journeyId, EmailVerificationStatus.Verified
      )
    }

    "redirect to the too many passcodes page if the email address has been locked" in {
      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
      EmailVerificationStub.getVerificationStatus(
        ggCredId,
        Right(List(EmailStatus(email.value.decryptedValue, verified = false, locked = true)))
      )
      EssttpBackend.EmailVerificationStatus.stubEmailVerificationStatus(TdAll.journeyId)

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
        EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
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
        ).foreach{
            case (verified, locked) =>
              withClue(s"For verified=$verified and locked=$locked: "){
                stubCommonActions()
                EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
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
        EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
        EmailVerificationStub.getVerificationStatus(
          ggCredId,
          Right(List(EmailStatus("another@email.com", verified = true, locked = false)))
        )

        an[Exception] shouldBe thrownBy(controller.emailCallback(fakeRequest).futureValue)

      }

    }

  }

  "GET /email-address-confirmed should" - {

    val email: Email = Email(SensitiveString("email@test.com"))

    "not allow journey when" - {

        def test(
            journeyStubMapping:       () => StubMapping,
            expectedRedirectLocation: Call
        ) = {
          stubCommonActions()
          journeyStubMapping()

          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
          val result = controller.emailAddressConfirmed(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirectLocation.url)
        }

      "an email verification result has not been obtained yet" in {
        test(
          () => EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)(),
          routes.EmailController.whichEmailDoYouWantToUse
        )
      }

      "an arrangement has already been submitted" in {
        test(
          () => EssttpBackend.SubmitArrangement.findJourney(testCrypto)(),
          routes.PaymentPlanSetUpController.paymentPlanSetUp
        )
      }

      "an email verification result has been obtained but it is locked" in {
        test(
          () => EssttpBackend.EmailVerificationStatus.findJourney(email.value.decryptedValue, EmailVerificationStatus.Locked, testCrypto)(),
          routes.EmailController.tooManyPasscodeAttempts
        )
      }

    }

    "display the page when the email address has successully been verified" in {
      stubCommonActions()
      EssttpBackend.EmailVerificationStatus.findJourney(email.value.decryptedValue, EmailVerificationStatus.Verified, testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.emailAddressConfirmed(fakeRequest)
      status(result) shouldBe OK

      val doc = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        "Email address confirmed",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None
      )

      val paragraphs = doc.select(".govuk-body").asScala.toList
      paragraphs.size shouldBe 3
      paragraphs(0).html() shouldBe s"The email address <strong>${email.value.decryptedValue}</strong> has been confirmed."

      doc.select(".govuk-button").attr("href") shouldBe routes.SubmitArrangementController.submitArrangement.url
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
      EssttpBackend.SelectEmail.findJourney(email.value.decryptedValue, testCrypto)()
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
        ""
      )
    }

  }

}
