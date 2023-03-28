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
import essttp.rootmodel.Email
import paymentsEmailVerification.models.EmailVerificationResult
import paymentsEmailVerification.models.api.StartEmailVerificationJourneyResponse
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

object EmailVerificationStub {

  private val startVerificationJourneyUrl: String = "/payments-email-verification/start"
  private val getVerificationResultUrl: String = "/payments-email-verification/status"
  private val getLockoutCreatedAtUrl = "/payments-email-verification/earliest-created-at"

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
      expectedAccessibilityStatementUrl: String,
      expectedPageTitle:                 String,
      expectedLanguageCode:              String,
      urlPrefix:                         String,
      backLocation:                      String
  ): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(startVerificationJourneyUrl))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "continueUrl": "$urlPrefix/set-up-a-payment-plan/email-callback",
               |  "origin": "essttp-frontend",
               |  "deskproServiceName": "essttp-frontend",
               |  "accessibilityStatementUrl": "$expectedAccessibilityStatementUrl",
               |  "pageTitle": "$expectedPageTitle",
               |  "backUrl": "$urlPrefix$backLocation",
               |  "enterEmailUrl": "$urlPrefix$backLocation",
               |  "email": "${emailAddress.value.decryptedValue}",
               |  "lang":"$expectedLanguageCode"
               |}
               |""".stripMargin
          )
        )
    )

  def getVerificationStatus(result: EmailVerificationResult): StubMapping =
    stubFor(
      post(urlPathEqualTo(getVerificationResultUrl))
        .willReturn{
          aResponse().withStatus(OK).withBody(Json.prettyPrint(Json.toJson(result)))
        }
    )

  def verifyGetEmailVerificationResult(
      emailAddress: Email
  ): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(getVerificationResultUrl))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "email": "${emailAddress.value.decryptedValue}"
               |}
               |""".stripMargin
          )
        )
    )

  def getLockoutCreatedAt(dateTime: Option[LocalDateTime]): StubMapping = stubFor(
    get(urlPathEqualTo(getLockoutCreatedAtUrl))
      .willReturn(
        aResponse().withStatus(OK).withBody(
          Json.prettyPrint(dateTime.fold[JsValue](
            Json.parse("{}")
          )(d =>
              Json.parse(s"""{ "earliestCreatedAtTime": ${Json.toJson(d).toString}  } """)))
        )
      )
  )

}
