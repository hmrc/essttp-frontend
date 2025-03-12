/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import essttp.rootmodel.ttp.RegimeType
import essttp.rootmodel.ttp.eligibility.{IdType, IdValue, Identification}
import models.EligibilityReqIdentificationFlag
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class CallEligibilityApiRequestSpec extends AnyFreeSpec, Matchers {

  val jsonStringTrueFlag = """
{
  "channelIdentifier": "eSSTTP",
  "identification": [{"idType":"someType", "idValue":"someValue"}],
  "regimeType": "PAYE",
  "returnFinancialAssessment": true
}
"""

  val jsonStringFalseFlag = """
{
  "channelIdentifier": "eSSTTP",
  "idType": "someType",
  "idValue": "someValue",
  "regimeType": "PAYE",
  "returnFinancialAssessment": true
}
"""

  "callEligibilityApiRequest" - {
    "when EligibilityReqIdentificationFlag is true" - {
      given EligibilityReqIdentificationFlag = EligibilityReqIdentificationFlag(value = true)

      "should correctly serialize" in {
        val request = CallEligibilityApiRequest(
          "eSSTTP",
          List(Identification(IdType("someType"), IdValue("someValue"))),
          RegimeType.EPAYE,
          returnFinancialAssessment = true
        )
        val json    = Json.toJson(request)(CallEligibilityApiRequest.format)
        (json \ "channelIdentifier").as[String] shouldEqual "eSSTTP"
        (json \ "identification")
          .as[List[JsValue]]
          .headOption
          .map(_.as[Identification])
          .getOrElse(Identification(IdType(""), IdValue(""))) shouldEqual Identification(
          IdType("someType"),
          IdValue("someValue")
        )
        (json \ "regimeType").as[String] shouldEqual "PAYE"
        (json \ "returnFinancialAssessment").as[Boolean] shouldEqual true
      }

      "should correctly deserialize" in {
        val json    = Json.parse(jsonStringTrueFlag)
        val request = json.as[CallEligibilityApiRequest](CallEligibilityApiRequest.format)
        request shouldEqual CallEligibilityApiRequest(
          "eSSTTP",
          List(Identification(IdType("someType"), IdValue("someValue"))),
          RegimeType.EPAYE,
          returnFinancialAssessment = true
        )
      }
    }

    "when EligibilityReqIdentificationFlag is false" - {
      given EligibilityReqIdentificationFlag = EligibilityReqIdentificationFlag(value = false)

      "should correctly serialize" in {
        val request = CallEligibilityApiRequest(
          "eSSTTP",
          List(Identification(IdType("someType"), IdValue("someValue"))),
          RegimeType.EPAYE,
          returnFinancialAssessment = true
        )
        val json    = Json.toJson(request)(CallEligibilityApiRequest.format)
        (json \ "channelIdentifier").as[String] shouldEqual "eSSTTP"
        (json \ "idType").as[String] shouldEqual "someType"
        (json \ "idValue").as[String] shouldEqual "someValue"
        (json \ "regimeType").as[String] shouldEqual "PAYE"
        (json \ "returnFinancialAssessment").as[Boolean] shouldEqual true
      }

      "should fail if there is nothing in the identification list" in {
        val request = CallEligibilityApiRequest(
          "eSSTTP",
          List.empty,
          RegimeType.EPAYE,
          returnFinancialAssessment = true
        )

        val exception = intercept[RuntimeException] {
          Json.toJson(request)(CallEligibilityApiRequest.format)
        }
        exception.getMessage should include(
          "There should be something in the identification list and there was nothing"
        )
      }

      "should fail if there is more than one Identification in the identification list" in {
        val request   = CallEligibilityApiRequest(
          "eSSTTP",
          List(
            Identification(IdType("type1"), IdValue("value2")),
            Identification(IdType("type2"), IdValue("value2"))
          ),
          RegimeType.EPAYE,
          returnFinancialAssessment = true
        )
        val exception = intercept[RuntimeException] {
          Json.toJson(request)(CallEligibilityApiRequest.format)
        }
        exception.getMessage should include(
          "There was more than one Identification in the identification list. We don't know what to do with that"
        )
      }

      "should correctly deserialize" in {
        val json    = Json.parse(jsonStringFalseFlag)
        val request = json.as[CallEligibilityApiRequest](CallEligibilityApiRequest.format)
        request shouldEqual CallEligibilityApiRequest(
          "eSSTTP",
          List(Identification(IdType("someType"), IdValue("someValue"))),
          RegimeType.EPAYE,
          returnFinancialAssessment = true
        )
      }
    }
  }
}
