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
import connectors.CallEligibilityApiRequest
import essttp.rootmodel.ttp.affordability.InstalmentAmountRequest
import essttp.rootmodel.ttp.affordablequotes.AffordableQuotesRequest
import essttp.rootmodel.ttp.arrangement.ArrangementRequest
import play.api.http.Status
import play.api.libs.json.Format
import testsupport.testdata.{TdAll, TtpJsonResponses}

object Ttp {

  private val eligibilityUrl: String = "/debts/time-to-pay/eligibility"
  private val affordabilityUrl: String = "/debts/time-to-pay/self-serve/affordability"
  private val affordableQuotesUrl: String = "/debts/time-to-pay/affordability/affordable-quotes"
  private val enactArrangementUrl: String = "/debts/time-to-pay/self-serve/arrangement"
  private val ttpCorrelationIdHeader: (String, String) = ("correlationId", TdAll.correlationId.value.toString)

  def ttpVerify[A](url: String)(implicit format: Format[A]): Unit =
    WireMockHelpers.verifyWithBodyParse(url, ttpCorrelationIdHeader)

  object Eligibility {
    def stubRetrieveEligibility(jsonBody: String = TtpJsonResponses.ttpEligibilityCallJson()): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(eligibilityUrl, jsonBody)

    def verifyTtpEligibilityRequests(): Unit = ttpVerify(eligibilityUrl)(CallEligibilityApiRequest.format)
  }

  object Affordability {
    def stubRetrieveAffordability(jsonBody: String = TtpJsonResponses.ttpAffordabilityResponseJson()): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(affordabilityUrl, jsonBody)

    def verifyTtpAffordabilityRequest(): Unit = ttpVerify(affordabilityUrl)(InstalmentAmountRequest.format)
  }

  object AffordableQuotes {
    def stubRetrieveAffordableQuotes(jsonBody: String = TtpJsonResponses.ttpAffordableQuotesResponseJson()): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(affordableQuotesUrl, jsonBody)

    def verifyTtpAffordableQuotesRequest(): Unit = ttpVerify(affordableQuotesUrl)(AffordableQuotesRequest.format)
  }

  object EnactArrangement {
    def stubEnactArrangement(jsonBody: String = TtpJsonResponses.ttpEnactArrangementResponseJson()): StubMapping =
      WireMockHelpers.stubForPostWithResponseBody(enactArrangementUrl, jsonBody, Status.CREATED)

    def stubEnactArrangementFail(): StubMapping = stubFor(
      post(urlPathEqualTo(enactArrangementUrl))
        .willReturn(serviceUnavailable())
    )

    def verifyTtpEnactArrangementRequest(): Unit = ttpVerify(enactArrangementUrl)(ArrangementRequest.format)
  }

}
