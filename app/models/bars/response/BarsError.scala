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

sealed trait BarsValidateError extends BarsError {
  override val barsResponse: BarsResponse
}

sealed trait SortCodeOnDenyListError extends BarsValidateError

sealed trait BarsVerifyError extends BarsError {
  override val barsResponse: VerifyResponse
}

final case class AccountNumberNotWellFormattedValidateResponse(barsResponse: ValidateResponse) extends BarsValidateError
final case class SortCodeNotPresentOnEiscdValidateResponse(barsResponse: ValidateResponse) extends BarsValidateError
final case class SortCodeDoesNotSupportDirectDebitValidateResponse(barsResponse: ValidateResponse) extends BarsValidateError

final case class SortCodeOnDenyListErrorResponse(barsResponse: SortCodeOnDenyList) extends SortCodeOnDenyListError

final case class AccountNumberNotWellFormatted(barsResponse: VerifyResponse) extends BarsVerifyError
final case class SortCodeNotPresentOnEiscd(barsResponse: VerifyResponse) extends BarsVerifyError
final case class SortCodeDoesNotSupportDirectDebit(barsResponse: VerifyResponse) extends BarsVerifyError
final case class ThirdPartyError(barsResponse: VerifyResponse) extends BarsVerifyError
final case class AccountDoesNotExist(barsResponse: VerifyResponse) extends BarsVerifyError
final case class NameDoesNotMatch(barsResponse: VerifyResponse) extends BarsVerifyError
final case class OtherBarsError(barsResponse: VerifyResponse) extends BarsVerifyError
// not strictly a BARs error, but we use this error to indicate too many attempts
final case class TooManyAttempts(barsResponse: VerifyResponse, lockoutExpiry: Instant) extends BarsVerifyError
