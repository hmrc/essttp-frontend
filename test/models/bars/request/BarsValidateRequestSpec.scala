/*
 * Copyright 2025 HM Revenue & Customs
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

package models.bars.request

import play.api.libs.json.{JsSuccess, Json}
import testsupport.UnitSpec
import testsupport.Givens.{canEqualJsResult, canEqualJsValue}

class BarsValidateRequestSpec extends UnitSpec {

  "BarsValidateRequest" - {

    "must have a format instance" in {
      val request = BarsValidateRequest(
        BarsBankAccount(
          "123456",
          "12345678"
        )
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "account": {
          |    "sortCode": "123456",
          |    "accountNumber": "12345678"
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(request) shouldBe expectedJson
      expectedJson.validate[BarsValidateRequest] shouldBe JsSuccess(request)
    }

  }

}
