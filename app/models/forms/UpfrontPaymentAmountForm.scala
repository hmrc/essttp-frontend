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

package models.forms

import essttp.journey.model.Journey
import essttp.journey.model.ttp.DebtTotalAmount
import essttp.rootmodel.AmountInPence
import essttp.utils.Errors
import models.MoneyUtil.amountOfMoneyFormatter
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms}

object UpfrontPaymentAmountForm {
  def form(journey: Journey, minimumPaymentAmount: AmountInPence): Form[BigDecimal] = {
    val maximumDebtAmount = formHelperToDeriveDebtAmount(journey)
    Form(
      mapping(
        "UpfrontPaymentAmount" -> Forms.of(
          amountOfMoneyFormatter(minimumPaymentAmount.inPounds > _, AmountInPence(maximumDebtAmount.value).inPounds < _)
        )
      )(identity)(Some(_))
    )
  }

  def formHelperToDeriveDebtAmount(journey: Journey): DebtTotalAmount = journey match {
    case j: Journey.BeforeEligibilityChecked =>
      Errors.throwBadRequestException(s"This should never happen, user should have an eligibilityCheck by now, investigate journey: [$j]")
    //    case j: Journey.Stages.AnsweredCanPayUpfront => j.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount).head
    case j: Journey.AfterEligibilityChecked => j.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount).head
  }

}
