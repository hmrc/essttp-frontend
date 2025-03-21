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

package models.bars.request

import play.api.libs.json.{Json, OFormat}

final case class BarsBankAccount(
  sortCode:      String,
  accountNumber: String
)

object BarsBankAccount {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  given OFormat[BarsBankAccount] = Json.format

  def padded(sortCode: String, accountNumber: String): BarsBankAccount =
    BarsBankAccount(sortCode, leftPad(accountNumber))

  private val minimumLength                          = 8
  private val padStr                                 = '0'
  private def leftPad(accountNumber: String): String =
    accountNumber.reverse
      .padTo(minimumLength, padStr)
      .reverse

}
