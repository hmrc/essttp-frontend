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

import controllers.BankDetailsControllerSpec.SummaryRow
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
import testsupport.testdata.{PageUrls, TdAll, TdJsonBodies}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, collectionAsScalaIterableConverter}

class BankDetailsControllerSpec extends ItSpec {

  private val controller: BankDetailsController = app.injector.instanceOf[BankDetailsController]
  private val expectedServiceName: String = TdAll.expectedServiceNamePaye

  object EnterDirectDebitDetailsPage {
    val expectedH1: String = "Set up Direct Debit"
    val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
    val accountNameContent: String = "Name on the account"
    val accountNameFieldId: String = "#name"
    val sortCodeContent: String = "Sort code"
    val sortCodeHintContent: String = "Must be 6 digits long"
    val sortCodeFieldId: String = "#sortCode"
    val accountNumberContent: String = "Account number"
    val accountNumberHintContent: String = "Must be between 6 and 8 digits long"
    val accountNumberFieldId: String = "#accountNumber"
    val accountHolderContent: String = "Are you an account holder?"
    val accountHolderHintContent: String = "You must be able to set up a Direct Debit without permission from any other account holders."
    val accountHolderRadioId: String = "#isSoleSignatory"
  }

  object ConfirmDirectDebitDetailsPage {
    val expectedH1: String = "Check your Direct Debit details"
    val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
  }

  object TermsAndConditionsPage {
    val expectedH1: String = "Terms and conditions"
    val expectedPageTitle: String = s"$expectedH1 - $expectedServiceName - GOV.UK"
  }

  def testFormError(formData: (String, String)*)(textAndHrefContent: List[(String, String)]): Unit = {
    val fakeRequest = FakeRequest(
      method = "POST",
      path   = "/set-up-direct-debit"
    ).withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(formData: _*)

    val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
    status(result) shouldBe Status.OK

    val pageContent: String = contentAsString(result)
    val doc: Document = Jsoup.parse(pageContent)
    val errorSummary = doc.select(".govuk-error-summary__list")
    val errorLinks = errorSummary.select("a").asScala.toList
    errorLinks.zip(textAndHrefContent).foreach { testData: (Element, (String, String)) =>
      {
        testData._1.text() shouldBe testData._2._1
        testData._1.attr("href") shouldBe testData._2._2
      }
    }
  }

