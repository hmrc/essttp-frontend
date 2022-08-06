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

package models.bars

import essttp.rootmodel.bank.{AccountNumber, SortCode}
import models.bars.BarsCommon.BarsBankAccount
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import testsupport.UnitSpec
import org.scalatest.prop.TableDrivenPropertyChecks._

class BarsCommonSpec extends UnitSpec {
  "BarsBankAccount.padded" - {
    "should ensure that Account Number is not less than 8 characters, left-padding with zeroes if necessary" in {

      val sortCode = SortCode("123456")
      forAll(
        Table(
          ("input accountNumber", "bars accountNumber"),
          (AccountNumber("12345678"), AccountNumber("12345678")),
          (AccountNumber("2345678"), AccountNumber("02345678")),
          (AccountNumber("345678"), AccountNumber("00345678")),
          (AccountNumber("123456789"), AccountNumber("123456789")), // frontend prevents this
          (AccountNumber(""), AccountNumber("00000000")) // frontend prevents this
        )
      ) { (inputAccountNumber: AccountNumber, barsAccountNumber: AccountNumber) =>
          val bankAccount = BarsBankAccount.padded(sortCode, inputAccountNumber)

          bankAccount.accountNumber shouldBe barsAccountNumber
        }
    }
  }

}
