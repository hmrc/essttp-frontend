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
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class GovUkControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[GovUkController]

  "isComingFromGovUk" in {
    val refererForGovUk = "https://www.gov.uk/"
    val requestMadeFromGovUk = FakeRequest().withHeaders("Referer" -> refererForGovUk)
    val requestMadeFromSomewhereElse = FakeRequest().withHeaders("Referer" -> "https://somewhere.else/")
    val requestWithoutReferer = FakeRequest()

    controller.isComingFromGovUk(requestMadeFromGovUk) shouldBe true
    controller.isComingFromGovUk(requestMadeFromSomewhereElse) shouldBe false
    controller.isComingFromGovUk(requestWithoutReferer) shouldBe false
  }

  override protected lazy val configOverrides: Map[String, Any] = Map(
    "refererForGovUk" -> "https://www.gov.uk"
  )

  "EPAYE start journey endpoint" - {
    "should start govuk journey and redirect to determine tax id when user is coming from govuk and user is authenticated" in {
      val refererForGovUk = "https://www.gov.uk/"
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyEpayeGovUk
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withHeaders("Referer" -> refererForGovUk)

      val result: Future[Result] = controller.startEpayeJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      EssttpBackend.StartJourney.verifyStartJourneyEpayeGovUk()
    }
    "should start detached url journey and redirect to nextUrl when user is authenticated" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyEpayeDetached
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startEpayeJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:19001/set-up-a-payment-plan/epaye-payment-plan")
      EssttpBackend.StartJourney.verifyStartJourneyEpayeDetached()
    }
  }

  "VAT start journey endpoint" - {
    "should start govuk journey and redirect to determine tax id when user is coming from govuk and user is authenticated" in {
      val refererForGovUk = "https://www.gov.uk/"
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyVatGovUk
      EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk)

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")
        .withHeaders("Referer" -> refererForGovUk)

      val result: Future[Result] = controller.startVatJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      EssttpBackend.StartJourney.verifyStartJourneyVatGovUk()
    }
    "should start detached url journey and redirect to nextUrl when user is authenticated" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyVatDetached
      EssttpBackend.StartJourney.findJourney(Origins.Vat.DetachedUrl)

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startVatJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:19001/set-up-a-payment-plan/vat-payment-plan")
      EssttpBackend.StartJourney.verifyStartJourneyVatDetached()
    }
  }

}

class GovUkVatNotEnabledControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.vat" -> false)

  private val controller = app.injector.instanceOf[GovUkController]

  "VAT start journey endpoint should return 501 (NOT IMPLEMENTED) when VAT is not enabled" in {
    val fakeRequest = FakeRequest()
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.startVatJourney(fakeRequest)
    status(result) shouldBe NOT_IMPLEMENTED

  }

}
