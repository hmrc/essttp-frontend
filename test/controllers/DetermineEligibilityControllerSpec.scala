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

import essttp.rootmodel.ttp.EligibilityRules
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuditConnectorStub, AuthStub, EssttpBackend, Ttp}
import testsupport.testdata.{PageUrls, TdAll, TtpJsonResponses}
import uk.gov.hmrc.http.SessionKeys

class DetermineEligibilityControllerSpec extends ItSpec {
  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility endpoint should route user correctly" - {
    forAll(Table(
      ("Scenario flavour", "eligibility rules", "ineligibility reason audit string", "expected redirect"),
      ("HasRlsOnAddress", TdAll.notEligibleHasRlsOnAddress, "hasRlsOnAddress", PageUrls.notEligibleUrl),
      ("MarkedAsInsolvent", TdAll.notEligibleMarkedAsInsolvent, "markedAsInsolvent", PageUrls.notEligibleUrl),
      ("IsLessThanMinDebtAllowance", TdAll.notEligibleIsLessThanMinDebtAllowance, "isLessThanMinDebtAllowance", PageUrls.notEligibleUrl),
      ("IsMoreThanMaxDebtAllowance", TdAll.notEligibleIsMoreThanMaxDebtAllowance, "isMoreThanMaxDebtAllowance", PageUrls.debtTooLargeUrl),
      ("DisallowedChargeLockTypes", TdAll.notEligibleDisallowedChargeLockTypes, "disallowedChargeLockTypes", PageUrls.notEligibleUrl),
      ("ExistingTTP", TdAll.notEligibleExistingTTP, "existingTTP", PageUrls.alreadyHaveAPaymentPlanUrl),
      ("ExceedsMaxDebtAge", TdAll.notEligibleExceedsMaxDebtAge, "chargesOverMaxDebtAge", PageUrls.debtTooOldUrl),
      ("EligibleChargeType", TdAll.notEligibleEligibleChargeType, "ineligibleChargeTypes", PageUrls.notEligibleUrl),
      ("MissingFiledReturns", TdAll.notEligibleMissingFiledReturns, "missingFiledReturns", PageUrls.fileYourReturnUrl)
    )) {
      (sf: String, eligibilityRules: EligibilityRules, auditIneligibilityReason: String, expectedRedirect: String) =>
        {
          s"Ineligible: [$sf] should redirect to $expectedRedirect" in {
            val eligibilityCheckResponseJson =
              TtpJsonResponses.ttpEligibilityCallJson(TdAll.notEligibleEligibilityPass, eligibilityRules)

            AuthStub.authorise()
            EssttpBackend.DetermineTaxId.findJourney()
            Ttp.Eligibility.retrieveEligibility(TtpJsonResponses.ttpEligibilityCallJson(TdAll.notEligibleEligibilityPass, eligibilityRules))
            Ttp.Eligibility.retrieveEligibility(eligibilityCheckResponseJson)
            EssttpBackend.EligibilityCheck.updateEligibilityResult(TdAll.journeyId)

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = controller.determineEligibility(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirect)
            Ttp.Eligibility.verifyTtpEligibilityRequests()
            AuditConnectorStub.verifyEventAudited(
              "EligibilityCheck",
              Json.parse(
                s"""
                   |{
                   |  "eligibilityResult" : "ineligible",
                   |  "enrollmentReasons": "did not pass eligibility check",
                   |  "noEligibilityReasons": 1,
                   |  "eligibilityReasons" : [ "$auditIneligibilityReason" ],
                   |  "origin": "Bta",
                   |  "taxType": "Epaye",
                   |  "taxDetail": {
                   |    "employerRef": "864FZ00049",
                   |    "accountsOfficeRef": "123PA44545546"
                   |  },
                   |  "authProviderId": "authId-999",
                   |  "chargeTypeAssessment" : ${(Json.parse(eligibilityCheckResponseJson).as[JsObject] \ "chargeTypeAssessment").get.toString}
                   |}
                   |""".
                  stripMargin
              ).as[JsObject]
            )
          }
        }
    }

    "Eligible: should redirect to your bill" in {
      val eligibilityCheckResponseJson =
        TtpJsonResponses.ttpEligibilityCallJson()

      AuthStub.authorise()
      EssttpBackend.DetermineTaxId.findJourney()
      Ttp.Eligibility.retrieveEligibility(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.updateEligibilityResult(TdAll.journeyId)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)
      Ttp.Eligibility.verifyTtpEligibilityRequests()
      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json.parse(
          s"""
             |{
             |  "eligibilityResult" : "eligible",
             |  "noEligibilityReasons": 0,
             |  "origin": "Bta",
             |  "taxType": "Epaye",
             |  "taxDetail": {
             |    "employerRef": "864FZ00049",
             |    "accountsOfficeRef": "123PA44545546"
             |  },
             |  "authProviderId": "authId-999",
             |  "chargeTypeAssessment" : ${(Json.parse(eligibilityCheckResponseJson).as[JsObject] \ "chargeTypeAssessment").get.toString}
             |}
             |""".
            stripMargin
        ).as[JsObject]
      )
    }

    "Eligibility already determined should route user to your bill is" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)
    }

    "Redirect to landing page if journey is in started state" in {
      AuthStub.authorise()
      EssttpBackend.StartJourney.findJourney()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.landingPageUrl)
    }
  }
}
