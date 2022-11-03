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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.crypto.CryptoFormat
import essttp.emailverification.EmailVerificationStatus
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

import scala.concurrent.Future

class SubmitArrangementControllerSpec extends ItSpec {

  private val controller: SubmitArrangementController = app.injector.instanceOf[SubmitArrangementController]

  "GET /submit-arrangement should" - {

    "trigger call to ttp enact arrangement api, send an audit event and also update backend" in {
      List(
        "T&C's accepted, no email required" -> (
          () => EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = false, testCrypto)()
        ),
        "email verification success" -> (
          () => EssttpBackend.EmailVerificationStatus.findJourney(
            "bobross@joyofpainting.com",
            EmailVerificationStatus.Verified,
            testCrypto
          )()
        )
      ).foreach{
          case (journeyDescription, journeyStubMapping) =>
            withClue(s"For journey $journeyDescription: "){

              stubCommonActions()
              journeyStubMapping()
              EssttpBackend.SubmitArrangement.stubUpdateSubmitArrangement(
                TdAll.journeyId,
                JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`
              )
              Ttp.EnactArrangement.stubEnactArrangement()

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

              val result: Future[Result] = controller.submitArrangement(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(PageUrls.confirmationUrl)

              Ttp.EnactArrangement.verifyTtpEnactArrangementRequest(CryptoFormat.NoOpCryptoFormat)
              AuditConnectorStub.verifyEventAudited(
                "PlanSetUp",
                Json.parse(
                  s"""
             |{
             |	"bankDetails": {
             |		"name": "Bob Ross",
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
             |	"taxType": "Epaye",
             |	"taxDetail": {
             |		"employerRef": "864FZ00049",
             |		"accountsOfficeRef": "123PA44545546"
             |	},
             |	"correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
             |	"ppReferenceNo": "123PA44545546",
             |	"authProviderId": "authId-999"
             |}
             |""".stripMargin
                ).as[JsObject]
              )
              EssttpBackend.SubmitArrangement.verifyUpdateSubmitArrangementRequest(TdAll.journeyId, TdAll.arrangementResponse)
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
          () => EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, testCrypto)(),
          routes.EmailController.whichEmailDoYouWantToUse
        )
      }

      "the user has just selected an email address" in {
        test(
          () => EssttpBackend.SelectEmail.findJourney("email@test.com", testCrypto)(),
          routes.EmailController.requestVerification
        )
      }

      "there is an email verification status of locked" in {
        test(
          () => EssttpBackend.EmailVerificationStatus.findJourney(
            "bobross@joyofpainting.com",
            EmailVerificationStatus.Locked,
            testCrypto
          )(),
          routes.EmailController.tooManyPasscodeAttempts
        )

      }

    }

    "should not update backend if call to ttp enact arrangement api fails (anything other than a 202 response)" in {
      stubCommonActions()
      EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = false, testCrypto)()
      Ttp.EnactArrangement.stubEnactArrangementFail()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.submitArrangement(fakeRequest)
      assertThrows[UpstreamErrorResponse](await(result))
      Ttp.EnactArrangement.verifyTtpEnactArrangementRequest(CryptoFormat.NoOpCryptoFormat)
      EssttpBackend.SubmitArrangement.verifyNoneUpdateSubmitArrangementRequest(TdAll.journeyId)
    }

  }

}
