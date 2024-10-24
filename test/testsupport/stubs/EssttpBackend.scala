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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.crypto.CryptoFormat
import essttp.journey.model._
import essttp.rootmodel._
import essttp.rootmodel.bank.DetailsAboutBankAccount
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.pega.{GetCaseResponse, StartCaseResponse}
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import paymentsEmailVerification.models.EmailVerificationResult
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import testsupport.stubs.WireMockHelpers._
import testsupport.testdata.{JourneyJsonTemplates, TdAll, TdJsonBodies}
import uk.gov.hmrc.crypto.Encrypter

import java.time.Instant

object EssttpBackend {

  private val findByLatestSessionIdUrl: String = "/essttp-backend/journey/find-latest-by-session-id"

  def findByLatestSessionId(jsonBody: String): StubMapping = stubFor(
    get(urlPathEqualTo(findByLatestSessionIdUrl))
      .willReturn(aResponse()
        .withStatus(OK)
        .withBody(jsonBody))
  )

  def findByLatestSessionNotFound(): StubMapping = stubFor(
    get(urlPathEqualTo(findByLatestSessionIdUrl))
      .willReturn(aResponse()
        .withStatus(NOT_FOUND))
  )

  def verifyFindByLatestSessionId(): Unit = verify(getRequestedFor(urlPathEqualTo(findByLatestSessionIdUrl)))

  object BarsVerifyStatusStub {
    private def noLockoutBody(numberOfAttempts: Int) = s"""{
                          |    "attempts": ${numberOfAttempts.toString}
                          |}""".stripMargin

    private def lockoutBody(expiry: Instant) = s"""{
                        |    "attempts": 3,
                        |    "lockoutExpiryDateTime": "${expiry.toString}"
                        |}""".stripMargin

    private val getVerifyStatusUrl: String = "/essttp-backend/bars/verify/status"
    private val updateVerifyStatusUrl: String = "/essttp-backend/bars/verify/update"

    def statusUnlocked(): StubMapping = stubPost(getVerifyStatusUrl, noLockoutBody(numberOfAttempts = 1))

    def statusLocked(expiry: Instant): StubMapping = stubPost(getVerifyStatusUrl, lockoutBody(expiry))

    def update(numberOfAttempts: Int = 1): StubMapping = stubPost(updateVerifyStatusUrl, noLockoutBody(numberOfAttempts))

    def updateAndLockout(expiry: Instant): StubMapping = stubPost(updateVerifyStatusUrl, lockoutBody(expiry))

    def ensureVerifyUpdateStatusIsCalled(): Unit = {
      verify(exactly(1), postRequestedFor(urlPathEqualTo(updateVerifyStatusUrl)))
    }

    def ensureVerifyUpdateStatusIsNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(updateVerifyStatusUrl)))

    private def stubPost(url: String, responseJson: String, status: Int = OK): StubMapping =
      stubForPostWithRequestBodyMatching(url, "$.taxId", responseJson, status)
  }

  object StartJourney {

    private val startJourneyGovUkEpayeUrl = "/essttp-backend/epaye/gov-uk/journey/start"
    private val startJourneyDetachedEpayeUrl = "/essttp-backend/epaye/detached-url/journey/start"
    private val startJourneyGovUkVatUrl = "/essttp-backend/vat/gov-uk/journey/start"
    private val startJourneyDetachedVatUrl = "/essttp-backend/vat/detached-url/journey/start"
    private val startJourneyGovUkSaUrl = "/essttp-backend/sa/gov-uk/journey/start"
    private val startJourneyDetachedSaUrl = "/essttp-backend/sa/detached-url/journey/start"
    private val startJourneyGovUkSiaUrl = "/essttp-backend/sia/gov-uk/journey/start"
    private val startJourneyDetachedSiaUrl = "/essttp-backend/sia/detached-url/journey/start"

    def startJourneyInBackend(origin: Origin): StubMapping = {
      val (url, expectedRequestBody, responseBody): (String, String, String) = origin match {
        case Origins.Epaye.GovUk =>
          (startJourneyGovUkEpayeUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk(TaxRegime.Epaye))
        case Origins.Epaye.DetachedUrl =>
          (startJourneyDetachedEpayeUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl(TaxRegime.Epaye))
        case Origins.Vat.GovUk =>
          (startJourneyGovUkVatUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk(TaxRegime.Vat))
        case Origins.Vat.DetachedUrl =>
          (startJourneyDetachedVatUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl(TaxRegime.Vat))
        case Origins.Sa.GovUk =>
          (startJourneyGovUkSaUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk(TaxRegime.Sa))
        case Origins.Sa.DetachedUrl =>
          (startJourneyDetachedSaUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl(TaxRegime.Sa))
        case Origins.Sia.GovUk =>
          (startJourneyGovUkSiaUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk(TaxRegime.Sia))
        case Origins.Sia.DetachedUrl =>
          (startJourneyDetachedSiaUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl(TaxRegime.Sia))
        case other => throw new Exception(s"Origin ${other.toString} not handled")
      }
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .willReturn(aResponse()
            .withStatus(ACCEPTED)
            .withBody(responseBody))
      )
    }

    def startJourneyEpayeGovUk: StubMapping = startJourneyInBackend(Origins.Epaye.GovUk)
    def startJourneyEpayeDetached: StubMapping = startJourneyInBackend(Origins.Epaye.DetachedUrl)
    def startJourneyVatGovUk: StubMapping = startJourneyInBackend(Origins.Vat.GovUk)
    def startJourneyVatDetached: StubMapping = startJourneyInBackend(Origins.Vat.DetachedUrl)
    def startJourneySaGovUk: StubMapping = startJourneyInBackend(Origins.Sa.GovUk)
    def startJourneySaDetached: StubMapping = startJourneyInBackend(Origins.Sa.DetachedUrl)
    def startJourneySiaGovUk: StubMapping = startJourneyInBackend(Origins.Sia.GovUk)
    def startJourneySiaDetached: StubMapping = startJourneyInBackend(Origins.Sia.DetachedUrl)

    def verifyStartJourney(url: String): Unit = verify(exactly(1), postRequestedFor(urlPathEqualTo(url)))
    def verifyStartJourneyEpayeGovUk(): Unit = verifyStartJourney(startJourneyGovUkEpayeUrl)
    def verifyStartJourneyEpayeDetached(): Unit = verifyStartJourney(startJourneyDetachedEpayeUrl)
    def verifyStartJourneyVatGovUk(): Unit = verifyStartJourney(startJourneyGovUkVatUrl)
    def verifyStartJourneyVatDetached(): Unit = verifyStartJourney(startJourneyDetachedVatUrl)
    def verifyStartJourneySaGovUk(): Unit = verifyStartJourney(startJourneyGovUkSaUrl)
    def verifyStartJourneySaDetached(): Unit = verifyStartJourney(startJourneyDetachedSaUrl)
    def verifyStartJourneySiaGovUk(): Unit = verifyStartJourney(startJourneyGovUkSiaUrl)
    def verifyStartJourneySiaDetached(): Unit = verifyStartJourney(startJourneyDetachedSiaUrl)

    def findJourney(origin: Origin = Origins.Epaye.Bta): StubMapping =
      findByLatestSessionId(JourneyJsonTemplates.Started(origin))
  }

  object DetermineTaxId {
    def findJourney(origin: Origin)(jsonBody: String = {
                                      val taxReference = origin.taxRegime match {
                                        case TaxRegime.Epaye=> "864FZ00049"
                                        case TaxRegime.Vat=> "101747001"
                                        case TaxRegime.Sa=> "1234567895"
                                        case TaxRegime.Sia=> "QQ123456A"
                                      }
                                      JourneyJsonTemplates.`Computed Tax Id`(origin, taxReference)
                                    }): StubMapping = findByLatestSessionId(jsonBody)

    def updateTaxIdUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-tax-id"

    def stubUpdateTaxId(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateTaxIdUrl(journeyId), updatedJourneyJson)

    def verifyTaxIdRequest(journeyId: JourneyId, taxId: TaxId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(updateTaxIdUrl(journeyId)))
          .withRequestBody(equalToJson(Json.toJson(taxId).toString()))
      )
  }

  object EligibilityCheck {
    def updateEligibilityResultUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-eligibility-result"

    def stubUpdateEligibilityResult(journeyId: JourneyId, responseBodyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(
        updateEligibilityResultUrl(journeyId),
        responseBodyJson
      )

    def verifyUpdateEligibilityRequest(
        journeyId:                      JourneyId,
        expectedEligibilityCheckResult: EligibilityCheckResult
    )(implicit cryptoFormat: CryptoFormat): Unit =
      WireMockHelpers.verifyWithBodyParse(updateEligibilityResultUrl(journeyId), expectedEligibilityCheckResult)(EligibilityCheckResult.format)

    def verifyNoneUpdateEligibilityRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(updateEligibilityResultUrl(journeyId))))

    def findJourney(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Eligibility Checked - Eligible`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

    def findJourneyWithNoInterestBearingCharges(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Eligibility Checked - Eligible - NoInterestBearingCharge`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

    def findJourneyWithDdInProgress(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Eligibility Checked - Eligible- ddInProgress`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object WhyCannotPayInFull {
    def updateWhyCannotPayInFullUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-why-cannot-pay-in-full"

    def stubUpdateWhyCannotPayInFull(journeyId: JourneyId, whyCannotPayInFull: WhyCannotPayInFullAnswers, updatedJourneyJson: String): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateWhyCannotPayInFullUrl(journeyId)))
          .withRequestBody(equalTo(Json.toJson(whyCannotPayInFull).toString))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(updatedJourneyJson)
          )
      )

    def verifyUpdateWhyCannotPayInFullRequest(journeyId: JourneyId, expectedWhyCannotPayInFull: WhyCannotPayInFullAnswers): Unit =
      WireMockHelpers.verifyWithBodyParse(updateWhyCannotPayInFullUrl(journeyId), expectedWhyCannotPayInFull)

    def verifyNoneUpdateWhyCannotPayInFullRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateWhyCannotPayInFullUrl(journeyId)))
      )

    def findJourney(
        encrypter: Encrypter,
        origin:    Origin    = Origins.Epaye.Bta
    )(jsonBody: String = JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

  }

  object CanPayUpfront {
    def updateCanPayUpfrontUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-can-pay-upfront"

    def stubUpdateCanPayUpfront(journeyId: JourneyId, canPayUpfrontScenario: Boolean, updatedJourneyJson: String): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId)))
          .withRequestBody(equalTo(canPayUpfrontScenario.toString))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(updatedJourneyJson)
          )
      )

    def verifyUpdateCanPayUpfrontRequest(journeyId: JourneyId, expectedCanPayUpFront: CanPayUpfront): Unit =
      WireMockHelpers.verifyWithBodyParse(updateCanPayUpfrontUrl(journeyId), expectedCanPayUpFront)(essttp.rootmodel.CanPayUpfront.format)

    def verifyNoneUpdateCanPayUpfrontRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Answered Can Pay Upfront - Yes`(origin)(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
  }

  object UpfrontPaymentAmount {
    def updateUpfrontPaymentAmountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-upfront-payment-amount"

    def stubUpdateUpfrontPaymentAmount(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateUpfrontPaymentAmountUrl(journeyId), updatedJourneyJson)

    def verifyUpdateUpfrontPaymentAmountRequest(journeyId: JourneyId, expectedUpfrontPaymentAmount: UpfrontPaymentAmount): Unit =
      WireMockHelpers.verifyWithBodyParse(updateUpfrontPaymentAmountUrl(journeyId), expectedUpfrontPaymentAmount)(essttp.rootmodel.UpfrontPaymentAmount.format)

    def verifyNoneUpdateUpfrontPaymentAmountRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateUpfrontPaymentAmountUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Entered Upfront payment amount`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object Dates {
    def updateExtremeDatesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-extreme-dates"

    def updateStartDatesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-start-dates"

    def stubUpdateExtremeDates(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateExtremeDatesUrl(journeyId), updatedJourneyJson)

    def stubUpdateStartDates(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateStartDatesUrl(journeyId), updatedJourneyJson)

    def verifyUpdateExtremeDates(journeyId: JourneyId, expectedExtremeDatesResponse: ExtremeDatesResponse): Unit =
      WireMockHelpers.verifyWithBodyParse(updateExtremeDatesUrl(journeyId), expectedExtremeDatesResponse)(ExtremeDatesResponse.format)

    def verifyNoneUpdateExtremeDates(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateExtremeDatesUrl(journeyId)))
      )

    def verifyUpdateStartDates(journeyId: JourneyId, expectedStartDatesResponse: StartDatesResponse): Unit =
      WireMockHelpers.verifyWithBodyParse(updateStartDatesUrl(journeyId), expectedStartDatesResponse)(StartDatesResponse.format)

    def verifyNoneUpdateStartDates(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateStartDatesUrl(journeyId)))
      )

    def findJourneyExtremeDates(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Retrieved Extreme Dates Response`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

    def findJourneyStartDates(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Retrieved Start Dates`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object AffordabilityMinMaxApi {
    def findJourney(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Retrieved Affordability`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

    def updateAffordabilityUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-affordability-result"

    def stubUpdateAffordability(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateAffordabilityUrl(journeyId), updatedJourneyJson)

    def verifyUpdateAffordabilityRequest(journeyId: JourneyId, expectedInstalmentAmounts: InstalmentAmounts): Unit =
      WireMockHelpers.verifyWithBodyParse(updateAffordabilityUrl(journeyId), expectedInstalmentAmounts)(InstalmentAmounts.format)

    def verifyNoneUpdateAffordabilityRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateAffordabilityUrl(journeyId)))
      )
  }

  object CanPayWithinSixMonths {
    def updateCanPayWithinSixMonthsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-can-pay-within-six-months"

    def stubUpdateCanPayWithinSixMonths(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateCanPayWithinSixMonthsUrl(journeyId), updatedJourneyJson)

    def verifyUpdateCanPayWithinSixMonthsRequest(journeyId: JourneyId, expectedCanPayWithinSixMonths: CanPayWithinSixMonthsAnswers): Unit =
      WireMockHelpers.verifyWithBodyParse(updateCanPayWithinSixMonthsUrl(journeyId), expectedCanPayWithinSixMonths)

    def verifyNoneUpdateCanPayWithinSixMonthsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateCanPayWithinSixMonthsUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Obtained Can Pay Within 6 months - not required`(origin)(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
  }

  object StartedPegaCase {
    private def updateStartPegaCaseResponseUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-pega-start-case-response"

    def stubUpdateStartPegaCaseResponse(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateStartPegaCaseResponseUrl(journeyId), updatedJourneyJson)

    def verifyUpdateStartPegaCaseResponseRequest(journeyId: JourneyId, expectedResponse: StartCaseResponse): Unit =
      WireMockHelpers.verifyWithBodyParse(updateStartPegaCaseResponseUrl(journeyId), expectedResponse)

    def findJourney(
        encrypter:                 Encrypter,
        origin:                    Origin,
        whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.AnswerNotRequired
    )(jsonBody: String = JourneyJsonTemplates.`Started PEGA case`(origin, whyCannotPayInFullAnswers)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object MonthlyPaymentAmount {
    def monthlyPaymentAmountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-monthly-payment-amount"

    def stubUpdateMonthlyPaymentAmount(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(monthlyPaymentAmountUrl(journeyId), updatedJourneyJson)

    def verifyUpdateMonthlyPaymentAmountRequest(journeyId: JourneyId, expectedMonthlyPaymentAmount: MonthlyPaymentAmount): Unit =
      WireMockHelpers.verifyWithBodyParse(monthlyPaymentAmountUrl(journeyId), expectedMonthlyPaymentAmount)(essttp.rootmodel.MonthlyPaymentAmount.format)

    def verifyNoneUpdateMonthlyAmountRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(monthlyPaymentAmountUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Entered Monthly Payment Amount`(origin)(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
  }

  object DayOfMonth {
    def dayOfMonthUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-day-of-month"

    def stubUpdateDayOfMonth(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(dayOfMonthUrl(journeyId), updatedJourneyJson)

    def verifyUpdateDayOfMonthRequest(journeyId: JourneyId, dayOfMonth: DayOfMonth = TdAll.dayOfMonth()): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId))).withRequestBody(equalToJson(s"""${dayOfMonth.value.toString}"""))
      )

    def verifyNoneUpdateDayOfMonthRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId)))
      )

    def findJourney(
        dayOfMonth: DayOfMonth,
        encrypter:  Encrypter,
        origin:     Origin
    )(jsonBody: String = JourneyJsonTemplates.`Entered Day of Month`(dayOfMonth, origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object AffordableQuotes {
    def affordableQuotesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-affordable-quotes"

    def stubUpdateAffordableQuotes(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(affordableQuotesUrl(journeyId), updatedJourneyJson)

    def verifyUpdateAffordableQuotesRequest(journeyId: JourneyId): Unit =
      WireMockHelpers.verifyWithBodyParse(affordableQuotesUrl(journeyId))(AffordableQuotesResponse.format)

    def verifyNoneUpdateAffordableQuotesRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(affordableQuotesUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Retrieved Affordable Quotes`(origin)(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
  }

  object SelectedPaymentPlan {
    def selectedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-selected-plan"

    def stubUpdateSelectedPlan(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(selectedPlanUrl(journeyId), updatedJourneyJson)

    def verifyUpdateSelectedPlanRequest(journeyId: JourneyId): Unit =
      WireMockHelpers.verifyWithBodyParse(selectedPlanUrl(journeyId))(PaymentPlan.format)

    def verifyNoneUpdateSelectedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(selectedPlanUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin)(
        jsonBody: String = JourneyJsonTemplates.`Chosen Payment Plan`(
          upfrontPaymentAmountJsonString = """{"DeclaredUpfrontPayment": {"amount": 200}}""",
          origin                         = origin
        )(encrypter)
    ): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object HasCheckedPlan {
    def hasCheckedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-checked-plan"

    def stubUpdateHasCheckedPlan(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(hasCheckedPlanUrl(journeyId), updatedJourneyJson)

    def verifyUpdateHasCheckedPlanRequest(journeyId: JourneyId): Unit =
      WireMockHelpers.verifyWithBodyParse(hasCheckedPlanUrl(journeyId))(PaymentPlanAnswers.format)

    def findJourney(
        withAffordability:         Boolean,
        encrypter:                 Encrypter,
        origin:                    Origin                    = Origins.Epaye.Bta,
        eligibilityMinPlanLength:  Int                       = 1,
        eligibilityMaxPlanLength:  Int                       = 12,
        whyCannotPayInFullAnswers: WhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.AnswerNotRequired
    )(
        jsonBody: String = if (withAffordability) {
          JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(origin, eligibilityMinPlanLength, eligibilityMaxPlanLength, whyCannotPayInFullAnswers)(encrypter)
        } else {
          JourneyJsonTemplates.`Has Checked Payment Plan - No Affordability`(origin, eligibilityMinPlanLength, eligibilityMaxPlanLength, whyCannotPayInFullAnswers)(encrypter)
        }
    ): StubMapping = findByLatestSessionId(jsonBody)
  }

  object EnteredDetailsAboutBankAccount {
    def enterDetailsAboutBankAccountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-details-about-bank-account"

    def stubUpdateEnteredDetailsAboutBankAccount(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(enterDetailsAboutBankAccountUrl(journeyId), updatedJourneyJson)

    def verifyUpdateEnteredDetailsAboutBankAccountRequest(journeyId: JourneyId, expectedDetailsAboutBankAccount: DetailsAboutBankAccount): Unit =
      WireMockHelpers.verifyWithBodyParse(enterDetailsAboutBankAccountUrl(journeyId), expectedDetailsAboutBankAccount)(DetailsAboutBankAccount.format)

    def verifyNoneUpdateEnteredDetailsAboutBankAccountRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(enterDetailsAboutBankAccountUrl(journeyId))))

    def findJourney(
        encrypter: Encrypter, origin: Origin
    )(
        jsonBody: String = JourneyJsonTemplates.`Entered Details About Bank Account`(isAccountHolder = true, origin)(encrypter)
    ): StubMapping = {
      findByLatestSessionId(jsonBody)
    }
  }

  object DirectDebitDetails {
    def directDebitDetailsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-direct-debit-details"

    def stubUpdateDirectDebitDetails(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(directDebitDetailsUrl(journeyId), updatedJourneyJson)

    def verifyUpdateDirectDebitDetailsRequest(
        journeyId:                  JourneyId,
        expectedDirectDebitDetails: essttp.rootmodel.bank.BankDetails
    )(implicit cryptoFormat: CryptoFormat): Unit =
      WireMockHelpers.verifyWithBodyParse(directDebitDetailsUrl(journeyId), expectedDirectDebitDetails)(essttp.rootmodel.bank.BankDetails.format)

    def verifyNoneUpdateDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(directDebitDetailsUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Entered Direct Debit Details`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object ConfirmedDirectDebitDetails {
    def confirmDirectDebitDetailsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-confirmed-direct-debit-details"

    def stubUpdateConfirmDirectDebitDetails(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(confirmDirectDebitDetailsUrl(journeyId), updatedJourneyJson)

    def verifyUpdateConfirmDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
      )

    def verifyNoneUpdateConfirmDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter, origin: Origin)(jsonBody: String = JourneyJsonTemplates.`Confirmed Direct Debit Details`(origin)(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
  }

  object TermsAndConditions {
    def agreedTermsAndConditionsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-agreed-terms-and-conditions"

    def stubUpdateAgreedTermsAndConditions(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(agreedTermsAndConditionsUrl(journeyId), updatedJourneyJson)

    def verifyUpdateAgreedTermsAndConditionsRequest(journeyId: JourneyId, isEmailAddressRequired: IsEmailAddressRequired): Unit =
      WireMockHelpers.verifyWithBodyParse(
        agreedTermsAndConditionsUrl(journeyId),
        IsEmailAddressRequired(isEmailAddressRequired)
      )

    def verifyNoneUpdateAgreedTermsAndConditionsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(agreedTermsAndConditionsUrl(journeyId)))
      )

    def findJourney(
        isEmailAddressRequired: Boolean,
        encrypter:              Encrypter,
        origin:                 Origin,
        etmpEmail:              Option[String],
        withAffordability:      Boolean        = false
    )(jsonBody: String = JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddressRequired, origin, etmpEmail, withAffordability)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object SelectEmail {
    def selectEmailUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-chosen-email"

    def stubUpdateSelectedEmail(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(selectEmailUrl(journeyId), updatedJourneyJson)

    def verifyUpdateSelectedEmailRequest(journeyId: JourneyId, email: Email)(implicit cryptoFormat: CryptoFormat): Unit =
      WireMockHelpers.verifyWithBodyParse(selectEmailUrl(journeyId), email)

    def verifyNoneUpdateSelectedEmailRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(selectEmailUrl(journeyId))))

    def findJourney(
        email:     String,
        encrypter: Encrypter,
        origin:    Origin,
        etmpEmail: Option[String] = Some(TdAll.etmpEmail)
    )(jsonBody: String = JourneyJsonTemplates.`Selected email to be verified`(email, origin, etmpEmail)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

  }

  object EmailVerificationResult {
    def updateEmailVerificationResultUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-email-verification-status"

    def stubUpdateEmailVerificationResult(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateEmailVerificationResultUrl(journeyId), updatedJourneyJson)

    def verifyUpdateEmailVerificationResultRequest(journeyId: JourneyId, status: EmailVerificationResult): Unit =
      WireMockHelpers.verifyWithBodyParse(updateEmailVerificationResultUrl(journeyId), status)

    def findJourney(
        email:     String,
        status:    EmailVerificationResult,
        encrypter: Encrypter,
        origin:    Origin
    )(jsonBody: String = JourneyJsonTemplates.`Email verification complete`(email, status, origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object SubmitArrangement {
    def submitArrangementUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-arrangement"

    def stubUpdateSubmitArrangement(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(submitArrangementUrl(journeyId), updatedJourneyJson)

    def verifyUpdateSubmitArrangementRequest(journeyId: JourneyId, expectedArrangementResponse: ArrangementResponse): Unit =
      WireMockHelpers.verifyWithBodyParse(submitArrangementUrl(journeyId), expectedArrangementResponse)

    def verifyNoneUpdateSubmitArrangementRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(submitArrangementUrl(journeyId))))

    def findJourney(
        origin:            Origin,
        encrypter:         Encrypter,
        withAffordability: Boolean   = false
    )(
        jsonBody: String = JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`(origin, withAffordability)(encrypter)
    ): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object Pega {

    type HttpStatus = Int

    private def caseUrl(journeyId: JourneyId) = s"/essttp-backend/pega/case/${journeyId.value}"

    private def saveJourneyUrl(journeyId: JourneyId): String = s"/essttp-backend/pega/journey/${journeyId.value}"

    private def recreatedSessionUrl(taxRegime: TaxRegime): String = {
      val regime = taxRegime match {
        case TaxRegime.Epaye => "epaye"
        case TaxRegime.Vat   => "vat"
        case TaxRegime.Sa    => "sa"
        case TaxRegime.Sia   => "sia"
      }

      s"/essttp-backend/pega/recreate-session/$regime"
    }

    def stubStartCase(journeyId: JourneyId, result: Either[HttpStatus, StartCaseResponse]): StubMapping =
      stubFor(
        post(caseUrl(journeyId))
          .willReturn(
            result.fold(
              aResponse().withStatus(_),
              response => aResponse().withStatus(CREATED).withBody(
                s"""{
                   |  "caseId": "${response.caseId.value}",
                   |  "assignmentId": "${response.assignmentId.value}"
                   |}
                   |""".stripMargin
              )
            )
          )
      )

    def stubGetCase(journeyId: JourneyId, result: Either[HttpStatus, GetCaseResponse]): StubMapping =
      stubFor(
        get(caseUrl(journeyId))
          .willReturn(
            result.fold(
              aResponse().withStatus(_),
              response => aResponse().withStatus(CREATED).withBody(
                Json.toJson(response).toString
              )
            )
          )
      )

    def stubSaveJourneyForPega(journeyId: JourneyId, result: Either[HttpStatus, Unit]): StubMapping =
      stubFor(
        post(saveJourneyUrl(journeyId))
          .willReturn(
            result.fold(
              aResponse().withStatus(_),
              _ => aResponse().withStatus(OK)
            )
          )
      )

    def stubRecreateSession(taxRegime: TaxRegime, result: Either[HttpStatus, JsValue]): StubMapping =
      stubFor(
        get(recreatedSessionUrl(taxRegime))
          .willReturn(
            result.fold(
              aResponse().withStatus(_),
              response => aResponse().withStatus(OK).withBody(response.toString())
            )
          )
      )

    def verifyStartCaseCalled(journeyId: JourneyId): Unit =
      verify(exactly(1), postRequestedFor(urlPathEqualTo(caseUrl(journeyId))))

    def verifyStartCaseNotCalled(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(caseUrl(journeyId))))

    def verifyGetCaseCalled(journeyId: JourneyId): Unit =
      verify(exactly(1), getRequestedFor(urlPathEqualTo(caseUrl(journeyId))))

    def verifySaveJourneyForPegaCalled(journeyId: JourneyId): Unit =
      verify(exactly(1), postRequestedFor(urlPathEqualTo(saveJourneyUrl(journeyId))))

    def verifySaveJourneyForPegaNotCalled(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(saveJourneyUrl(journeyId))))

    def verifyRecreateSessionCalled(taxRegime: TaxRegime): Unit =
      verify(exactly(1), getRequestedFor(urlPathEqualTo(recreatedSessionUrl(taxRegime))))

    def verifyRecreateSessionNotCalled(taxRegime: TaxRegime): Unit =
      verify(exactly(0), getRequestedFor(urlPathEqualTo(recreatedSessionUrl(taxRegime))))

  }
}
