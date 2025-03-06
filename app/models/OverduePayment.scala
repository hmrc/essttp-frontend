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

package models

import essttp.rootmodel.AmountInPence
import essttp.rootmodel.ttp.eligibility.MainTrans
import essttp.rootmodel.ttp.{DdInProgress, IsInterestBearingCharge}
import play.api.libs.json.{Format, Json}

final case class OverduePayment(
  invoicePeriod:         InvoicePeriod,
  amount:                AmountInPence,
  interestBearingCharge: Option[IsInterestBearingCharge],
  ddInProgress:          Option[DdInProgress] = None,
  mainTrans:             MainTrans
)

object OverduePayment {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  given Format[OverduePayment] = Json.format[OverduePayment]
}
