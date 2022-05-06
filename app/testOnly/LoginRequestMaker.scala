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

package testOnly

import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import play.api.libs.json.{JsArray, JsNull, JsObject, Json}
import testOnly.testusermodel.{EpayeEnrolment, TestUser}

object LoginRequestMaker {

  def makeLoginRequestBody(testUser: TestUser): JsObject = {
    val credId: String = testUser.authorityId.value
    val maybeNino = testUser.nino.fold(Json.obj())(v => Json.obj("nino" -> v.value))

    val affinityGroup = testUser.affinityGroup.toJson match {
      case o: JsObject => o
      case _           => Json.obj("affinityGroup" -> testUser.affinityGroup.toString)
    }

    val enrolments: JsArray = {
      val eList: Seq[JsObject] = List(
        testUser.epayeEnrolment.map(makeEpayeEnrolmentJson)
      ).collect { case Some(e) => e }
      JsArray(eList)
    }

    Json.obj(
      "credId" -> credId,
      "affinityGroup" -> testUser.affinityGroup.toString,
      "confidenceLevel" -> testUser.confidenceLevel,
      "credentialStrength" -> "weak",
      "credentialRole" -> "User",
      "usersName" -> JsNull,
      "enrolments" -> enrolments,
      "delegatedEnrolments" -> Json.arr(),
      "email" -> "user@test.com",
      "gatewayInformation" -> Json.obj()
    ) ++
      maybeNino ++
      affinityGroup
  }

  private def makeEpayeEnrolmentJson(epayeEnrolment: EpayeEnrolment) = Json.obj(
    "key" -> "IR-PAYE",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "TaxOfficeNumber",
        "value" -> epayeEnrolment.taxOfficeNumber.value
      ),
      Json.obj(
        "key" -> "TaxOfficeReference",
        "value" -> epayeEnrolment.taxOfficeReference.value
      ),
    ),
    "state" -> epayeEnrolment.enrolmentStatus.toString
  )
}
