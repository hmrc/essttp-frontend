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
import essttp.rootmodel.{DayOfMonth, TaxId}
import play.api.libs.json.Json
import testsupport.testdata.{JourneyJsonTemplates, TdAll, TdJsonBodies}

object EssttpBackend {

  private val findByLatestSessionIdUrl: String = "/essttp-backend/journey/find-latest-by-session-id"

  def verifyFindByLatestSessionId(): Unit = verify(postRequestedFor(urlPathEqualTo(findByLatestSessionIdUrl)))

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

    val startJourneyEpayeBta: StubMapping = startJourneyInBackend(Origins.Epaye.Bta)
    val startJourneyEpayeGovUk: StubMapping = startJourneyInBackend(Origins.Epaye.GovUk)
    val startJourneyEpayeDetached: StubMapping = startJourneyInBackend(Origins.Epaye.DetachedUrl)

    def verifyStartJourney(url: String): Unit = verify(exactly(0), postRequestedFor(urlPathEqualTo(url)))

    def findJourney(jsonBody: String = JourneyJsonTemplates.Started): StubMapping =
      stubFor(
        get(urlPathEqualTo(findByLatestSessionIdUrl))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(jsonBody))
      )
  }

  object DetermineTaxId {

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Computed Tax Id`): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    def updateTaxIdUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-tax-id"

    def updateTaxId(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateTaxIdUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyTaxIdRequest(journeyId: JourneyId, taxId: TaxId) =
      verify(
        postRequestedFor(urlPathEqualTo(updateTaxIdUrl(journeyId)))
          .withRequestBody(equalToJson(Json.toJson(taxId).toString()))
      )

  }

  object EligibilityCheck {

    def updateEligibilityResultUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-eligibility-result"

    def updateEligibilityResult(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateEligibilityResultUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    //todo add withRequestbody - tie in with backend test data cor?
    def verifyUpdateEligibilityRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(updateEligibilityResultUrl(journeyId))))

    //todo add withRequestbody - tie in with backend test data cor?
    def verifyNoneUpdateEligibilityRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateEligibilityResultUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Eligibility Checked - Eligible`): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object CanPayUpfront {

    def updateCanPayUpfrontUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-can-pay-upfront"

    def updateCanPayUpfront(journeyId: JourneyId, canPayUpfrontScenario: Boolean): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId)))
          .withRequestBody(equalTo(canPayUpfrontScenario.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    //todo add withRequestbody - tie in with backend test data cor?
    def verifyUpdateCanPayUpfrontRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId))))

    //todo add withRequestbody - tie in with backend test data cor?
    def verifyNoneUpdateCanPayUpfrontRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateCanPayUpfrontUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Answered Can Pay Upfront - Yes`): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object UpfrontPaymentAmount {

    def updateUpfrontPaymentAmountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-upfront-payment-amount"

    def updateUpfrontPaymentAmount(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateUpfrontPaymentAmountUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    //todo add withRequestbody - tie in with backend test data cor?
    def verifyUpdateUpfrontPaymentAmountRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(updateUpfrontPaymentAmountUrl(journeyId))))

    //todo add withRequestbody - tie in with backend test data cor?
    def verifyNoneUpdateUpfrontPaymentAmountRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateUpfrontPaymentAmountUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Entered Upfront payment amount`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object Dates {
    def updateExtremeDatesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-extreme-dates"

    def updateStartDatesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-start-dates"

    def updateExtremeDates(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateExtremeDatesUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def updateStartDates(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateStartDatesUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateExtremeDates(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(updateExtremeDatesUrl(journeyId))))

    def verifyNoneUpdateExtremeDates(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateExtremeDatesUrl(journeyId)))
      )

    def verifyUpdateStartDates(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(updateStartDatesUrl(journeyId))))

    def verifyNoneUpdateStartDates(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateStartDatesUrl(journeyId)))
      )

    def findJourneyExtremeDates(
        jsonBody: String = JourneyJsonTemplates.`Retrieved Extreme Dates Response`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    def findJourneyStartDates(
        jsonBody: String = JourneyJsonTemplates.`Retrieved Start Dates`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object AffordabilityMinMaxApi {
    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Retrieved Affordability`()
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    def updateAffordabilityUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-affordability-result"

    def updateAffordability(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(updateAffordabilityUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateAffordabilityRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(updateAffordabilityUrl(journeyId))))

    def verifyNoneUpdateAffordabilityRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(updateAffordabilityUrl(journeyId)))
      )
  }

  object MonthlyPaymentAmount {
    def monthlyPaymentAmountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-monthly-payment-amount"

    def updateMonthlyPaymentAmount(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(monthlyPaymentAmountUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateMonthlyPaymentAmountRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(monthlyPaymentAmountUrl(journeyId))))

    def verifyNoneUpdateMonthlyAmountRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(monthlyPaymentAmountUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Entered Monthly Payment Amount`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object DayOfMonth {
    def dayOfMonthUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-day-of-month"

    def updateDayOfMonth(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(dayOfMonthUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateDayOfMonthRequest(journeyId: JourneyId, dayOfMonth: DayOfMonth = TdAll.dayOfMonth()): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId))).withRequestBody(equalToJson(s"""${dayOfMonth.value}"""))
      )

    def verifyNoneUpdateDayOfMonthRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId)))
      )

    def findJourneyAfterUpdateDayOfMonth(
        jsonBody: String = JourneyJsonTemplates.`Entered Day of Month`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object AffordableQuotes {
    def affordableQuotesUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-affordable-quotes"

    def updateAffordableQuotes(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(affordableQuotesUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateAffordableQuotesRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(affordableQuotesUrl(journeyId)))
      )

    def verifyNoneUpdateAffordableQuotesRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(affordableQuotesUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Retrieved Affordable Quotes`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object SelectedPaymentPlan {
    def selectedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-selected-plan"

    def updateSelectedPlan(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(selectedPlanUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateSelectedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(selectedPlanUrl(journeyId)))
      )

    def verifyNoneUpdateSelectedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(selectedPlanUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Chosen Payment Plan`("""{"DeclaredUpfrontPayment": {"amount": 200}}""")
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object HasCheckedPlan {
    def hasCheckedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-checked-plan"

    def updateHasCheckedPlan(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(hasCheckedPlanUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateHasCheckedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(hasCheckedPlanUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Has Checked Payment Plan`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object ChosenTypeOfBankAccount {
    def chosenTypeOfBankAccountUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-chosen-type-of-bank-account"

    def updateChosenTypeOfBankAccount(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(chosenTypeOfBankAccountUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateChosenTypeOfBankAccountRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(chosenTypeOfBankAccountUrl(journeyId))))

    def verifyNoneUpdateChosenTypeOfBankAccountRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(chosenTypeOfBankAccountUrl(journeyId))))

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Chosen Type of Bank Account`): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object DirectDebitDetails {
    def directDebitDetailsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-direct-debit-details"

    def updateDirectDebitDetails(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(directDebitDetailsUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(directDebitDetailsUrl(journeyId)))
      )

    def verifyNoneUpdateDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(directDebitDetailsUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Entered Direct Debit Details - Is Account Holder`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object ConfirmedDirectDebitDetails {
    def confirmDirectDebitDetailsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-confirmed-direct-debit-details"

    def updateConfirmDirectDebitDetails(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateConfirmDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
      )

    def verifyNoneUpdateConfirmDirectDebitDetailsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(confirmDirectDebitDetailsUrl(journeyId)))
      )

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Confirmed Direct Debit Details`): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object TermsAndConditions {
    def agreedTermsAndConditionsUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-agreed-terms-and-conditions"

    def updateAgreedTermsAndConditions(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(agreedTermsAndConditionsUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateAgreedTermsAndConditionsRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(agreedTermsAndConditionsUrl(journeyId)))
      )

    def verifyNoneUpdateAgreedTermsAndConditionsRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(agreedTermsAndConditionsUrl(journeyId)))
      )

    def findJourney(
        jsonBody: String = JourneyJsonTemplates.`Agreed Terms and Conditions`
    ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

  object SubmitArrangement {

    def submitArrangementUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-arrangement"

    def updateSubmitArrangement(journeyId: JourneyId): StubMapping =
      stubFor(
        post(urlPathEqualTo(submitArrangementUrl(journeyId)))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def verifyUpdateSubmitArrangementRequest(journeyId: JourneyId): Unit =
      verify(postRequestedFor(urlPathEqualTo(submitArrangementUrl(journeyId))))

    def verifyNoneUpdateSubmitArrangementRequest(journeyId: JourneyId): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(submitArrangementUrl(journeyId))))

    def findJourney(jsonBody: String = JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }

}
