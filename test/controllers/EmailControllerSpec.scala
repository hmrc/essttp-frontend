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

import essttp.rootmodel.Email
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{PageUrls, TdAll}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, iterableAsScalaIterableConverter}

class EmailControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[EmailController]
  configOverrides + ("features.email-journey" -> true)

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
      redirectLocation(result) shouldBe Some(PageUrls.confirmYourEmailUrl)
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
      redirectLocation(result) shouldBe Some(PageUrls.confirmYourEmailUrl)
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

  "GET /confirm-your-email-address" - {
    "should return the confirm your email address page" in {
      stubCommonActions()
      EssttpBackend.SelectEmail.findJourney("bobross@joyOfPainting.com", encrypter = testCrypto)()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.confirmYourEmail(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

}