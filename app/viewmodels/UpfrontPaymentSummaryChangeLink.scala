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

package viewmodels

import controllers.routes
import enumeratum._
import play.api.mvc.Call

sealed trait UpfrontPaymentSummaryChangeLink extends EnumEntry {
  def targetPage: Call
  def changeLink: Call = routes.UpfrontPaymentController.changeFromUpfrontPaymentSummary(entryName)
}

object UpfrontPaymentSummaryChangeLink extends Enum[UpfrontPaymentSummaryChangeLink] {

  case object CanPayUpfront extends UpfrontPaymentSummaryChangeLink {
    lazy val targetPage: Call = routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment
  }

  case object UpfrontPaymentAmount extends UpfrontPaymentSummaryChangeLink {
    lazy val targetPage: Call = routes.UpfrontPaymentController.upfrontPaymentAmount
  }

  override val values = findValues

}
