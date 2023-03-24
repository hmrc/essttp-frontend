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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.crypto.CryptoFormat
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.EmailSource
import paymentsEmailVerification.models.EmailVerificationResult
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuditConnectorStub, EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}

import java.util.Locale
import scala.concurrent.Future

class SubmitArrangementControllerSpec extends ItSpec {

  private val controller: SubmitArrangementController = app.injector.instanceOf[SubmitArrangementController]

  "GET /submit-arrangement should" - {

    List[(TaxRegime, Origin)](
      TaxRegime.Epaye -> Origins.Epaye.Bta,
      TaxRegime.Vat -> Origins.Vat.Bta
    ).foreach {
        case (taxRegime, origin) =>

          List(
            (
              "T&C's accepted, no email required",
              () => EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = false, testCrypto, origin, etmpEmail = Some(TdAll.etmpEmail))(),
              None, None
            ),
            (
              "email verification success - same email as ETMP",
              () => EssttpBackend.EmailVerificationResult.findJourney(
                "bobross@joyofpainting.com",
                EmailVerificationResult.Verified,
                testCrypto,
                origin
              )(),
              Some("bobross@joyofpainting.com"), Some(EmailSource.ETMP)
            ),
            (
              "email verification success - new email",
              () => EssttpBackend.EmailVerificationResult.findJourney(
                "grogu@mandalorian.com",
                EmailVerificationResult.Verified,
                testCrypto,
                origin
              )(),
              Some("grogu@mandalorian.com"), Some(EmailSource.TEMP)
            ),
            (
              "email verification success - ETMP - same email with different casing",
              () => EssttpBackend.EmailVerificationResult.findJourney(
                "BobRoss@joyofpainting.com",
                EmailVerificationResult.Verified,
                testCrypto,
                origin
              )(),
              Some("BobRoss@joyofpainting.com"), Some(EmailSource.ETMP)
            )
          ).foreach {
              case (journeyDescription, journeyStubMapping, expectedEmail, expectedEmailSource) =>
                s"[taxRegime: ${taxRegime.toString}] trigger call to ttp enact arrangement api, send an audit event " +
                  s"and also update backend for $journeyDescription" in {
                    stubCommonActions()
                    journeyStubMapping()
                    EssttpBackend.SubmitArrangement.stubUpdateSubmitArrangement(
                      TdAll.journeyId,
                      JourneyJsonTemplates.`Arrangement Submitted - with upfront payment and email`("bobross@joyofpainting.com", origin)
                    )
                    Ttp.EnactArrangement.stubEnactArrangement(taxRegime)()

                    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

                    val result: Future[Result] = controller.submitArrangement(fakeRequest)
                    status(result) shouldBe Status.SEE_OTHER
                    redirectLocation(result) shouldBe Some(taxRegime match {
                      case TaxRegime.Epaye => PageUrls.epayeConfirmationUrl
                      case TaxRegime.Vat   => PageUrls.vatConfirmationUrl
                    })

                    Ttp.EnactArrangement.verifyTtpEnactArrangementRequest(
                      TdAll.customerDetail(expectedEmail.getOrElse(TdAll.etmpEmail).toLowerCase(Locale.UK), expectedEmailSource.getOrElse(EmailSource.ETMP)),
                      TdAll.someRegimeDigitalCorrespondenceTrue,
                      taxRegime
                    )(CryptoFormat.NoOpCryptoFormat)

                    val taxType = taxRegime match {
                      case TaxRegime.Epaye => "Epaye"
                      case TaxRegime.Vat   => "Vat"
                    }

                    AuditConnectorStub.verifyEventAudited(
                      "PlanSetUp",
                      Json.parse(
                        s"""
                     |{
                     |	"bankDetails": {
                     |		"name": "${TdAll.testAccountName}",
                     |		"sortCode": "123456",
                     |		"accountNumber": "12345678"
                     |	},
                     |	"schedule": {
                     |		"initialPaymentAmount": 123.12,
                     |		"collectionDate": 28,
                     |		"collectionLengthCalendarMonths": 2,
                     |		"collections": [{
                     |			"collectionNumber": 2,
                     |			"amount": 555.70,
                     |			"paymentDate": "2022-09-28"
                     |		}, {
                     |			"collectionNumber": 1,
                     |			"amount": 555.70,
                     |			"paymentDate": "2022-08-28"
                     |		}],
                     |		"totalNoPayments": 3,
                     |		"totalInterestCharged": 0.06,
                     |		"totalPayable": 1111.47,
                     |		"totalPaymentWithoutInterest": 1111.41
                     |	},
                     |	"status": "successfully sent to TTP",
                     |	"failedSubmissionReason": 202,
                     |	"origin": "Bta",
                     |	"taxType": "$taxType",
                     |	"taxDetail": ${TdAll.taxDetailJsonString(taxRegime)},
                     |	"correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                     |	"ppReferenceNo": "${TdAll.customerReference(taxRegime).value}",
                     |	"authProviderId": "authId-999",
                     |  ${expectedEmail.fold("")(email => s""" "emailAddress":"$email", """)}
                     |  ${expectedEmailSource.fold("")(source => s""" "emailSource":"${source.value}", """)}
                     |  "regimeDigitalCorrespondence": true
                     |}
                     |""".stripMargin
                      ).as[JsObject]
                    )
                    EssttpBackend.SubmitArrangement.verifyUpdateSubmitArrangementRequest(TdAll.journeyId, TdAll.arrangementResponse(taxRegime))
                  }
            }
      }

    "not allow journeys when" - {

        def test(
            journeyStubMapping:       () => StubMapping,
            expectedRedirectLocation: Call
        ) = {

          stubCommonActions()
          journeyStubMapping()

          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
          val result = controller.submitArrangement(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirectLocation.url)
        }

      "T&C's have not been agreed yet" in {
        test(
          () => EssttpBackend.CanPayUpfront.findJourney(testCrypto)(),
          routes.UpfrontPaymentController.upfrontPaymentAmount
        )
      }

      "T&C's have just been agreed but an email is required" in {
        test(
          () => EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, testCrypto, Origins.Epaye.Bta, etmpEmail = Some(TdAll.etmpEmail))(),
          routes.EmailController.whichEmailDoYouWantToUse
        )
      }

      "the user has just selected an email address" in {
        test(
          () => EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto, Origins.Vat.GovUk)(),
          routes.EmailController.requestVerification
        )
      }

      "there is an email verification status of locked" in {
        test(
          () => EssttpBackend.EmailVerificationResult.findJourney(
            "bobross@joyofpainting.com",
            EmailVerificationResult.Locked,
            testCrypto,
            Origins.Vat.Bta
          )(),
          routes.EmailController.tooManyPasscodeAttempts
        )

      }

    }

    "should not update backend if call to ttp enact arrangement api fails (anything other than a 202 response)" in {
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = false, testCrypto, origin = Origins.Epaye.Bta, etmpEmail = Some(TdAll.etmpEmail))()
      Ttp.EnactArrangement.stubEnactArrangementFail()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.submitArrangement(fakeRequest)
      assertThrows[UpstreamErrorResponse](await(result))
      Ttp.EnactArrangement.verifyTtpEnactArrangementRequest(
        TdAll.customerDetail(),
        TdAll.someRegimeDigitalCorrespondenceTrue,
        TaxRegime.Epaye
      )(CryptoFormat.NoOpCryptoFormat)
      EssttpBackend.SubmitArrangement.verifyNoneUpdateSubmitArrangementRequest(TdAll.journeyId)
    }

  }

}
