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

import controllers.BankDetailsControllerSpec.SummaryRow
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.bank.{CanSetUpDirectDebit, TypeOfBankAccount, TypesOfBankAccount}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend.BarsVerifyStatusStub
import testsupport.stubs.{AuditConnectorStub, BarsStub, EssttpBackend}
import testsupport.testdata.BarsJsonResponses.{ValidateJson, VerifyJson}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import java.time._
import java.util.Locale
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}

class BankDetailsControllerSpec extends ItSpec {

  private val controller: BankDetailsController = app.injector.instanceOf[BankDetailsController]

  object CheckYouCanSetupDAccountPage {
    val expectedH1: String = "Check you can set up a Direct Debit"

    val typeOfAccountHeading: String       = "What type of account details are you providing?"
    val radioButtonContentBusiness: String = "Business bank account"
    val radioButtonContentPersonal: String = "Personal bank account"

    val accountHolderHeading: String     = "Can you set up a Direct Debit for this payment plan?"
    val accountHolderHintContent: String =
      "You must be the sole account holder, or for multi-signature accounts you must have authority to set up a Direct Debit without additional signatures."
    val accountHolderRadioId: String     = "#isSoleSignatory"

    val buttonContent: String = "Continue"

  }

  object EnterDirectDebitDetailsPage {
    val expectedH1: String               = "Bank account details"
    val bankDetails: String              = "Bank details"
    val accountType: String              = "Account Type"
    val businessAccount: String          = "Business"
    val personalAccount: String          = "Personal"
    val accountNameContent: String       = "Name on the account"
    val accountTypeFieldId: String       = "#business"
    val accountNameFieldId: String       = "#name"
    val sortCodeContent: String          = "Sort code"
    val sortCodeHintContent: String      = "Must be 6 digits long"
    val sortCodeFieldId: String          = "#sortCode"
    val accountNumberContent: String     = "Account number"
    val accountNumberHintContent: String = "Must be between 6 and 8 digits long"
    val accountNumberFieldId: String     = "#accountNumber"
  }

  object ConfirmDirectDebitDetailsPage {
    val expectedH1: String = "Check your Direct Debit details"
  }

  object CannotSetupDirectDebitPage {
    val expectedH1: String                              = "Call us about a payment plan"
    def paragraphContent1(taxRegime: TaxRegime): String =
      taxRegime match {
        case TaxRegime.Epaye =>
          "You cannot set up an Employers’ PAYE payment plan online if you are not the only account holder."
        case TaxRegime.Vat   => "You cannot set up a VAT payment plan online if you are not the only account holder."
        case TaxRegime.Sa    =>
          "You cannot set up a Self Assessment payment plan online if you are not the only account holder."
        case TaxRegime.Simp  =>
          "You cannot set up a Simple Assessment payment plan online if you are not the only account holder."
      }
    val paragraphContent2: String                       =
      "Call us on 0300 123 1813 if you need to set up a Direct Debit from a joint account. All account holders must be present when calling."
  }

  object BarsLockoutPage {
    val expectedH1: String = "You’ve tried to confirm your bank details too many times"
  }

  trait SubmitSuccessSetup {
    stubCommonActions()
    val formData: List[(String, String)]                     = List(
      ("accountType", "Personal"),
      ("name", TdAll.testAccountName),
      ("sortCode", "123456"),
      ("accountNumber", "12345678")
    )
    val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(
      method = "POST",
      path = "/bank-account-details"
    ).withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(formData*)
  }

  private def getExpectedFormValue(field: String, formData: Seq[(String, String)]): String =
    formData.collectFirst { case (x, formValue) if x == field => formValue }.getOrElse("")

  def testFormError(action: Action[AnyContent])(formData: (String, String)*)(
    textAndHrefContent: List[(String, String)],
    additionalChecks:   Document => Unit
  ): Unit = {
    val fakeRequest = FakeRequest()
      .withMethod("POST")
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(formData*)

    val result: Future[Result] = action(fakeRequest)

    RequestAssertions.assertGetRequestOk(result)

    val pageContent: String = contentAsString(result)
    val doc: Document       = Jsoup.parse(pageContent)

    val errorSummary = doc.select(".govuk-error-summary__list")
    val errorLinks   = errorSummary.select("a").asScala.toList
    errorLinks.zip(textAndHrefContent).foreach { (testData: (Element, (String, String))) =>
      testData._1.text() shouldBe testData._2._1
      testData._1.attr("href") shouldBe testData._2._2
    }

    additionalChecks(doc)
  }

