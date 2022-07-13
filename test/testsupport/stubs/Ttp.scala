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
import testsupport.testdata.TtpJsonResponses

object Ttp {

  private val eligibilityUrl: String = "/time-to-pay/self-serve/eligibility"
  private val affordabilityUrl: String = "/time-to-pay/self-serve/affordability"
  private val affordableQuotesUrl: String = "/time-to-pay/self-serve/affordable-quotes"

  def retrieveEligibility(jsonBody: String = TtpJsonResponses.ttpEligibilityCallJson()): StubMapping = stubFor(
    post(urlPathEqualTo(eligibilityUrl))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody))
  )

  //todo add withRequestbody - tie in with backend test data cor?
  def verifyTtpEligibilityRequests(): Unit =
    verify(
      postRequestedFor(urlPathEqualTo(eligibilityUrl))
    )

  def retrieveAffordability(jsonBody: String = TtpJsonResponses.ttpAffordabilityResponseJson()): StubMapping = stubFor(
    post(urlPathEqualTo(affordabilityUrl))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody))
  )

  def verifyTtpAffordabilityRequest(): Unit =
    verify(
      postRequestedFor(urlPathEqualTo(affordabilityUrl))
    )

  def retrieveAffordableQuotes(jsonBody: String = TtpJsonResponses.ttpAffordableQuotesResponseJson()): StubMapping = stubFor(
    post(urlPathEqualTo(affordableQuotesUrl))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody))
  )

  def verifyTtpAffordableQuotesRequest(): Unit =
    verify(
      postRequestedFor(urlPathEqualTo(affordableQuotesUrl))
    )

}
