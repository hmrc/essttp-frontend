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

package models.bars.request

import org.scalatest.prop.TableDrivenPropertyChecks._
import testsupport.UnitSpec

class BarsSubjectSpec extends UnitSpec {

  "BarsSubject creation" - {
    "should fail in the following combinations" in {
      forAll(
        Table(
          ("full name", "first name", "last name"),
          (None, None, None),
          (None, Some("onlyFirstName"), None),
          (None, None, Some("onlyLastName")),
          (Some("fullName"), Some("andFirstName"), None),
          (Some("fullName"), None, Some("andLastName")),
          (Some("fullName"), Some("andFirstName"), Some("andLastName"))
        )
      ) { (fullName: Option[String], firstName: Option[String], lastName: Option[String]) =>
          assertThrows[IllegalArgumentException] {
            BarsSubject(
              title     = None,
              name      = fullName,
              firstName = firstName,
              lastName  = lastName,
              dob       = None,
              address   = None
            )
          }
        }
    }
  }
}
