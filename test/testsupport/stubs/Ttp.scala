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
import connectors.CallEligibilityApiRequest
import essttp.crypto.CryptoFormat
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.pega.PegaCaseId
import essttp.rootmodel.ttp.affordablequotes.AffordableQuotesRequest
import essttp.rootmodel.ttp.arrangement.ArrangementRequest
import essttp.rootmodel.ttp.eligibility.{ContactDetail, CustomerDetail, RegimeDigitalCorrespondence}
import models.EligibilityReqIdentificationFlag
import play.api.http.Status
import play.api.libs.json.Format
import testsupport.testdata.{TdAll, TtpJsonResponses}

object Ttp {

  private val eligibilityUrl: String                   = "/debts/time-to-pay/eligibility"
  private val affordabilityUrl: String                 = "/debts/time-to-pay/self-serve/affordability"
  private val affordableQuotesUrl: String              = "/debts/time-to-pay/affordability/affordable-quotes"
  private val enactArrangementUrl: String              = "/debts/time-to-pay/self-serve/arrangement"
  private val ttpCorrelationIdHeader: (String, String) = ("correlationId", TdAll.correlationId.value.toString)

  def ttpVerify[A](url: String, expectedPayload: A)(using Format[A]): Unit =
    WireMockHelpers.verifyWithBodyParse(url, ttpCorrelationIdHeader, expectedPayload)

  object Eligibility {
    def stubRetrieveEligibility(taxRegime: TaxRegime)(
      jsonBody: String = TtpJsonResponses.ttpEligibilityCallJson(taxRegime, regimeDigitalCorrespondence = true)
    ): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(eligibilityUrl, jsonBody)

    def stubServiceUnavailableRetrieveEligibility(): StubMapping =
      stubFor(post(urlPathEqualTo(eligibilityUrl)).willReturn(aResponse().withStatus(Status.SERVICE_UNAVAILABLE)))

    def stub422RetrieveEligibility(): StubMapping =
      stubFor(post(urlPathEqualTo(eligibilityUrl)).willReturn(aResponse().withStatus(Status.UNPROCESSABLE_ENTITY)))

    def verifyTtpEligibilityRequests(
      taxRegime: TaxRegime
    )(using EligibilityReqIdentificationFlag): Unit = {

      val request = taxRegime match {
        case TaxRegime.Epaye => TdAll.callEligibilityApiRequestEpaye
        case TaxRegime.Vat   => TdAll.callEligibilityApiRequestVat
        case TaxRegime.Sa    => TdAll.callEligibilityApiRequestSa
        case TaxRegime.Simp  => TdAll.callEligibilityApiRequestSimp
      }
      ttpVerify(eligibilityUrl, request)(using CallEligibilityApiRequest.format)
    }
  }

  object Affordability {
    def stubRetrieveAffordability(jsonBody: String = TtpJsonResponses.ttpAffordabilityResponseJson()): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(affordabilityUrl, jsonBody)

    def verifyTtpAffordabilityRequest(taxRegime: TaxRegime, maxPlanLength: Int = 12)(using CryptoFormat): Unit =
      ttpVerify(affordabilityUrl, TdAll.instalmentAmountRequest(taxRegime, maxPlanLength))
  }

  object AffordableQuotes {
    def stubRetrieveAffordableQuotes(
      jsonBody: String = TtpJsonResponses.ttpAffordableQuotesResponseJson()
    ): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(affordableQuotesUrl, jsonBody)

    def verifyTtpAffordableQuotesRequest(taxRegime: TaxRegime)(
      expectedRequest: AffordableQuotesRequest = TdAll.affordableQuotesRequest(taxRegime)
    )(using CryptoFormat): Unit =
      ttpVerify(affordableQuotesUrl, expectedRequest)
  }

  object EnactArrangement {
    def stubEnactArrangement(taxRegime: TaxRegime)(
      jsonBody: String = TtpJsonResponses.ttpEnactArrangementResponseJson(taxRegime)
    ): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(enactArrangementUrl, jsonBody, Status.CREATED)

    def stubEnactArrangementFail(): StubMapping = stubFor(
      post(urlPathEqualTo(enactArrangementUrl))
        .willReturn(serviceUnavailable())
    )

    def verifyTtpEnactArrangementRequest(
      customerDetails:             Option[List[CustomerDetail]],
      contactDetails:              Option[ContactDetail],
      regimeDigitalCorrespondence: Option[RegimeDigitalCorrespondence],
      taxRegime:                   TaxRegime,
      accountNumber:               String = "12345678",
      hasAffordability:            Boolean = false,
      caseId:                      Option[PegaCaseId] = None
    )(using CryptoFormat): Unit =
      ttpVerify(
        enactArrangementUrl,
        TdAll.arrangementRequest(
          customerDetails,
          contactDetails,
          regimeDigitalCorrespondence,
          taxRegime,
          accountNumber,
          hasAffordability,
          caseId
        )
      )
  }

}
