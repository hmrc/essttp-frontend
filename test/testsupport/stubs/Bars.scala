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
import testsupport.testdata.BarsJsonResponses.Validate._
import wiremock.org.apache.http.HttpStatus

object Bars {

  object Validate {
    def success(): StubMapping = stubForValidate(successJson)

    def accountNumberNotWellFormatted(): StubMapping = stubForValidate(accountNumberNotWellFormattedJson)

    def sortCodeNotPresentOnEiscd(): StubMapping = stubForValidate(sortCodeNotPresentOnEiscdJson)

    def sortCodeDoesNotSupportsDirectDebit(): StubMapping = stubForValidate(sortCodeDoesNotSupportsDirectDebitJson)

    private def stubForValidate(responseJson: String): StubMapping = {
      val validateUrl = "/validate/bank-details"
      stubFor(
        post(urlPathEqualTo(validateUrl))
          .willReturn(
            aResponse()
              .withStatus(HttpStatus.SC_OK)
              .withBody(responseJson)
          )
      )
    }
  }
}