  def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map { e =>
    SummaryRow(
      e.select(".govuk-summary-list__key").text(),
      e.select(".govuk-summary-list__value").text(),
      e.select(".govuk-summary-list__actions > .govuk-link").attr("href")
    )
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

      doc.title() shouldBe EnterDirectDebitDetailsPage.expectedPageTitle
      doc.select(".govuk-heading-xl").text() shouldBe EnterDirectDebitDetailsPage.expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.PaymentScheduleController.checkPaymentSchedule().url // todo update this, it depends on journey

      val subheadings = doc.select(".govuk-label--m").asScala.toList
      subheadings(0).text() shouldBe EnterDirectDebitDetailsPage.accountNameContent
      subheadings(1).text() shouldBe EnterDirectDebitDetailsPage.sortCodeContent
      subheadings(2).text() shouldBe EnterDirectDebitDetailsPage.accountNumberContent
      subheadings(3).text() shouldBe EnterDirectDebitDetailsPage.accountHolderContent

      doc.select("#sortCode-hint").text() shouldBe EnterDirectDebitDetailsPage.sortCodeHintContent
      doc.select("#accountNumber-hint").text() shouldBe EnterDirectDebitDetailsPage.accountNumberHintContent
      doc.select("#isSoleSignatory-hint").text() shouldBe EnterDirectDebitDetailsPage.accountHolderHintContent

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

      doc.select(EnterDirectDebitDetailsPage.accountNameFieldId).`val`() shouldBe "Bob Ross"
      doc.select(EnterDirectDebitDetailsPage.sortCodeFieldId).`val`() shouldBe "123456"
      doc.select(EnterDirectDebitDetailsPage.accountNumberFieldId).`val`() shouldBe "12345678"
      doc.select(EnterDirectDebitDetailsPage.accountHolderRadioId).`val`() shouldBe "Yes"
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
      val formData: List[(String, String)] = List(
        ("name", ""),
        ("sortCode", ""),
        ("accountNumber", ""),
        ("isSoleSignatory", "")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Enter the name on the account", EnterDirectDebitDetailsPage.accountNameFieldId),
        ("Enter sort code", EnterDirectDebitDetailsPage.sortCodeFieldId),
        ("Enter account number", EnterDirectDebitDetailsPage.accountNumberFieldId),
        ("Select yes if you are the account holder", EnterDirectDebitDetailsPage.accountHolderRadioId)
      )
      testFormError(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
    }

    "show correct error messages when submitted sort code and account number are not numeric" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()
      val formData: List[(String, String)] = List(
        ("name", "Bob Ross"),
        ("sortCode", "12E456"),
        ("accountNumber", "12E45678"),
        ("isSoleSignatory", "Yes")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Sort code must be a number", EnterDirectDebitDetailsPage.sortCodeFieldId),
        ("Account number must be a number", EnterDirectDebitDetailsPage.accountNumberFieldId)
      )
      testFormError(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
    }

    "show correct error messages when submitted sort code and account number are more than 6 and 8 digits respectively" in {
      AuthStub.authorise()
      EssttpBackend.HasCheckedPlan.findJourneyAfterUpdateHasCheckedPlan()
      val formData: List[(String, String)] = List(
        ("name", "Bob Ross"),
        ("sortCode", "1234567"),
        ("accountNumber", "123456789"),
        ("isSoleSignatory", "Yes")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Sort code must be 6 digits", EnterDirectDebitDetailsPage.sortCodeFieldId),
        ("Account number must be between 6 and 8 digits", EnterDirectDebitDetailsPage.accountNumberFieldId)
      )
      testFormError(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
    }
  }

  "GET /check-your-direct-debit-details should" - {
    "return 200 and the check your direct debit details page" in {
      AuthStub.authorise()
      EssttpBackend.DirectDebitDetails.findJourneyAfterUpdateDirectDebitDetails()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.checkBankDetails(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe ConfirmDirectDebitDetailsPage.expectedPageTitle
      doc.select(".govuk-heading-xl").text() shouldBe ConfirmDirectDebitDetailsPage.expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.BankDetailsController.enterBankDetails().url

      val summaries = doc.select(".govuk-summary-list").select(".govuk-summary-list__row").iterator().asScala.toList
      summaries.size shouldBe 3

      val expectedAccountNameRow = SummaryRow("Account name", "Bob Ross", routes.BankDetailsController.enterBankDetails().url)
      val expectedSortCodeRow = SummaryRow("Sort code", "123456", routes.BankDetailsController.enterBankDetails().url)
      val expectedAccountNumberRow = SummaryRow("Account number", "12345678", routes.BankDetailsController.enterBankDetails().url)
      val expectedSummaryRows = List(expectedAccountNameRow, expectedSortCodeRow, expectedAccountNumberRow)
      extractSummaryRows(summaries) shouldBe expectedSummaryRows

      doc.select(".govuk-heading-m").text() shouldBe "The Direct Debit Guarantee"
      val directDebitGuaranteeParagraphs = doc.select(".govuk-body").asScala.toList.dropRight(1)
      directDebitGuaranteeParagraphs(0).text() shouldBe "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
      directDebitGuaranteeParagraphs(1).text() shouldBe "If there are any changes to the amount, date or frequency of your Direct Debit HMRC NDDS will notify you 10 working days in advance of your account being debited or as otherwise agreed. If you request HMRC NDDS to collect a payment, confirmation of the amount and date will be given to you at the time of the request."
      directDebitGuaranteeParagraphs(2).text() shouldBe "If an error is made in the payment of your Direct Debit by HMRC NDDS or your bank or building society you are entitled to a full and immediate refund of the amount paid from your bank or building society. If you receive a refund you are not entitled to, you must pay it back when HMRC NDDS asks you to."
      directDebitGuaranteeParagraphs(3).text() shouldBe "You can cancel a Direct Debit at any time by simply contacting your bank or building society. Written confirmation may be required. Please also notify us."
    }

    "redirect user to cannot setup direct debit if they try and force browse, but they said they aren't the account holder" in {
      AuthStub.authorise()
      EssttpBackend.DirectDebitDetails.findJourneyAfterUpdateDirectDebitDetails(TdJsonBodies.afterDirectDebitDetailsJourneyJson(isAccountHolder = false))

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.checkBankDetails(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.cannotSetupDirectDebitOnlineUrl)
    }
  }

  "POST /check-your-direct-debit-details should" - {
    "redirect the user to terms and conditions and update backend" in {
      AuthStub.authorise()
      EssttpBackend.DirectDebitDetails.findJourneyAfterUpdateDirectDebitDetails()
      EssttpBackend.ConfirmedDirectDebitDetails.updateConfirmDirectDebitDetails(TdAll.journeyId)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.checkBankDetailsSubmit(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.termsAndConditionsUrl)
      EssttpBackend.ConfirmedDirectDebitDetails.verifyUpdateConfirmDirectDebitDetailsRequest(TdAll.journeyId)
    }
    "redirect the user to cannot setup direct debit if they try and force browse, but they aren't the account holder" in {
      AuthStub.authorise()
      EssttpBackend.DirectDebitDetails.findJourneyAfterUpdateDirectDebitDetails(TdJsonBodies.afterDirectDebitDetailsJourneyJson(isAccountHolder = false))
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.checkBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.cannotSetupDirectDebitOnlineUrl)
      EssttpBackend.ConfirmedDirectDebitDetails.verifyNoneUpdateConfirmDirectDebitDetailsRequest(TdAll.journeyId)
    }
  }

  "GET /terms-and-conditions should" - {
    "return 200 and the terms and conditions page" in {
      AuthStub.authorise()
      EssttpBackend.ConfirmedDirectDebitDetails.findJourneyAfterConfirmUpdateDirectDebitDetails()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.termsAndConditions(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      doc.title() shouldBe TermsAndConditionsPage.expectedPageTitle
      doc.select(".govuk-heading-xl").text() shouldBe TermsAndConditionsPage.expectedH1
      doc.select(".hmrc-header__service-name").text() shouldBe expectedServiceName
      doc.select(".hmrc-sign-out-nav__link").attr("href") shouldBe "http://localhost:9949/auth-login-stub/session/logout"
      doc.select("#back").attr("href") shouldBe routes.BankDetailsController.checkBankDetails().url

      val contentItems = List(
        "We can cancel this agreement if you:",
        "If we cancel this agreement, you will need to pay the total amount you owe straight away.",
        "We can use any refunds you might get to pay off your tax charges.",
        "If your circumstances change and you can pay more or you can pay in full, you need to let us know.",
        "I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt."
      )
      doc.select(".govuk-body").asScala.toList
        .zip(contentItems)
        .map(things => things._1.text() shouldBe things._2)

      val bulletListItems = List(
        "pay late or miss a payment",
        "pay another tax bill late",
        "do not submit your future tax returns on time"
      )
      doc.select(".govuk-list--bullet").select("li").asScala.toList
        .zip(bulletListItems)
        .map(things => things._1.text() shouldBe things._2)

      doc.select(".govuk-heading-m").text() shouldBe "Declaration"
      doc.select(".govuk-button").text() shouldBe "Agree and continue"
    }
  }

  "POST /terms-and-conditions should" - {
    "redirect the user to terms and conditions and update backend" in {
      AuthStub.authorise()
      EssttpBackend.ConfirmedDirectDebitDetails.findJourneyAfterConfirmUpdateDirectDebitDetails()
      EssttpBackend.TermsAndConditions.updateAgreedTermsAndConditions(TdAll.journeyId)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.confirmationUrl)
      EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId)
    }
  }
}

object BankDetailsControllerSpec {
  final case class SummaryRow(question: String, answer: String, changeLink: String)
}
