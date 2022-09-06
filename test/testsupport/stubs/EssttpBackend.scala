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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.journey.model.{JourneyId, Origin, Origins}
import essttp.rootmodel.bank.TypeOfBankAccount
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.ttp.EligibilityCheckResult
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.{CanPayUpfront, DayOfMonth, MonthlyPaymentAmount, TaxId, UpfrontPaymentAmount}
import play.api.libs.json.Json
import testsupport.testdata.{JourneyJsonTemplates, TdAll, TdJsonBodies}
import wiremock.org.apache.http.HttpStatus._
import java.time.Instant

object EssttpBackend {

  private val findByLatestSessionIdUrl: String = "/essttp-backend/journey/find-latest-by-session-id"

  def findByLatestSessionId(jsonBody: String): StubMapping = stubFor(
    get(urlPathEqualTo(findByLatestSessionIdUrl))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody))
  )

  def verifyFindByLatestSessionId(): Unit = verify(postRequestedFor(urlPathEqualTo(findByLatestSessionIdUrl)))

  object BarsVerifyStatusStub {
    private val noLockoutBody = """{
                          |    "attempts": 4
                          |}""".stripMargin

    private def lockoutBody(expiry: Instant) = s"""{
                        |    "attempts": 4,
                        |    "lockoutExpiryDateTime": "${expiry.toString}"
                        |}""".stripMargin

    private val getVerifyStatusUrl: String = "/essttp-backend/bars/verify/status"
    private val updateVerifyStatusUrl: String = "/essttp-backend/bars/verify/update"

    def statusUnlocked(): StubMapping = stubPost(getVerifyStatusUrl, SC_OK, noLockoutBody)

    def update(): StubMapping = stubPost(updateVerifyStatusUrl, SC_OK, noLockoutBody)

    def updateAndLockout(expiry: Instant): StubMapping = stubPost(updateVerifyStatusUrl, SC_OK, lockoutBody(expiry))

    def ensureVerifyUpdateStatusIsCalled(): Unit = {
      verify(exactly(1), postRequestedFor(urlPathEqualTo(updateVerifyStatusUrl)))
    }

    def ensureVerifyUpdateStatusIsNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(updateVerifyStatusUrl)))

    private def stubPost(url: String, status: Int, responseJson: String): StubMapping = {
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(matchingJsonPath("$.taxId"))
          .willReturn(
            aResponse()
              .withStatus(status)
              .withBody(responseJson)
          )
      )
    }
  }

  object StartJourney {
    private val startJourneyBtaUrl = "/essttp-backend/epaye/bta/journey/start"
    private val startJourneyGovUkUrl = "/essttp-backend/epaye/gov-uk/journey/start"
    private val startJourneyDetachedUrl = "/essttp-backend/epaye/detached-url/journey/start"

    def startJourneyInBackend(origin: Origin): StubMapping = {
      val (url, expectedRequestBody, responseBody): (String, String, String) = origin match {
        case Origins.Epaye.Bta         => (startJourneyBtaUrl, TdJsonBodies.StartJourneyRequestBodies.simple, TdJsonBodies.StartJourneyResponses.bta)
        case Origins.Epaye.GovUk       => (startJourneyGovUkUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk)
        case Origins.Epaye.DetachedUrl => (startJourneyDetachedUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl)
      }
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .willReturn(aResponse()
            .withStatus(201)
            .withBody(responseBody))
      )
    }

    def startJourneyEpayeBta: StubMapping = startJourneyInBackend(Origins.Epaye.Bta)
    def startJourneyEpayeGovUk: StubMapping = startJourneyInBackend(Origins.Epaye.GovUk)
    def startJourneyEpayeDetached: StubMapping = startJourneyInBackend(Origins.Epaye.DetachedUrl)

    def verifyStartJourney(url: String): Unit = verify(exactly(1), postRequestedFor(urlPathEqualTo(url)))
    def verifyStartJourneyEpayeBta(): Unit = verifyStartJourney(startJourneyBtaUrl)
    def verifyStartJourneyEpayeGovUk(): Unit = verifyStartJourney(startJourneyGovUkUrl)
    def verifyStartJourneyEpayeDetached(): Unit = verifyStartJourney(startJourneyDetachedUrl)

    def findJourney(jsonBody: String = JourneyJsonTemplates.Started): StubMapping = findByLatestSessionId(jsonBody)
  }

  object DetermineTaxId {
    def findJourney(jsonBody: String = JourneyJsonTemplates.`Computed Tax Id`): StubMapping = findByLatestSessionId(jsonBody)

    def updateTaxIdUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-tax-id"

    def stubUpdateTaxId(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(updateTaxIdUrl(journeyId))

    def verifyTaxIdRequest(journeyId: JourneyId, taxId: TaxId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(updateTaxIdUrl(journeyId)))
          .withRequestBody(equalToJson(Json.toJson(taxId).toString()))
      )
  }

  object EligibilityCheck {
    def updateEligibilityResultUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-eligibility-result"

    def stubUpdateEligibilityResult(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(updateEligibilityResultUrl(journeyId))

    def verifyUpdateEligibilityRequest(journeyId: JourneyId, expectedEligibilityCheckResult: EligibilityCheckResult): Unit =
      WireMockHelpers.verifyWithBodyParse(updateEligibilityResultUrl(journeyId), expectedEligibilityCheckResult)(EligibilityCheckResult.format)

    def verifyNoneUpdateEligibilityRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(updateEligibilityResultUrl(journeyId))))

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Eligibility Checked - Eligible`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object CanPayUpfront {
    def updateCanPayUpfrontUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-can-pay-upfront"

    def stubUpdateCanPayUpfront(journeyId: JourneyId, canPayUpfrontScenario: Boolean): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId)))
          .withRequestBody(equalTo(canPayUpfrontScenario.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateCanPayUpfrontRequest(journeyId: JourneyId, expectedCanPayUpFront: CanPayUpfront): Unit =
      WireMockHelpers.verifyWithBodyParse(updateCanPayUpfrontUrl(journeyId), expectedCanPayUpFront)(essttp.rootmodel.CanPayUpfront.format)

    def verifyNoneUpdateCanPayUpfrontRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Answered Can Pay Upfront - Yes`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object UpfrontPaymentAmount {
    def updateUpfrontPaymentAmountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-upfront-payment-amount"

    def stubUpdateUpfrontPaymentAmount(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(updateUpfrontPaymentAmountUrl(journeyId))

    def verifyUpdateUpfrontPaymentAmountRequest(journeyId: JourneyId, expectedUpfrontPaymentAmount: UpfrontPaymentAmount): Unit =
      WireMockHelpers.verifyWithBodyParse(updateUpfrontPaymentAmountUrl(journeyId), expectedUpfrontPaymentAmount)(essttp.rootmodel.UpfrontPaymentAmount.format)

    def verifyNoneUpdateUpfrontPaymentAmountRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateUpfrontPaymentAmountUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Entered Upfront payment amount`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object Dates {
    def updateExtremeDatesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-extreme-dates"

    def updateStartDatesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-start-dates"

    def stubUpdateExtremeDates(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(updateExtremeDatesUrl(journeyId))

    def stubUpdateStartDates(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(updateStartDatesUrl(journeyId))

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

    def findJourneyExtremeDates(jsonBody: String = JourneyJsonTemplates.`Retrieved Extreme Dates Response`): StubMapping = findByLatestSessionId(jsonBody)

    def findJourneyStartDates(jsonBody: String = JourneyJsonTemplates.`Retrieved Start Dates`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object AffordabilityMinMaxApi {
    def findJourney(jsonBody: String = JourneyJsonTemplates.`Retrieved Affordability`()): StubMapping = findByLatestSessionId(jsonBody)

    def updateAffordabilityUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-affordability-result"

    def stubUpdateAffordability(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(updateAffordabilityUrl(journeyId))

    def verifyUpdateAffordabilityRequest(journeyId: JourneyId, expectedInstalmentAmounts: InstalmentAmounts): Unit =
      WireMockHelpers.verifyWithBodyParse(updateAffordabilityUrl(journeyId), expectedInstalmentAmounts)(InstalmentAmounts.format)

    def verifyNoneUpdateAffordabilityRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateAffordabilityUrl(journeyId)))
      )
  }

  object MonthlyPaymentAmount {
    def monthlyPaymentAmountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-monthly-payment-amount"

    def stubUpdateMonthlyPaymentAmount(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(monthlyPaymentAmountUrl(journeyId))

    def verifyUpdateMonthlyPaymentAmountRequest(journeyId: JourneyId, expectedMonthlyPaymentAmount: MonthlyPaymentAmount): Unit =
      WireMockHelpers.verifyWithBodyParse(monthlyPaymentAmountUrl(journeyId), expectedMonthlyPaymentAmount)(essttp.rootmodel.MonthlyPaymentAmount.format)

    def verifyNoneUpdateMonthlyAmountRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(monthlyPaymentAmountUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Entered Monthly Payment Amount`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object DayOfMonth {
    def dayOfMonthUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-day-of-month"

    def stubUpdateDayOfMonth(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(dayOfMonthUrl(journeyId))

    def verifyUpdateDayOfMonthRequest(journeyId: JourneyId, dayOfMonth: DayOfMonth = TdAll.dayOfMonth()): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId))).withRequestBody(equalToJson(s"""${dayOfMonth.value}"""))
      )

    def verifyNoneUpdateDayOfMonthRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Entered Day of Month`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object AffordableQuotes {
    def affordableQuotesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-affordable-quotes"

    def stubUpdateAffordableQuotes(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(affordableQuotesUrl(journeyId))

    def verifyUpdateAffordableQuotesRequest(journeyId: JourneyId): Unit =
      WireMockHelpers.verifyWithBodyParse(affordableQuotesUrl(journeyId))(AffordableQuotesResponse.format)

    def verifyNoneUpdateAffordableQuotesRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(affordableQuotesUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Retrieved Affordable Quotes`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object SelectedPaymentPlan {
    def selectedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-selected-plan"

    def stubUpdateSelectedPlan(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(selectedPlanUrl(journeyId))

    def verifyUpdateSelectedPlanRequest(journeyId: JourneyId): Unit =
      WireMockHelpers.verifyWithBodyParse(selectedPlanUrl(journeyId))(PaymentPlan.format)

    def verifyNoneUpdateSelectedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(selectedPlanUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Chosen Payment Plan`("""{"DeclaredUpfrontPayment": {"amount": 200}}""")): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object HasCheckedPlan {
    def hasCheckedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-checked-plan"

    def stubUpdateHasCheckedPlan(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(hasCheckedPlanUrl(journeyId))

    def verifyUpdateHasCheckedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(hasCheckedPlanUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Has Checked Payment Plan`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object ChosenTypeOfBankAccount {
    def chosenTypeOfBankAccountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-chosen-type-of-bank-account"

    def stubUpdateChosenTypeOfBankAccount(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(chosenTypeOfBankAccountUrl(journeyId))

    def verifyUpdateChosenTypeOfBankAccountRequest(journeyId: JourneyId, expectedTypeOfAccount: TypeOfBankAccount): Unit =
      WireMockHelpers.verifyWithBodyParse(chosenTypeOfBankAccountUrl(journeyId), expectedTypeOfAccount)(TypeOfBankAccount.format)

    def verifyNoneUpdateChosenTypeOfBankAccountRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(chosenTypeOfBankAccountUrl(journeyId))))

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Chosen Type of Bank Account - Business`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object DirectDebitDetails {
    def directDebitDetailsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-direct-debit-details"

    def stubUpdateDirectDebitDetails(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(directDebitDetailsUrl(journeyId))

    def verifyUpdateDirectDebitDetailsRequest(journeyId: JourneyId, expectedDirectDebitDetails: essttp.rootmodel.bank.DirectDebitDetails): Unit =
      WireMockHelpers.verifyWithBodyParse(directDebitDetailsUrl(journeyId), expectedDirectDebitDetails)(essttp.rootmodel.bank.DirectDebitDetails.format)

    def verifyNoneUpdateDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(directDebitDetailsUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Entered Direct Debit Details - Is Account Holder`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object ConfirmedDirectDebitDetails {
    def confirmDirectDebitDetailsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-confirmed-direct-debit-details"

    def stubUpdateConfirmDirectDebitDetails(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(confirmDirectDebitDetailsUrl(journeyId))

    def verifyUpdateConfirmDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
      )

    def verifyNoneUpdateConfirmDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Confirmed Direct Debit Details`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object TermsAndConditions {
    def agreedTermsAndConditionsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-agreed-terms-and-conditions"

    def stubUpdateAgreedTermsAndConditions(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(agreedTermsAndConditionsUrl(journeyId))

    def verifyUpdateAgreedTermsAndConditionsRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(agreedTermsAndConditionsUrl(journeyId)))
      )

    def verifyNoneUpdateAgreedTermsAndConditionsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(agreedTermsAndConditionsUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Agreed Terms and Conditions`): StubMapping = findByLatestSessionId(jsonBody)
  }

  object SubmitArrangement {
    def submitArrangementUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-arrangement"

    def stubUpdateSubmitArrangement(journeyId: JourneyId): StubMapping =
      WireMockHelpers.stubForPostNoResponseBody(submitArrangementUrl(journeyId))

    def verifyUpdateSubmitArrangementRequest(journeyId: JourneyId, expectedArrangementResponse: ArrangementResponse): Unit =
      WireMockHelpers.verifyWithBodyParse(submitArrangementUrl(journeyId), expectedArrangementResponse)(ArrangementResponse.format)

    def verifyNoneUpdateSubmitArrangementRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(submitArrangementUrl(journeyId))))

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`): StubMapping = findByLatestSessionId(jsonBody)
  }

}
