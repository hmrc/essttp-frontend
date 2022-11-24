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

package models.audit.eligibility

import cats.implicits.catsSyntaxEq
import cats.instances.string._
import essttp.rootmodel.ttp.ChargeTypeAssessment
import models.audit.{AuditDetail, TaxDetail}
import play.api.libs.json._

final case class EligibilityCheckAuditDetail(
    eligibilityResult:               EligibilityResult,
    enrollmentReasons:               Option[EnrollmentReasons],
    noEligibilityReasons:            Int,
    eligibilityReasons:              List[String],
    origin:                          String,
    taxType:                         String,
    taxDetail:                       TaxDetail,
    authProviderId:                  String,
    chargeTypeAssessment:            List[ChargeTypeAssessment],
    correlationId:                   String,
    futureChargeLiabilitiesExcluded: Option[Boolean]
) extends AuditDetail {
  val auditType: String = "EligibilityCheck"
}

object EligibilityCheckAuditDetail {
  implicit val writes: OWrites[EligibilityCheckAuditDetail] = {
    val w: OWrites[EligibilityCheckAuditDetail] = Json.writes[EligibilityCheckAuditDetail]
    w.transform { jsObject: JsObject =>
      jsObject
        .transformIfFieldName("debtTotalAmount", penceToPounds)
        .transformIfFieldName("accruedInterest", penceToPounds)
        .transformIfFieldName("outstandingAmount", penceToPounds)
    }
  }

  private def penceToPounds(jsValue: JsValue): JsNumber = jsValue match {
    case j: JsNumber => JsNumber(j.value / 100)
    case e           => throw new IllegalArgumentException(s"Expected JsNumber but got ${e.getClass.toString}")
  }

  implicit class JsObjectStuff(private val jsObject: JsObject) extends AnyVal {
    def transformIfFieldName(fieldName: String, transformation: JsValue => JsValue): JsObject = {
        @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
        def transform(currentFieldNameAndJsValue: (String, JsValue)): (String, JsValue) = {
          val (currentFieldName, currentJsValue) = currentFieldNameAndJsValue
          currentJsValue match {
            case JsObject(underlying) =>
              val transformedJsObject = JsObject(underlying.map(transform))
              if (currentFieldName === fieldName) {
                (currentFieldName, transformation(transformedJsObject))
              } else {
                (currentFieldName, transformedJsObject)
              }
            case JsArray(arr) =>
              val transformedJsArray: JsArray = JsArray(arr.map(j => transform((currentFieldName, j))._2))
              if (currentFieldName === fieldName) {
                (currentFieldName, transformation(transformedJsArray))
              } else {
                (currentFieldName, transformedJsArray)
              }
            case value if currentFieldName === fieldName => (currentFieldName, transformation(value))
            case value if currentFieldName =!= fieldName => (currentFieldName, value)
            case value: JsValue                          => (currentFieldName, value)
          }
        }

      JsObject(jsObject.fieldSet.map(transform).toSeq)
    }
  }
}
