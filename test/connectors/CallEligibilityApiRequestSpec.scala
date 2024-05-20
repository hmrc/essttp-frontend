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

import essttp.rootmodel.ttp.eligibility.{IdType, IdValue, Identification}
import models.EligibilityReqIdentificationFlag
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class CallEligibilityApiRequestSpec extends AnyFreeSpec with Matchers {

  "callEligibilityApiRequest" - {
    "when EligibilityReqIdentificationFlag is true" - {
      implicit val flag: EligibilityReqIdentificationFlag = EligibilityReqIdentificationFlag(value = true)

      "should correctly serialize" in {
        val request = CallEligibilityApiRequest(
          "eSSTTP", List(Identification(IdType("someType"), IdValue("someValue"))), "regime", returnFinancialAssessment = true
        )
        val json = Json.toJson(request)(CallEligibilityApiRequest.format)
        (json \ "channelIdentifier").as[String] shouldEqual "eSSTTP"
        (json \ "identification").as[List[JsValue]].headOption.map(_.as[Identification])
          .getOrElse(Identification(IdType(""), IdValue(""))) shouldEqual Identification(IdType("someType"), IdValue("someValue"))
        (json \ "regimeType").as[String] shouldEqual "regime"
        (json \ "returnFinancialAssessment").as[Boolean] shouldEqual true
      }
    }

    "when EligibilityReqIdentificationFlag is false" - {
      implicit val flag: EligibilityReqIdentificationFlag = EligibilityReqIdentificationFlag(value = false)

      "should correctly serialize" in {
        val request = CallEligibilityApiRequest(
          "eSSTTP", List(Identification(IdType("someType"), IdValue("someValue"))), "regime", returnFinancialAssessment = true
        )
        val json = Json.toJson(request)(CallEligibilityApiRequest.format)
        (json \ "channelIdentifier").as[String] shouldEqual "eSSTTP"
        (json \ "idType").as[String] shouldEqual "someType"
        (json \ "idValue").as[String] shouldEqual "someValue"
        (json \ "regimeType").as[String] shouldEqual "regime"
        (json \ "returnFinancialAssessment").as[Boolean] shouldEqual true
      }
    }
  }
}
