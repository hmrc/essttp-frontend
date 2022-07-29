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
import testsupport.testdata.{TdAll, TtpJsonResponses}

object Ttp {

  private val eligibilityUrl: String = "/debts/time-to-pay/eligibility"
  private val affordabilityUrl: String = "/debts/time-to-pay/self-serve/affordability"
  private val affordableQuotesUrl: String = "/debts/time-to-pay/affordability/affordable-quotes"
  private val enactArrangementUrl: String = "/debts/time-to-pay/self-serve/arrangement"
  private val ttpCorrelationIdHeader: (String, String) = ("correlationId", TdAll.correlationId.value.toString)

  def ttpVerify(url: String): Unit = verify(
    postRequestedFor(urlPathEqualTo(url))
      .withHeader(ttpCorrelationIdHeader._1, equalTo(ttpCorrelationIdHeader._2))
  )

  object Eligibility {
    def retrieveEligibility(jsonBody: String = TtpJsonResponses.ttpEligibilityCallJson()): StubMapping = stubFor(
      post(urlPathEqualTo(eligibilityUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    def verifyTtpEligibilityRequests(): Unit = ttpVerify(eligibilityUrl)
  }

  object Affordability {
    def retrieveAffordability(jsonBody: String = TtpJsonResponses.ttpAffordabilityResponseJson()): StubMapping = stubFor(
      post(urlPathEqualTo(affordabilityUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    def verifyTtpAffordabilityRequest(): Unit = ttpVerify(affordabilityUrl)
  }

  object AffordableQuotes {
    def retrieveAffordableQuotes(jsonBody: String = TtpJsonResponses.ttpAffordableQuotesResponseJson()): StubMapping = stubFor(
      post(urlPathEqualTo(affordableQuotesUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    def verifyTtpAffordableQuotesRequest(): Unit = ttpVerify(affordableQuotesUrl)
  }

  object EnactArrangement {
    def enactArrangement(jsonBody: String = TtpJsonResponses.ttpEnactArrangementResponseJson()): StubMapping = stubFor(
      post(urlPathEqualTo(enactArrangementUrl))
        .willReturn(aResponse()
          .withStatus(202)
          .withBody(jsonBody))
    )

    def enactArrangementFail(): StubMapping = stubFor(
      post(urlPathEqualTo(enactArrangementUrl))
        .willReturn(aResponse()
          .withStatus(400))
    )

    def verifyTtpEnactArrangementRequest(): Unit = ttpVerify(enactArrangementUrl)
  }

}
