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

package messages

import models.{Language, Languages}
import testsupport.UnitSpec

class DateMessagesSpec extends UnitSpec {

  "DateMessages" - {

    "must have a getSuffix method that" - {

      "throws an exception when passed a number less than 1" in {
        implicit val lang: Language = Languages.English
        an[IllegalArgumentException] shouldBe thrownBy(DateMessages.getSuffix(0))
      }

      "throws an exception when passed a number more than 31" in {
        implicit val lang: Language = Languages.Welsh
        an[IllegalArgumentException] shouldBe thrownBy(DateMessages.getSuffix(32))
      }

      "returns the correct suffix in english" in {
        implicit val lang: Language = Languages.English
        List(
          1 -> "1st",
          2 -> "2nd",
          3 -> "3rd",
          4 -> "4th",
          5 -> "5th",
          6 -> "6th",
          7 -> "7th",
          8 -> "8th",
          9 -> "9th",
          10 -> "10th",
          11 -> "11th",
          12 -> "12th",
          13 -> "13th",
          14 -> "14th",
          15 -> "15th",
          16 -> "16th",
          17 -> "17th",
          18 -> "18th",
          19 -> "19th",
          20 -> "20th",
          21 -> "21st",
          22 -> "22nd",
          23 -> "23rd",
          24 -> "24th",
          26 -> "26th",
          27 -> "27th",
          28 -> "28th",
          29 -> "29th",
          30 -> "30th",
          31 -> "31st",
        ).foreach{
            case (day, expectedString) =>
              withClue(s"For day ${day.toString} and expected string $expectedString: "){
                val suffix = DateMessages.getSuffix(day)
                s"${day.toString}$suffix" shouldBe expectedString
              }
          }
      }

      "returns the correct suffix in welsh" in {
        implicit val lang: Language = Languages.Welsh
        List(
          1 -> "1af",
          2 -> "2il",
          3 -> "3ydd",
          4 -> "4ydd",
          5 -> "5ed",
          6 -> "6ed",
          7 -> "7fed",
          8 -> "8fed",
          9 -> "9fed",
          10 -> "10fed",
          11 -> "11eg",
          12 -> "12fed",
          13 -> "13eg",
          14 -> "14eg",
          15 -> "15fed",
          16 -> "16eg",
          17 -> "17eg",
          18 -> "18fed",
          19 -> "19eg",
          20 -> "20fed",
          21 -> "21ain",
          22 -> "22ain",
          23 -> "23ain",
          24 -> "24ain",
          26 -> "26ain",
          27 -> "27ain",
          28 -> "28ain",
          29 -> "29ain",
          30 -> "30ain",
          31 -> "31ain",
        ).foreach{
            case (day, expectedString) =>
              withClue(s"For day ${day.toString} and expected string $expectedString: "){
                val suffix: String = DateMessages.getSuffix(day)
                s"${day.toString}$suffix" shouldBe expectedString
              }
          }
      }

    }

  }

}
