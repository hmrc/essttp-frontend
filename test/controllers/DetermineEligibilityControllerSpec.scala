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

import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.{EligibilityRules, RegimeDigitalCorrespondence}
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuditConnectorStub, EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll, TtpJsonResponses}
import uk.gov.hmrc.http.SessionKeys

class DetermineEligibilityControllerSpec extends ItSpec {
  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility endpoint should route user correctly and send an audit event" - {
    forAll(Table(
      ("Scenario flavour", "eligibility rules", "ineligibility reason audit string", "expected redirect", "updated journey json", "origin"),
      ("HasRlsOnAddress", TdAll.notEligibleHasRlsOnAddress, "hasRlsOnAddress", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("MarkedAsInsolvent", TdAll.notEligibleMarkedAsInsolvent, "markedAsInsolvent", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("IsLessThanMinDebtAllowance", TdAll.notEligibleIsLessThanMinDebtAllowance, "isLessThanMinDebtAllowance", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMniDebtAllowance`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("IsMoreThanMaxDebtAllowance - EPAYE", TdAll.notEligibleIsMoreThanMaxDebtAllowance, "isMoreThanMaxDebtAllowance", PageUrls.epayeDebtTooLargeUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("IsMoreThanMaxDebtAllowance - VAT", TdAll.notEligibleIsMoreThanMaxDebtAllowance, "isMoreThanMaxDebtAllowance", PageUrls.vatDebtTooLargeUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("DisallowedChargeLockTypes", TdAll.notEligibleDisallowedChargeLockTypes, "disallowedChargeLockTypes", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("ExistingTTP - EPAYE", TdAll.notEligibleExistingTTP, "existingTTP", PageUrls.epayeAlreadyHaveAPaymentPlanUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("ExistingTTP - VAT", TdAll.notEligibleExistingTTP, "existingTTP", PageUrls.vatAlreadyHaveAPaymentPlanUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("ExceedsMaxDebtAge - EPAYE", TdAll.notEligibleExceedsMaxDebtAge, "chargesOverMaxDebtAge", PageUrls.epayeDebtTooOldUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("ExceedsMaxDebtAge - VAT", TdAll.notEligibleExceedsMaxDebtAge, "chargesOverMaxDebtAge", PageUrls.vatDebtTooOldUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("EligibleChargeType", TdAll.notEligibleEligibleChargeType, "ineligibleChargeTypes", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("MissingFiledReturns - EPAYE", TdAll.notEligibleMissingFiledReturns, "missingFiledReturns", PageUrls.epayeFileYourReturnUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("MissingFiledReturns - VAT", TdAll.notEligibleMissingFiledReturns, "missingFiledReturns", PageUrls.vatFileYourReturnUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("NoDueDatesReached - EPAYE", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("NoDueDatesReached - VAT", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Vat.Bta),
        Origins.Vat.Bta),

      ("HasInvalidInterestSignals - EPAYE", TdAll.notEligibleHasInvalidInterestSignals, "hasInvalidInterestSignals", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("HasInvalidInterestSignals - VAT", TdAll.notEligibleHasInvalidInterestSignals, "hasInvalidInterestSignals", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Vat.Bta), Origins.Vat.Bta),

      ("DmSpecialOfficeProcessingRequired - EPAYE", TdAll.notEligibleDmSpecialOfficeProcessingRequired, "dmSpecialOfficeProcessingRequired", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("DmSpecialOfficeProcessingRequired - VAT", TdAll.notEligibleDmSpecialOfficeProcessingRequired, "dmSpecialOfficeProcessingRequired", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Vat.Bta), Origins.Vat.Bta),

      ("CannotFindLockReason - EPAYE", TdAll.notEligibleCannotFindLockReason, "cannotFindLockReason", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("CannotFindLockReason - VAT", TdAll.notEligibleCannotFindLockReason, "cannotFindLockReason", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Vat.Bta), Origins.Vat.Bta),

      ("CreditsNotAllowed - EPAYE", TdAll.notEligibleCreditsNotAllowed, "creditsNotAllowed", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("CreditsNotAllowed - VAT", TdAll.notEligibleCreditsNotAllowed, "creditsNotAllowed", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Vat.Bta), Origins.Vat.Bta)
    )) {
      (sf: String, eligibilityRules: EligibilityRules, auditIneligibilityReason: String, expectedRedirect: String, updatedJourneyJson: String, origin: Origin) =>
        {
          s"Ineligible: [$sf] should redirect to $expectedRedirect" in {
            val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(origin.taxRegime, TdAll.notEligibleEligibilityPass, eligibilityRules)
            // for audit event
            val eligibilityCheckResponseJsonAsPounds =
              TtpJsonResponses.ttpEligibilityCallJson(origin.taxRegime, TdAll.notEligibleEligibilityPass, eligibilityRules, poundsInsteadOfPence = true)

            stubCommonActions()
            EssttpBackend.DetermineTaxId.findJourney(origin)()
            Ttp.Eligibility.stubRetrieveEligibility(origin.taxRegime)(eligibilityCheckResponseJson)
            EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, updatedJourneyJson)

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = controller.determineEligibility(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirect)
            Ttp.Eligibility.verifyTtpEligibilityRequests(origin.taxRegime)

            EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
              journeyId                      = TdAll.journeyId,
              expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(TdAll.notEligibleEligibilityPass, eligibilityRules, origin.taxRegime, None)
            )(testOperationCryptoFormat)

            val (expectedTaxType, expectedTaxDetailsJson) =
              origin.taxRegime match {
                case TaxRegime.Epaye =>
                  (
                    "Epaye",
                    """{
                      |    "employerRef": "864FZ00049",
                      |    "accountsOfficeRef": "123PA44545546"
                      |  }""".stripMargin
                  )
                case TaxRegime.Vat =>
                  ("Vat", """{ "vrn": "101747001" }""")
              }

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
                   |  "taxType": "$expectedTaxType",
                   |  "taxDetail": $expectedTaxDetailsJson,
                   |  "authProviderId": "authId-999",
                   |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "chargeTypeAssessment" :${(Json.parse(eligibilityCheckResponseJsonAsPounds).as[JsObject] \ "chargeTypeAssessment").get.toString},
                   |  "futureChargeLiabilitiesExcluded": false
                   |}
                   |""".
                  stripMargin
              ).as[JsObject]
            )
          }
        }
    }

    "Eligible: should redirect to your bill and send an audit event" in {
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(TaxRegime.Epaye, regimeDigitalCorrespondence = true)
      // for audit event
      val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(TaxRegime.Epaye, poundsInsteadOfPence = true, regimeDigitalCorrespondence = true)

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Epaye)(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Eligible`()
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Epaye)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId                      = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(TdAll.eligibleEligibilityPass, TdAll.eligibleEligibilityRules, TaxRegime.Epaye, Some(RegimeDigitalCorrespondence(true)))
      )(testOperationCryptoFormat)

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
             |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
             |  "regimeDigitalCorrespondence": true,
             |  "futureChargeLiabilitiesExcluded": false,
             |  "chargeTypeAssessment" : ${(Json.parse(eligibilityCheckResponseJsonAsPounds).as[JsObject] \ "chargeTypeAssessment").get.toString}
             |}
             |""".
            stripMargin
        ).as[JsObject]
      )
    }

    "Eligibility already determined should route user to your bill is and not update backend again" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)
      EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
    }

    "Redirect to landing page if journey is in started state" in {
      stubCommonActions()
      EssttpBackend.StartJourney.findJourney()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.epayeLandingPageUrl)
      EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
    }

    "throw an error when ttp eligibility call returns a 422 response if the tax regime is paye" in {
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
      Ttp.Eligibility.stub422RetrieveEligibility()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val error = intercept[Exception](controller.determineEligibility(fakeRequest).futureValue)
      error.getMessage shouldBe "The future returned an exception of type: uk.gov.hmrc.http.Upstream4xxResponse, with message: POST of 'http://localhost:11111/debts/time-to-pay/eligibility' returned 422. Response body: ''."

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Epaye)
      EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
      AuditConnectorStub.verifyNoAuditEvent()
    }

    List(Origins.Epaye.Bta -> TaxRegime.Epaye, Origins.Vat.Bta -> TaxRegime.Vat).foreach {
      case (origin, taxRegime) =>
        s"[${taxRegime.entryName}] throw an error when ttp eligibility call returns a legitimate error (not a 422)" in {
          stubCommonActions()
          EssttpBackend.DetermineTaxId.findJourney(origin)()
          Ttp.Eligibility.stubServiceUnavailableRetrieveEligibility()

          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
          val error = intercept[Exception](controller.determineEligibility(fakeRequest).futureValue)
          error.getMessage should include("The future returned an exception of type: uk.gov.hmrc.http.Upstream5xxResponse")

          Ttp.Eligibility.verifyTtpEligibilityRequests(taxRegime)
          EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
          AuditConnectorStub.verifyNoAuditEvent()
        }
    }

    "Redirect to generic vat call us page when ttp eligibility call returns a 422 response if the tax regime is vat" in {
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      Ttp.Eligibility.stub422RetrieveEligibility()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.vatNotEligibleUrl)

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Vat)
      EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
      AuditConnectorStub.verifyNoAuditEvent()
    }

  }
}

class DetermineEligibilityControllerFfSpec extends ItSpec {
  override lazy val configOverrides: Map[String, Any] = Map("features.enable-422-error-logic" -> false)
  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]
  //todo remove this test as part of OPS-10724
  List(Origins.Epaye.Bta -> TaxRegime.Epaye, Origins.Vat.Bta -> TaxRegime.Vat).foreach {
    case (origin, taxRegime) =>
      s"[${taxRegime.entryName}] recover from any error thrown by TTP while feature flag is false (legacy behaviour)" in {
        stubCommonActions()
        EssttpBackend.DetermineTaxId.findJourney(origin)()
        Ttp.Eligibility.stubServiceUnavailableRetrieveEligibility()

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
        val result = controller.determineEligibility(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(taxRegime match {
          case TaxRegime.Epaye => PageUrls.payeNotEligibleUrl
          case TaxRegime.Vat   => PageUrls.vatNotEligibleUrl
        })

        Ttp.Eligibility.verifyTtpEligibilityRequests(taxRegime)
        EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
        AuditConnectorStub.verifyNoAuditEvent()
      }
  }
}
