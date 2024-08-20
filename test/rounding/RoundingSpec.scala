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

package rounding

import controllers.MonthlyPaymentAmountController
import essttp.rootmodel.AmountInPence
import org.scalatest.prop.TableDrivenPropertyChecks._
import testsupport.UnitSpec

class RoundingSpec extends UnitSpec {
  "MonthlyPaymentAmountController.roundingForMinMax" - {
    "should round correctly to nearest £1 for amountLeft less than £10, or nearest £10 for amountLeft more than £10" in {
      forAll(
        Table(
          ("amountLeft", "minAmount", "maxAmount", "roundedMin", "roundedMax"),
          (AmountInPence(1), AmountInPence(1000), AmountInPence(2000), AmountInPence(1000), AmountInPence(2000)),
          (AmountInPence(999), AmountInPence(1000), AmountInPence(2000), AmountInPence(1000), AmountInPence(2000)),
          (AmountInPence(999), AmountInPence(1049), AmountInPence(2049), AmountInPence(1000), AmountInPence(2000)),
          (AmountInPence(999), AmountInPence(1051), AmountInPence(2049), AmountInPence(1100), AmountInPence(2000)),
          (AmountInPence(999), AmountInPence(1049), AmountInPence(2051), AmountInPence(1000), AmountInPence(2100)),
          (AmountInPence(999), AmountInPence(1051), AmountInPence(2051), AmountInPence(1100), AmountInPence(2100)),
          (AmountInPence(1001), AmountInPence(1501), AmountInPence(21146), AmountInPence(2000), AmountInPence(21000)),
          (AmountInPence(1001), AmountInPence(1499), AmountInPence(21146), AmountInPence(1000), AmountInPence(21000)),
          (AmountInPence(1001), AmountInPence(1501), AmountInPence(25001), AmountInPence(2000), AmountInPence(25000)),
          (AmountInPence(1001), AmountInPence(1499), AmountInPence(25001), AmountInPence(1000), AmountInPence(25000)),
          (AmountInPence(1001), AmountInPence(1505), AmountInPence(21146), AmountInPence(2000), AmountInPence(21000)),
          (AmountInPence(10001), AmountInPence(14400), AmountInPence(25001), AmountInPence(14000), AmountInPence(25000)),
          (AmountInPence(10001), AmountInPence(15500), AmountInPence(25650), AmountInPence(15000), AmountInPence(26000)),
          (AmountInPence(10001), AmountInPence(15501), AmountInPence(25650), AmountInPence(16000), AmountInPence(26000))
        )
      ) { (amountLeft: AmountInPence, minAmount: AmountInPence, maxAmount: AmountInPence, roundedMin: AmountInPence, roundedMax: AmountInPence) =>
          MonthlyPaymentAmountController.roundingForMinMax(amountLeft, minAmount, maxAmount) shouldBe Tuple2(roundedMin, roundedMax)
        }
    }
  }
}
