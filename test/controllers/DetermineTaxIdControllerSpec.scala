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

import essttp.enrolments.EnrolmentDef
import essttp.journey.model.Origins
import essttp.rootmodel.{EmpRef, Nino, SaUtr, Vrn}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.*
import testsupport.ItSpec
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, TdAll}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

import scala.util.Random

class DetermineTaxIdControllerSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "sa-legacy.pass-through-percentage" -> 100
  )

  val controller: DetermineTaxIdController = app.injector.instanceOf[DetermineTaxIdController]

  "Determine tax id controller endpoint should redirect properly" - {

    def enrolment(key: String, activated: Boolean)(idKeyWithValue: (String, String)*): Enrolment =
      Enrolment(key, idKeyWithValue.map(EnrolmentIdentifier.apply), if (activated) "activated" else "other")

    "for EPAYE when" - {

      "the tax id has already been determined" in {
        stubCommonActions()
        EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)
      }

      "the relevant enrolment is found with the necessary identifiers and the enrolment is active" in {
        val enrolments =
          Set(
            enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref",
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeNumber`.identifierKey    -> "Number"
            )
          )

        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney()
        EssttpBackend.DetermineTaxId.stubUpdateTaxId(
          TdAll.journeyId,
          JourneyJsonTemplates.`Computed Tax Id`(origin = Origins.Epaye.Bta, taxReference = "NumberRef")
        )

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)

        EssttpBackend.DetermineTaxId.verifyTaxIdRequest(TdAll.journeyId, EmpRef("NumberRef"))
      }

      "the relevant enrolment is found with the necessary identifiers but the enrolment is not active" in {
        val enrolments =
          Set(
            enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = false)(
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref",
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeNumber`.identifierKey    -> "Number"
            )
          )

        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney()

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotEnrolledController.notEnrolled.url)
        AuditConnectorStub.verifyEventAudited(
          "EligibilityCheck",
          Json
            .parse(
              s"""
               |{
               |  "eligibilityResult" : "ineligible",
               |  "enrollmentReasons": "inactive enrollment",
               |  "noEligibilityReasons": 0,
               |  "eligibilityReasons" : [  ],
               |  "origin": "Bta",
               |  "taxType": "Epaye",
               |  "taxDetail": { },
               |  "authProviderId": "authId-999",
               |  "chargeTypeAssessment" : []
               |}
               |""".stripMargin
            )
            .as[JsObject]
        )
      }

      "the relevant enrolment is found but the correct identifiers could not be found" in {
        List(
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(
            EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref"
          ),
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(
            EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeNumber`.identifierKey    -> "Number"
          ),
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(),
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = false)(
            EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref"
          ),
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = false)(
            EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref"
          ),
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = false)()
        ).foreach { e =>
          stubCommonActions(authAllEnrolments = Some(Set(e)))
          EssttpBackend.StartJourney.findJourney()
          EssttpBackend.DetermineTaxId.stubUpdateTaxId(
            TdAll.journeyId,
            JourneyJsonTemplates.`Computed Tax Id`(origin = Origins.Epaye.Bta, taxReference = "NumberRef")
          )

          a[RuntimeException] shouldBe thrownBy(await(controller.determineTaxId()(fakeRequest)))
        }

      }

      "the relevant enrolment is not found" in {
        stubCommonActions(authAllEnrolments = Some(Set.empty))
        EssttpBackend.StartJourney.findJourney()

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotEnrolledController.notEnrolled.url)
        AuditConnectorStub.verifyEventAudited(
          "EligibilityCheck",
          Json
            .parse(
              s"""
               |{
               |  "eligibilityResult" : "ineligible",
               |  "enrollmentReasons": "not enrolled",
               |  "noEligibilityReasons": 0,
               |  "eligibilityReasons" : [  ],
               |  "origin": "Bta",
               |  "taxType": "Epaye",
               |  "taxDetail": { },
               |  "authProviderId": "authId-999",
               |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
               |  "chargeTypeAssessment" : []
               |}
               |""".stripMargin
            )
            .as[JsObject]
        )
      }

    }

    "for VAT when" - {
      "the tax id has already been determined" in {
        stubCommonActions(authAllEnrolments = Some(Set(TdAll.vatEnrolment)))
        EssttpBackend.DetermineTaxId.findJourney(Origins.Vat.Bta)(
          JourneyJsonTemplates.`Computed Tax Id`(Origins.Vat.Bta, "101747001")
        )

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)
      }

      "the relevant enrolment is found with the necessary identifiers and the enrolment is active" in {
        val enrolments =
          Set(
            enrolment(EnrolmentDef.Vat.`HMRC-MTD-VAT`.enrolmentKey, activated = true)(
              EnrolmentDef.Vat.`HMRC-MTD-VAT`.identifierKey -> "Ref"
            )
          )

        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney(Origins.Vat.Bta)
        EssttpBackend.DetermineTaxId.stubUpdateTaxId(
          TdAll.journeyId,
          JourneyJsonTemplates.`Computed Tax Id`(origin = Origins.Vat.Bta, taxReference = "Ref")
        )

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)

        EssttpBackend.DetermineTaxId.verifyTaxIdRequest(TdAll.journeyId, Vrn("Ref"))
      }

      "the relevant enrolment is found but the correct identifiers could not be found" in {
        val enrolments = Set(enrolment(EnrolmentDef.Vat.`HMRC-MTD-VAT`.enrolmentKey, activated = false)())
        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk)

        a[RuntimeException] shouldBe thrownBy(await(controller.determineTaxId()(fakeRequest)))
      }

      "the relevant enrolment is not found" in {
        stubCommonActions(authAllEnrolments = Some(Set.empty))
        EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk)

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotEnrolledController.notVatRegistered.url)
        AuditConnectorStub.verifyEventAudited(
          "EligibilityCheck",
          Json
            .parse(
              s"""
               |{
               |  "eligibilityResult" : "ineligible",
               |  "enrollmentReasons": "not enrolled",
               |  "noEligibilityReasons": 0,
               |  "eligibilityReasons" : [  ],
               |  "origin": "GovUk",
               |  "taxType": "Vat",
               |  "taxDetail": { },
               |  "authProviderId": "authId-999",
               |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
               |  "chargeTypeAssessment" : []
               |}
               |""".stripMargin
            )
            .as[JsObject]
        )
      }
    }

    "for SA when" - {
      "the tax id has already been determined" in {
        stubCommonActions(authAllEnrolments = Some(Set(TdAll.saEnrolment)))
        EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)(
          JourneyJsonTemplates.`Computed Tax Id`(Origins.Sa.Bta, "1234567895")
        )

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)
      }

      "the relevant enrolment is found with the necessary identifier and the enrolment is active" in {
        val enrolments =
          Set(
            enrolment(EnrolmentDef.Sa.`IR-SA`.enrolmentKey, activated = true)(
              EnrolmentDef.Sa.`IR-SA`.identifierKey -> "Ref"
            )
          )

        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney(Origins.Sa.Bta)
        EssttpBackend.DetermineTaxId.stubUpdateTaxId(
          TdAll.journeyId,
          JourneyJsonTemplates.`Computed Tax Id`(origin = Origins.Sa.Bta, taxReference = "Ref")
        )

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)

        EssttpBackend.DetermineTaxId.verifyTaxIdRequest(TdAll.journeyId, SaUtr("Ref"))
      }

      "the relevant enrolment is found but the correct identifiers could not be found" in {
        val enrolments = Set(enrolment(EnrolmentDef.Sa.`IR-SA`.enrolmentKey, activated = false)())
        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney(Origins.Sa.GovUk)

        a[RuntimeException] shouldBe thrownBy(await(controller.determineTaxId()(fakeRequest)))
      }

      "the relevant enrolment is not found" in {
        stubCommonActions(authAllEnrolments = Some(Set.empty))
        EssttpBackend.StartJourney.findJourney(Origins.Sa.GovUk)

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotEnrolledController.notSaEnrolled.url)
        AuditConnectorStub.verifyEventAudited(
          "EligibilityCheck",
          Json
            .parse(
              s"""
               |{
               |  "eligibilityResult" : "ineligible",
               |  "enrollmentReasons": "not enrolled",
               |  "noEligibilityReasons": 0,
               |  "eligibilityReasons" : [  ],
               |  "origin": "GovUk",
               |  "taxType": "Sa",
               |  "taxDetail": { },
               |  "authProviderId": "authId-999",
               |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
               |  "chargeTypeAssessment" : []
               |}
               |""".stripMargin
            )
            .as[JsObject]
        )
      }
    }

    "for SIMP when" - {
      "the tax id has already been determined" in {
        stubCommonActions(authAllEnrolments = Some(Set()))
        EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Pta)(
          JourneyJsonTemplates.`Computed Tax Id`(Origins.Simp.Pta, "1234567895")
        )

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)
      }

      "a NINO can be found in the auth retrievals" in {
        stubCommonActions(authNino = Some("AB123456C"))
        EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Pta)(JourneyJsonTemplates.Started(Origins.Simp.Pta))
        EssttpBackend.DetermineTaxId.stubUpdateTaxId(
          TdAll.journeyId,
          JourneyJsonTemplates.`Computed Tax Id`(origin = Origins.Simp.Pta, taxReference = "AB123456C")
        )
        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)

        EssttpBackend.DetermineTaxId.verifyTaxIdRequest(TdAll.journeyId, Nino("AB123456C"))
      }

      "a NINO cannot be found in the auth retrievals" in {
        stubCommonActions()
        EssttpBackend.DetermineTaxId.findJourney(Origins.Simp.Pta)(JourneyJsonTemplates.Started(Origins.Simp.Pta))

        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IneligibleController.simpGenericIneligiblePage.url)

        val expectedAuditEvent =
          Json
            .parse(
              """{
            |  "noEligibilityReasons" : 0,
            |  "taxDetail" : { },
            |  "origin" : "Pta",
            |  "taxType" : "Simp",
            |  "eligibilityReasons" : [ ],
            |  "enrollmentReasons" : "no nino found",
            |  "chargeTypeAssessment" : [ ],
            |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
            |  "regimeDigitalCorrespondence" : true,
            |  "authProviderId" : "authId-999",
            |  "eligibilityResult" : "ineligible"
            |}
            |""".stripMargin
            )
            .as[JsObject]

        AuditConnectorStub.verifyEventAudited("EligibilityCheck", expectedAuditEvent)
      }

    }
  }
}

class TotalRedirectionToLegacySaSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "sa-legacy.pass-through-percentage" -> 0
  )

  lazy val controller: DetermineTaxIdController = app.injector.instanceOf[DetermineTaxIdController]

  "Calls to determine tax id for SA should all redirect to the legacy SA service if configured to do so" in {
    (1 to 500).foreach { _ =>
      stubCommonActions(authAllEnrolments = Some(Set(TdAll.saEnrolment)))
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)(
        JourneyJsonTemplates.`Computed Tax Id`(Origins.Sa.Bta, "1234567895")
      )

      val result = controller.determineTaxId()(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "http://localhost:9063/pay-what-you-owe-in-instalments/arrangement/determine-eligibility"
      )
    }

  }

}

@SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
class PartialRedirectionToLegacySaSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map(
    "sa-legacy.pass-through-percentage" -> 50,
    "platform.frontend.host"            -> "http://platform-host"
  )

  lazy val controller: DetermineTaxIdController = app.injector.instanceOf[DetermineTaxIdController]

  "Calls to determine tax id for SA should sometimes redirect to the legacy SA service if configured to do so" in {
    val redirectToLegacyCount = (1 to 1000).foldLeft(0) { case (acc, _) =>
      stubCommonActions(authAllEnrolments = Some(Set(TdAll.saEnrolment)))
      EssttpBackend.DetermineTaxId.findJourney(Origins.Sa.Bta)(
        JourneyJsonTemplates.`Computed Tax Id`(Origins.Sa.Bta, "1234567895")
      )

      val result = controller.determineTaxId()(fakeRequest)
      status(result) shouldBe SEE_OTHER

      if (redirectLocation(result).contains("/pay-what-you-owe-in-instalments/arrangement/determine-eligibility")) {
        acc + 1
      } else if (redirectLocation(result).contains(routes.DetermineEligibilityController.determineEligibility.url)) {
        acc
      } else {
        fail(s"Found unexpected redirect location: ${redirectLocation(result).toString}")
      }
    }

    redirectToLegacyCount shouldBe 500 +- 10

  }

}
