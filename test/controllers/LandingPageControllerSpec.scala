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

import actionrefiners.ShutteringSpec
import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import messages.Messages
import models.Languages
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{AuthStub, EssttpBackend}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class LandingPageControllerSpec extends ItSpec {

  private val controller: LandingController = app.injector.instanceOf[LandingController]

  private def testCallHmrcDetails(doc: Document) = {
    val details = doc.select("details.govuk-details")

    val detailsSummaryText = details.select(".govuk-details__summary > .govuk-details__summary-text")
    detailsSummaryText.text() shouldBe "If you do not think you can set up a plan online, call HMRC and find out if you can set up a plan over the phone."

    val detailsText = details.select(".govuk-details__text")
    val detailsTextParagraphs = detailsText.select(".govuk-body").asScala.toList
    detailsTextParagraphs.size shouldBe 7

    detailsTextParagraphs(0).text() shouldBe "Telephone: 0300 123 1813"
    detailsTextParagraphs(1).text() shouldBe "Outside UK: +44 2890 538 192"
    detailsTextParagraphs(2).text() shouldBe "Our phone line opening hours are:"
    detailsTextParagraphs(3).text() shouldBe "Monday to Friday: 8am to 6pm"
    detailsTextParagraphs(4).text() shouldBe "Closed weekends and bank holidays."

    detailsText.select("h2.govuk-heading-m").text() shouldBe "Text service"

    detailsTextParagraphs(5).text() shouldBe "Use Relay UK if you cannot hear or speak on the telephone, dial 18001 then 0345 300 3900. " +
      "Find out more on the Relay UK website (opens in new tab)."
    detailsTextParagraphs(6).text() shouldBe "If a health condition or personal circumstances make it difficult to contact us, " +
      "read our guidance Get help from HMRC if you need extra support (opens in new tab)."

    val detailsLink1 = detailsTextParagraphs(5).select("a.govuk-link")
    detailsLink1.text() shouldBe "Relay UK website (opens in new tab)"
    detailsLink1.attr("href") shouldBe "https://www.relayuk.bt.com/"
    detailsLink1.attr("rel") shouldBe "noreferrer noopener"
    detailsLink1.attr("target") shouldBe "_blank"

    val detailsLink2 = detailsTextParagraphs(6).select("a.govuk-link")
    detailsLink2.text() shouldBe "Get help from HMRC if you need extra support (opens in new tab)"
    detailsLink2.attr("href") shouldBe "https://www.gov.uk/get-help-hmrc-extra-support"
    detailsLink2.attr("rel") shouldBe "noreferrer noopener"
    detailsLink2.attr("target") shouldBe "_blank"
  }

  "GET /epaye-payment-plan" - {
    "return 200 and the PAYE landing page when logged in" in {
      EssttpBackend.StartJourney.findJourney()
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId").withAuthToken()
      val result: Future[Result] = controller.epayeLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up an Employers’ PAYE payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Epaye),
        shouldServiceNameBeInHeader = false,
        backLinkUrlOverride         = Some("/set-up-a-payment-plan/test-only/bta-page?starting-page")
      )

      val paragraphs = doc.select("p.govuk-body").asScala.toList

      paragraphs(0).text() shouldBe "Use this service to set up a payment plan for your outstanding employers’ PAYE bill. " +
        "Payments are taken by Direct Debit and include interest charged at the Bank of England base rate plus 2.5% per year."

      val insetText = doc.select(".govuk-inset-text").asScala.toList
      insetText.size shouldBe 1
      insetText(0).text() shouldBe "You must be able to authorise a Direct Debit without a signature from any other " +
        "account holders and be named on the UK bank account you’ll use to pay."

      paragraphs(1).text() shouldBe "You’ll need to stay up to date with your payments or we could ask you to pay in full."
      paragraphs(2).text() shouldBe "To set up a plan, your company or partnership must:"

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 1
      val bullets = lists(0).select("li").asScala.toList
      bullets.size shouldBe 5

      bullets(0).text() shouldBe "have missed the deadline to pay a PAYE bill"
      bullets(1).text() shouldBe "owe £100,000 or less"
      bullets(2).text() shouldBe "have debts that are 5 years old or less"
      bullets(3).text() shouldBe "have no other payment plans or debts with HMRC"
      bullets(4).text() shouldBe "have no outstanding employers’ PAYE submissions or Construction Industry Scheme returns"

      testCallHmrcDetails(doc)

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.epayeLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
    }

    "return 200 and the PAYE landing page when not logged in" in {
      EssttpBackend.StartJourney.findJourney()
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.epayeLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up an Employers’ PAYE payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        signedIn                    = false,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Epaye),
        shouldServiceNameBeInHeader = false
      )
    }
  }

  "GET /epaye-payment-plan-continue" - {
    "should redirect to the login page and continue to the same continue endpoint once login is successful " +
      "if the user is not logged in" in {
        val result = controller.epayeLandingPageContinue(FakeRequest("GET", routes.LandingController.epayeLandingPageContinue.url))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fepaye-payment-plan-continue&origin=essttp-frontend")
      }

    "should redirect to start a detached journey with an updated session if no existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.epayeLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedEpayeJourney.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe Some("true")
    }

    "should redirect to determine tax id if an existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.epayeLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe None
    }
  }

  "GET /vat-payment-plan" - {
    "return 200 and the VAT landing page when logged in" in {
      EssttpBackend.StartJourney.findJourney(Origins.Vat.DetachedUrl)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId").withAuthToken()
      val result: Future[Result] = controller.vatLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a VAT payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Vat),
        shouldServiceNameBeInHeader = false,
        backLinkUrlOverride         = Some("/set-up-a-payment-plan/test-only/bta-page?starting-page")
      )

      val paragraphs = doc.select("p.govuk-body").asScala.toList

      paragraphs(0).text() shouldBe "Use this service to set up a payment plan for your outstanding VAT bill. " +
        "Payments are taken by Direct Debit and include interest charged at the Bank of England base rate plus 2.5% per year."

      val insetText = doc.select(".govuk-inset-text").asScala.toList
      insetText.size shouldBe 1
      insetText(0).text() shouldBe "You must be able to authorise a Direct Debit without a signature from any other " +
        "account holders and be named on the UK bank account you’ll use to pay."

      paragraphs(1).text() shouldBe "You’ll need to stay up to date with your payments or we could ask you to pay in full."
      paragraphs(2).text() shouldBe "To set up a plan, your company or partnership must:"

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 2

      val bullets1 = lists(0).select("li").asScala.toList
      bullets1.size shouldBe 5
      bullets1(0).text() shouldBe "have missed the deadline to pay a VAT bill"
      bullets1(1).text() shouldBe "owe £100,000 or less"
      bullets1(2).text() shouldBe "have a debt for an accounting period that started in 2023 or later"
      bullets1(3).text() shouldBe "have no other payment plans or debts with HMRC"
      bullets1(4).text() shouldBe "have filed your tax returns"

      paragraphs(3).text() shouldBe "If you have a Customer Compliance Manager, discuss your needs with them before using this service."
      paragraphs(4).text() shouldBe "You cannot use this service if you are:"

      val bullets2 = lists(1).select("li").asScala.toList
      bullets2.size shouldBe 3
      bullets2(0).text() shouldBe "a cash accounting customer"
      bullets2(1).text() shouldBe "an annual accounting scheme member"
      bullets2(2).text() shouldBe "a payment on account customer"

      testCallHmrcDetails(doc)

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.vatLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
    }
    "return 200 and the VAT landing page when not logged in" in {
      EssttpBackend.StartJourney.findJourney(Origins.Vat.DetachedUrl)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.vatLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a VAT payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        signedIn                    = false,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Vat),
        shouldServiceNameBeInHeader = false
      )
    }
  }

  "GET /vat-payment-plan-continue" - {
    "should redirect to the login page and continue to the same continue endpoint once login is successful " +
      "if the user is not logged in" in {
        val result = controller.vatLandingPageContinue(FakeRequest("GET", routes.LandingController.vatLandingPageContinue.url))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fvat-payment-plan-continue&origin=essttp-frontend")
      }

    "should redirect to start a detached journey with an updated session if no existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.vatLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedVatJourney.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe Some("true")
    }

    "should redirect to determine tax id if an existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.vatLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe None
    }
  }

  "GET /sa-payment-plan" - {
    "return 200 and the SA landing page when logged in" in {
      EssttpBackend.StartJourney.findJourney(origin = Origins.Sa.Bta)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId").withAuthToken()
      val result: Future[Result] = controller.saLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a Self Assessment payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Sa),
        shouldServiceNameBeInHeader = false,
        backLinkUrlOverride         = Some("/set-up-a-payment-plan/test-only/bta-page?starting-page")
      )

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 1

      val paragraphs = doc.select("p.govuk-body").asScala.toList
      paragraphs(0).text() shouldBe "A payment plan allows you to pay your tax charges in instalments over a period of time."
      paragraphs(1).text() shouldBe "Your plan covers the tax you owe and, if applicable, the 2 advance payments towards your tax bill. " +
        "It also covers any penalties or charges against your account. You’ll have to pay interest on the amount you pay late."
      paragraphs(2).text() shouldBe "To be eligible to set up an online payment plan you need to:"

      val firstListBullets = lists(0).select("li").asScala.toList
      firstListBullets.size shouldBe 4

      firstListBullets(0).text() shouldBe "ensure your tax returns are up to date"
      firstListBullets(1).text() shouldBe "owe £30,000 or less"
      firstListBullets(2).text() shouldBe "have no other tax debts"
      firstListBullets(3).text() shouldBe "have no other HMRC payment plans set up"

      paragraphs(3).text() shouldBe "You can use this service within 60 days of the payment deadline."

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.saLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
    }
    "return 200 and the SA landing page when not logged in" in {
      EssttpBackend.StartJourney.findJourney(origin = Origins.Sa.Bta)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.saLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a Self Assessment payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        signedIn                    = false,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Sa),
        shouldServiceNameBeInHeader = false
      )
    }
  }

  "GET /sa-payment-plan-continue" - {
    "should redirect to the login page and continue to the same continue endpoint once login is successful " +
      "if the user is not logged in" in {
        val result = controller.saLandingPageContinue(FakeRequest("GET", routes.LandingController.saLandingPageContinue.url))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fsa-payment-plan-continue&origin=essttp-frontend")
      }

    "should redirect to start a detached journey with an updated session if no existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.saLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedSaJourney.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe Some("true")
    }

    "should redirect to determine tax id if an existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin = Origins.Sa.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.saLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe None
    }
  }

  "GET /simple-assessment-payment-plan" - {
    "return 200 and the SIMP landing page when logged in" in {
      EssttpBackend.StartJourney.findJourney(origin = Origins.Simp.Pta)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId").withAuthToken()
      val result: Future[Result] = controller.simpLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a Simple Assessment payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Simp),
        shouldServiceNameBeInHeader = false,
        backLinkUrlOverride         = Some("/set-up-a-payment-plan/test-only/bta-page?starting-page")
      )

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 3

      val paragraphs = doc.select("p.govuk-body").asScala.toList
      paragraphs(0).text() shouldBe "You can use this service to pay overdue payments in instalments."
      paragraphs(1).text() shouldBe "You are eligible to set up an online payment plan if:"

      val firstListBullets = lists(0).select("li").asScala.toList
      firstListBullets.size shouldBe 3

      firstListBullets(0).text() shouldBe "you owe £50,000 or less"
      firstListBullets(1).text() shouldBe "you do not have any other debts with HMRC"
      firstListBullets(2).text() shouldBe "you do not have any payment plans with HMRC"

      paragraphs(2).text() shouldBe "You can choose to pay:"

      val secondListBullets = lists(1).select("li").asScala.toList
      secondListBullets.size shouldBe 2

      secondListBullets(0).text() shouldBe "part of the payment upfront and part in monthly instalments"
      secondListBullets(1).text() shouldBe "monthly instalments only"

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.simpLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
    }

    "return 200 and the SA landing page when not logged in" in {
      EssttpBackend.StartJourney.findJourney(origin = Origins.Simp.Pta)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.simpLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a Simple Assessment payment plan",
        shouldBackLinkBePresent     = true,
        expectedSubmitUrl           = None,
        signedIn                    = false,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Simp),
        shouldServiceNameBeInHeader = false
      )
    }
  }

  "GET /simple-assessment-payment-plan-continue" - {
    "should redirect to the login page and continue to the same continue endpoint once login is successful " +
      "if the user is not logged in" in {
        val result = controller.simpLandingPageContinue(FakeRequest("GET", routes.LandingController.simpLandingPageContinue.url))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fsimple-assessment-payment-plan-continue&origin=essttp-frontend")
      }

    "should redirect to start a detached journey with an updated session if no existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.simpLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedSimpJourney.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe Some("true")
    }

    "should redirect to determine tax id if an existing journey is found" in {
      val existingSessionData = Map(SessionKeys.sessionId -> "IamATestSessionId")

      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin = Origins.Simp.Pta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(existingSessionData.toList: _*)
      val result = controller.simpLandingPageContinue(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe None
    }
  }

}

class LandingPageSaNotEnabledControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.sa" -> false)

  private val controller = app.injector.instanceOf[LandingController]

  "GET /sa-payment-plan should redirect to the SA SUPP service when SA is not enabled" in {
    val fakeRequest = FakeRequest()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.saLandingPage(fakeRequest)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some("http://localhost:9063/pay-what-you-owe-in-instalments")

  }

}

class LandingPageSimpNotEnabledControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.simp" -> false)

  private val controller = app.injector.instanceOf[LandingController]

  "GET /simple-assessment-payment-plan should show an error when SIMP is not enabled" in {
    val fakeRequest = FakeRequest()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val error = intercept[Exception](await(controller.simpLandingPage(fakeRequest)))
    error.getMessage shouldBe "Simple Assessment is not available"
  }

}

class LandingPageShutteringControllerSpec extends ItSpec with ShutteringSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "shuttering.shuttered-tax-regimes" -> List("epaye", "vat", "sa", "SIMP")
  )

  private val controller = app.injector.instanceOf[LandingController]

  "When shuttering is enabled the shutter page should show for" - {

      def test(result: Future[Result]): Unit = {
        RequestAssertions.assertGetRequestOk(result)
        val doc: Document = Jsoup.parse(contentAsString(result))
        assertShutteringPageContent(doc, None, Languages.English)
      }

    "GET /epaye-payment-plan" in {
      test(controller.epayeLandingPage(fakeRequest))
    }

    "GET /epaye-payment-plan-continue" in {
      AuthStub.authorise()

      test(controller.epayeLandingPageContinue(fakeRequest))
    }

    "GET /vat-payment-plan" in {
      test(controller.vatLandingPage(fakeRequest))
    }

    "GET /vat-payment-plan-continue" in {
      AuthStub.authorise()

      test(controller.vatLandingPageContinue(fakeRequest))
    }

    "GET /sa-payment-plan" in {
      test(controller.saLandingPage(fakeRequest))
    }

    "GET /sa-payment-plan-continue" in {
      AuthStub.authorise()

      test(controller.saLandingPageContinue(fakeRequest))
    }

    "GET /simple-assessment-payment-plan" in {
      test(controller.simpLandingPage(fakeRequest))
    }

    "GET /simple-assessment-payment-plan-continue" in {
      AuthStub.authorise()

      test(controller.simpLandingPageContinue(fakeRequest))
    }
  }

}
