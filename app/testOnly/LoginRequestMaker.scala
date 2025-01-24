/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{JsArray, JsNull, JsObject, Json}
import testOnly.models.testusermodel.{EpayeEnrolment, IrSaEnrolment, MtdItEnrolment, TestUser, VatEnrolment}

object LoginRequestMaker {

  def makeLoginRequestBody(testUser: TestUser): JsObject = {
    val credId: String = testUser.authorityId.value
    val maybeNino = testUser.nino.fold(Json.obj())(v => Json.obj("nino" -> v.value))

    val affinityGroup: JsObject = testUser.affinityGroup.toJson match {
      case o: JsObject => o
      case _           => Json.obj("affinityGroup" -> testUser.affinityGroup.toString)
    }

    val enrolments: JsArray = {
      val eList: Seq[JsObject] = List(
        testUser.epayeEnrolment.map(makeEpayeEnrolmentJson),
        testUser.vatEnrolment.map(makeVatEnrolmentJson),
        testUser.irSaEnrolment.map(makeIrSaEnrolmentJson),
        testUser.mtdItEnrolment.map(makeIrSaAndMtdItEnrolmentJson)
      ).collect { case Some(e) => e }
      JsArray(eList)
    }

    Json.obj(
      "credId" -> credId,
      "affinityGroup" -> testUser.affinityGroup.toString,
      "confidenceLevel" -> testUser.confidenceLevel,
      "credentialStrength" -> "strong",
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

  private def makeEpayeEnrolmentJson(epayeEnrolment: EpayeEnrolment): JsObject = Json.obj(
    "key" -> "IR-PAYE",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "TaxOfficeNumber",
        "value" -> epayeEnrolment.taxOfficeNumber.value
      ),
      Json.obj(
        "key" -> "TaxOfficeReference",
        "value" -> epayeEnrolment.taxOfficeReference.value
      )
    ),
    "state" -> epayeEnrolment.enrolmentStatus.toString
  )

  private def makeVatEnrolmentJson(vatEnrolment: VatEnrolment): JsObject = Json.obj(
    "key" -> "HMRC-MTD-VAT",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "VRN",
        "value" -> vatEnrolment.vrn.value
      )
    ),
    "state" -> vatEnrolment.enrolmentStatus.toString
  )

  private def makeIrSaEnrolmentJson(irSaEnrolment: IrSaEnrolment): JsObject = Json.obj(
    "key" -> "IR-SA",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "UTR",
        "value" -> irSaEnrolment.saUtr.value
      )
    ),
    "state" -> irSaEnrolment.enrolmentStatus.toString
  )

  private def makeIrSaAndMtdItEnrolmentJson(mtdItEnrolment: MtdItEnrolment): JsObject = Json.obj(
    "key" -> "HMRC-MTD-IT",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "MTDITID",
        "value" -> mtdItEnrolment.mtdItId
      )
    ),
    "state" -> mtdItEnrolment.enrolmentStatus.toString
  )

}
