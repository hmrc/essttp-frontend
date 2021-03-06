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
import testsupport.testdata.TdEssttpDatesBodies

object Dates {
  private val startDatesUrl: String = "/essttp-dates/start-dates"
  private val extremeDatesUrl: String = "/essttp-dates/extreme-dates"
  def verifyStartDates(): Unit = verify(postRequestedFor(urlPathEqualTo(startDatesUrl)))
  def verifyExtremeDates(): Unit = verify(postRequestedFor(urlPathEqualTo(extremeDatesUrl)))
  def extremeDatesCall(
      jsonBody: String = TdEssttpDatesBodies.extremeDates()
  ): StubMapping = stubFor(
    post(urlPathEqualTo(extremeDatesUrl))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody))
  )
  def startDatesCall(
      jsonBody: String = TdEssttpDatesBodies.startDatesWithUpfrontPayment()
  ): StubMapping = stubFor(
    post(urlPathEqualTo(startDatesUrl))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody))
  )
}
