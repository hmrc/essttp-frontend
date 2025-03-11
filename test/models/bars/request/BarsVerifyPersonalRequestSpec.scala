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

class BarsVerifyPersonalRequestSpec extends UnitSpec {

  "BarsVerifyBusinessRequest" - {

    "must have a format instance" in {
      val request = BarsVerifyPersonalRequest(
        BarsBankAccount(
          "123456",
          "12345678"
        ),
        BarsSubject(
          Some("Dr"),
          None,
          Some("Plopsy"),
          Some("Plop"),
          Some("1234-12-04"),
          Some(
            BarsAddress(
              List("line1", "line2"),
              Some("town"),
              Some("postcode")
            )
          )
        )
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "account": {
          |    "sortCode": "123456",
          |    "accountNumber": "12345678"
          |  },
          |  "subject": {
          |    "title": "Dr",
          |    "firstName": "Plopsy",
          |    "lastName": "Plop",
          |    "dob": "1234-12-04",
          |    "address": {
          |      "lines": [ "line1", "line2" ],
          |      "town": "town",
          |      "postcode": "postcode"
          |    }
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(request) shouldBe expectedJson
      expectedJson.validate[BarsVerifyPersonalRequest] shouldBe JsSuccess(request)
    }

  }

}
