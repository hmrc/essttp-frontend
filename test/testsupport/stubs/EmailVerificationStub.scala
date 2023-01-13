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
import essttp.emailverification.{EmailVerificationResult, StartEmailVerificationJourneyResponse}
import essttp.journey.model.JourneyId
import essttp.rootmodel.{Email, GGCredId}
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import testsupport.testdata.TdJsonBodies
import uk.gov.hmrc.crypto.Encrypter

import java.time.LocalDateTime

object EmailVerificationStub {

  private def startVerificationJourneyUrl(journeyId: JourneyId): String = s"/essttp-backend/email-verification/${journeyId.value}/start"

  private def getVerificationResultUrl(journeyId: JourneyId): String = s"/essttp-backend/email-verification/${journeyId.value}/result"

  private val getLockoutCreatedAtUrl = "/essttp-backend/email-verification/earliest-created-at"

  type HttpStatus = Int

  def requestEmailVerification(result: StartEmailVerificationJourneyResponse)(journeyId: JourneyId): StubMapping =
    stubFor(
      post(urlPathEqualTo(startVerificationJourneyUrl(journeyId)))
        .willReturn(
          aResponse().withStatus(CREATED).withBody(Json.prettyPrint(Json.toJson(result)))
        )
    )

  def verifyRequestEmailVerification(
      emailAddress:                      Email,
      ggCredId:                          GGCredId,
      expectedAccessibilityStatementUrl: String,
      expectedPageTitle:                 String,
      expectedLanguageCode:              String,
      urlPrefix:                         String,
      backLocation:                      String,
      isLocal:                           Boolean,
      encrypter:                         Encrypter
  )(journeyId: JourneyId): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(startVerificationJourneyUrl(journeyId)))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "credId": "${ggCredId.value}",
               |  "continueUrl": "$urlPrefix/set-up-a-payment-plan/email-callback",
               |  "origin": "essttp-frontend",
               |  "deskproServiceName": "essttp-frontend",
               |  "accessibilityStatementUrl": "$expectedAccessibilityStatementUrl",
               |  "pageTitle": "$expectedPageTitle",
               |  "backUrl": "$urlPrefix$backLocation",
               |  "enterEmailUrl": "$urlPrefix$backLocation",
               |  "email": "${TdJsonBodies.encryptString(emailAddress.value.decryptedValue, encrypter)}",
               |  "lang":"$expectedLanguageCode",
               |  "isLocal": ${isLocal.toString}
               |}
               |""".stripMargin
          )
        )
    )

  def getVerificationStatus(result: EmailVerificationResult)(journeyId: JourneyId): StubMapping =
    stubFor(
      post(urlPathEqualTo(getVerificationResultUrl(journeyId)))
        .willReturn{
          aResponse().withStatus(OK).withBody(Json.prettyPrint(Json.toJson(result)))
        }
    )

  def verifyGetEmailVerificationResult(
      emailAddress: Email,
      ggCredId:     GGCredId,
      encrypter:    Encrypter
  )(journeyId: JourneyId): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(getVerificationResultUrl(journeyId)))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "credId": "${ggCredId.value}",
               |  "email": "${TdJsonBodies.encryptString(emailAddress.value.decryptedValue, encrypter)}"
               |}
               |""".stripMargin
          )
        )
    )

  def getLockoutCreatedAt: StubMapping = stubFor(
    post(urlPathEqualTo(getLockoutCreatedAtUrl))
      .willReturn(
        aResponse().withStatus(OK).withBody(Json.prettyPrint(Json.toJson(LocalDateTime.of(2023, 1, 7, 11, 13))))
      )
  )

}
