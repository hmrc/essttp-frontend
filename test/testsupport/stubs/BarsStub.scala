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
import testsupport.testdata.BarsJsonResponses.{ValidateJson, VerifyJson}
import wiremock.org.apache.http.HttpStatus

object BarsStub {

  object ValidateStub {
    private val validateUrl = "/validate/bank-details"

    def success(): StubMapping = stubOk(validateUrl, ValidateJson.success)

    def accountNumberNotWellFormatted(): StubMapping = stubOk(validateUrl, ValidateJson.accountNumberNotWellFormatted)

    def sortCodeNotPresentOnEiscd(): StubMapping = stubOk(validateUrl, ValidateJson.sortCodeNotPresentOnEiscd)

    def sortCodeDoesNotSupportsDirectDebit(): StubMapping = stubOk(validateUrl, ValidateJson.sortCodeDoesNotSupportsDirectDebit)

    def ensureBarsValidateNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(validateUrl)))
  }

  object VerifyPersonalStub {
    private val verifyPersonalUrl = "/verify/personal"

    def success(): StubMapping = stubOk(verifyPersonalUrl, VerifyJson.success)

    def accountExistsError(): StubMapping = stubOk(verifyPersonalUrl, VerifyJson.accountExistsError)

    def nameMatchesError(): StubMapping = stubOk(verifyPersonalUrl, VerifyJson.nameMatchesError)

    def nameDoesNotMatch(): StubMapping = stubOk(verifyPersonalUrl, VerifyJson.nameDoesNotMatch)

    def ensureBarsVerifyPersonalNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(verifyPersonalUrl)))
  }

  object VerifyBusinessStub {
    private val verifyBusinessUrl = "/verify/business"

    def success(): StubMapping = stubOk(verifyBusinessUrl, VerifyJson.success)

    def accountExistsError(): StubMapping = stubOk(verifyBusinessUrl, VerifyJson.accountExistsError)

    def nameMatchesError(): StubMapping = stubOk(verifyBusinessUrl, VerifyJson.nameMatchesError)

    def nameDoesNotMatch(): StubMapping = stubOk(verifyBusinessUrl, VerifyJson.nameDoesNotMatch)

    def ensureBarsVerifyBusinessNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(verifyBusinessUrl)))
  }

  def verifyBarsNotCalled(): Unit = {
    ValidateStub.ensureBarsValidateNotCalled()
    VerifyPersonalStub.ensureBarsVerifyPersonalNotCalled()
    VerifyBusinessStub.ensureBarsVerifyBusinessNotCalled()
  }

  private def stubOk(url: String, responseJson: String): StubMapping = {
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
