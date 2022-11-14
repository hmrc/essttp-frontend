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

import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import messages.Messages
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.collection.JavaConverters._

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
        regimeBeingTested           = Some(TaxRegime.Epaye)
      )

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 3

      val firstListBullets = lists(0).select("li").asScala.toList
      firstListBullets.size shouldBe 6

      firstListBullets(0).text() shouldBe "you plan to pay the debt off within the next 6 months or less"
      firstListBullets(1).text() shouldBe "you owe £15,000 or less"

      val paragraphs = doc.select("p.govuk-body").asScala.toList
      paragraphs(2).text() shouldBe "You can use this service within 35 days of the overdue payment deadline."

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.epayeLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
    }
  }

  "GET /epaye-payment-plan-continue" - {
    "should redirect to start a detached journey with an updated session" in {
      val existingSessionData = Map("a" -> "b")

      val result = controller.epayeLandingPageContinue(FakeRequest().withSession(existingSessionData.toList: _*))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedEpayeJourney.url)
      session(result) shouldBe Session(existingSessionData.updated(LandingController.hasSeenLandingPageSessionKey, "true"))
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
        regimeBeingTested           = Some(TaxRegime.Vat)
      )

      val lists = doc.select(".govuk-list").asScala.toList
      lists.size shouldBe 3

      val firstListBullets = lists(0).select("li").asScala.toList
      firstListBullets.size shouldBe 4

      firstListBullets(0).text() shouldBe "you plan to pay the debt off within the next 6 months or less"
      firstListBullets(1).text() shouldBe "you owe £20,000 or less"

      val paragraphs = doc.select("p.govuk-body").asScala.toList
      paragraphs(2).text() shouldBe "You can use this service within 28 days of the overdue payment deadline."

      val button = doc.select(".govuk-button")
      button.attr("href") shouldBe routes.LandingController.vatLandingPageContinue.url
      button.text() shouldBe Messages.`Start now`.english
    }
  }

  "GET /vat-payment-plan-continue" - {
    "should redirect to start a detached journey with an updated session" in {
      val existingSessionData = Map("a" -> "b")

      val result = controller.vatLandingPageContinue(FakeRequest().withSession(existingSessionData.toList: _*))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedVatJourney.url)
      session(result) shouldBe Session(existingSessionData.updated(LandingController.hasSeenLandingPageSessionKey, "true"))
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
