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

import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import play.api.mvc.{Result, Session}
import play.api.test.Helpers._
import testsupport.Givens.canEqualPlaySession
import testsupport.ItSpec
import testsupport.stubs.EssttpBackend

import scala.concurrent.Future

class SignOutControllerSpec extends ItSpec {

  private val controller: SignOutController = app.injector.instanceOf[SignOutController]

  "signOutFromTimeout should" - {

    "return the timed out page" in {

      val result: Future[Result] = controller.signOutFromTimeout(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost/set-up-a-payment-plan/timed-out"
      )
    }
  }

  "exitSurveyPaye should" - {
    "redirect to feedback frontend with eSSTTP-PAYE as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)()

      val result: Future[Result] = controller.exitSurveyPaye(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-PAYE")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "exitSurveyVat should" - {
    "redirect to feedback frontend with eSSTTP-VAT as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)()

      val result: Future[Result] = controller.exitSurveyVat(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-VAT")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "exitSurveySa should" - {
    "redirect to feedback frontend with eSSTTP-SA as the service identifier" in {
      stubCommonActions()
      EssttpBackend.SubmitArrangement.findJourney(Origins.Sa.Bta, testCrypto)()

      val result: Future[Result] = controller.exitSurveySa(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9514/feedback/eSSTTP-SA")
      session(result) shouldBe Session(Map.empty)
    }
  }

  "signOut should" - {
    TaxRegime.values.foreach { taxRegime =>
      s"[taxRegime = ${taxRegime.toString}] redirect to the tax regime specific exist survey route with no sessionId" in {
        val (origin, expectedRedirectLocation) = taxRegime match {
          case TaxRegime.Epaye =>
            Origins.Epaye.Bta -> "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/eSSTTP-PAYE"
          case TaxRegime.Vat   =>
            Origins.Vat.Bta -> "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/eSSTTP-VAT"
          case TaxRegime.Sa    =>
            Origins.Sa.Bta -> "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/eSSTTP-SA"
          case TaxRegime.Simp  =>
            Origins.Simp.Pta -> "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/eSSTTP-SIMP"
        }

        stubCommonActions()
        EssttpBackend.StartJourney.findJourney(origin)

        val result: Future[Result] = controller.signOut()(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectLocation)
        session(result) shouldBe Session(Map.empty)
      }
    }
  }

}
