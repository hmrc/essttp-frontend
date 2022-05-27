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
import essttp.rootmodel.CanPayUpfront
import testsupport.testdata.{TdAll, TdJsonBodies}

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
  }

  object DetermineTaxId {

    def findJourneyAfterDetermineTaxId(jsonBody: String = TdJsonBodies.afterDetermineTaxIdJourneyJson()): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
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

    def findJourneyAfterEligibilityCheck(jsonBody: String = TdJsonBodies.afterEligibilityCheckJourneyJson()): StubMapping = stubFor(
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

    def findJourneyAfterUpdateCanPayUpfront(canPayUpfront: CanPayUpfront): StubMapping = {
      val additionalPayloadInfo: (String, Boolean) =
        if (canPayUpfront.value) {
          ("Yes", true)
        } else {
          ("No", false)
        }
      stubFor(
        get(urlPathEqualTo(findByLatestSessionIdUrl))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(TdJsonBodies.afterCanPayUpfrontJourneyJson(additionalPayloadInfo._1, additionalPayloadInfo._2)))
      )
    }
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

    def findJourneyAfterUpdateUpfrontPaymentAmount(
                                                    jsonBody: String = TdJsonBodies.afterUpfrontPaymentAmountJourneyJson(TdAll.upfrontPaymentAmount)
                                                  ): StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )
  }
}
