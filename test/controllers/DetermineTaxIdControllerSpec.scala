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

import actions.EnrolmentDef
import essttp.rootmodel.EmpRef
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.SessionKeys

class DetermineTaxIdControllerSpec extends ItSpec {

  val controller: DetermineTaxIdController = app.injector.instanceOf[DetermineTaxIdController]

  "Determine tax id controller endpoint should redirect properly" - {

      def enrolment(key: String, activated: Boolean)(idKeyWithValue: (String, String)*) =
        Enrolment(key, idKeyWithValue.map(EnrolmentIdentifier.tupled), if (activated) "activated" else "other")

    "for EPAYE when" - {

      "the tax id has already been determined" in {
        stubCommonActions()
        EssttpBackend.DetermineTaxId.findJourney()

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)
      }

      "the relevant enrolment is found with the necessary identifiers and the enrolment is active" in {
        val enrolments =
          Set(
            enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref",
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeNumber`.identifierKey -> "Number"
            )
          )

        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney()
        EssttpBackend.DetermineTaxId.stubUpdateTaxId(TdAll.journeyId)

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
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
              EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeNumber`.identifierKey -> "Number"
            )
          )

        stubCommonActions(authAllEnrolments = Some(enrolments))
        EssttpBackend.StartJourney.findJourney()

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotEnrolledController.notEnrolled.url)
        AuditConnectorStub.verifyEventAudited(
          "EligibilityCheck",
          Json.parse(
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
               |""".
              stripMargin
          ).as[JsObject]
        )
      }

      "the relevant enrolment is found but the correct identifiers could not be found" in {
        List(
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(
            EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.identifierKey -> "Ref"
          ),
          enrolment(EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeReference`.enrolmentKey, activated = true)(
            EnrolmentDef.Epaye.`IR-PAYE-TaxOfficeNumber`.identifierKey -> "Number"
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
            EssttpBackend.DetermineTaxId.stubUpdateTaxId(TdAll.journeyId)

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            a[RuntimeException] shouldBe thrownBy(await(controller.determineTaxId()(fakeRequest)))
          }

      }

      "the relevant enrolment is not found" in {
        stubCommonActions(authAllEnrolments = Some(Set.empty))
        EssttpBackend.StartJourney.findJourney()

        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
        val result = controller.determineTaxId()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NotEnrolledController.notEnrolled.url)
        AuditConnectorStub.verifyEventAudited(
          "EligibilityCheck",
          Json.parse(
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
               |  "chargeTypeAssessment" : []
               |}
               |""".
              stripMargin
          ).as[JsObject]
        )
      }

    }
  }
}
