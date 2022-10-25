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

package models.emailverification

import essttp.crypto.CryptoFormat
import essttp.crypto.CryptoFormat.NoOpCryptoFormat
import essttp.rootmodel.Email
import models.{GGCredId, Language}
import models.emailverification.RequestEmailVerificationRequest.EmailDetails
import play.api.libs.json.{JsString, Json, OWrites, Writes}

final case class RequestEmailVerificationRequest(
    credId:                    GGCredId,
    continueUrl:               String,
    origin:                    String,
    deskproServiceName:        String,
    accessibilityStatementUrl: String,
    pageTitle:                 String,
    backUrl:                   String,
    email:                     EmailDetails,
    lang:                      Language
)

object RequestEmailVerificationRequest {

  final case class EmailDetails(address: Email, enterUrl: String)

  object EmailDetails {

    implicit val writes: OWrites[EmailDetails] = {
      implicit val crypto: CryptoFormat = NoOpCryptoFormat
      Json.writes
    }
  }

  implicit val writes: OWrites[RequestEmailVerificationRequest] = {
    implicit val langWrites: Writes[Language] = Writes(l => JsString(l.code))
    Json.writes
  }

}
