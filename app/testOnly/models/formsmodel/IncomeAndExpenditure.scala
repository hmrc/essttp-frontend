/*
 * Copyright 2025 HM Revenue & Customs
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

package testOnly.models.formsmodel

import essttp.rootmodel.AmountInPence
import play.api.libs.json.{Json, OFormat}
import testOnly.models.formsmodel.IncomeAndExpenditure.{Expenditure, Income}

final case class IncomeAndExpenditure(
    income:      Income,
    expenditure: Expenditure
)

object IncomeAndExpenditure {

  final case class Income(mainIncome: AmountInPence, otherIncome: AmountInPence)

  object Income {

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    implicit val formaat: OFormat[Income] = Json.format

  }

  final case class Expenditure(
      wagesAndSalaries:      AmountInPence,
      mortgageAndRent:       AmountInPence,
      bills:                 AmountInPence,
      materialAndStockCosts: AmountInPence,
      businessTravel:        AmountInPence,
      employeeBenefits:      AmountInPence,
      other:                 AmountInPence
  )

  object Expenditure {

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    implicit val formaat: OFormat[Expenditure] = Json.format

  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val formaat: OFormat[IncomeAndExpenditure] = Json.format

  val default: IncomeAndExpenditure = IncomeAndExpenditure(
    Income(
      AmountInPence(600000),
      AmountInPence.zero
    ),
    Expenditure(
      AmountInPence.zero,
      AmountInPence.zero,
      AmountInPence.zero,
      AmountInPence.zero,
      AmountInPence.zero,
      AmountInPence.zero,
      AmountInPence.zero
    )
  )

}
