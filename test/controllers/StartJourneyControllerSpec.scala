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

import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class StartJourneyControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[StartJourneyController]

  "GET /govuk/epaye/start" - {
    "should start a gov uk EPAYE journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyEpayeGovUk

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startGovukEpayeJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      EssttpBackend.StartJourney.verifyStartJourneyEpayeGovUk()
    }
  }

  "GET /govuk/vat/start" - {
    "should start a gov uk VAT journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyVatGovUk

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startGovukVatJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      EssttpBackend.StartJourney.verifyStartJourneyVatGovUk()
    }
  }

  "GET /epaye/start" - {
    "should start a detached EPAYE journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyEpayeDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startDetachedEpayeJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:19001/set-up-a-payment-plan/epaye-payment-plan")
      EssttpBackend.StartJourney.verifyStartJourneyEpayeDetached()
    }
  }

  "GET /vat/start" - {
    "should start a detached VAT journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyVatDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startDetachedVatJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:19001/set-up-a-payment-plan/vat-payment-plan")
      EssttpBackend.StartJourney.verifyStartJourneyVatDetached()
    }
  }

}

class GovUkVatNotEnabledControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.vat" -> false)

  private val controller = app.injector.instanceOf[StartJourneyController]

  "VAT start govuk journey endpoint should return 501 (NOT IMPLEMENTED) when VAT is not enabled" in {
    val fakeRequest = FakeRequest()
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.startGovukVatJourney(fakeRequest)
    status(result) shouldBe NOT_IMPLEMENTED
  }

  "VAT start detached journey endpoint should return 501 (NOT IMPLEMENTED) when VAT is not enabled" in {
    val fakeRequest = FakeRequest()
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.startDetachedVatJourney(fakeRequest)
    status(result) shouldBe NOT_IMPLEMENTED
  }

}
