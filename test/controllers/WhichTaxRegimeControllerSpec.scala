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
import org.jsoup.Jsoup
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.retrieve.Credentials

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class WhichTaxRegimeControllerSpec extends ItSpec {

  val controller = app.injector.instanceOf[WhichTaxRegimeController]

  val authCredentials: Credentials = Credentials("authId-999", "GovernmentGateway")

  "GET /which-tax should" - {

    val fakeRequest = FakeRequest().withAuthToken()

      def testPageIsDisplayed(result: Future[Result]): Unit = {
        RequestAssertions.assertGetRequestOk(result)

        val doc = Jsoup.parse(contentAsString(result))

        ContentAssertions.commonPageChecks(
          doc,
          "Which tax do you want to set up a payment plan for?",
          shouldBackLinkBePresent = false,
          expectedSubmitUrl       = Some(routes.WhichTaxRegimeController.whichTaxRegimeSubmit.url),
          regimeBeingTested       = None
        )

        val radios = doc.select(".govuk-radios__item").asScala.toList
        radios.size shouldBe 2

        radios(0).select(".govuk-radios__input").`val`() shouldBe "EPAYE"
        radios(0).select(".govuk-radios__label").text() shouldBe "Employers’ PAYE"

        radios(1).select(".govuk-radios__input").`val`() shouldBe "VAT"
        radios(1).select(".govuk-radios__label").text() shouldBe "VAT"
        ()
      }

    "redirect to the EPAYE landing page if the user has a EPAYE enrolment and no other enrolments for " +
      "supported tax regimes" in {
        AuthStub.authorise(Some(Set(TdAll.payeEnrolment)), Some(authCredentials))

        val result = controller.whichTaxRegime(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.LandingController.epayeLandingPage.url)
      }

    "redirect to the VAT landing page if the user has a VAT enrolment and no other enrolments for " +
      "supported tax regimes" in {
        AuthStub.authorise(Some(Set(TdAll.vatEnrolment)), Some(authCredentials))

        val result = controller.whichTaxRegime(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.LandingController.vatLandingPage.url)
      }

    "display the page when there are no enrolments for supported tax regimes" in {
      AuthStub.authorise(Some(Set()), Some(authCredentials))

      testPageIsDisplayed(controller.whichTaxRegime(fakeRequest))
    }

    "display the page when there are enrolments for more than one supported tax regime" in {
      AuthStub.authorise(Some(Set(TdAll.payeEnrolment, TdAll.vatEnrolment)), Some(authCredentials))

      testPageIsDisplayed(controller.whichTaxRegime(fakeRequest))
    }

  }

  "POST /which-tax should" - {

    val fakeRequest = FakeRequest(method = "POST", path = "").withAuthToken()

    "show a form error if nothing is submitted" in {
      stubCommonActions()

      val result: Future[Result] = controller.whichTaxRegimeSubmit(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Which tax do you want to set up a payment plan for?",
        expectedSubmitUrl       = Some(routes.WhichTaxRegimeController.whichTaxRegime.url),
        shouldBackLinkBePresent = false,
        hasFormError            = true,
        regimeBeingTested       = None
      )

      val errorSummary = doc.select(".govuk-error-summary")
      val errorLink = errorSummary.select("a")
      errorLink.text() shouldBe "Select which tax you want to set up a payment plan for"
      errorLink.attr("href") shouldBe "#WhichTaxRegime"
    }

    "redirect to the start EPAYE journey endpoint if the user selects EPAYE" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyInBackend(Origins.Epaye.DetachedUrl)

      val request = fakeRequest.withFormUrlEncodedBody("WhichTaxRegime" -> "EPAYE")
      val result: Future[Result] = controller.whichTaxRegimeSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedEpayeJourney.url)
    }

    "redirect to the start VAT journey endpoint if the user selects VAT" in {
      stubCommonActions()
      EssttpBackend.StartJourney.startJourneyInBackend(Origins.Vat.DetachedUrl)

      val request = fakeRequest.withFormUrlEncodedBody("WhichTaxRegime" -> "VAT")
      val result: Future[Result] = controller.whichTaxRegimeSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.StartJourneyController.startDetachedVatJourney.url)
    }

  }

}

class WhichTaxRegimeVatDisabledControllerSpec extends ItSpec {

  val controller = app.injector.instanceOf[WhichTaxRegimeController]

  override lazy val configOverrides: Map[String, Any] = Map(
    "features.vat" -> false
  )

  "A 501 NotImplemented response should be returned by" - {

    "GET /which-tax" in {
      status(controller.whichTaxRegime(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }

    "POST /which-tax" in {
      status(controller.whichTaxRegimeSubmit(FakeRequest())) shouldBe NOT_IMPLEMENTED
    }
  }
}
