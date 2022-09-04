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

package models.bars.response

import java.time.Instant

sealed trait BarsError {
  val barsResponse: BarsResponse
}

final case class ThirdPartyError(barsResponse: BarsResponse) extends BarsError
final case class AccountNumberNotWellFormatted(barsResponse: BarsResponse) extends BarsError
final case class SortCodeNotPresentOnEiscd(barsResponse: BarsResponse) extends BarsError
final case class SortCodeDoesNotSupportDirectDebit(barsResponse: BarsResponse) extends BarsError
final case class AccountDoesNotExist(barsResponse: BarsResponse) extends BarsError
final case class NameDoesNotMatch(barsResponse: BarsResponse) extends BarsError
final case class SortCodeOnDenyListError(barsResponse: BarsResponse) extends BarsError
final case class OtherBarsError(barsResponse: BarsResponse) extends BarsError
// not strictly a BARs error, but we use this error to indicate too many attempts
final case class TooManyAttempts(barsResponse: BarsResponse, lockoutExpiry: Instant) extends BarsError
