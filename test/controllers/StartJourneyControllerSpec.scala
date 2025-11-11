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

import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class StartJourneyControllerSpec extends ItSpec {

  private val controller = app.injector.instanceOf[StartJourneyController]

  "GET /govuk/start must" - {

    "redirect to the which tax page adding a marker to the cookie session" in {
      stubCommonActions()

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startGovuk(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.WhichTaxRegimeController.whichTaxRegime.url)
      session(result).data.get("is-govuk-origin") shouldBe Some("true")
    }

  }

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

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result =
        controller.startGovukVatJourney(FakeRequest("GET", routes.StartJourneyController.startGovukEpayeJourney.url))
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fgovuk%2Fepaye%2Fstart&origin=essttp-frontend"
      )
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

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result =
        controller.startGovukVatJourney(FakeRequest("GET", routes.StartJourneyController.startGovukVatJourney.url))
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fgovuk%2Fvat%2Fstart&origin=essttp-frontend"
      )
    }
  }

  "GET /govuk/sa/start" - {
    "should start a gov uk SA journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneySaGovUk

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startGovukSaJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      EssttpBackend.StartJourney.verifyStartJourneySaGovUk()
    }

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result =
        controller.startGovukSaJourney(FakeRequest("GET", routes.StartJourneyController.startGovukSaJourney.url))
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fgovuk%2Fsa%2Fstart&origin=essttp-frontend"
      )
    }
  }

  "GET /govuk/simp/start" - {
    "should start a gov uk SIMP journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneySimpGovUk

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startGovukSimpJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      EssttpBackend.StartJourney.verifyStartJourneySimpGovUk()
    }

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result =
        controller.startGovukSimpJourney(FakeRequest("GET", routes.StartJourneyController.startGovukSimpJourney.url))
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fgovuk%2Fsimp%2Fstart&origin=essttp-frontend"
      )
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

    "should start a detached EPAYE journey and redirect if the user has already seen the landing page" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyEpayeDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(
          SessionKeys.sessionId                          -> "IamATestSessionId",
          LandingController.hasSeenLandingPageSessionKey -> "true"
        )

      val result: Future[Result] = controller.startDetachedEpayeJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey).isEmpty shouldBe true

      EssttpBackend.StartJourney.verifyStartJourneyEpayeDetached()
    }

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result = controller.startDetachedEpayeJourney(
        FakeRequest("GET", routes.StartJourneyController.startDetachedEpayeJourney.url)
      )
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fepaye%2Fstart&origin=essttp-frontend"
      )
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

    "should start a detached VAT journey and redirect if the user has already seen the landing page" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyVatDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(
          SessionKeys.sessionId                          -> "IamATestSessionId",
          LandingController.hasSeenLandingPageSessionKey -> "true"
        )

      val result: Future[Result] = controller.startDetachedVatJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe empty

      EssttpBackend.StartJourney.verifyStartJourneyVatDetached()
    }

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result = controller.startDetachedVatJourney(
        FakeRequest("GET", routes.StartJourneyController.startDetachedVatJourney.url)
      )
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fvat%2Fstart&origin=essttp-frontend"
      )
    }
  }

  "GET /sa/start" - {
    "should start a detached VAT journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneySaDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startDetachedSaJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:19001/set-up-a-payment-plan/sa-payment-plan")
      EssttpBackend.StartJourney.verifyStartJourneySaDetached()
    }

    "should start a detached VAT journey and redirect if the user has already seen the landing page" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneySaDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(
          SessionKeys.sessionId                          -> "IamATestSessionId",
          LandingController.hasSeenLandingPageSessionKey -> "true"
        )

      val result: Future[Result] = controller.startDetachedSaJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe empty

      EssttpBackend.StartJourney.verifyStartJourneySaDetached()
    }

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result =
        controller.startDetachedSaJourney(FakeRequest("GET", routes.StartJourneyController.startDetachedSaJourney.url))
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fsa%2Fstart&origin=essttp-frontend"
      )
    }
  }

  "GET /simp/start" - {
    "should start a detached VAT journey and redirect" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneySimpDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.startDetachedSimpJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:19001/set-up-a-payment-plan/simple-assessment-payment-plan"
      )
      EssttpBackend.StartJourney.verifyStartJourneySimpDetached()
    }

    "should start a detached VAT journey and redirect if the user has already seen the landing page" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneySimpDetached

      val fakeRequest = FakeRequest()
        .withAuthToken()
        .withSession(
          SessionKeys.sessionId                          -> "IamATestSessionId",
          LandingController.hasSeenLandingPageSessionKey -> "true"
        )

      val result: Future[Result] = controller.startDetachedSimpJourney(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineTaxIdController.determineTaxId.url)
      session(result).data.get(LandingController.hasSeenLandingPageSessionKey) shouldBe empty

      EssttpBackend.StartJourney.verifyStartJourneySimpDetached()
    }

    "should redirect to login with the correct continue url if the user is not logged in" in {
      val result = controller.startDetachedSimpJourney(
        FakeRequest("GET", routes.StartJourneyController.startDetachedSimpJourney.url)
      )
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fsimp%2Fstart&origin=essttp-frontend"
      )
    }
  }

}

class GovUkSaNotEnabledControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.sa" -> false)

  private val controller = app.injector.instanceOf[StartJourneyController]

  "SA start govuk journey endpoint should redirect to the SA SUPP service when SA is not enabled" in {
    val fakeRequest = FakeRequest()
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.startGovukSaJourney(fakeRequest)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some("http://localhost:9063/pay-what-you-owe-in-instalments")
  }

  "SA start detached journey endpoint should redirect to the SA SUPP service when SA is not enabled" in {
    val fakeRequest = FakeRequest()
      .withAuthToken()
      .withSession(SessionKeys.sessionId -> "IamATestSessionId")

    val result: Future[Result] = controller.startDetachedSaJourney(fakeRequest)
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some("http://localhost:9063/pay-what-you-owe-in-instalments")
  }

}
