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
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.bank.{DetailsAboutBankAccount, TypeOfBankAccount, TypesOfBankAccount}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend.BarsVerifyStatusStub
import testsupport.stubs.{AuditConnectorStub, BarsStub, EssttpBackend}
import testsupport.testdata.BarsJsonResponses.{ValidateJson, VerifyJson}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.Locale
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{asScalaIteratorConverter, collectionAsScalaIterableConverter}

class BankDetailsControllerSpec extends ItSpec {

  private val controller: BankDetailsController = app.injector.instanceOf[BankDetailsController]

  object DetailsAboutBankAccountPage {
    val expectedH1: String = "About your bank account"

    val typeOfAccountHeading: String = "What type of account details are you providing?"
    val radioButtonContentBusiness: String = "Business bank account"
    val radioButtonContentPersonal: String = "Personal bank account"

    val accountHolderHeading: String = "Are you the account holder?"
    val accountHolderHintContent: String =
      "You must be the sole account holder, or for multi-signature accounts you must have authority to set up a Direct Debit without additional signatures."
    val accountHolderRadioId: String = "#isSoleSignatory"

    val buttonContent: String = "Continue"

  }

  object EnterDirectDebitDetailsPage {
    val expectedH1: String = "Set up Direct Debit"
    val accountNameContent: String = "Name on the account"
    val accountNameFieldId: String = "#name"
    val sortCodeContent: String = "Sort code"
    val sortCodeHintContent: String = "Must be 6 digits long"
    val sortCodeFieldId: String = "#sortCode"
    val accountNumberContent: String = "Account number"
    val accountNumberHintContent: String = "Must be between 6 and 8 digits long"
    val accountNumberFieldId: String = "#accountNumber"

  }

  object ConfirmDirectDebitDetailsPage {
    val expectedH1: String = "Check your Direct Debit details"
  }

  object CannotSetupDirectDebitPage {
    val expectedH1: String = "You cannot set up a Direct Debit online"
    val paragraphContent1: String =
      "You need a named account holder or someone with authorisation to set up a Direct Debit."
    val paragraphContent2: String =
      "If you are not the account holder or you wish to set up a Direct Debit with a multi-signature account, we recommend " +
        "you speak to an adviser on 0300 200 3835 at the Payment Support Service. You must ensure all account holders are " +
        "present when calling."
    val buttonContent: String = "Go to tax account"
  }

  object BarsLockoutPage {
    val expectedH1: String = "You’ve tried to confirm your bank details too many times"
  }

  private def getExpectedFormValue(field: String, formData: Seq[(String, String)]): String =
    formData.collectFirst { case (x, value) if x == field => value }.getOrElse("")

  def testFormError(action: Action[AnyContent])(formData: (String, String)*)(
      textAndHrefContent: List[(String, String)],
      additionalChecks:   Document => Unit
  ): Unit = {
    val fakeRequest = FakeRequest()
      .withMethod("POST")
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(formData: _*)

    val result: Future[Result] = action(fakeRequest)

    RequestAssertions.assertGetRequestOk(result)

    val pageContent: String = contentAsString(result)
    val doc: Document = Jsoup.parse(pageContent)

    val errorSummary = doc.select(".govuk-error-summary__list")
    val errorLinks = errorSummary.select("a").asScala.toList
    errorLinks.zip(textAndHrefContent).foreach { testData: (Element, (String, String)) =>
      testData._1.text() shouldBe testData._2._1
      testData._1.attr("href") shouldBe testData._2._2
    }

    additionalChecks(doc)
  }

