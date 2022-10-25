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
import essttp.rootmodel.Email
import models.GGCredId
import models.emailverification.RequestEmailVerificationResponse
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import testsupport.testdata.PageUrls

object EmailVerificationStub {

  val requestVerificationUrl: String = "/email-verification/verify-email"

  type HttpStatus = Int

  def requestEmailVerification(result: Either[HttpStatus, RequestEmailVerificationResponse.Success]): StubMapping =
    stubFor(
      post(urlPathEqualTo(requestVerificationUrl))
        .willReturn{
          result.fold(
            status => aResponse().withStatus(status),
            { success =>
              val body = Json.parse(s"""{ "redirectUri": "${success.redirectUri}" }""")
              aResponse().withStatus(CREATED).withBody(Json.prettyPrint(body))
            }
          )
        }
    )

  def verifyRequestEmailVerification(
      emailAddress:                      Email,
      ggCredId:                          GGCredId,
      expectedAccessibilityStatementUrl: String,
      expectedPageTitle:                 String,
      expectedLanguageCode:              String,
      urlPrefix:                         String
  ): Unit =
    verify(
      exactly(1),
      postRequestedFor(urlPathEqualTo(requestVerificationUrl))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "credId": "${ggCredId.value}",
               |  "continueUrl": "$urlPrefix/set-up-a-payment-plan/email-callback",
               |  "origin": "essttp-frontend",
               |  "deskproServiceName": "essttp-frontend",
               |  "accessibilityStatementUrl": "$expectedAccessibilityStatementUrl",
               |  "pageTitle": "$expectedPageTitle",
               |  "backUrl": "$urlPrefix${PageUrls.whichEmailDoYouWantToUseUrl}",
               |  "email": {
               |      "address": "${emailAddress.value.decryptedValue}",
               |      "enterUrl": "$urlPrefix${PageUrls.whichEmailDoYouWantToUseUrl}"
               |  },
               |  "lang":"$expectedLanguageCode"
               |}
               |""".stripMargin
          )
        )
    )

}
