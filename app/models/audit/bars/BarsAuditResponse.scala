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

package models.audit.bars

import models.bars.response._
import play.api.libs.json.{Json, OWrites}

final case class BarsAuditResponse(
    isBankAccountValid: Boolean,
    barsResponse:       Either[BarsError, VerifyResponse]
)

object BarsAuditResponse {

  implicit val responseWrites: OWrites[Either[BarsError, VerifyResponse]] = {
    OWrites{
      case Left(e: BarsError) =>
        e.barsResponse match {
          case ValidateResponse(barsValidateResponse) => BarsValidateResponse.format.writes(barsValidateResponse)
          case VerifyResponse(barsVerifyResponse)     => BarsVerifyResponse.format.writes(barsVerifyResponse)
          case SortCodeOnDenyList(barsErrorResponse)  => BarsErrorResponse.format.writes(barsErrorResponse)
        }

      case Right(VerifyResponse(barsVerifyResponse: BarsVerifyResponse)) => BarsVerifyResponse.format.writes(barsVerifyResponse)
    }
  }

  implicit val writes: OWrites[BarsAuditResponse] = Json.writes

}
