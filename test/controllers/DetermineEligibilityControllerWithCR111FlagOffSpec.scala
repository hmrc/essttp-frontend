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
import essttp.rootmodel.ttp.eligibility.EligibilityRules
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuditConnectorStub, EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll, TtpJsonResponses}
import testsupport.{CombinationsHelper, ItSpec}
import uk.gov.hmrc.http.SessionKeys

class DetermineEligibilityControllerWithCR111FlagOffSpec extends ItSpec with CombinationsHelper {
  private val controller: DetermineEligibilityController = app.injector.instanceOf[DetermineEligibilityController]

  override lazy val configOverrides: Map[String, Any] = Map("features.cr111" -> false)

  "Determine eligibility endpoint with CR11 flag set to false should route user correctly for reason noDueDatesReached and send an audit event" - {
    forAll(Table(
      ("Scenario flavour", "eligibility rules", "ineligibility reason audit string", "expected redirect", "updated journey json", "origin"),
      ("NoDueDatesReached - EPAYE", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.payeNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Epaye.Bta),
        Origins.Epaye.Bta),
      ("NoDueDatesReached - VAT", TdAll.notEligibleNoDueDatesReached, "noDueDatesReached", PageUrls.vatNotEligibleUrl,
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - NoDueDatesReached`(Origins.Vat.Bta),
        Origins.Vat.Bta)
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
                  throw new NotImplementedError()
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
  }
}