  def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map { e =>
    SummaryRow(
      e.select(".govuk-summary-list__key").text(),
      e.select(".govuk-summary-list__value").text()
    )
  }

  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat),
    ("SA", Origins.Sa.Bta, TaxRegime.Sa),
    ("SIMP", Origins.Simp.Pta, TaxRegime.Simp)
  ).foreach { case (regime, origin, taxRegime) =>
    "GET /check-you-can-set-up-a-direct-debit should" - {

      s"[$regime journey] return 200 and display the 'check you can set up a direct debit' page" in {
        stubCommonActions()
        EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)()

        val result: Future[Result] = controller.detailsAboutBankAccount(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = CheckYouCanSetupDAccountPage.expectedH1,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.BankDetailsController.detailsAboutBankAccountSubmit.url),
          regimeBeingTested = Some(taxRegime)
        )

        val formGroups = doc.select(".govuk-form-group").asScala.toList
        formGroups.size shouldBe 1

        val accountHolderFormGroup = formGroups(0)
        accountHolderFormGroup
          .select(".govuk-fieldset__legend")
          .text() shouldBe CheckYouCanSetupDAccountPage.accountHolderHeading

        val accountHolderRadioContent = accountHolderFormGroup.select(".govuk-radios__label").asScala.toList
        accountHolderRadioContent.size shouldBe 2
        accountHolderRadioContent(0).text() shouldBe "Yes"
        accountHolderRadioContent(1).text() shouldBe "No"

        doc.select(".govuk-button").text() shouldBe CheckYouCanSetupDAccountPage.buttonContent
      }

      Seq(
        (true, JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = true, origin), 0),
        (false, JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = false, origin), 1)
      ).foreach { case (isAccountHolder, wiremockJson, isAccountHolderCheckedElementIndex) =>
        s"[$regime journey] prepopulate the form when the user has isAccountHolder=${isAccountHolder.toString} in their journey" in {
          stubCommonActions()
          EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(wiremockJson)

          val result: Future[Result] = controller.detailsAboutBankAccount(fakeRequest)
          val pageContent: String    = contentAsString(result)
          val doc: Document          = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)

          val formGroups = doc.select(".govuk-form-group").asScala.toList
          formGroups.size shouldBe 1

          formGroups(0)
            .select(".govuk-radios__input")
            .asScala
            .toList(isAccountHolderCheckedElementIndex)
            .hasAttr("checked") shouldBe true
        }
      }
    }

    "POST /check-you-can-set-up-a-direct-debit should" - {

      def testRedirect(
        formBody: (String, String)
      )(expectedRedirectUrl: String, expectedDetailsAboutBankAccount: CanSetUpDirectDebit): Unit = {
        val updatedJourneyJson = JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(
          expectedDetailsAboutBankAccount.isAccountHolder,
          origin
        )

        stubCommonActions()
        EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)()
        EssttpBackend.EnteredCanSetUpDirectDebit
          .stubUpdateEnteredCanSetUpDirectDebit(TdAll.journeyId, updatedJourneyJson)

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/what-type-of-account-details-are-you-providing"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(formBody)

        val result: Future[Result] = controller.detailsAboutBankAccountSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
        EssttpBackend.EnteredCanSetUpDirectDebit
          .verifyUpdateEnteredCanSetUpDirectDebitRequest(TdAll.journeyId, expectedDetailsAboutBankAccount)

      }

      s"[$regime journey] redirect to /bank-account-details when valid form is submitted and the user is an account holder" in {
        testRedirect(
          ("isSoleSignatory", "Yes")
        )(PageUrls.directDebitDetailsUrl, TdAll.detailsAboutBankAccount(isAccountHolder = true))
      }

      s"[$regime journey] redirect to /bank-account-details when valid form is submitted and the user is not an account holder" in {
        testRedirect(
          ("isSoleSignatory", "No")
        )(PageUrls.cannotSetupDirectDebitOnlineUrl, TdAll.detailsAboutBankAccount(isAccountHolder = false))
      }

      s"[$regime journey] show correct error messages when form is empty" in {

        val emptyForm                              = ("isSoleSignatory", "")
        val expectedErrors: List[(String, String)] = List(
          (
            "Select yes if you can set up a Direct Debit for this payment plan",
            CheckYouCanSetupDAccountPage.accountHolderRadioId
          )
        )

        stubCommonActions()
        EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)()

        testFormError(controller.detailsAboutBankAccountSubmit)(emptyForm)(
          expectedErrors,
          { doc =>
            val radioInputs = doc.select(".govuk-radios__input")

            radioInputs.asScala.toList.foreach(_.hasAttr("checked") shouldBe false)

          }
        )
        EssttpBackend.EnteredCanSetUpDirectDebit.verifyNoneUpdateEnteredCanSetUpDirectDebitRequest(TdAll.journeyId)
      }

    }

    "GET /bank-account-details should" - {

      s"[$regime journey] should return 200 and the bank details page" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.enterBankDetails(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = EnterDirectDebitDetailsPage.expectedH1,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.BankDetailsController.enterBankDetailsSubmit.url),
          regimeBeingTested = Some(taxRegime)
        )

        val nameInput          = doc.select("input[name=name]")
        val sortCodeInput      = doc.select("input[name=sortCode]")
        val accountNumberInput = doc.select("input[name=accountNumber]")

        nameInput.attr("autocomplete") shouldBe "name"
        nameInput.attr("spellcheck") shouldBe "false"

        sortCodeInput.attr("autocomplete") shouldBe "off"
        sortCodeInput.attr("inputmode") shouldBe "numeric"
        sortCodeInput.attr("spellcheck") shouldBe "false"

        accountNumberInput.attr("autocomplete") shouldBe "off"
        accountNumberInput.attr("inputmode") shouldBe "numeric"
        accountNumberInput.attr("spellcheck") shouldBe "false"

        val subheadings = doc.select(".govuk-label").asScala.toList
        subheadings.size shouldBe 6
        subheadings(0).text() shouldBe EnterDirectDebitDetailsPage.businessAccount
        subheadings(1).text() shouldBe EnterDirectDebitDetailsPage.personalAccount
        subheadings(2).text() shouldBe EnterDirectDebitDetailsPage.accountNameContent
        subheadings(3).text() shouldBe EnterDirectDebitDetailsPage.bankDetails
        subheadings(4).text() shouldBe EnterDirectDebitDetailsPage.sortCodeContent
        subheadings(5).text() shouldBe EnterDirectDebitDetailsPage.accountNumberContent

        doc.select("#sortCode-hint").text() shouldBe EnterDirectDebitDetailsPage.sortCodeHintContent
        doc.select("#accountNumber-hint").text() shouldBe EnterDirectDebitDetailsPage.accountNumberHintContent
      }

      s"[$regime journey] prepopulate the form when the user has the direct debit details in their journey" in {
        stubCommonActions()
        EssttpBackend.DirectDebitDetails.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.enterBankDetails(fakeRequest)
        val pageContent: String    = contentAsString(result)
        val doc: Document          = Jsoup.parse(pageContent)

        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = EnterDirectDebitDetailsPage.expectedH1,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.BankDetailsController.enterBankDetailsSubmit.url),
          regimeBeingTested = Some(taxRegime)
        )
        RequestAssertions.assertGetRequestOk(result)

        doc.select(EnterDirectDebitDetailsPage.accountNameFieldId).`val`() shouldBe TdAll.testAccountName
        doc.select(EnterDirectDebitDetailsPage.sortCodeFieldId).`val`() shouldBe "123456"
        doc.select(EnterDirectDebitDetailsPage.accountNumberFieldId).`val`() shouldBe "12345678"
      }

      s"[$regime journey] redirect to the 'cannot set up direct debit' page if the user has said they are not an account holder" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
          JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = false, origin)
        )

        val result: Future[Result] = controller.enterBankDetails(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage.url)
      }

    }

    "POST /bank-account-details should" - {

      def testBankDetailsFormError(
        action:             Action[AnyContent]
      )(formData: (String, String)*)(
        textAndHrefContent: List[(String, String)],
        fieldErrors:        Seq[(String, String)] = Seq.empty
      ): Unit = {

        def assertRadioButtonsChecked(doc: Document, form: Seq[(String, String)]): Unit =
          Seq("Business", "Personal").foreach { value =>
            doc.select(s".govuk-radios__input[value=\"$value\"]").hasAttr("checked") shouldBe
              form.exists { case (k, v) => k == "accountType" && v == value }
          }

        def assertFieldsPopulated(
          doc:         Document,
          form:        Seq[(String, String)],
          fieldErrors: Seq[(String, String)]
        ): Unit = {
          assertRadioButtonsChecked(doc, form)
          doc.select(EnterDirectDebitDetailsPage.accountNameFieldId).`val`() shouldBe getExpectedFormValue("name", form)
          doc.select(EnterDirectDebitDetailsPage.sortCodeFieldId).`val`() shouldBe getExpectedFormValue(
            "sortCode",
            form
          )
          doc.select(EnterDirectDebitDetailsPage.accountNumberFieldId).`val`() shouldBe getExpectedFormValue(
            "accountNumber",
            form
          )

          fieldErrors.foreach { case (field, errorMessage) =>
            doc.getElementById(s"$field-error").text.trim shouldBe s"Error: $errorMessage"
          }
        }

        testFormError(action)(formData*)(textAndHrefContent, assertFieldsPopulated(_, formData, fieldErrors))
      }

      s"[$regime journey] redirect to the 'cannot set up direct debit' page if the user has said they are not an account holder" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
          JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = false, origin)
        )

        val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest.withMethod("POST"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage.url)
      }

      s"[$regime journey] redirect to /check-bank-details when valid form is submitted" in new SubmitSuccessSetup {
        BarsStub.ValidateStub.success()
        BarsStub.VerifyPersonalStub.success()
        BarsVerifyStatusStub.update()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
          JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = true, origin)
        )
        EssttpBackend.DirectDebitDetails
          .stubUpdateDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Entered Direct Debit Details`(origin))

        val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)

        EssttpBackend.DirectDebitDetails.verifyUpdateDirectDebitDetailsRequest(
          TdAll.journeyId,
          TdAll.directDebitDetails(TdAll.testAccountName, "123456", "12345678")
        )(using testOperationCryptoFormat)

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(formData)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled() // don't update verify count on BARs success

        AuditConnectorStub.verifyEventAudited(
          auditType = "BarsCheck",
          auditEvent = Json
            .parse(
              s"""
             |{
             |  "taxDetail": ${TdAll.taxDetailJsonString(taxRegime)},
             |  "taxType": "${taxRegime.toString}",
             |  "origin": "${origin.toString().split('.').last}",
             |  "request": {
             |    "account": {
             |       "accountType": "personal",
             |       "accountHolderName": "${TdAll.testAccountName}",
             |       "sortCode": "123456",
             |       "accountNumber": "12345678"
             |    }
             |  },
             |  "response": {
             |    "isBankAccountValid": true,
             |    "barsResponse":  ${VerifyJson.success}
             |  },
             |  "barsVerify": {
             |    "unsuccessfulAttempts" : 1
             |  },
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059"
             |}
            """.stripMargin
            )
            .as[JsObject]
        )
      }

      s"[$regime journey] redirect to /check-bank-details and do not call BARs or update backend when the same form is resubmitted" in new SubmitSuccessSetup {
        EssttpBackend.DirectDebitDetails.findJourney(testCrypto, origin)(
          JourneyJsonTemplates.`Entered Direct Debit Details`(origin)
        )

        val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)

        BarsStub.verifyBarsNotCalled()
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled()

        AuditConnectorStub.verifyNoAuditEvent()
      }

      s"[$regime journey] show correct error messages when form submitted is empty" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)()

        val formData: List[(String, String)]               = List(
          ("accountType", ""),
          ("name", ""),
          ("sortCode", ""),
          ("accountNumber", "")
        )
        val expectedContentAndHref: List[(String, String)] = List(
          ("Select what type of account details you are providing", EnterDirectDebitDetailsPage.accountTypeFieldId),
          ("Enter the name on the account", EnterDirectDebitDetailsPage.accountNameFieldId),
          ("Enter sort code", EnterDirectDebitDetailsPage.sortCodeFieldId),
          ("Enter account number", EnterDirectDebitDetailsPage.accountNumberFieldId)
        )

        testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData*)(expectedContentAndHref)
        EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
        AuditConnectorStub.verifyNoAuditEvent()
      }

      s"[$regime journey] show correct error messages when submitted sort code and account number are not numeric" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)()

        val formData: List[(String, String)]               = List(
          ("accountType", "Personal"),
          ("name", "Bob Ross"),
          ("sortCode", "12E456"),
          ("accountNumber", "12E45678")
        )
        val expectedContentAndHref: List[(String, String)] = List(
          ("Sort code must be a number", EnterDirectDebitDetailsPage.sortCodeFieldId),
          ("Account number must be a number", EnterDirectDebitDetailsPage.accountNumberFieldId)
        )

        testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData*)(expectedContentAndHref)
        EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
        AuditConnectorStub.verifyNoAuditEvent()
      }

      s"[$regime journey] show correct error message when account name doesn't match regex" in {
        def testBankDetailsFormError(
          action:             Action[AnyContent]
        )(formData: (String, String)*)(
          textAndHrefContent: List[(String, String)],
          fieldErrors:        Seq[(String, String)] = Seq.empty
        ): Unit = {
          def assertFieldsPopulated(
            doc:         Document,
            form:        Seq[(String, String)],
            fieldErrors: Seq[(String, String)]
          ): Unit = {
            doc.select(EnterDirectDebitDetailsPage.accountNameFieldId).`val`() shouldBe getExpectedFormValue(
              "name",
              form
            )
            doc.select(EnterDirectDebitDetailsPage.sortCodeFieldId).`val`() shouldBe getExpectedFormValue(
              "sortCode",
              form
            )
            doc.select(EnterDirectDebitDetailsPage.accountNumberFieldId).`val`() shouldBe getExpectedFormValue(
              "accountNumber",
              form
            )

            fieldErrors.foreach { case (field, errorMessage) =>
              doc.getElementById(s"$field-error").text.trim shouldBe s"Error: $errorMessage"
            }
          }

          testFormError(action)(formData*)(textAndHrefContent, assertFieldsPopulated(_, formData, fieldErrors))
        }

        val nameTooShortError = "Name on the account must be between 2 and 39 characters"
        val nameTooLongError  = "Name on the account must be between 2 and 39 characters"

        val inputAndExpectedError = List[(String, String)](
          ""          -> "Enter the name on the account",
          "a"         -> nameTooShortError,
          "a" * 40    -> nameTooLongError,
          "ab£2"      -> "Name on the account must not contain £",
          "a$$b£c"    -> "Name on the account must not contain $ or £",
          "a?$$b£c?"  -> "Name on the account must not contain ?, $ or £",
          "a?$$b£c=?" -> "Name on the account must not contain ?, $, £ or ="
        )

        inputAndExpectedError.foreach { case (accountName, errorMessage) =>
          withClue(s"For accountName $accountName: ") {
            stubCommonActions()
            EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)()
            val formData: List[(String, String)]               = List(
              ("accountType", "Personal"),
              ("name", accountName),
              ("sortCode", "123456"),
              ("accountNumber", "12345678")
            )
            val expectedContentAndHref: List[(String, String)] = List(
              (errorMessage, EnterDirectDebitDetailsPage.accountNameFieldId)
            )
            testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData*)(expectedContentAndHref)
            EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
            AuditConnectorStub.verifyNoAuditEvent()
          }
        }
      }

      s"[$regime journey] must allow" - {

        def testIsAllowed(accountName: String) = {
          stubCommonActions()
          BarsStub.ValidateStub.success()
          BarsStub.VerifyPersonalStub.success()
          BarsVerifyStatusStub.update()
          EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
            JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = true, origin)
          )
          EssttpBackend.DirectDebitDetails
            .stubUpdateDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Entered Direct Debit Details`(origin))

          val formDataWithUnallowedCharacters: List[(String, String)] = List(
            ("accountType", "Personal"),
            ("name", accountName),
            ("sortCode", "123456"),
            ("accountNumber", "12345678")
          )
          val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded]    = FakeRequest(
            method = "POST",
            path = "/bank-account-details"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")
            .withFormUrlEncodedBody(formDataWithUnallowedCharacters*)

          val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)
        }

        List(
          "account names with 2 characters"  -> "a1",
          "account names with 39 characters" -> "a" * 39,
          "whitespace"                       -> "a 1",
          "ampersands &"                     -> "a&1",
          "'at' symbols @"                   -> "a@1",
          "left brackets"                    -> "a(1",
          "right brackets"                   -> "a)1",
          "exclamation marks !"              -> "a!1",
          "colons :"                         -> "a:1",
          "commas ,"                         -> "a,1",
          "plus-signs +"                     -> "a+1",
          "back-ticks `"                     -> "a`1",
          "hyphens -"                        -> "a-1",
          "back slashes \\"                  -> "a\\1",
          "single quotes '"                  -> "a'1",
          "fullstops ."                      -> "a.1",
          "forward slashes /"                -> "a/1",
          "carets ^"                         -> "a^1",
          "line breaks"                      -> "a\nb",
          "back spaces"                      -> "a\bb",
          "tab spaces"                       -> "a\tb",
          "carriage returns"                 -> "a\rb"
        ).foreach { case (description, value) =>
          description in testIsAllowed(value)
        }
      }

      s"[$regime journey] show correct error messages when submitted sort code and account number are more than 6 and 8 digits respectively" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)()

        val formData: List[(String, String)]               = List(
          ("accountType", "Personal"),
          ("name", "Bob Ross"),
          ("sortCode", "1234567"),
          ("accountNumber", "123456789")
        )
        val expectedContentAndHref: List[(String, String)] = List(
          ("Sort code must be 6 digits", EnterDirectDebitDetailsPage.sortCodeFieldId),
          ("Account number must be between 6 and 8 digits", EnterDirectDebitDetailsPage.accountNumberFieldId)
        )

        testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData*)(expectedContentAndHref)
        EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
        AuditConnectorStub.verifyNoAuditEvent()
      }

      s"[$regime journey] should strip out allowed separators" in {
        stubCommonActions()
        BarsStub.ValidateStub.success()
        BarsStub.VerifyPersonalStub.success()
        BarsVerifyStatusStub.update()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
          JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = true, origin)
        )
        EssttpBackend.DirectDebitDetails
          .stubUpdateDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Entered Direct Debit Details`(origin))

        val formDataWithUnallowedCharacters: List[(String, String)] = List(
          ("accountType", "Personal"),
          ("name", "Bob Ross"),
          ("sortCode", "1 2-3–4−5—6"),
          ("accountNumber", "1-2–3−4—5678")
        )
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded]    = FakeRequest(
          method = "POST",
          path = "/bank-account-details"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(formDataWithUnallowedCharacters*)

        val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)

        EssttpBackend.DirectDebitDetails.verifyUpdateDirectDebitDetailsRequest(
          TdAll.journeyId,
          TdAll.directDebitDetails("Bob Ross", "123456", "12345678")
        )(using testOperationCryptoFormat)

        val expectedFormData: List[(String, String)] = List(
          ("accountType", "Personal"),
          ("name", "Bob Ross"),
          ("sortCode", "123456"),
          ("accountNumber", "12345678")
        )

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(expectedFormData)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled() // don't update verify count on BARs success

        AuditConnectorStub.verifyEventAudited(
          auditType = "BarsCheck",
          auditEvent = Json
            .parse(
              s"""
                   |{
                   |  "taxDetail": ${TdAll.taxDetailJsonString(taxRegime)},
                   |  "taxType": "${taxRegime.toString}",
                   |  "origin": "${origin.toString().split('.').last}",
                   |  "request": {
                   |    "account": {
                   |       "accountType": "personal",
                   |       "accountHolderName": "Bob Ross",
                   |       "sortCode": "123456",
                   |       "accountNumber": "12345678"
                   |    }
                   |  },
                   |  "response": {
                   |    "isBankAccountValid": true,
                   |    "barsResponse":  ${VerifyJson.success}
                   |  },
                   |  "barsVerify": {
                   |    "unsuccessfulAttempts" : 1
                   |  },
                   |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059"
                   |}
            """.stripMargin
            )
            .as[JsObject]
        )
      }

      abstract class BarsErrorSetup(typeOfAccount: TypeOfBankAccount = TypesOfBankAccount.Personal) {
        stubCommonActions()
        BarsVerifyStatusStub.update(numberOfAttempts = 2)
        EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)

        val formData: List[(String, String)] = List(
          ("accountType", s"${typeOfAccount.toString}"),
          ("name", "Bob Ross"),
          ("sortCode", "123456"),
          ("accountNumber", "12345678")
        )

        def toExpectedBarsAuditDetailJson(
          barsResponseJson:           String,
          isBankAccountValid:         Boolean = false,
          numberOfBarsVerifyAttempts: Int = 1,
          barsVerifyLockoutTime:      Option[Instant] = None
        ): JsObject = {
          val barsVerifyJsonString =
            s"""
            |"barsVerify": {
            |  "unsuccessfulAttempts": ${numberOfBarsVerifyAttempts.toString}${barsVerifyLockoutTime.fold("")(t =>
                s""","lockoutExpiryDateTime": "${t.toString}""""
              )}
            |}
            |""".stripMargin

          Json
            .parse(
              s"""
             |{
             |  "taxDetail": ${TdAll.taxDetailJsonString(taxRegime)},
             |  "taxType": "${taxRegime.toString}",
             |  "origin": "${origin.toString.split('.').last}",
             |  "request": {
             |    "account": {
             |       "accountType": "${typeOfAccount.entryName.toLowerCase(Locale.UK)}",
             |       "accountHolderName": "Bob Ross",
             |       "sortCode": "123456",
             |       "accountNumber": "12345678"
             |    }
             |  },
             |  "response": {
             |   "isBankAccountValid": ${isBankAccountValid.toString},
             |   "barsResponse":  $barsResponseJson
             |  },
             |  $barsVerifyJsonString,
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059"
             |}
            """.stripMargin
            )
            .as[JsObject]
        }

        val fakeRequest = FakeRequest(
          method = "POST",
          path = "/bank-account-details"
        ).withAuthToken()
          .withSession(SessionKeys.sessionId -> "IamATestSessionId")
          .withFormUrlEncodedBody(formData*)

        EssttpBackend.DirectDebitDetails
          .stubUpdateDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Entered Direct Debit Details`(origin))

        typeOfAccount match {
          case TypesOfBankAccount.Personal =>
            EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
              JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = true, origin)
            )
          case TypesOfBankAccount.Business =>
            EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
              JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = true, origin)
            )
        }
      }

      abstract class BarsFormErrorSetup(barsError: String, typeOfAccount: TypeOfBankAccount)
          extends BarsErrorSetup(typeOfAccount) {

        val validForm: List[(String, String)] = formData

        val validFormBusiness: List[(String, String)] =
          List(
            ("accountType", "Business"),
            ("name", "Bob Ross"),
            ("sortCode", "123456"),
            ("accountNumber", "12345678")
          )

        private val sortCodeAndAccountNumberFieldError: List[(String, String)] =
          List("sort-code-and-account-number" -> "Enter a valid combination of bank account number and sort code")

        private val nameFieldError: List[(String, String)] =
          List("name" -> "Enter the name on the account as it appears on bank statements.")

        private val sortCodeFieldError: List[(String, String)] = List(
          (
            "sortCode",
            "You have entered a sort code which does not accept this type of payment. " +
              "Check you have entered a valid sort code or enter details for a different account"
          )
        )

        val (expectedErrorSummaryContentAndHref, expectedFieldErrors, expectedAuditResponseJson)
          : (List[(String, String)], Seq[(String, String)], String) =
          barsError match {
            case "accountNumberNotWellFormatted" =>
              BarsStub.ValidateStub.accountNumberNotWellFormatted()
              (
                List(("Enter a valid combination of bank account number and sort code", "#sortCode")),
                sortCodeAndAccountNumberFieldError,
                ValidateJson.accountNumberNotWellFormatted
              )

            case "sortCodeNotPresentOnEiscd" =>
              BarsStub.ValidateStub.sortCodeNotPresentOnEiscd()
              (
                List(("Enter a valid combination of bank account number and sort code", "#sortCode")),
                sortCodeAndAccountNumberFieldError,
                ValidateJson.sortCodeNotPresentOnEiscd
              )

            case "sortCodeDoesNotSupportsDirectDebit" =>
              BarsStub.ValidateStub.sortCodeDoesNotSupportsDirectDebit()
              (
                List(
                  (
                    "You have entered a sort code which does not accept this type of payment. " +
                      "Check you have entered a valid sort code or enter details for a different account",
                    "#sortCode"
                  )
                ),
                sortCodeFieldError,
                ValidateJson.sortCodeDoesNotSupportsDirectDebit
              )

            case "nameDoesNotMatch" =>
              BarsStub.ValidateStub.success()
              typeOfAccount match {
                case TypesOfBankAccount.Personal => BarsStub.VerifyPersonalStub.nameDoesNotMatch()
                case TypesOfBankAccount.Business => BarsStub.VerifyBusinessStub.nameDoesNotMatch()
              }
              (
                List(("Enter the name on the account as it appears on bank statements.", "#name")),
                nameFieldError,
                VerifyJson.nameDoesNotMatch
              )

            case "accountDoesNotExist" =>
              BarsStub.ValidateStub.success()
              typeOfAccount match {
                case TypesOfBankAccount.Personal => BarsStub.VerifyPersonalStub.accountDoesNotExist()
                case TypesOfBankAccount.Business => BarsStub.VerifyBusinessStub.accountDoesNotExist()
              }
              (
                List(("Enter a valid combination of bank account number and sort code", "#sortCode")),
                sortCodeAndAccountNumberFieldError,
                VerifyJson.accountDoesNotExist
              )

            case "sortCodeOnDenyList" =>
              BarsStub.ValidateStub.sortCodeOnDenyList()
              (
                List(("Enter a valid combination of bank account number and sort code", "#sortCode")),
                sortCodeAndAccountNumberFieldError,
                ValidateJson.sortCodeOnDenyList
              )

            case "otherBarsError" =>
              BarsStub.ValidateStub.success()
              typeOfAccount match {
                case TypesOfBankAccount.Personal => BarsStub.VerifyPersonalStub.otherBarsError()
                case TypesOfBankAccount.Business => BarsStub.VerifyBusinessStub.otherBarsError()
              }
              (
                List(("Enter a valid combination of bank account number and sort code", "#sortCode")),
                sortCodeAndAccountNumberFieldError,
                VerifyJson.otherBarsError
              )
          }

        def expectedBarsAuditDetailJson(
          numberOfBarsVerifyAttempts: Int = 1,
          barsVerifyLockoutTime:      Option[Instant] = None
        ): JsObject =
          toExpectedBarsAuditDetailJson(
            expectedAuditResponseJson,
            numberOfBarsVerifyAttempts = numberOfBarsVerifyAttempts,
            barsVerifyLockoutTime = barsVerifyLockoutTime
          )
      }

      s"[$regime journey] show correct error message when BARs validate response is accountNumberNotWellFormatted" in
        new BarsFormErrorSetup("accountNumberNotWellFormatted", TypesOfBankAccount.Personal) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
          BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = expectedBarsAuditDetailJson()
          )
        }

      s"[$regime journey] show correct error message when BARs validate response is sortCodeNotPresentOnEiscd" in
        new BarsFormErrorSetup("sortCodeNotPresentOnEiscd", TypesOfBankAccount.Personal) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
          BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = expectedBarsAuditDetailJson()
          )
        }

      s"[$regime journey] show correct error message when BARs validate response is sortCodeDoesNotSupportsDirectDebit" in
        new BarsFormErrorSetup("sortCodeDoesNotSupportsDirectDebit", TypesOfBankAccount.Personal) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
          BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = expectedBarsAuditDetailJson()
          )
        }

      s"[$regime journey] show correct error message when BARs verify response is nameDoesNotMatch with a personal bank account" in
        new BarsFormErrorSetup("nameDoesNotMatch", typeOfAccount = TypesOfBankAccount.Personal) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(validForm)
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = expectedBarsAuditDetailJson(numberOfBarsVerifyAttempts = 2)
          )
        }

      s"[$regime journey] show correct error message when BARs verify response is nameDoesNotMatch with a business bank account" in
        new BarsFormErrorSetup("nameDoesNotMatch", typeOfAccount = TypesOfBankAccount.Business) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validFormBusiness*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(validFormBusiness)
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = expectedBarsAuditDetailJson(numberOfBarsVerifyAttempts = 2)
          )
        }

      s"[$regime journey] show correct error message when bars verify-personal responds with accountExists is No" in
        new BarsFormErrorSetup("accountDoesNotExist", typeOfAccount = TypesOfBankAccount.Personal) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(validForm)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        }

      s"[$regime journey] show correct error message when bars verify-business responds with accountExists is No" in
        new BarsFormErrorSetup("accountDoesNotExist", typeOfAccount = TypesOfBankAccount.Business) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validFormBusiness*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(validFormBusiness)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        }

      s"[$regime journey] show correct error message when bars validate response is 400 sortCodeOnDenyList" in
        new BarsFormErrorSetup("sortCodeOnDenyList", typeOfAccount = TypesOfBankAccount.Business) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validFormBusiness*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.ValidateStub.ensureBarsValidateCalled(validFormBusiness)
          BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled()
        }

      s"[$regime journey] show correct error message when bars verify-personal is an undocumented error response" in
        new BarsFormErrorSetup("otherBarsError", typeOfAccount = TypesOfBankAccount.Personal) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )

          BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(validForm)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        }

      s"[$regime journey] show correct error message when bars verify-business is an undocumented error response" in
        new BarsFormErrorSetup("otherBarsError", typeOfAccount = TypesOfBankAccount.Business) {
          testBankDetailsFormError(controller.enterBankDetailsSubmit)(validFormBusiness*)(
            expectedErrorSummaryContentAndHref,
            expectedFieldErrors
          )
          BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(validFormBusiness)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        }

      s"[$regime journey] go to technical difficulties page when bars verify-personal response has accountExists is ERROR" in
        new BarsErrorSetup(TypesOfBankAccount.Personal) {
          BarsStub.ValidateStub.success()
          BarsStub.VerifyPersonalStub.accountExistsError()

          a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

          BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(formData)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()

          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = toExpectedBarsAuditDetailJson(
              VerifyJson.accountExistsError,
              numberOfBarsVerifyAttempts = 2
            )
          )
        }

      s"[$regime journey] go to technical difficulties page when bars verify-business response has accountExists is ERROR" in
        new BarsErrorSetup(TypesOfBankAccount.Business) {

          BarsStub.ValidateStub.success()
          BarsStub.VerifyBusinessStub.accountExistsError()

          a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

          BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(formData)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = toExpectedBarsAuditDetailJson(
              VerifyJson.accountExistsError,
              numberOfBarsVerifyAttempts = 2
            )
          )
        }

      s"[$regime journey] go to technical difficulties page when bars verify-personal response has nameMatches is Error" in
        new BarsErrorSetup(TypesOfBankAccount.Personal) {

          BarsStub.ValidateStub.success()
          BarsStub.VerifyPersonalStub.nameMatchesError()

          a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

          BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(formData)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = toExpectedBarsAuditDetailJson(
              VerifyJson.nameMatchesError,
              numberOfBarsVerifyAttempts = 2
            )
          )
        }

      s"[$regime journey] go to technical difficulties page when bars verify-business response has nameMatches is Error" in
        new BarsErrorSetup(TypesOfBankAccount.Business) {
          BarsStub.ValidateStub.success()
          BarsStub.VerifyBusinessStub.nameMatchesError()

          a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

          BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(formData)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = toExpectedBarsAuditDetailJson(
              VerifyJson.nameMatchesError,
              numberOfBarsVerifyAttempts = 2
            )
          )
        }

      s"[$regime journey] redirect to the lockout page when update bars verify status response contains an expiry date-time" in
        new BarsErrorSetup(TypesOfBankAccount.Business) {

          private val expiry = Instant.now

          BarsStub.ValidateStub.success()
          BarsStub.VerifyBusinessStub.otherBarsError() // any error will do
          BarsVerifyStatusStub.updateAndLockout(expiry)

          val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.lockoutUrl)

          BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(formData)
          BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
          AuditConnectorStub.verifyEventAudited(
            auditType = "BarsCheck",
            auditEvent = toExpectedBarsAuditDetailJson(
              VerifyJson.otherBarsError,
              numberOfBarsVerifyAttempts = 3,
              barsVerifyLockoutTime = Some(expiry)
            )
          )
        }
    }

    "GET /lockout should" - {

      s"[$regime journey] redirect to the relevant page when the journey has not been locked out" in {
        stubCommonActions(barsLockoutExpiry = None)
        EssttpBackend.CanPayUpfront.findJourney(testCrypto, origin)()

        val result = controller.barsLockout(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.upfrontPaymentAmount.url)

      }

      s"[$regime journey] return 200 when the journey has been locked out" in {
        // expiry time displayed is in UK time - for 30th Sep, 14:59 UTC is 15:59 BST
        val expiry = LocalDateTime
          .of(
            LocalDate.of(2020, 9, 30),
            LocalTime.of(14, 59, 46)
          )
          .toInstant(ZoneOffset.UTC)

        stubCommonActions(barsLockoutExpiry = Some(expiry))
        EssttpBackend.DetermineTaxId.findJourney(origin)(JourneyJsonTemplates.`Computed Tax Id`(origin))

        val result: Future[Result] = controller.barsLockout(fakeRequest)
        status(result) shouldBe Status.OK

        val pageContent: String = contentAsString(result)
        val doc: Document       = Jsoup.parse(pageContent)

        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = BarsLockoutPage.expectedH1,
          shouldBackLinkBePresent = false,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        val paragraphs = doc.select("p.govuk-body").asScala.toList
        paragraphs(0)
          .text() shouldBe s"You’ll need to wait until 30 September 2020, 3:59pm before trying to confirm your bank details again."
        paragraphs(1).text() shouldBe "You may still be able to set up a payment plan over the phone."
        paragraphs(2).text() shouldBe "For further support you can contact us on 0300 123 1813 to speak to an adviser."

        doc.select("h2").asScala.toList(0).text() shouldBe "If you need extra support"
        paragraphs(3)
          .html() shouldBe "Find out the different ways to <a href=\"https://www.gov.uk/get-help-hmrc-extra-support\" class=\"govuk-link\">deal with HMRC if you need some help</a>."
        paragraphs(4)
          .html() shouldBe "You can also use <a href=\"https://www.relayuk.bt.com/\" class=\"govuk-link\">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."
        paragraphs(5).html() shouldBe "If you are outside the UK: <strong>+44 2890 538 192</strong>"
      }
    }

    "GET /check-your-direct-debit-details should" - {
      s"[$regime journey] return 200 and the check your direct debit details page" in {
        stubCommonActions()
        EssttpBackend.DirectDebitDetails.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.checkBankDetails(fakeRequest)

        RequestAssertions.assertGetRequestOk(result)

        val pageContent: String = contentAsString(result)
        val doc: Document       = Jsoup.parse(pageContent)

        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = ConfirmDirectDebitDetailsPage.expectedH1,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.BankDetailsController.checkBankDetailsSubmit.url),
          regimeBeingTested = Some(taxRegime)
        )

        val summaries = doc.select(".govuk-summary-list").select(".govuk-summary-list__row").iterator().asScala.toList
        summaries.size shouldBe 4

        val expectedAccountTypeRow   = SummaryRow("Account type", TdAll.typeOfBankAccount("Personal").toString)
        val expectedAccountNameRow   = SummaryRow("Name on the account", TdAll.testAccountName)
        val expectedSortCodeRow      = SummaryRow("Sort code", "123456")
        val expectedAccountNumberRow = SummaryRow("Account number", "12345678")
        val expectedSummaryRows      =
          List(expectedAccountTypeRow, expectedAccountNameRow, expectedSortCodeRow, expectedAccountNumberRow)
        extractSummaryRows(summaries) shouldBe expectedSummaryRows

        val cardTitle = doc.select(".govuk-card__header__text").text()
        cardTitle shouldBe "Bank account details"

        val cardTitleLink = doc.select(".govuk-summary-card__actions").select(".govuk-link")
        cardTitleLink.attr("href") shouldBe "/set-up-a-payment-plan/check-you-can-set-up-a-direct-debit"
        cardTitleLink.textNodes().get(0).text() shouldBe "Change your Direct Debit details"

        doc.select(".govuk-heading-m").text() shouldBe "The Direct Debit Guarantee"
        val directDebitGuaranteeParagraphs = doc.select(".govuk-body").asScala.toList
        directDebitGuaranteeParagraphs(0)
          .text() shouldBe "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits."
        directDebitGuaranteeParagraphs(1)
          .text() shouldBe "If there are any changes to the amount, date or frequency of your Direct Debit HMRC NDDS will notify you 10 working days in advance of your account being debited or as otherwise agreed. If you request HMRC NDDS to collect a payment, confirmation of the amount and date will be given to you at the time of the request."
        directDebitGuaranteeParagraphs(2)
          .text() shouldBe "If an error is made in the payment of your Direct Debit by HMRC NDDS or your bank or building society you are entitled to a full and immediate refund of the amount paid from your bank or building society. If you receive a refund you are not entitled to, you must pay it back when HMRC NDDS asks you to."
        directDebitGuaranteeParagraphs(3)
          .text() shouldBe "You can cancel a Direct Debit at any time by simply contacting your bank or building society. Written confirmation may be required. Please also notify us."
      }
    }

    "POST /check-your-direct-debit-details should" - {
      s"[$regime journey] redirect the user to terms and conditions and update backend" in {
        stubCommonActions()
        EssttpBackend.DirectDebitDetails.findJourney(testCrypto, origin)()
        EssttpBackend.ConfirmedDirectDebitDetails.stubUpdateConfirmDirectDebitDetails(
          TdAll.journeyId,
          JourneyJsonTemplates.`Confirmed Direct Debit Details`(origin)
        )

        val result: Future[Result] = controller.checkBankDetailsSubmit(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.termsAndConditionsUrl)
        EssttpBackend.ConfirmedDirectDebitDetails.verifyUpdateConfirmDirectDebitDetailsRequest(TdAll.journeyId)
      }
    }

    "GET /you-cannot-set-up-a-direct-debit-online should" - {
      s"[$regime journey] return 200 and Call us about a payment plan page" in {
        stubCommonActions()
        EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, origin)(
          JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = false, origin)
        )

        val result: Future[Result] = controller.cannotSetupDirectDebitOnlinePage(fakeRequest)
        RequestAssertions.assertGetRequestOk(result)

        val pageContent: String = contentAsString(result)
        val doc: Document       = Jsoup.parse(pageContent)

        ContentAssertions.commonPageChecks(
          doc,
          expectedH1 = CannotSetupDirectDebitPage.expectedH1,
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = None,
          regimeBeingTested = Some(taxRegime)
        )

        val paragraphs = doc.select(".govuk-body").asScala.toList
        paragraphs.size shouldBe 6

        paragraphs(0).text() shouldBe CannotSetupDirectDebitPage.paragraphContent1(taxRegime)
        paragraphs(1).text() shouldBe CannotSetupDirectDebitPage.paragraphContent2

        doc.select("h2").asScala.toList(0).text() shouldBe "If you need extra support"
        paragraphs(2)
          .html() shouldBe "Find out the different ways to <a href=\"https://www.gov.uk/get-help-hmrc-extra-support\" class=\"govuk-link\">deal with HMRC if you need some help</a>."
        paragraphs(3)
          .html() shouldBe "You can also use <a href=\"https://www.relayuk.bt.com/\" class=\"govuk-link\">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>."

        doc.select("h2").asScala.toList(1).text() shouldBe "If you’re calling from outside the UK"
        paragraphs(4).html() shouldBe "Call us on <strong>+44 2890 538 192</strong>."
        paragraphs(5)
          .html() shouldBe "Our opening times are Monday to Friday, 8am to 6pm (UK time). We are closed on weekends and bank holidays."
      }
    }

  }
}

object BankDetailsControllerSpec {

  final case class SummaryRow(question: String, answer: String) derives CanEqual

}