  def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map { e =>
    SummaryRow(
      e.select(".govuk-summary-list__key").text(),
      e.select(".govuk-summary-list__value").text(),
      e.select(".govuk-summary-list__actions > .govuk-link").attr("href")
    )
  }
  Seq[(String, Origin, TaxRegime)](
    ("EPAYE", Origins.Epaye.Bta, TaxRegime.Epaye),
    ("VAT", Origins.Vat.Bta, TaxRegime.Vat)
  ).foreach {
      case (regime, origin, taxRegime) =>

        "GET /about-your-bank-account should" - {

          s"[$regime journey] return 200 and display the 'about your bank account' page" in {
            stubCommonActions()
            EssttpBackend.HasCheckedPlan.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val result: Future[Result] = controller.detailsAboutBankAccount(fakeRequest)
            val pageContent: String = contentAsString(result)
            val doc: Document = Jsoup.parse(pageContent)

            RequestAssertions.assertGetRequestOk(result)
            ContentAssertions.commonPageChecks(
              doc,
              expectedH1              = DetailsAboutBankAccountPage.expectedH1,
              shouldBackLinkBePresent = true,
              expectedSubmitUrl       = Some(routes.BankDetailsController.detailsAboutBankAccountSubmit.url),
              regimeBeingTested       = Some(taxRegime)
            )

            val formGroups = doc.select(".govuk-form-group").asScala.toList
            formGroups.size shouldBe 2

            val typeOfAccountFormGroup = formGroups(0)
            typeOfAccountFormGroup.select(".govuk-fieldset__legend").text() shouldBe DetailsAboutBankAccountPage.typeOfAccountHeading

            val typeOfAccountRadioContent = typeOfAccountFormGroup.select(".govuk-radios__label").asScala.toList
            typeOfAccountRadioContent.size shouldBe 2
            typeOfAccountRadioContent(0).text() shouldBe DetailsAboutBankAccountPage.radioButtonContentBusiness
            typeOfAccountRadioContent(1).text() shouldBe DetailsAboutBankAccountPage.radioButtonContentPersonal

            val accountHolderFormGroup = formGroups(1)
            accountHolderFormGroup.select(".govuk-fieldset__legend").text() shouldBe DetailsAboutBankAccountPage.accountHolderHeading

            val accountHolderRadioContent = accountHolderFormGroup.select(".govuk-radios__label").asScala.toList
            accountHolderRadioContent.size shouldBe 2
            accountHolderRadioContent(0).text() shouldBe "Yes"
            accountHolderRadioContent(1).text() shouldBe "No"

            doc.select(".govuk-button").text() shouldBe DetailsAboutBankAccountPage.buttonContent
          }

          Seq(
            ("Business", true, JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = true, origin), 0, 0),
            ("Business", false, JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = false, origin), 0, 1),
            ("Personal", true, JourneyJsonTemplates.`Entered Details About Bank Account - Personal`(isAccountHolder = true, origin), 1, 0),
            ("Personal", false, JourneyJsonTemplates.`Entered Details About Bank Account - Personal`(isAccountHolder = false, origin), 1, 1)
          ).foreach {
              case (typeOfAccount, isAccountHolder, wiremockJson, accountTypeCheckedElementIndex, isAccountHolderCheckedElementIndex) =>
                s"[$regime journey] prepopulate the form when the user has a chosen $typeOfAccount bank account type and " +
                  s"isAccountHolder=$isAccountHolder in their journey" in {
                    stubCommonActions()
                    EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto, origin)(wiremockJson)

                    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

                    val result: Future[Result] = controller.detailsAboutBankAccount(fakeRequest)
                    val pageContent: String = contentAsString(result)
                    val doc: Document = Jsoup.parse(pageContent)

                    RequestAssertions.assertGetRequestOk(result)

                    val formGroups = doc.select(".govuk-form-group").asScala.toList
                    formGroups.size shouldBe 2

                    formGroups(0).select(".govuk-radios__input").asScala.toList(accountTypeCheckedElementIndex).hasAttr("checked") shouldBe true
                    formGroups(1).select(".govuk-radios__input").asScala.toList(isAccountHolderCheckedElementIndex).hasAttr("checked") shouldBe true
                  }
            }
        }

        "POST /about-your-bank-account should" - {

            def testRedirect(
                formBody: (String, String)*
            )(expectedRedirectUrl: String, expectedDetailsAboutBankAccount: DetailsAboutBankAccount) = {
              val updatedJourneyJson =
                expectedDetailsAboutBankAccount.typeOfBankAccount match {
                  case TypesOfBankAccount.Personal =>
                    JourneyJsonTemplates.`Entered Details About Bank Account - Personal`(expectedDetailsAboutBankAccount.isAccountHolder, origin)
                  case TypesOfBankAccount.Business =>
                    JourneyJsonTemplates.`Entered Details About Bank Account - Business`(expectedDetailsAboutBankAccount.isAccountHolder, origin)
                }

              stubCommonActions()
              EssttpBackend.HasCheckedPlan.findJourney(testCrypto, origin)()
              EssttpBackend.EnteredDetailsAboutBankAccount.stubUpdateEnteredDetailsAboutBankAccount(TdAll.journeyId, updatedJourneyJson)

              val fakeRequest = FakeRequest(
                method = "POST",
                path   = "/what-type-of-account-details-are-you-providing"
              ).withAuthToken()
                .withSession(SessionKeys.sessionId -> "IamATestSessionId")
                .withFormUrlEncodedBody(formBody: _*)

              val result: Future[Result] = controller.detailsAboutBankAccountSubmit(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
              EssttpBackend.EnteredDetailsAboutBankAccount.verifyUpdateEnteredDetailsAboutBankAccountRequest(TdAll.journeyId, expectedDetailsAboutBankAccount)

            }

          Seq("Business", "Personal").foreach { typeOfAccount =>
            s"[$regime journey] redirect to /set-up-direct-debit when valid form is submitted and the user is an account holder - $typeOfAccount" in {
              testRedirect(
                ("typeOfAccount", typeOfAccount),
                ("isSoleSignatory", "Yes")
              )(PageUrls.directDebitDetailsUrl, TdAll.detailsAboutBankAccount(typeOfAccount, isAccountHolder = true))
            }
          }

          Seq("Business", "Personal").foreach { typeOfAccount =>
            s"[$regime journey] redirect to /set-up-direct-debit when valid form is submitted and the user is not an account holder - $typeOfAccount" in {
              testRedirect(
                ("typeOfAccount", typeOfAccount),
                ("isSoleSignatory", "No")
              )(PageUrls.cannotSetupDirectDebitOnlineUrl, TdAll.detailsAboutBankAccount(typeOfAccount, isAccountHolder = false))
            }
          }

          Seq(
            (
              List(
                ("typeOfAccount", ""),
                ("isSoleSignatory", "Yes")
              ),
                List(
                  ("Select what type of account details you are providing", "#typeOfAccount")
                ),
                  Some("isSoleSignatory")
            ),
            (
              List(
                ("typeOfAccount", "Personal"),
                ("isSoleSignatory", "")
              ),
                List(
                  ("Select yes if you are the account holder", "#isSoleSignatory")
                ),
                  Option("typeOfAccount-2")
            ),
            (
              List(
                ("typeOfAccount", ""),
                ("isSoleSignatory", "")
              ), List(
                  ("Select what type of account details you are providing", "#typeOfAccount"),
                  ("Select yes if you are the account holder", "#isSoleSignatory")
                ),
                  None
            )
          ).foreach {
              case (formData, expectedErrors, populatedRadioId) =>
                s"[$regime journey] show correct error messages when ${formData.filter(_._2.isEmpty).map(_._1).mkString("&")} is empty" in {
                  stubCommonActions()
                  EssttpBackend.HasCheckedPlan.findJourney(testCrypto, origin)()

                  testFormError(controller.detailsAboutBankAccountSubmit)(formData: _*)(expectedErrors, { doc =>
                    val radioInputs = doc.select(".govuk-radios__input")

                    populatedRadioId match {
                      case Some(id) =>
                        radioInputs.select(s"#$id").hasAttr("checked") shouldBe true
                        radioInputs.select(s":not(#$id)").asScala.toList.foreach(_.hasAttr("checked") shouldBe false)

                      case None =>
                        radioInputs.asScala.toList.foreach(_.hasAttr("checked") shouldBe false)
                    }
                  })
                  EssttpBackend.EnteredDetailsAboutBankAccount.verifyNoneUpdateEnteredDetailsAboutBankAccountRequest(TdAll.journeyId)
                }
            }

        }
    }

  "GET /set-up-direct-debit should" - {

    "should return 200 and the bank details page" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.enterBankDetails(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = EnterDirectDebitDetailsPage.expectedH1,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.BankDetailsController.enterBankDetailsSubmit.url)
      )

      val nameInput = doc.select("input[name=name]")
      val sortCodeInput = doc.select("input[name=sortCode]")
      val accountNumberInput = doc.select("input[name=accountNumber]")

      nameInput.attr("autocomplete") shouldBe "name"
      nameInput.attr("spellcheck") shouldBe "false"

      sortCodeInput.attr("autocomplete") shouldBe "off"
      sortCodeInput.attr("inputmode") shouldBe "numeric"
      sortCodeInput.attr("spellcheck") shouldBe "false"

      accountNumberInput.attr("autocomplete") shouldBe "off"
      accountNumberInput.attr("inputmode") shouldBe "numeric"
      accountNumberInput.attr("spellcheck") shouldBe "false"

      val subheadings = doc.select(".govuk-label--m").asScala.toList
      subheadings.size shouldBe 3
      subheadings(0).text() shouldBe EnterDirectDebitDetailsPage.accountNameContent
      subheadings(1).text() shouldBe EnterDirectDebitDetailsPage.sortCodeContent
      subheadings(2).text() shouldBe EnterDirectDebitDetailsPage.accountNumberContent

      doc.select("#sortCode-hint").text() shouldBe EnterDirectDebitDetailsPage.sortCodeHintContent
      doc.select("#accountNumber-hint").text() shouldBe EnterDirectDebitDetailsPage.accountNumberHintContent
    }

    "prepopulate the form when the user has the direct debit details in their journey" in {
      stubCommonActions()
      EssttpBackend.DirectDebitDetails.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.enterBankDetails(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = EnterDirectDebitDetailsPage.expectedH1,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.BankDetailsController.enterBankDetailsSubmit.url)
      )
      RequestAssertions.assertGetRequestOk(result)

      doc.select(EnterDirectDebitDetailsPage.accountNameFieldId).`val`() shouldBe "Bob Ross"
      doc.select(EnterDirectDebitDetailsPage.sortCodeFieldId).`val`() shouldBe "123456"
      doc.select(EnterDirectDebitDetailsPage.accountNumberFieldId).`val`() shouldBe "12345678"
    }

    "redirect to the 'cannot set up direct debit' page if the user has said they are not an account holder" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)(
        JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = false)
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.enterBankDetails(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage.url)
    }

  }

  trait SubmitSuccessSetup {

    stubCommonActions()

    val formData = List(
      ("name", "Bob Ross"),
      ("sortCode", "123456"),
      ("accountNumber", "12345678")
    )

    val fakeRequest = FakeRequest(
      method = "POST",
      path   = "/set-up-direct-debit"
    ).withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")
      .withFormUrlEncodedBody(formData: _*)
  }

  "POST /set-up-direct-debit should" - {

      def testBankDetailsFormError(
          action: Action[AnyContent]
      )(formData: (String, String)*)(
          textAndHrefContent: List[(String, String)],
          fieldErrors:        Seq[(String, String)]  = Seq.empty
      ) = {
          def assertFieldsPopulated(doc: Document, form: Seq[(String, String)], fieldErrors: Seq[(String, String)]): Unit = {
            doc.select(EnterDirectDebitDetailsPage.accountNameFieldId).`val`() shouldBe getExpectedFormValue("name", form)
            doc.select(EnterDirectDebitDetailsPage.sortCodeFieldId).`val`() shouldBe getExpectedFormValue("sortCode", form)
            doc.select(EnterDirectDebitDetailsPage.accountNumberFieldId).`val`() shouldBe getExpectedFormValue("accountNumber", form)

            fieldErrors.foreach {
              case (field, errorMessage) =>
                doc.getElementById(s"$field-error").text.trim shouldBe s"Error: $errorMessage"
            }
          }

        testFormError(action)(formData: _*)(textAndHrefContent, assertFieldsPopulated(_, formData, fieldErrors))
      }

    "redirect to the 'cannot set up direct debit' page if the user has said they are not an account holder" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)(
        JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = false)
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId").withMethod("POST")

      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage.url)
    }

    "redirect to /check-bank-details when valid form is submitted" in new SubmitSuccessSetup {
      BarsStub.ValidateStub.success()
      BarsStub.VerifyPersonalStub.success()
      BarsVerifyStatusStub.update()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)(
        JourneyJsonTemplates.`Entered Details About Bank Account - Personal`(isAccountHolder = true)
      )
      EssttpBackend.DirectDebitDetails.stubUpdateDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Entered Direct Debit Details`)

      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)

      EssttpBackend.DirectDebitDetails.verifyUpdateDirectDebitDetailsRequest(
        TdAll.journeyId,
        TdAll.directDebitDetails("Bob Ross", "123456", "12345678")
      )(testOperationCryptoFormat)

      BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(formData)
      BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled() // don't update verify count on BARs success

      AuditConnectorStub.verifyEventAudited(
        auditType  = "BarsCheck",
        auditEvent = Json.parse(
          s"""
             |{
             |  "taxDetail": {
             |    "accountsOfficeRef": "123PA44545546",
             |    "employerRef": "864FZ00049"
             |  },
             |  "taxType": "Epaye",
             |  "origin": "Bta",
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
        ).as[JsObject]
      )
    }

    "redirect to /check-bank-details and do not call BARs or update backend when the same form is resubmitted" in new SubmitSuccessSetup {
      EssttpBackend.DirectDebitDetails.findJourney(testCrypto)(JourneyJsonTemplates.`Entered Direct Debit Details`)

      val result: Future[Result] = controller.enterBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.checkDirectDebitDetailsUrl)

      BarsStub.verifyBarsNotCalled()
      BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled()

      AuditConnectorStub.verifyNoAuditEvent()
    }

    "show correct error messages when form submitted is empty" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)()

      val formData: List[(String, String)] = List(
        ("name", ""),
        ("sortCode", ""),
        ("accountNumber", "")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Enter the name on the account", EnterDirectDebitDetailsPage.accountNameFieldId),
        ("Enter sort code", EnterDirectDebitDetailsPage.sortCodeFieldId),
        ("Enter account number", EnterDirectDebitDetailsPage.accountNumberFieldId)
      )

      testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
      AuditConnectorStub.verifyNoAuditEvent()
    }

    "show correct error messages when submitted sort code and account number are not numeric" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)()

      val formData: List[(String, String)] = List(
        ("name", "Bob Ross"),
        ("sortCode", "12E456"),
        ("accountNumber", "12E45678")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Sort code must be a number", EnterDirectDebitDetailsPage.sortCodeFieldId),
        ("Account number must be a number", EnterDirectDebitDetailsPage.accountNumberFieldId)
      )

      testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
      AuditConnectorStub.verifyNoAuditEvent()
    }

    "show correct error message when account name is more than 70 characters" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)()

      val formData: List[(String, String)] = List(
        ("name", "a" * 71),
        ("sortCode", "123456"),
        ("accountNumber", "12345678")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Name on the account must be 70 characters or less", EnterDirectDebitDetailsPage.accountNameFieldId)
      )

      testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
      AuditConnectorStub.verifyNoAuditEvent()
    }

    "show correct error messages when submitted sort code and account number are more than 6 and 8 digits respectively" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)()

      val formData: List[(String, String)] = List(
        ("name", "Bob Ross"),
        ("sortCode", "1234567"),
        ("accountNumber", "123456789")
      )
      val expectedContentAndHref: List[(String, String)] = List(
        ("Sort code must be 6 digits", EnterDirectDebitDetailsPage.sortCodeFieldId),
        ("Account number must be between 6 and 8 digits", EnterDirectDebitDetailsPage.accountNumberFieldId)
      )

      testBankDetailsFormError(controller.enterBankDetailsSubmit)(formData: _*)(expectedContentAndHref)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)
      AuditConnectorStub.verifyNoAuditEvent()
    }

    abstract class BarsErrorSetup(typeOfAccount: TypeOfBankAccount) {
      stubCommonActions()
      BarsVerifyStatusStub.update(numberOfAttempts = 2)
      EssttpBackend.DirectDebitDetails.verifyNoneUpdateDirectDebitDetailsRequest(TdAll.journeyId)

      val formData: List[(String, String)] = List(
        ("name", "Bob Ross"),
        ("sortCode", "123456"),
        ("accountNumber", "12345678")
      )

      def toExpectedBarsAuditDetailJson(
          barsResponseJson:           String,
          isBankAccountValid:         Boolean         = false,
          numberOfBarsVerifyAttempts: Int             = 1,
          barsVerifyLockoutTime:      Option[Instant] = None
      ): JsObject = {
        val barsVerifyJsonString =
          s"""
            |"barsVerify": {
            |  "unsuccessfulAttempts": $numberOfBarsVerifyAttempts${
            barsVerifyLockoutTime.fold("")(t => s""","lockoutExpiryDateTime": "${t.toString}"""")
          }
            |}
            |""".stripMargin

        Json.parse(
          s"""
             |{
             |  "taxDetail": {
             |    "accountsOfficeRef": "123PA44545546",
             |    "employerRef": "864FZ00049"
             |  },
             |  "taxType": "Epaye",
             |  "origin": "Bta",
             |  "request": {
             |    "account": {
             |       "accountType": "${typeOfAccount.entryName.toLowerCase(Locale.UK)}",
             |       "accountHolderName": "Bob Ross",
             |       "sortCode": "123456",
             |       "accountNumber": "12345678"
             |    }
             |  },
             |  "response": {
             |   "isBankAccountValid": $isBankAccountValid,
             |   "barsResponse":  $barsResponseJson
             |  },
             |  $barsVerifyJsonString,
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059"
             |}
            """.stripMargin
        ).as[JsObject]
      }

      val fakeRequest = FakeRequest(
        method = "POST",
        path   = "/set-up-direct-debit"
      ).withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withFormUrlEncodedBody(formData: _*)

      EssttpBackend.DirectDebitDetails.stubUpdateDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Entered Direct Debit Details`)

      typeOfAccount match {
        case TypesOfBankAccount.Personal =>
          EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)(
            JourneyJsonTemplates.`Entered Details About Bank Account - Personal`(isAccountHolder = true)
          )
        case TypesOfBankAccount.Business =>
          EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)(
            JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = true)
          )
      }
    }

    abstract class BarsFormErrorSetup(barsError: String, typeOfAccount: TypeOfBankAccount)
      extends BarsErrorSetup(typeOfAccount) {

      val validForm: List[(String, String)] = formData

      private val sortCodeAndAccountNumberFieldError: List[(String, String)] =
        List("sort-code-and-account-number" -> "Enter a valid combination of bank account number and sort code")

      private val nameFieldError: List[(String, String)] = List("name" -> "Enter the name on the account as it appears on bank statements. Do not copy and paste it.")

      private val sortCodeFieldError: List[(String, String)] = List((
        "sortCode",
        "You have entered a sort code which does not accept this type of payment. " +
        "Check you have entered a valid sort code or enter details for a different account"
      ))

      val (expectedErrorSummaryContentAndHref, expectedFieldErrors, expectedAuditResponseJson): (List[(String, String)], Seq[(String, String)], String) =
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
              List(("Enter the name on the account as it appears on bank statements. Do not copy and paste it.", "#name")),
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
          numberOfBarsVerifyAttempts: Int             = 1,
          barsVerifyLockoutTime:      Option[Instant] = None
      ): JsObject =
        toExpectedBarsAuditDetailJson(
          expectedAuditResponseJson,
          numberOfBarsVerifyAttempts = numberOfBarsVerifyAttempts,
          barsVerifyLockoutTime      = barsVerifyLockoutTime
        )
    }

    "show correct error message when BARs validate response is accountNumberNotWellFormatted" in
      new BarsFormErrorSetup("accountNumberNotWellFormatted", TypesOfBankAccount.Personal) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
        BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = expectedBarsAuditDetailJson()
        )
      }

    "show correct error message when BARs validate response is sortCodeNotPresentOnEiscd" in
      new BarsFormErrorSetup("sortCodeNotPresentOnEiscd", TypesOfBankAccount.Personal) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
        BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = expectedBarsAuditDetailJson()
        )
      }

    "show correct error message when BARs validate response is sortCodeDoesNotSupportsDirectDebit" in
      new BarsFormErrorSetup("sortCodeDoesNotSupportsDirectDebit", TypesOfBankAccount.Personal) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
        BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = expectedBarsAuditDetailJson()
        )
      }

    "show correct error message when BARs verify response is nameDoesNotMatch with a personal bank account" in
      new BarsFormErrorSetup("nameDoesNotMatch", typeOfAccount = TypesOfBankAccount.Personal) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(validForm)
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = expectedBarsAuditDetailJson(numberOfBarsVerifyAttempts = 2)
        )
      }

    "show correct error message when BARs verify response is nameDoesNotMatch with a business bank account" in
      new BarsFormErrorSetup("nameDoesNotMatch", typeOfAccount = TypesOfBankAccount.Business) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(validForm)
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = expectedBarsAuditDetailJson(numberOfBarsVerifyAttempts = 2)
        )
      }

    "show correct error message when bars verify-personal responds with accountExists is No" in
      new BarsFormErrorSetup("accountDoesNotExist", typeOfAccount = TypesOfBankAccount.Personal) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(validForm)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
      }

    "show correct error message when bars verify-business responds with accountExists is No" in
      new BarsFormErrorSetup("accountDoesNotExist", typeOfAccount = TypesOfBankAccount.Business) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(validForm)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
      }

    "show correct error message when bars validate response is 400 sortCodeOnDenyList" in
      new BarsFormErrorSetup("sortCodeOnDenyList", typeOfAccount = TypesOfBankAccount.Business) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.ValidateStub.ensureBarsValidateCalled(validForm)
        BarsStub.VerifyStub.ensureBarsVerifyNotCalled()
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsNotCalled()
      }

    "show correct error message when bars verify-personal is an undocumented error response" in
      new BarsFormErrorSetup("otherBarsError", typeOfAccount = TypesOfBankAccount.Personal) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(validForm)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
      }

    "show correct error message when bars verify-business is an undocumented error response" in
      new BarsFormErrorSetup("otherBarsError", typeOfAccount = TypesOfBankAccount.Business) {
        testBankDetailsFormError(controller.enterBankDetailsSubmit)(validForm: _*)(expectedErrorSummaryContentAndHref, expectedFieldErrors)
        BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(validForm)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
      }

    "go to technical difficulties page when bars verify-personal response has accountExists is ERROR" in
      new BarsErrorSetup(TypesOfBankAccount.Personal) {
        BarsStub.ValidateStub.success()
        BarsStub.VerifyPersonalStub.accountExistsError()

        a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(formData)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()

        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = toExpectedBarsAuditDetailJson(
            VerifyJson.accountExistsError,
            numberOfBarsVerifyAttempts = 2
          )
        )
      }

    "go to technical difficulties page when bars verify-business response has accountExists is ERROR" in
      new BarsErrorSetup(TypesOfBankAccount.Business) {

        BarsStub.ValidateStub.success()
        BarsStub.VerifyBusinessStub.accountExistsError()

        a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

        BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(formData)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = toExpectedBarsAuditDetailJson(
            VerifyJson.accountExistsError,
            numberOfBarsVerifyAttempts = 2
          )
        )
      }

    "go to technical difficulties page when bars verify-personal response has nameMatches is Error" in
      new BarsErrorSetup(TypesOfBankAccount.Personal) {

        BarsStub.ValidateStub.success()
        BarsStub.VerifyPersonalStub.nameMatchesError()

        a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

        BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalCalled(formData)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = toExpectedBarsAuditDetailJson(
            VerifyJson.nameMatchesError,
            numberOfBarsVerifyAttempts = 2
          )
        )
      }

    "go to technical difficulties page when bars verify-business response has nameMatches is Error" in
      new BarsErrorSetup(TypesOfBankAccount.Business) {
        BarsStub.ValidateStub.success()
        BarsStub.VerifyBusinessStub.nameMatchesError()

        a[RuntimeException] shouldBe thrownBy(await(controller.enterBankDetailsSubmit(fakeRequest)))

        BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessCalled(formData)
        BarsVerifyStatusStub.ensureVerifyUpdateStatusIsCalled()
        AuditConnectorStub.verifyEventAudited(
          auditType  = "BarsCheck",
          auditEvent = toExpectedBarsAuditDetailJson(
            VerifyJson.nameMatchesError,
            numberOfBarsVerifyAttempts = 2
          )
        )
      }

    "redirect to the lockout page when update bars verify status response contains an expiry date-time" in
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
          auditType  = "BarsCheck",
          auditEvent = toExpectedBarsAuditDetailJson(
            VerifyJson.otherBarsError,
            numberOfBarsVerifyAttempts = 3,
            barsVerifyLockoutTime      = Some(expiry)
          )
        )
      }
  }

  "GET /lockout should" - {

    "redirect to the relevant page when the journey has not been locked out" in {
      stubCommonActions(barsLockoutExpiry = None)
      EssttpBackend.CanPayUpfront.findJourney(testCrypto)()

      val request = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.barsLockout(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.upfrontPaymentAmount.url)

    }

    "return 200 when the journey has been locked ou" in {
      // expiry time displayed is in UK time - for 30th Sep, 14:59 UTC is 15:59 BST
      val expiry = LocalDateTime.of(
        LocalDate.of(2020, 9, 30),
        LocalTime.of(14, 59, 46)
      ).toInstant(ZoneOffset.UTC)

      stubCommonActions(barsLockoutExpiry = Some(expiry))
      EssttpBackend.DetermineTaxId.findJourney()

      val request = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.barsLockout(request)
      status(result) shouldBe Status.OK

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = BarsLockoutPage.expectedH1,
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None
      )

      val paragraphs = doc.select("p.govuk-body").asScala.toList
      paragraphs(0).text() shouldBe s"You’ll need to wait until 30 September 2020, 3:59pm before trying to confirm your bank details again."
      paragraphs(1).text() shouldBe "You may still be able to set up a payment plan over the phone."
      paragraphs(2).text() shouldBe "For further support you can contact the Payment Support Service on 0300 200 3835 to speak to an adviser."
    }
  }

  "GET /check-your-direct-debit-details should" - {
    "return 200 and the check your direct debit details page" in {
      stubCommonActions()
      EssttpBackend.DirectDebitDetails.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.checkBankDetails(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = ConfirmDirectDebitDetailsPage.expectedH1,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.BankDetailsController.checkBankDetailsSubmit.url)
      )

      val summaries = doc.select(".govuk-summary-list").select(".govuk-summary-list__row").iterator().asScala.toList
      summaries.size shouldBe 3
      val changeLinks = summaries.map(_.select(".govuk-summary-list__actions").select(".govuk-link"))
      changeLinks.size shouldBe 3
      changeLinks.foreach(_.attr("href") shouldBe routes.BankDetailsController.enterBankDetails.url)

      val expectedAccountNameRow =
        SummaryRow("Account name", "Bob Ross", routes.BankDetailsController.enterBankDetails.url)
      val expectedSortCodeRow = SummaryRow("Sort code", "123456", routes.BankDetailsController.enterBankDetails.url)
      val expectedAccountNumberRow =
        SummaryRow("Account number", "12345678", routes.BankDetailsController.enterBankDetails.url)
      val expectedSummaryRows = List(expectedAccountNameRow, expectedSortCodeRow, expectedAccountNumberRow)
      extractSummaryRows(summaries) shouldBe expectedSummaryRows

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
    "redirect the user to terms and conditions and update backend" in {
      stubCommonActions()
      EssttpBackend.DirectDebitDetails.findJourney(testCrypto)()
      EssttpBackend.ConfirmedDirectDebitDetails.stubUpdateConfirmDirectDebitDetails(TdAll.journeyId, JourneyJsonTemplates.`Confirmed Direct Debit Details`)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.checkBankDetailsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.termsAndConditionsUrl)
      EssttpBackend.ConfirmedDirectDebitDetails.verifyUpdateConfirmDirectDebitDetailsRequest(TdAll.journeyId)
    }
  }

  "GET /you-cannot-set-up-a-direct-debit-online should" - {
    "return 200 and You cannot set up a direct debit online page" in {
      stubCommonActions()
      EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto)(
        JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = false)
      )
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.cannotSetupDirectDebitOnlinePage(fakeRequest)
      RequestAssertions.assertGetRequestOk(result)

      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = CannotSetupDirectDebitPage.expectedH1,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = None
      )

      val paragraphs = doc.select(".govuk-body").asScala.toList
      paragraphs.size shouldBe 2

      paragraphs(0).text() shouldBe CannotSetupDirectDebitPage.paragraphContent1
      paragraphs(1).text() shouldBe CannotSetupDirectDebitPage.paragraphContent2

      val cta = doc.select(".govuk-button")
      cta.text() shouldBe CannotSetupDirectDebitPage.buttonContent
      cta.attr("href") shouldBe "http://localhost:9020/business-account"
    }
  }
}

object BankDetailsControllerSpec {
  final case class SummaryRow(question: String, answer: String, changeLink: String)
}
