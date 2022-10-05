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

package util

import play.api.libs.json._

object JsonTransformers {

  private val penceToPounds: Reads[JsNumber] = Reads.of[JsNumber].map {
    case JsNumber(pence) => JsNumber(pence / 100)
  }

  private val updateAccruedInterest: Reads[JsObject] = (__ \ "accruedInterest").json.update(penceToPounds)

  private val updateOutstandingAmount: Reads[JsObject] = (__ \ "outstandingAmount").json.update(penceToPounds)

  private val updateDebtTotalAmount: Reads[JsObject] = (__ \ "debtTotalAmount").json.update(penceToPounds)

  val updateCharges: Reads[JsObject] = (__ \ "charges").json.update(
    Reads.of[JsArray].map {
      case JsArray(arr) =>
        JsArray(arr.map(_.transform(updateAccruedInterest andThen updateOutstandingAmount).get))
    }
  ) andThen updateDebtTotalAmount

  val updateChargeTypeAssessment: Reads[JsObject] =
    (__ \ "chargeTypeAssessment").json.update(
      Reads.of[JsArray].map { case JsArray(arr) => JsArray(arr.map(_.transform(updateCharges).get)) }
    )

}
