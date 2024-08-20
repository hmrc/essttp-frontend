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

package connectors

import essttp.rootmodel.ttp.RegimeType
import essttp.rootmodel.ttp.eligibility.{IdType, IdValue, Identification}
import models.EligibilityReqIdentificationFlag
import play.api.libs.functional.syntax._
import play.api.libs.json._

final case class CallEligibilityApiRequest(
    channelIdentifier:         String,
    identification:            List[Identification],
    regimeType:                RegimeType,
    returnFinancialAssessment: Boolean
)

object CallEligibilityApiRequest {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit def format(implicit e: EligibilityReqIdentificationFlag): Format[CallEligibilityApiRequest] = {
    val writes = if (e.value) {
      Json.writes[CallEligibilityApiRequest]
    } else {
      OWrites { callEligibilityApiRequest: CallEligibilityApiRequest =>
        callEligibilityApiRequest.identification match {
          case Nil => sys.error("There should be something in the identification list and there was nothing")
          case head :: Nil => Json.obj(
            "channelIdentifier" -> callEligibilityApiRequest.channelIdentifier,
            "idType" -> head.idType,
            "idValue" -> head.idValue,
            "regimeType" -> RegimeType.format.writes(callEligibilityApiRequest.regimeType),
            "returnFinancialAssessment" -> callEligibilityApiRequest.returnFinancialAssessment
          )
          case _ => sys.error("There was more than one Identification in the identification list. We don't know what to do with that")
        }
      }
    }

    val reads: Reads[CallEligibilityApiRequest] = if (e.value) {
      ((__ \ "channelIdentifier").read[String] and
        (__ \ "identification").read[List[Identification]] and
        (__ \ "regimeType").read[RegimeType] and
        (__ \ "returnFinancialAssessment").read[Boolean])(CallEligibilityApiRequest(_, _, _, _))
    } else {
      ((__ \ "channelIdentifier").read[String] and
        ((__ \ "idType").read[IdType] and (__ \ "idValue").read[IdValue]).tupled.map((Identification.apply _).tupled).map(List(_)) and
        (__ \ "regimeType").read[RegimeType] and
        (__ \ "returnFinancialAssessment").read[Boolean])(CallEligibilityApiRequest(_, _, _, _))
    }

    Format(reads, writes)
  }

}

