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
import play.api.http.Status._
import testsupport.stubs.WireMockHelpers._

object BarsStub {

  object ValidateStub {
    private val validateUrl = "/validate/bank-details"

    def success(): StubMapping = stubForPostWithResponseBody(validateUrl, ValidateJson.success)

    def accountNumberNotWellFormatted(): StubMapping = stubForPostWithResponseBody(validateUrl, ValidateJson.accountNumberNotWellFormatted)

    def sortCodeNotPresentOnEiscd(): StubMapping = stubForPostWithResponseBody(validateUrl, ValidateJson.sortCodeNotPresentOnEiscd)

    def sortCodeDoesNotSupportsDirectDebit(): StubMapping = stubForPostWithResponseBody(validateUrl, ValidateJson.sortCodeDoesNotSupportsDirectDebit)

    def sortCodeOnDenyList(): StubMapping =
      stubForPostWithResponseBody(validateUrl, ValidateJson.sortCodeOnDenyList, BAD_REQUEST)

    def ensureBarsValidateNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(validateUrl)))

    def ensureBarsValidateCalled(formData: List[(String, String)]): Unit = {
      val sortCode = getExpectedFormValue("sortCode", formData)
      val accountNumber = getExpectedFormValue("accountNumber", formData)

      verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo(validateUrl))
          .withRequestBody(equalToJson(
            s"""{
            |  "account" : {
            |    "sortCode" : "$sortCode",
            |    "accountNumber" : "$accountNumber"
            |  }
            |}""".stripMargin
          ))
      )
    }
  }

  object VerifyStub {
    def ensureBarsVerifyNotCalled(): Unit = {
      VerifyPersonalStub.ensureBarsVerifyPersonalNotCalled()
      VerifyBusinessStub.ensureBarsVerifyBusinessNotCalled()
    }

  }
  object VerifyPersonalStub {
    private val verifyPersonalUrl = "/verify/personal"

    def success(): StubMapping = stubForPostWithResponseBody(verifyPersonalUrl, VerifyJson.success)

    def accountExistsError(): StubMapping = stubForPostWithResponseBody(verifyPersonalUrl, VerifyJson.accountExistsError)

    def accountDoesNotExist(): StubMapping = stubForPostWithResponseBody(verifyPersonalUrl, VerifyJson.accountDoesNotExist)

    def nameMatchesError(): StubMapping = stubForPostWithResponseBody(verifyPersonalUrl, VerifyJson.nameMatchesError)

    def nameDoesNotMatch(): StubMapping = stubForPostWithResponseBody(verifyPersonalUrl, VerifyJson.nameDoesNotMatch)

    def otherBarsError(): StubMapping = stubForPostWithResponseBody(verifyPersonalUrl, VerifyJson.otherBarsError)

    def ensureBarsVerifyPersonalNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(verifyPersonalUrl)))

    def ensureBarsVerifyPersonalCalled(formData: List[(String, String)]): Unit = {
      BarsStub.ValidateStub.ensureBarsValidateCalled(formData)
      BarsStub.VerifyBusinessStub.ensureBarsVerifyBusinessNotCalled()

      val sortCode = getExpectedFormValue("sortCode", formData)
      val accountNumber = getExpectedFormValue("accountNumber", formData)
      val name = getExpectedFormValue("name", formData)

      verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo(verifyPersonalUrl))
          .withRequestBody(equalToJson(
            s"""{
               |  "account" : {
               |    "sortCode" : "$sortCode",
               |    "accountNumber" : "$accountNumber"
               |  },
               |  "subject" : {
               |    "name" : "$name"
               |  }
               |}""".stripMargin
          ))
      )
    }
  }

  object VerifyBusinessStub {
    private val verifyBusinessUrl = "/verify/business"

    def success(): StubMapping = stubForPostWithResponseBody(verifyBusinessUrl, VerifyJson.success)

    def accountExistsError(): StubMapping = stubForPostWithResponseBody(verifyBusinessUrl, VerifyJson.accountExistsError)

    def accountDoesNotExist(): StubMapping = stubForPostWithResponseBody(verifyBusinessUrl, VerifyJson.accountDoesNotExist)

    def nameMatchesError(): StubMapping = stubForPostWithResponseBody(verifyBusinessUrl, VerifyJson.nameMatchesError)

    def nameDoesNotMatch(): StubMapping = stubForPostWithResponseBody(verifyBusinessUrl, VerifyJson.nameDoesNotMatch)

    def otherBarsError(): StubMapping = stubForPostWithResponseBody(verifyBusinessUrl, VerifyJson.otherBarsError)

    def ensureBarsVerifyBusinessNotCalled(): Unit =
      verify(exactly(0), postRequestedFor(urlPathEqualTo(verifyBusinessUrl)))

    def ensureBarsVerifyBusinessCalled(formData: List[(String, String)]): Unit = {
      BarsStub.ValidateStub.ensureBarsValidateCalled(formData)
      BarsStub.VerifyPersonalStub.ensureBarsVerifyPersonalNotCalled()

      val sortCode = getExpectedFormValue("sortCode", formData)
      val accountNumber = getExpectedFormValue("accountNumber", formData)
      val companyName = getExpectedFormValue("name", formData)

      verify(
        exactly(1),
        postRequestedFor(urlPathEqualTo(verifyBusinessUrl))
          .withRequestBody(equalToJson(
            s"""{
               |  "account" : {
               |    "sortCode" : "$sortCode",
               |    "accountNumber" : "$accountNumber"
               |  },
               |  "business" : {
               |    "companyName" : "$companyName"
               |  }
               |}""".stripMargin
          ))
      )
    }
  }

  def verifyBarsNotCalled(): Unit = {
    ValidateStub.ensureBarsValidateNotCalled()
    VerifyPersonalStub.ensureBarsVerifyPersonalNotCalled()
    VerifyBusinessStub.ensureBarsVerifyBusinessNotCalled()
  }

  private def getExpectedFormValue(field: String, formData: List[(String, String)]): String =
    formData.collectFirst{ case (x, value) if x == field => value }
      .getOrElse(throw new Exception(s"Field: $field, not present in form: ${formData.toString}"))

}
