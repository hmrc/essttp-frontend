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

package testsupport.testdata

object TdEssttpDatesBodies {

  def extremeDatesRequest(): String =
    """{
      |
      |""".stripMargin

  def extremeDatesResponse(): String =
    """{
      |  "initialPaymentDate": "2022-06-24",
      |  "earliestPlanStartDate": "2022-07-14",
      |  "latestPlanStartDate": "2022-08-13"
      |}""".stripMargin

  def startDatesRequest(initialPayment: Boolean): String =
    s"""
      |{
      |   "initialPayment" : $initialPayment,
      |   "preferredDayOfMonth" : 28
      |}
      |""".stripMargin

  def startDatesResponseWithUpfrontPayment(): String =
    """{
      |  "initialPaymentDate": "2022-07-03",
      |  "instalmentStartDate": "2022-07-28"
      |}""".stripMargin

  def startDatesResponseWithoutUpfrontPayment(): String =
    """{
      |  "instalmentStartDate": "2022-08-01"
      |}""".stripMargin
}
