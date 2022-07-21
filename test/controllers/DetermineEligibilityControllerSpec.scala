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

import essttp.journey.model.ttp.EligibilityRules
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuthStub, EssttpBackend, Ttp}
import testsupport.testdata.{PageUrls, TdAll, TtpJsonResponses}
import uk.gov.hmrc.http.SessionKeys

class DetermineEligibilityControllerSpec extends ItSpec {
  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility endpoint should route user correctly" - {
    forAll(Table(
      ("Scenario flavour", "eligibility rules", "expected redirect"),
      ("HasRlsOnAddress", TdAll.notEligibleHasRlsOnAddress, PageUrls.notEligibleUrl),
      ("MarkedAsInsolvent", TdAll.notEligibleMarkedAsInsolvent, PageUrls.notEligibleUrl),
      ("IsLessThanMinDebtAllowance", TdAll.notEligibleIsLessThanMinDebtAllowance, PageUrls.notEligibleUrl),
      ("IsMoreThanMaxDebtAllowance", TdAll.notEligibleIsMoreThanMaxDebtAllowance, PageUrls.debtTooLargeUrl),
      ("DisallowedChargeLocks", TdAll.notEligibleDisallowedChargeLocks, PageUrls.notEligibleUrl),
      ("ExistingTTP", TdAll.notEligibleExistingTTP, PageUrls.alreadyHaveAPaymentPlanUrl),
      ("ExceedsMaxDebtAge", TdAll.notEligibleExceedsMaxDebtAge, PageUrls.debtTooOldUrl),
      ("EligibleChargeType", TdAll.notEligibleEligibleChargeType, PageUrls.notEligibleUrl),
      ("MissingFiledReturns", TdAll.notEligibleMissingFiledReturns, PageUrls.fileYourReturnUrl)
    )) {
      (sf: String, eligibilityRules: EligibilityRules, expectedRedirect: String) =>
        {
          s"Eligibility failure: [$sf] should redirect to $expectedRedirect" in {
            AuthStub.authorise()
            EssttpBackend.DetermineTaxId.findJourney()
            Ttp.retrieveEligibility(TtpJsonResponses.ttpEligibilityCallJson(TdAll.notEligibleOverallEligibilityStatus, eligibilityRules))
            EssttpBackend.EligibilityCheck.updateEligibilityResult(TdAll.journeyId)
            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = controller.determineEligibility(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirect)
            Ttp.verifyTtpEligibilityRequests()
          }
        }
    }

    "Eligibility already determined should route user to your bill is" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)
    }
  }
}
