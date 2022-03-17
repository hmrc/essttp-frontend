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

package testOnly.models

import cats.Eq

sealed trait TestOnlyJourney extends Product with Serializable

object TestOnlyJourney {
  case object EpayeFromGovUk extends TestOnlyJourney
  case object EpayeFromBTA extends TestOnlyJourney
  case object EpayeNoOrigin extends TestOnlyJourney
  case object VATFromGovUk extends TestOnlyJourney
  case object VATFromBTA extends TestOnlyJourney
  case object VATNoOrigin extends TestOnlyJourney
  implicit val eq: Eq[TestOnlyJourney] = Eq.fromUniversalEquals
}
