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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.rootmodel.dates.extremedates.ExtremeDatesRequest
import essttp.rootmodel.dates.startdates.StartDatesRequest
import testsupport.testdata.TdEssttpDatesBodies

object EssttpDates {
  private val startDatesUrl: String = "/essttp-dates/start-dates"
  private val extremeDatesUrl: String = "/essttp-dates/extreme-dates"

  def verifyStartDates(expectedStartDatesRequest: StartDatesRequest): Unit =
    WireMockHelpers.verifyWithBodyParse(startDatesUrl, expectedStartDatesRequest)(StartDatesRequest.format)

  def verifyExtremeDates(expectedExtremeDatesRequest: ExtremeDatesRequest): Unit =
    WireMockHelpers.verifyWithBodyParse(extremeDatesUrl, expectedExtremeDatesRequest)(ExtremeDatesRequest.format)

  def stubExtremeDatesCall(jsonBody: String = TdEssttpDatesBodies.extremeDatesResponse()): StubMapping =
    WireMockHelpers.stubForPostWithResponseBody(extremeDatesUrl, jsonBody)

  def stubStartDatesCall(jsonBody: String = TdEssttpDatesBodies.startDatesResponseWithUpfrontPayment()): StubMapping =
    WireMockHelpers.stubForPostWithResponseBody(startDatesUrl, jsonBody)
}
