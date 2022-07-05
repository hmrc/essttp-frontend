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

package models

import cats.implicits.catsSyntaxEq
import essttp.rootmodel.AmountInPence
import essttp.rootmodel.bank.BankDetails
import play.api.libs.json.{Format, Json}

//todo delete this file when we finish building journey
final case class UserAnswers(
    hasUpfrontPayment: Option[Boolean],
    upfrontAmount:     Option[AmountInPence],
    affordableAmount:  Option[AmountInPence],
    paymentDay:        Option[String],
    differentDay:      Option[Int],
    monthsToPay:       Option[InstalmentOption],
    bankDetails:       Option[BankDetails]
) {
  def getAffordableAmount: AmountInPence = affordableAmount.getOrElse(sys.error("trying to get non-existent affordable amount"))
  def getMonthsToPay: InstalmentOption = monthsToPay.getOrElse(sys.error("trying to get non-existent months to pay"))
  def getPaymentDay: Int = paymentDay match {
    case Some(s: String) => if (s === "28") 28 else differentDay.getOrElse(sys.error("trying to get non-existent payment day"))
    case None            => sys.error("trying to get non-existent payment day")
  }
  def getHasUpfrontPayment: String = hasUpfrontPayment match {
    case Some(b: Boolean) => if (b) "Yes" else "No"
    case None             => sys.error("trying to get non-existent HasUpfrontPayment")
  }
}
object UserAnswers {

  val empty: UserAnswers = UserAnswers(None, None, None, None, None, None, None)

  implicit val format: Format[UserAnswers] = Json.format

}
