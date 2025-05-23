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

package actionrefiners

import controllers.DetermineTaxIdController
import essttp.journey.model.Origins
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.PageUrls

class ContinueToLandingPagesAuthenticatedActionRefinerSpec extends ItSpec {

  val controller: DetermineTaxIdController = app.injector.instanceOf[DetermineTaxIdController]

  "ContinueToLandingPagesAuthenticatedActionRefiner" - {
    "should return redirect to determine eligibility when tax id is already determined" in {
      AuthStub.authorise()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
      val result = controller.determineTaxId()(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.determineEligibilityUrl)
    }

    "redirect to not enrolled when user doesn't have the right enrolments (via AuthenticatedActionRefiner)" in {
      AuthStub.authorise(allEnrolments = Some(Set.empty))
      EssttpBackend.StartJourney.findJourney()
      val result = controller.determineTaxId()(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.notEnrolledUrl)
    }

    "redirect to login page when user has no active session (i.e. no auth token)" in {
      AuthStub.authorise(None, None)
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
      val fakeRequest = FakeRequest()
      val result      = controller.determineTaxId()(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9949/auth-login-stub/gg-sign-in?" +
          "continue=http%3A%2F%2Flocalhost%3A9215%2Fset-up-a-payment-plan%2Fwhich-tax" +
          "&origin=essttp-frontend"
      )
    }

    "redirect to the which-tax-regime page when there is no session found in backend" in {
      AuthStub.authorise()
      val result = controller.determineTaxId()(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.whichTaxRegimeUrl)
    }
  }
}
