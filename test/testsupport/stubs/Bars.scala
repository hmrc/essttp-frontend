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
import wiremock.org.apache.http.HttpStatus

object Bars {

  // validate/bank-details endpoint
  object Validate {
    private val validateUrl = "/validate/bank-details"

    private val validateSuccessJson =
      """{
        |  "accountNumberIsWellFormatted": "yes",
        |  "nonStandardAccountDetailsRequiredForBacs": "no",
        |  "sortCodeIsPresentOnEISCD": "yes",
        |  "sortCodeSupportsDirectDebit": "yes",
        |  "sortCodeSupportsDirectCredit": "no",
        |  "iban": "GB21BARC20710244344655",
        |  "sortCodeBankName": "BARCLAYS BANK UK PLC"
        |}""".stripMargin

    def validateSuccess(): StubMapping = stubFor(
      post(urlPathEqualTo(validateUrl))
        .willReturn(aResponse()
          .withStatus(HttpStatus.SC_OK)
          .withBody(validateSuccessJson))
    )

  }
}
