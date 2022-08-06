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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import testsupport.testdata.BarsJsonResponses.{ValidateJson, VerifyBusinessJson, VerifyPersonalJson}
import wiremock.org.apache.http.HttpStatus

object Bars {

  object ValidateStub {
    def success(): StubMapping = stub(ValidateJson.success)

    def accountNumberNotWellFormatted(): StubMapping = stub(ValidateJson.accountNumberNotWellFormatted)

    def sortCodeNotPresentOnEiscd(): StubMapping = stub(ValidateJson.sortCodeNotPresentOnEiscd)

    def sortCodeDoesNotSupportsDirectDebit(): StubMapping = stub(ValidateJson.sortCodeDoesNotSupportsDirectDebit)

    private def stub(responseJson: String): StubMapping = {
      val url = "/validate/bank-details"
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.SC_OK)
              .withBody(responseJson)
          )
      )
    }
  }

  object VerifyPersonalStub {
    def success(): StubMapping = stub(VerifyPersonalJson.success)

    private def stub(responseJson: String): StubMapping = {
      val url = "/verify/personal"
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.SC_OK)
              .withBody(responseJson)
          )
      )
    }
  }

  object VerifyBusinessStub {
    def success(): StubMapping = stub(VerifyBusinessJson.success)

    private def stub(responseJson: String): StubMapping = {
      val url = "/verify/business"
      stubFor(
        post(urlPathEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.SC_OK)
              .withBody(responseJson)
          )
      )
    }
  }
}
