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
import essttp.crypto.CryptoFormat
import essttp.emailverification.EmailVerificationStatus
import essttp.journey.model.{JourneyId, Origin, Origins}
import essttp.rootmodel.bank.DetailsAboutBankAccount
import essttp.rootmodel.dates.extremedates.ExtremeDatesResponse
import essttp.rootmodel.dates.startdates.StartDatesResponse
import essttp.rootmodel.ttp.EligibilityCheckResult
import essttp.rootmodel.ttp.affordability.InstalmentAmounts
import essttp.rootmodel.ttp.affordablequotes.{AffordableQuotesResponse, PaymentPlan}
import essttp.rootmodel.ttp.arrangement.ArrangementResponse
import essttp.rootmodel.{CanPayUpfront, DayOfMonth, Email, IsEmailAddressRequired, MonthlyPaymentAmount, TaxId, TaxRegime, UpfrontPaymentAmount}
import play.api.libs.json.Json
import testsupport.stubs.WireMockHelpers._
import testsupport.testdata.{JourneyJsonTemplates, TdAll, TdJsonBodies}
import play.api.http.Status._
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

  def verifyFindByLatestSessionId(): Unit = verify(postRequestedFor(urlPathEqualTo(findByLatestSessionIdUrl)))

  object BarsVerifyStatusStub {
    private def noLockoutBody(numberOfAttempts: Int) = s"""{
                          |    "attempts": $numberOfAttempts
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
    private val startJourneyBtaEpayeUrl = "/essttp-backend/epaye/bta/journey/start"
    private val startJourneyEpayeEpayeServiceUrl = "/essttp-backend/epaye/epaye-service/journey/start"
    private val startJourneyGovUkEpayeUrl = "/essttp-backend/epaye/gov-uk/journey/start"
    private val startJourneyDetachedEpayeUrl = "/essttp-backend/epaye/detached-url/journey/start"
    private val startJourneyBtaVatUrl = "/essttp-backend/vat/bta/journey/start"
    private val startJourneyVatVatServiceUrl = "/essttp-backend/vat/vat-service/journey/start"
    private val startJourneyGovUkVatUrl = "/essttp-backend/vat/gov-uk/journey/start"
    private val startJourneyDetachedVatUrl = "/essttp-backend/vat/detached-url/journey/start"

    def startJourneyInBackend(origin: Origin): StubMapping = {
      val (url, expectedRequestBody, responseBody): (String, String, String) = origin match {
        case Origins.Epaye.Bta          => (startJourneyBtaEpayeUrl, TdJsonBodies.StartJourneyRequestBodies.simple, TdJsonBodies.StartJourneyResponses.bta(TaxRegime.Epaye))
        case Origins.Epaye.EpayeService => (startJourneyEpayeEpayeServiceUrl, TdJsonBodies.StartJourneyRequestBodies.simple, TdJsonBodies.StartJourneyResponses.epaye(TaxRegime.Epaye))
        case Origins.Epaye.GovUk        => (startJourneyGovUkEpayeUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk(TaxRegime.Epaye))
        case Origins.Epaye.DetachedUrl  => (startJourneyDetachedEpayeUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl(TaxRegime.Epaye))
        case Origins.Vat.Bta            => (startJourneyBtaVatUrl, TdJsonBodies.StartJourneyRequestBodies.simple, TdJsonBodies.StartJourneyResponses.bta(TaxRegime.Vat))
        case Origins.Vat.VatService     => (startJourneyVatVatServiceUrl, TdJsonBodies.StartJourneyRequestBodies.simple, TdJsonBodies.StartJourneyResponses.vat(TaxRegime.Vat))
        case Origins.Vat.GovUk          => (startJourneyGovUkVatUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.govUk(TaxRegime.Vat))
        case Origins.Vat.DetachedUrl    => (startJourneyDetachedVatUrl, TdJsonBodies.StartJourneyRequestBodies.empty, TdJsonBodies.StartJourneyResponses.detachedUrl(TaxRegime.Vat))
      }
      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .willReturn(aResponse()
            .withStatus(ACCEPTED)
            .withBody(responseBody))
      )
    }

    def startJourneyEpayeBta: StubMapping = startJourneyInBackend(Origins.Epaye.Bta)
    def startJourneyEpayeEpayeService: StubMapping = startJourneyInBackend(Origins.Epaye.EpayeService)
    def startJourneyEpayeGovUk: StubMapping = startJourneyInBackend(Origins.Epaye.GovUk)
    def startJourneyEpayeDetached: StubMapping = startJourneyInBackend(Origins.Epaye.DetachedUrl)
    def startJourneyVatBta: StubMapping = startJourneyInBackend(Origins.Vat.Bta)
    def startJourneyVatVatService: StubMapping = startJourneyInBackend(Origins.Vat.VatService)
    def startJourneyVatGovUk: StubMapping = startJourneyInBackend(Origins.Vat.GovUk)
    def startJourneyVatDetached: StubMapping = startJourneyInBackend(Origins.Vat.DetachedUrl)

    def verifyStartJourney(url: String): Unit = verify(exactly(1), postRequestedFor(urlPathEqualTo(url)))
    def verifyStartJourneyEpayeBta(): Unit = verifyStartJourney(startJourneyBtaEpayeUrl)
    def verifyStartJourneyEpayeEpayeService(): Unit = verifyStartJourney(startJourneyEpayeEpayeServiceUrl)
    def verifyStartJourneyEpayeGovUk(): Unit = verifyStartJourney(startJourneyGovUkEpayeUrl)
    def verifyStartJourneyEpayeDetached(): Unit = verifyStartJourney(startJourneyDetachedEpayeUrl)
    def verifyStartJourneyVatBta(): Unit = verifyStartJourney(startJourneyBtaVatUrl)
    def verifyStartJourneyVatVatService(): Unit = verifyStartJourney(startJourneyVatVatServiceUrl)
    def verifyStartJourneyVatGovUk(): Unit = verifyStartJourney(startJourneyGovUkVatUrl)
    def verifyStartJourneyVatDetached(): Unit = verifyStartJourney(startJourneyDetachedVatUrl)

    def findJourney(origin: Origin = Origins.Epaye.Bta): StubMapping =
      findByLatestSessionId(JourneyJsonTemplates.Started(origin))
  }

  object DetermineTaxId {
    def findJourney(jsonBody: String = JourneyJsonTemplates.`Computed Tax Id`()): StubMapping = findByLatestSessionId(jsonBody)

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

    def findJourneyExtremeDates(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Retrieved Extreme Dates Response`(origin)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

    def findJourneyStartDates(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Retrieved Start Dates`(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object AffordabilityMinMaxApi {
    def findJourney(encrypter: Encrypter, origin: Origin = Origins.Epaye.Bta)(jsonBody: String = JourneyJsonTemplates.`Retrieved Affordability`(origin)(encrypter)): StubMapping =
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

    def findJourney(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Entered Monthly Payment Amount`(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
  }

  object DayOfMonth {
    def dayOfMonthUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-day-of-month"

    def stubUpdateDayOfMonth(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(dayOfMonthUrl(journeyId), updatedJourneyJson)

    def verifyUpdateDayOfMonthRequest(journeyId: JourneyId, dayOfMonth: DayOfMonth = TdAll.dayOfMonth()): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId))).withRequestBody(equalToJson(s"""${dayOfMonth.value}"""))
      )

    def verifyNoneUpdateDayOfMonthRequest(journeyId: JourneyId): Unit =
      verify(
        exactly(0),
        postRequestedFor(urlPathEqualTo(dayOfMonthUrl(journeyId)))
      )

    def findJourney(
        dayOfMonth: DayOfMonth,
        encrypter:  Encrypter
    )(jsonBody: String = JourneyJsonTemplates.`Entered Day of Month`(dayOfMonth)(encrypter)): StubMapping =
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

    def findJourney(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Retrieved Affordable Quotes`(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
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

    def findJourney(encrypter: Encrypter)(
        jsonBody: String = JourneyJsonTemplates.`Chosen Payment Plan`(
          upfrontPaymentAmountJsonString = """{"DeclaredUpfrontPayment": {"amount": 200}}"""
        )(encrypter)
    ): StubMapping =
      findByLatestSessionId(jsonBody)
  }

  object HasCheckedPlan {
    def hasCheckedPlanUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-has-checked-plan"

    def stubUpdateHasCheckedPlan(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(hasCheckedPlanUrl(journeyId), updatedJourneyJson)

    def verifyUpdateHasCheckedPlanRequest(journeyId: JourneyId): Unit =
      verify(
        postRequestedFor(urlPathEqualTo(hasCheckedPlanUrl(journeyId)))
      )

    def findJourney(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Has Checked Payment Plan`(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
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
        encrypter: Encrypter
    )(
        jsonBody: String = JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = true)(encrypter)
    ): StubMapping =
      findByLatestSessionId(jsonBody)
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

    def findJourney(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Entered Direct Debit Details`(encrypter)): StubMapping =
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

    def findJourney(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Confirmed Direct Debit Details`(encrypter)): StubMapping = findByLatestSessionId(jsonBody)
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
        encrypter:              Encrypter
    )(jsonBody: String = JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddressRequired)(encrypter)): StubMapping =
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
        encrypter: Encrypter
    )(jsonBody: String = JourneyJsonTemplates.`Selected email to be verified`(email)(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)

  }

  object EmailVerificationStatus {
    def updateEmailVerificationStatusUrl(journeyId: JourneyId) = s"/essttp-backend/journey/${journeyId.value}/update-email-verification-status"

    def stubEmailVerificationStatus(journeyId: JourneyId, updatedJourneyJson: String): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(updateEmailVerificationStatusUrl(journeyId), updatedJourneyJson)

    def verifyEmailVerificationStatusRequest(journeyId: JourneyId, status: EmailVerificationStatus): Unit =
      WireMockHelpers.verifyWithBodyParse(updateEmailVerificationStatusUrl(journeyId), status)

    def findJourney(
        email:     String,
        status:    EmailVerificationStatus,
        encrypter: Encrypter
    )(jsonBody: String = JourneyJsonTemplates.`Email verification complete`(email, status)(encrypter)): StubMapping =
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

    def findJourney(encrypter: Encrypter)(jsonBody: String = JourneyJsonTemplates.`Arrangement Submitted - with upfront payment`(encrypter)): StubMapping =
      findByLatestSessionId(jsonBody)
  }

}
