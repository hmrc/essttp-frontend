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

import config.AppConfig
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.collection.JavaConverters._

class SignOutControllerSpec extends ItSpec {

  private val controller: SignOutController = app.injector.instanceOf[SignOutController]
  private val appConfig = app.injector.instanceOf[AppConfig]

  private def checkDoYouWantToGiveFeedbackContent(doc: Document): Unit = {
    val radioContent = doc.select(".govuk-radios__label").asScala.toList
    radioContent(0).text() shouldBe "Yes"
    radioContent(1).text() shouldBe "No"

    doc.select("#DoYouWantToGiveFeedback-hint").text() shouldBe "If you select no, you will be directed to GOV.UK."
    ()
  }

  "signOutFromTimeout should" - {

    "return the timed out page" in {
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.signOutFromTimeout(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "For your security, we signed you out",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = None,
        signedIn                = false,
        regimeBeingTested       = None
      )
    }
  }

  "exitSurveyPaye should" - {
    "redirect to feedback frontend with eSSTTP-PAYE as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.exitSurveyPaye(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-PAYE")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "exitSurveyVat should" - {
    "redirect to feedback frontend with eSSTTP-VAT as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.exitSurveyVat(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-VAT")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "signOut should" - {

    TaxRegime.values.foreach{ taxRegime =>

      s"[taxRegime = $taxRegime] redirect to the doYouWantToGiveFeedback page after clearing the session and storing the tax regime in the cookie" in {
        val origin: Origin = taxRegime match {
          case TaxRegime.Epaye => Origins.Epaye.Bta
          case TaxRegime.Vat   => Origins.Vat.Bta
        }

        stubCommonActions()
        EssttpBackend.StartJourney.findJourney(origin)

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

        val result: Future[Result] = controller.signOut(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SignOutController.doYouWantToGiveFeedback.url)
        session(result) shouldBe Session(Map(SignOutController.feedbackRegimeKey -> taxRegime.entryName))
      }

    }

  }

  "doYouWantToGiveFeedback should" - {

    "display the page" in {
      val fakeRequest = FakeRequest().withSession(SignOutController.feedbackRegimeKey -> "Epaye")
      val result: Future[Result] = controller.doYouWantToGiveFeedback(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Do you want to give feedback on this service?",
        shouldBackLinkBePresent = false,
        expectedSubmitUrl       = Some(routes.SignOutController.doYouWantToGiveFeedbackSubmit.url),
        signedIn                = false
      )

      checkDoYouWantToGiveFeedbackContent(doc)
    }

  }

  "doYouWantToGiveFeedbackSubmit should" - {

      def testFormError(
          formData: (String, String)*
      )(expectedErrorMessage: String): Unit = {
        val fakeRequest = FakeRequest().withMethod("POST").withFormUrlEncodedBody(formData: _*).withSession(SignOutController.feedbackRegimeKey -> "Epaye")
        val result: Future[Result] = controller.doYouWantToGiveFeedbackSubmit(fakeRequest)
        val pageContent: String = contentAsString(result)
        val doc: Document = Jsoup.parse(pageContent)

        RequestAssertions.assertGetRequestOk(result)
        ContentAssertions.commonPageChecks(
          doc,
          expectedH1              = "Do you want to give feedback on this service?",
          shouldBackLinkBePresent = false,
          expectedSubmitUrl       = Some(routes.SignOutController.doYouWantToGiveFeedbackSubmit.url),
          hasFormError            = true,
          signedIn                = false
        )

        checkDoYouWantToGiveFeedbackContent(doc)

        val errorSummary = doc.select(".govuk-error-summary")
        val errorLink = errorSummary.select("a")
        errorLink.text() shouldBe expectedErrorMessage
        errorLink.attr("href") shouldBe "#DoYouWantToGiveFeedback"
        ()
      }

    "return a form error when nothing is submitted" in {
      testFormError()("Select yes if you want to give feedback on this service")
    }

    "return a form error when the value submitted is not recognised" in {
      testFormError("DoYouWantToGiveFeedback" -> "2")("Select yes if you want to give feedback on this service")
    }

    "throw an exception is the user selects yes but no taxRegime can be found in the cookie session" in {
      val fakeRequest =
        FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody("DoYouWantToGiveFeedback" -> "Yes")

      an[Exception] shouldBe thrownBy(await(controller.doYouWantToGiveFeedbackSubmit(fakeRequest)))
    }

    "redirect to the exitSurveyPaye endpoint if the user selects yes and the tax regime is EPAYE" in {
      val fakeRequest =
        FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody("DoYouWantToGiveFeedback" -> "Yes")
          .withSession(SignOutController.feedbackRegimeKey -> "Epaye")

      val result: Future[Result] = controller.doYouWantToGiveFeedbackSubmit(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.SignOutController.exitSurveyPaye.url)
    }

    "redirect to the exitSurveyPaye endpoint if the user selects yes and the tax regime is Vat" in {
      val fakeRequest =
        FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody("DoYouWantToGiveFeedback" -> "Yes")
          .withSession(SignOutController.feedbackRegimeKey -> "Vat")

      val result: Future[Result] = controller.doYouWantToGiveFeedbackSubmit(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.SignOutController.exitSurveyVat.url)
    }

    "redirect to govUk if the user selects no" in {
      val fakeRequest =
        FakeRequest()
          .withMethod("POST")
          .withFormUrlEncodedBody("DoYouWantToGiveFeedback" -> "No")
          .withSession(SignOutController.feedbackRegimeKey -> "Epaye")

      val result: Future[Result] = controller.doYouWantToGiveFeedbackSubmit(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.Urls.govUkUrl)
    }

  }

}
