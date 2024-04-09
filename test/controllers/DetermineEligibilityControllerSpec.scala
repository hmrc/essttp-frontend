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

import cats.syntax.eq._

import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.{EligibilityRules, RegimeDigitalCorrespondence}
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.{CombinationsHelper, ItSpec}
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuditConnectorStub, EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll, TtpJsonResponses}
import uk.gov.hmrc.http.SessionKeys

class DetermineEligibilityControllerSpec extends ItSpec with CombinationsHelper {
  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  "Determine eligibility endpoint should route user correctly and send an audit event" - {
    forAll(Table(
      ("Scenario flavour", "eligibility rules", "ineligibility reason audit string", "expected redirect", "updated journey json", "origin"),
      ("HasRlsOnAddress - EPAYE", TdAll.notEligibleHasRlsOnAddress, "hasRlsOnAddress", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("HasRlsOnAddress - VAT", TdAll.notEligibleHasRlsOnAddress, "hasRlsOnAddress", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("HasRlsOnAddress - SA", TdAll.notEligibleHasRlsOnAddress, "hasRlsOnAddress", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("MarkedAsInsolvent - EPAYE", TdAll.notEligibleMarkedAsInsolvent, "markedAsInsolvent", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("MarkedAsInsolvent - VAT", TdAll.notEligibleMarkedAsInsolvent, "markedAsInsolvent", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("MarkedAsInsolvent - SA", TdAll.notEligibleMarkedAsInsolvent, "markedAsInsolvent", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MarkedAsInsolvent`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("IsLessThanMinDebtAllowance - EPAYE", TdAll.notEligibleIsLessThanMinDebtAllowance, "isLessThanMinDebtAllowance", PageUrls.epayeDebtTooSmallUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("IsLessThanMinDebtAllowance - VAT", TdAll.notEligibleIsLessThanMinDebtAllowance, "isLessThanMinDebtAllowance", PageUrls.vatDebtTooSmallUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("IsLessThanMinDebtAllowance - SA", TdAll.notEligibleIsLessThanMinDebtAllowance, "isLessThanMinDebtAllowance", PageUrls.saDebtTooSmallUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsLessThanMinDebtAllowance`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("IsMoreThanMaxDebtAllowance - EPAYE", TdAll.notEligibleIsMoreThanMaxDebtAllowance, "isMoreThanMaxDebtAllowance", PageUrls.epayeDebtTooLargeUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("IsMoreThanMaxDebtAllowance - VAT", TdAll.notEligibleIsMoreThanMaxDebtAllowance, "isMoreThanMaxDebtAllowance", PageUrls.vatDebtTooLargeUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("IsMoreThanMaxDebtAllowance - SA", TdAll.notEligibleIsMoreThanMaxDebtAllowance, "isMoreThanMaxDebtAllowance", PageUrls.saDebtTooLargeUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxDebtAllowance`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("DisallowedChargeLockTypes - EPAYE", TdAll.notEligibleDisallowedChargeLockTypes, "disallowedChargeLockTypes", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("DisallowedChargeLockTypes - VAT", TdAll.notEligibleDisallowedChargeLockTypes, "disallowedChargeLockTypes", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("DisallowedChargeLockTypes - SA", TdAll.notEligibleDisallowedChargeLockTypes, "disallowedChargeLockTypes", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DisallowedChargeLockTypes`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("ExistingTTP - EPAYE", TdAll.notEligibleExistingTTP, "existingTTP", PageUrls.epayeAlreadyHaveAPaymentPlanUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("ExistingTTP - VAT", TdAll.notEligibleExistingTTP, "existingTTP", PageUrls.vatAlreadyHaveAPaymentPlanUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("ExistingTTP - SA", TdAll.notEligibleExistingTTP, "existingTTP", PageUrls.saAlreadyHaveAPaymentPlanUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExistingTTP`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("ExceedsMaxDebtAge - EPAYE", TdAll.notEligibleExceedsMaxDebtAge, "chargesOverMaxDebtAge", PageUrls.epayeDebtTooOldUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("ExceedsMaxDebtAge - VAT", TdAll.notEligibleExceedsMaxDebtAge, "chargesOverMaxDebtAge", PageUrls.vatDebtTooOldUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("ExceedsMaxDebtAge - SA", TdAll.notEligibleExceedsMaxDebtAge, "chargesOverMaxDebtAge", PageUrls.saDebtTooOldUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ExceedsMaxDebtAge`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("IneligibleChargeType - EPAYE", TdAll.notEligibleEligibleChargeType, "ineligibleChargeTypes", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("IneligibleChargeType - VAT", TdAll.notEligibleEligibleChargeType, "ineligibleChargeTypes", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("IneligibleChargeType - SA", TdAll.notEligibleEligibleChargeType, "ineligibleChargeTypes", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - EligibleChargeType`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("MissingFiledReturns - EPAYE", TdAll.notEligibleMissingFiledReturns, "missingFiledReturns", PageUrls.epayeFileYourReturnUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("MissingFiledReturns - VAT", TdAll.notEligibleMissingFiledReturns, "missingFiledReturns", PageUrls.vatFileYourReturnUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("MissingFiledReturns - SA", TdAll.notEligibleMissingFiledReturns, "missingFiledReturns", PageUrls.saFileYourReturnUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - MissingFiledReturns`(Origins.Sa.Bta),
        Origins.Sa.Bta),
      ("NoDueDatesReached - EPAYE", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.payeNoDueDatesReachedUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("NoDueDatesReached - VAT", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.vatNoDueDatesReachedUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Vat.Bta),
        Origins.Vat.Bta),
      ("NoDueDatesReached - SA", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Sa.Bta),
        Origins.Sa.Bta),

      ("HasInvalidInterestSignals - EPAYE", TdAll.notEligibleHasInvalidInterestSignals, "hasInvalidInterestSignals", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("HasInvalidInterestSignals - VAT", TdAll.notEligibleHasInvalidInterestSignals, "hasInvalidInterestSignals", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Vat.Bta), Origins.Vat.Bta),
      ("HasInvalidInterestSignals - SA", TdAll.notEligibleHasInvalidInterestSignals, "hasInvalidInterestSignals", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignals`(Origins.Sa.Bta), Origins.Sa.Bta),

      ("HasInvalidInterestSignalsCESA - SA", TdAll.notEligibleHasInvalidInterestSignalsCESA, "hasInvalidInterestSignalsCESA", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasInvalidInterestSignalsCESA`(Origins.Sa.Bta), Origins.Sa.Bta),

      ("DmSpecialOfficeProcessingRequired - EPAYE", TdAll.notEligibleDmSpecialOfficeProcessingRequired, "dmSpecialOfficeProcessingRequired", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("DmSpecialOfficeProcessingRequired - VAT", TdAll.notEligibleDmSpecialOfficeProcessingRequired, "dmSpecialOfficeProcessingRequired", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Vat.Bta), Origins.Vat.Bta),
      ("DmSpecialOfficeProcessingRequired - SA", TdAll.notEligibleDmSpecialOfficeProcessingRequired, "dmSpecialOfficeProcessingRequired", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequired`(Origins.Sa.Bta), Origins.Sa.Bta),

      ("CannotFindLockReason - EPAYE", TdAll.notEligibleCannotFindLockReason, "cannotFindLockReason", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("CannotFindLockReason - VAT", TdAll.notEligibleCannotFindLockReason, "cannotFindLockReason", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Vat.Bta), Origins.Vat.Bta),
      ("CannotFindLockReason - SA", TdAll.notEligibleCannotFindLockReason, "cannotFindLockReason", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CannotFindLockReason`(Origins.Sa.Bta), Origins.Sa.Bta),

      ("CreditsNotAllowed - EPAYE", TdAll.notEligibleCreditsNotAllowed, "creditsNotAllowed", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("CreditsNotAllowed - VAT", TdAll.notEligibleCreditsNotAllowed, "creditsNotAllowed", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Vat.Bta), Origins.Vat.Bta),
      ("CreditsNotAllowed - SA", TdAll.notEligibleCreditsNotAllowed, "creditsNotAllowed", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - CreditsNotAllowed`(Origins.Sa.Bta), Origins.Sa.Bta),

      ("IsMoreThanMaxPaymentReference - EPAYE", TdAll.notEligibleIsMoreThanMaxPaymentReference, "isMoreThanMaxPaymentReference", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Epaye.Bta), Origins.Epaye.Bta),
      ("IsMoreThanMaxPaymentReference - VAT", TdAll.notEligibleIsMoreThanMaxPaymentReference, "isMoreThanMaxPaymentReference", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Vat.Bta), Origins.Vat.Bta),
      ("IsMoreThanMaxPaymentReference - SA", TdAll.notEligibleIsMoreThanMaxPaymentReference, "isMoreThanMaxPaymentReference", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - IsMoreThanMaxPaymentReference`(Origins.Sa.Bta), Origins.Sa.Bta),
      ("ChargesBeforeMaxAccountingDate - VAT", TdAll.notEligibleChargesBeforeMaxAccountingDate, "chargesBeforeMaxAccountingDate", PageUrls.vatDebtBeforeAccountingDateUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - ChargesBeforeMaxAccountingDate`(Origins.Vat.Bta),
        Origins.Vat.Bta),

      ("HasDisguisedRemuneration - SA", TdAll.notEligibleHasDisguisedRemuneration, "hasDisguisedRemuneration", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasDisguisedRemuneration`(Origins.Sa.Bta), Origins.Sa.Bta),
      ("HasCapacitor - SA", TdAll.notEligibleHasCapacitor, "hasCapacitor", PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasCapacitor`(Origins.Sa.Bta), Origins.Sa.Bta),

      ("DmSpecialOfficeProcessingRequiredCDCS - SA",
        TdAll.notEligibleDmSpecialOfficeProcessingRequiredCDCS,
        "dmSpecialOfficeProcessingRequiredCDCS",
        PageUrls.saNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequiredCDCS`(Origins.Sa.Bta),
        Origins.Sa.Bta
      )
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

                case TaxRegime.Sa =>
                  ("Sa", """{ "utr": "1234567895" }""")
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

    "Ineligible: NoDueDatesReached with isLessThanMinDebtAllowance also true should redirect correctly" - {
      forAll(Table(
        ("Scenario flavour", "eligibility rules", "expected redirect", "updated journey json", "origin"),
        ("NoDueDatesReached - EPAYE", TdAll.notEligibleNoDueDatesReached.copy(isLessThanMinDebtAllowance = true), PageUrls.payeNoDueDatesReachedUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Epaye.Bta), Origins.Epaye.Bta),
        ("NoDueDatesReached - VAT", TdAll.notEligibleNoDueDatesReached.copy(isLessThanMinDebtAllowance = true), PageUrls.vatNoDueDatesReachedUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Vat.Bta), Origins.Vat.Bta),
        ("NoDueDatesReached - SA", TdAll.notEligibleNoDueDatesReached.copy(isLessThanMinDebtAllowance = true), PageUrls.saNotEligibleUrl,
          JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Sa.Bta), Origins.Sa.Bta)
      )) {
        (sf: String, eligibilityRules: EligibilityRules, expectedRedirect: String, updatedJourneyJson: String, origin: Origin) =>
          {
            s"[$sf] should redirect to noDueDatesReached page" in {
              val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(origin.taxRegime, TdAll.notEligibleEligibilityPass, eligibilityRules)

              stubCommonActions()
              EssttpBackend.DetermineTaxId.findJourney(origin)()
              Ttp.Eligibility.stubRetrieveEligibility(origin.taxRegime)(eligibilityCheckResponseJson)
              EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, updatedJourneyJson)

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
              val result = controller.determineEligibility(fakeRequest)

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }
      }
    }

    "throw an error is dmSpecialOfficeProcessingRequiredCDCS is received for a tax regime that is not SA" in {
      val taxRegimesAndOrigins = TaxRegime.values.map{ taxRegime =>
        val origin = taxRegime match {
          case TaxRegime.Epaye => Origins.Epaye.Bta
          case TaxRegime.Vat   => Origins.Vat.Bta
          case TaxRegime.Sa    => Origins.Sa.Bta
        }
        taxRegime -> origin
      }

      taxRegimesAndOrigins.filter(p => p._1 =!= TaxRegime.Sa).foreach{
        case (taxRegime, origin) =>
          withClue(s"For tax regime ${taxRegime.entryName} and origin ${origin.toString}: "){
            val eligibilityResponseJson =
              TtpJsonResponses.ttpEligibilityCallJson(
                taxRegime,
                TdAll.notEligibleEligibilityPass,
                TdAll.notEligibleDmSpecialOfficeProcessingRequiredCDCS
              )

            stubCommonActions()
            EssttpBackend.DetermineTaxId.findJourney(origin)()
            Ttp.Eligibility.stubRetrieveEligibility(taxRegime)(eligibilityResponseJson)
            EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, JourneyJsonTemplates.`Eligibility Checked - Ineligible - DmSpecialOfficeProcessingRequiredCDCS`(origin))

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val error = intercept[Exception](controller.determineEligibility(fakeRequest).futureValue)
            error.getMessage should include(s"dmSpecialOfficeProcessingRequiredCDCS ineligibility reason not relevant to ${taxRegime.entryName}")
          }
      }

    }

    "Eligible for Epaye: should redirect to your bill and send an audit event" - {
      allCombinationOfTwoBooleanOptions.foreach(combo => {
        val maybeChargeIsInterestBearingCharge = combo._1
        val maybeChargeUseChargeReference = combo._2
        s"where 'isInterestBearingCharge' field is ${maybeChargeIsInterestBearingCharge.toString} " +
          s"and 'useChargeReference' is ${maybeChargeUseChargeReference.toString}" in {

            val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
              TaxRegime.Epaye,
              regimeDigitalCorrespondence        = true,
              maybeChargeIsInterestBearingCharge = maybeChargeIsInterestBearingCharge,
              maybeChargeUseChargeReference      = maybeChargeUseChargeReference
            )
            // for audit event
            val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
              TaxRegime.Epaye,
              poundsInsteadOfPence               = true,
              regimeDigitalCorrespondence        = true,
              maybeChargeIsInterestBearingCharge = maybeChargeIsInterestBearingCharge,
              maybeChargeUseChargeReference      = maybeChargeUseChargeReference
            )

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
              expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
                TdAll.eligibleEligibilityPass,
                TdAll.eligibleEligibilityRules,
                TaxRegime.Epaye,
                Some(RegimeDigitalCorrespondence(true)),
                maybeChargeIsInterestBearingCharge,
                maybeChargeUseChargeReference
              )
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
      })
    }

    "Eligible for Vat: should redirect to your bill and send an audit event" in {
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        regimeDigitalCorrespondence        = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )
      // for audit event
      val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Vat,
        poundsInsteadOfPence               = true,
        regimeDigitalCorrespondence        = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Eligible`()
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Vat)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId                      = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          TaxRegime.Vat,
          Some(RegimeDigitalCorrespondence(true)),
          chargeChargeBeforeMaxAccountingDate = Some(true)
        )
      )(testOperationCryptoFormat)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json.parse(
          s"""
           |{
           |  "eligibilityResult" : "eligible",
           |  "noEligibilityReasons": 0,
           |  "origin": "Bta",
           |  "taxType": "Vat",
           |  "taxDetail": {
           |    "vrn":"101747001"
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

    "Eligible for Sa: should redirect to your bill and send an audit event" in {
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Sa,
        regimeDigitalCorrespondence        = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )
      // for audit event
      val eligibilityCheckResponseJsonAsPounds = TtpJsonResponses.ttpEligibilityCallJson(
        TaxRegime.Sa,
        poundsInsteadOfPence               = true,
        regimeDigitalCorrespondence        = true,
        maybeChargeBeforeMaxAccountingDate = Some(true)
      )

      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)()
      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Sa)(eligibilityCheckResponseJson)
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(
        TdAll.journeyId,
        JourneyJsonTemplates.`Eligibility Checked - Eligible`()
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)

      Ttp.Eligibility.verifyTtpEligibilityRequests(TaxRegime.Sa)

      EssttpBackend.EligibilityCheck.verifyUpdateEligibilityRequest(
        journeyId                      = TdAll.journeyId,
        expectedEligibilityCheckResult = TdAll.eligibilityCheckResult(
          TdAll.eligibleEligibilityPass,
          TdAll.eligibleEligibilityRules,
          TaxRegime.Sa,
          Some(RegimeDigitalCorrespondence(true)),
          chargeChargeBeforeMaxAccountingDate = Some(true)
        )
      )(testOperationCryptoFormat)

      AuditConnectorStub.verifyEventAudited(
        "EligibilityCheck",
        Json.parse(
          s"""
             |{
             |  "eligibilityResult" : "eligible",
             |  "noEligibilityReasons": 0,
             |  "origin": "Bta",
             |  "taxType": "Sa",
             |  "taxDetail": {
             |    "utr": "1234567895"
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

    List(
      Origins.Epaye.Bta -> TaxRegime.Epaye,
      Origins.Vat.Bta -> TaxRegime.Vat,
      Origins.Sa.Bta -> TaxRegime.Sa
    ).foreach {
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

          s"[${taxRegime.entryName}] Redirect to generic ineligible call us page when ttp eligibility call returns a 422 response" in {
            stubCommonActions()
            EssttpBackend.DetermineTaxId.findJourney(origin)()
            Ttp.Eligibility.stub422RetrieveEligibility()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = controller.determineEligibility(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER

            val expectedPageUrl: String = taxRegime match {
              case TaxRegime.Epaye => PageUrls.payeNotEligibleUrl
              case TaxRegime.Vat   => PageUrls.vatNotEligibleUrl
              case TaxRegime.Sa    => PageUrls.saNotEligibleUrl
            }
            redirectLocation(result) shouldBe Some(expectedPageUrl)

            Ttp.Eligibility.verifyTtpEligibilityRequests(taxRegime)
            EssttpBackend.EligibilityCheck.verifyNoneUpdateEligibilityRequest(TdAll.journeyId)
            AuditConnectorStub.verifyNoAuditEvent()
          }
      }

    "VAT user with a debt below the minimum amount and debt too old should be redirected to generic ineligible page" in {
      val eligibilityRules = TdAll.notEligibleIsLessThanMinDebtAllowance.copy(chargesOverMaxDebtAge = Some(true))
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(TaxRegime.Vat, TdAll.notEligibleEligibilityPass, eligibilityRules)

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Vat)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - debt too low and old`(Origins.Vat.Bta))

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.vatDebtTooSmallUrl)
    }

    "EPAYE user with a debt below the minimum amount and debt too old should be redirected to debt too low ineligible page" in {
      val eligibilityRules = TdAll.notEligibleIsLessThanMinDebtAllowance.copy(chargesOverMaxDebtAge = Some(true))
      val eligibilityCheckResponseJson = TtpJsonResponses.ttpEligibilityCallJson(TaxRegime.Epaye, TdAll.notEligibleEligibilityPass, eligibilityRules)

      Ttp.Eligibility.stubRetrieveEligibility(TaxRegime.Epaye)(eligibilityCheckResponseJson)
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()
      EssttpBackend.EligibilityCheck.stubUpdateEligibilityResult(TdAll.journeyId, JourneyJsonTemplates.`Eligibility Checked - Ineligible - MultipleReasons - debt too low and old`())

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.determineEligibility(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.epayeDebtTooSmallUrl)
    }

  }
}
