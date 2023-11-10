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

  "GET /epaye-payment-plan" - {
    "return 200 and the PAYE landing page" in {
      EssttpBackend.StartJourney.findJourney()
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.epayeLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up an Employers’ PAYE payment plan",
        shouldBackLinkBePresent     = false,
        expectedSubmitUrl           = None,
        signedIn                    = false,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Epaye),
        shouldServiceNameBeInHeader = false
      )

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 3

      val firstListBullets = lists(0).select("li").asScala.toList
      firstListBullets.size shouldBe 6

      firstListBullets(0).text() shouldBe "you plan to pay the debt off within the next 12 months or less"
      firstListBullets(1).text() shouldBe "you owe £50,000 or less"
      firstListBullets(2).text() shouldBe "your debts are 5 years old or less"
      firstListBullets(3).text() shouldBe "your Employers’ PAYE submissions are up to date"
      firstListBullets(4).text() shouldBe "your Construction Industry Scheme (CIS) returns are up to date (if applicable)"
      firstListBullets(5).text() shouldBe "you do not have any payment plans or debts with HMRC"

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.epayeLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
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
    "return 200 and the VAT landing page" in {
      EssttpBackend.StartJourney.findJourney(Origins.Vat.DetachedUrl)
      val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.vatLandingPage(fakeRequest)

      RequestAssertions.assertGetRequestOk(result)
      val doc: Document = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        expectedH1                  = "Set up a VAT payment plan",
        shouldBackLinkBePresent     = false,
        expectedSubmitUrl           = None,
        signedIn                    = false,
        shouldH1BeSameAsServiceName = true,
        regimeBeingTested           = Some(TaxRegime.Vat),
        shouldServiceNameBeInHeader = false
      )

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 3

      val firstListBullets = lists(0).select("li").asScala.toList
      firstListBullets.size shouldBe 4

      firstListBullets(0).text() shouldBe "you plan to pay the debt off within the next 12 months or less"
      firstListBullets(1).text() shouldBe "you owe £50,000 or less"

      val paragraphs = doc.select("p.govuk-body").asScala.toList
      paragraphs(2).text() shouldBe "You can use this service within 28 days of the overdue payment deadline."

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.vatLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
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

}

class LandingPageVatNotEnabledControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.vat" -> false)

  private val controller = app.injector.instanceOf[LandingController]

  "GET /vat-payment-plan should return 501 (NOT IMPLEMENTED) when VAT is not enabled" in {
    val fakeRequest = FakeRequest()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.vatLandingPage(fakeRequest)
    status(result) shouldBe NOT_IMPLEMENTED

  }

}

class LandingPageShutteringControllerSpec extends ItSpec with ShutteringSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "shuttering.shuttered-tax-regimes" -> List("epaye", "vat")
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
  }

}
