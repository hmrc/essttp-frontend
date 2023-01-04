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
import essttp.emailverification.{EmailVerificationState, StartEmailVerificationJourneyResponse}
import essttp.rootmodel.{Email, GGCredId}
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import testsupport.testdata.TdJsonBodies
import uk.gov.hmrc.crypto.Encrypter

object EmailVerificationStub {

  private val startVerificationJourneyUrl: String = "/essttp-backend/email-verification/start"

  private val getVerificationResultUrl: String = s"/essttp-backend/email-verification/result"

  type HttpStatus = Int

  def requestEmailVerification(result: StartEmailVerificationJourneyResponse): StubMapping =
    stubFor(
      post(urlPathEqualTo(startVerificationJourneyUrl))
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
  ): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(startVerificationJourneyUrl))
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

  def getVerificationStatus(result: EmailVerificationState): StubMapping =
    stubFor(
      post(urlPathEqualTo(getVerificationResultUrl))
        .willReturn{
          aResponse().withStatus(OK).withBody(Json.prettyPrint(Json.toJson(result)))
        }
    )

  def verifyGetEmailVerificationResult(
      emailAddress: Email,
      ggCredId:     GGCredId,
      encrypter:    Encrypter
  ): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(getVerificationResultUrl))
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

}
