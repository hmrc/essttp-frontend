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

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.{AuthStub, EssttpBackend}
import uk.gov.hmrc.http.SessionKeys
import testsupport.TdRequest.FakeRequestOps
import testsupport.testdata.{PageUrls, TdAll}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

class BankDetailsControllerSpec extends ItSpec {

  private val controller: BankDetailsController = app.injector.instanceOf[BankDetailsController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye
  private val expectedH1: String = "Set up Direct Debit"
  private val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
  private val accountNameContent: String = "Name on the account"
  private val accountNameFieldId: String = "#name"
  private val sortCodeContent: String = "Sort code"
  private val sortCodeHintContent: String = "Must be 6 digits long"
  private val sortCodeFieldId: String = "#sortCode"
  private val accountNumberContent: String = "Account number"
  private val accountNumberHintContent: String = "Must be between 6 and 8 digits long"
  private val accountNumberFieldId: String = "#accountNumber"
  private val accountHolderContent: String = "Are you an account holder?"
  private val accountHolderHintContent: String = "You must be able to set up a Direct Debit without permission from any other account holders."
  private val accountHolderRadioId: String = "#isSoleSignatory"

  def testFormError(elements: Element*)(textAndHrefContent: List[(String, String)]): Unit =
    elements.zip(textAndHrefContent).foreach { testData: (Element, (String, String)) => {
      testData._1.text() shouldBe testData._2._1
      testData._1.attr("href") shouldBe testData._2._2
    }
  }

  "GET /set-up-direct-debit should" - {

    "should return 200 and the bank details page" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.enterBankDetails(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe expectedPageTitle
      doc.select(".govuk-heading-xl").text() shouldBe expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.PaymentScheduleController.checkPaymentSchedule().url // todo update this, it depends on journey

      val subheadings = doc.select(".govuk-label--m").asScala.toList
      subheadings(0).text() shouldBe accountNameContent
      subheadings(1).text() shouldBe sortCodeContent
      subheadings(2).text() shouldBe accountNumberContent
      subheadings(3).text() shouldBe accountHolderContent

      doc.select("#sortCode-hint").text() shouldBe sortCodeHintContent
      doc.select("#accountNumber-hint").text() shouldBe accountNumberHintContent
      doc.select("#isSoleSignatory-hint").text() shouldBe accountHolderHintContent

      val radioContent = doc.select(".govuk-radios__label").asScala.toList
      radioContent(0).text() shouldBe "Yes"
      radioContent(1).text() shouldBe "No"
    }

    "prepopulate the form when the user has the direct debit details in their journey" in {
      AuthStub.authorise()
      EssttpBackend.DirectDebitDetails.findJourneyAfterUpdateDirectDebitDetails()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.enterBankDetails(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.select(accountNameFieldId).`val`() shouldBe "Bob Ross"
      doc.select(sortCodeFieldId).`val`() shouldBe "123456"
      doc.select(accountNumberFieldId).`val`() shouldBe "12345678"
      doc.select(accountHolderRadioId).`val`() shouldBe "Yes"
    }
  }

  "POST /set-up-direct-debit should" - {

    "redirect to /check-bank-details when valid form is submitted" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()
      EssttpBackend.DirectDebitDetails.updateDirectDebitDetails(TdAll.journeyId)
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/set-up-direct-debit"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("name", "Bob Ross"),
          ("sortCode", "123456"),
          ("accountNumber", "12345678"),
          ("isSoleSignatory", "Yes")
        )
      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)
      EssttpBackend.DirectDebitDetails.verifyUpdateDirectDebitDetailsRequest(TdAll.journeyId)
    }

    "redirect to /you-cannot-set-up-a-direct-debit-online when user submits no for radio option relating to being account holder" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()
      EssttpBackend.DirectDebitDetails.updateDirectDebitDetails(TdAll.journeyId)
      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/set-up-direct-debit"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("name", "Bob Ross"),
          ("sortCode", "123456"),
          ("accountNumber", "12345678"),
          ("isSoleSignatory", "No")
        )
      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.cannotSetupDirectDebitOnlineUrl)
      EssttpBackend.DirectDebitDetails.verifyUpdateDirectDebitDetailsRequest(TdAll.journeyId)
    }

    "show correct error messages when form submitted is empty" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/set-up-direct-debit"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("name", ""),
          ("sortCode", ""),
          ("accountNumber", ""),
          ("isSoleSignatory", "")
        )
      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.OK
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      val errorSummary = doc.select(".govuk-error-summary__list")
      val errorLinks = errorSummary.select("a").asScala.toList
      val expectedContentAndHref: List[(String, String)] = List(
        ("Enter the name on the account", accountNameFieldId),
        ("Enter sort code", sortCodeFieldId),
        ("Enter account number", accountNumberFieldId),
        ("Select yes if you are the account holder", accountHolderRadioId)
      )
      testFormError(errorLinks: _*)(expectedContentAndHref)
    }

    "show correct error messages when submitted sort code and account number are not numeric" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/set-up-direct-debit"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("name", "Bob Ross"),
          ("sortCode", "12E456"),
          ("accountNumber", "12E45678"),
          ("isSoleSignatory", "Yes")
        )
      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.OK
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)
      val errorSummary = doc.select(".govuk-error-summary__list")
      val errorLinks: List[Element] = errorSummary.select("a").asScala.toList
      val expectedContentAndHref: List[(String, String)] = List(
        ("Sort code must be a number", sortCodeFieldId),
        ("Account number must be a number", accountNumberFieldId)
      )
      testFormError(errorLinks: _*)(expectedContentAndHref)
    }

    "show correct error messages when submitted sort code and account number are more than 6 and 8 digits respectively" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/set-up-direct-debit"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(
          ("name", "Bob Ross"),
          ("sortCode", "1234567"),
          ("accountNumber", "123456789"),
          ("isSoleSignatory", "Yes")
        )
      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.OK
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)
      val errorSummary = doc.select(".govuk-error-summary__list")
      val errorLinks = errorSummary.select("a").asScala.toList
      val expectedContentAndHref: List[(String, String)] = List(
        ("Sort code must be 6 digits", sortCodeFieldId),
        ("Account number must be between 6 and 8 digits", accountNumberFieldId)
      )
      testFormError(errorLinks: _*)(expectedContentAndHref)
    }
  }

}
