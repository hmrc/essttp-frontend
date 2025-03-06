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

import cats.Semigroup
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits.{catsSyntaxTuple3Semigroupal, catsSyntaxTuple7Semigroupal}
import cats.syntax.option._
import cats.syntax.either._
import cats.syntax.validated._
import essttp.rootmodel.AmountInPence
import play.api.data.Forms.{mapping, of}
import play.api.data.{Form, FormError}
import play.api.data.format.Formatter
import testOnly.models.formsmodel.IncomeAndExpenditure.{Expenditure, Income}

import scala.util.Try

object IncomeAndExpenditureForm {

  val mainIncomeKey            = "mainIncome"
  val otherIncomeKey           = "otherIncome"
  val wagesAndSalariesKey      = "wagesAndSalaries"
  val mortgageAndRentKey       = "mortgageAndRent"
  val billsKey                 = "bills"
  val materialAndStockCostsKey = "materialAndStockCosts"
  val businessTravelKey        = "businessTravel"
  val employeeBenefitsKey      = "employeeBenefits"
  val otherKey                 = "other"

  private def validateAmountOfMoney(
    key:  String,
    data: Map[String, String]
  ): ValidatedNel[FormError, AmountInPence] = {
    val nonEmptyCheck: ValidatedNel[FormError, String] =
      data
        .get(key)
        .toValidNel(FormError(key, "field is required"))

    nonEmptyCheck
      .andThen { s =>
        Try(BigDecimal(s.trim)).toEither
          .leftMap(_ => "invalid format")
          .flatMap { d =>
            if (!(d * 100).isWhole) Left("number of decimal places must be two or less")
            else if (d < 0) Left("amount of money must be non-negative")
            else Right(AmountInPence(d))
          }
          .leftMap(FormError(key, _))
          .toValidatedNel
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val incomeAndExpenditureFormatter: Formatter[IncomeAndExpenditure] = new Formatter[IncomeAndExpenditure] {
    implicit val amountInPenceSemiGroup: Semigroup[AmountInPence] = new Semigroup[AmountInPence] {
      override def combine(x: AmountInPence, y: AmountInPence): AmountInPence = x + y
    }

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], IncomeAndExpenditure] = {
      val incomeCheck                                                            = validateIncome(data)
      val expenditureCheck                                                       = validateExpenditure(data)
      val incomeAndExpenditureTotalCheck: ValidatedNel[FormError, AmountInPence] = {
        val totalIncome              = incomeCheck.map(i => i.mainIncome + i.otherIncome)
        // make the total negative so 'combine' below takes the expenditure away rather than adding it
        val totalExpenditureNegative = expenditureCheck.map { e =>
          val total = e.wagesAndSalaries + e.mortgageAndRent + e.bills + e.materialAndStockCosts +
            e.businessTravel + e.employeeBenefits + e.other
          AmountInPence.zero - total
        }

        totalIncome.combine(totalExpenditureNegative).andThen { total =>
          if (total.value <= 0)
            FormError(
              "",
              "Net income must be greater than zero. Try increasing the income or decreasing the expenditure"
            ).invalidNel
          else
            total.validNel
        }
      }

      (incomeCheck, expenditureCheck, incomeAndExpenditureTotalCheck)
        .mapN { case (income, expenditure, _) =>
          IncomeAndExpenditure(income, expenditure)
        }
        .toEither
        .leftMap(_.toList)
    }

    override def unbind(key: String, value: IncomeAndExpenditure): Map[String, String] =
      Map(
        mainIncomeKey            -> value.income.mainIncome,
        otherIncomeKey           -> value.income.otherIncome,
        wagesAndSalariesKey      -> value.expenditure.wagesAndSalaries,
        mortgageAndRentKey       -> value.expenditure.mortgageAndRent,
        billsKey                 -> value.expenditure.bills,
        materialAndStockCostsKey -> value.expenditure.materialAndStockCosts,
        businessTravelKey        -> value.expenditure.businessTravel,
        employeeBenefitsKey      -> value.expenditure.employeeBenefits,
        otherKey                 -> value.expenditure.other
      ).map { case (k, v) => k -> v.inPounds.toString }

    private def validateIncome(data: Map[String, String]): ValidatedNel[FormError, Income] = {
      val mainIncomeCheck: ValidatedNel[FormError, AmountInPence] =
        validateAmountOfMoney(mainIncomeKey, data)

      val otherIncomeCheck: ValidatedNel[FormError, AmountInPence] =
        validateAmountOfMoney(otherIncomeKey, data)

      val greaterThanZeroCheck =
        mainIncomeCheck
          .combine(otherIncomeCheck)
          .andThen(total =>
            if (total == AmountInPence.zero) {
              val errorMessage = "total income must be greater than zero"
              NonEmptyList(
                FormError(mainIncomeKey, errorMessage),
                List(FormError(otherIncomeKey, errorMessage))
              ).invalid
            } else
              total.valid
          )

      (mainIncomeCheck, otherIncomeCheck, greaterThanZeroCheck)
        .mapN { case (mainIncome, otherIncome, _) =>
          Income(mainIncome, otherIncome)
        }
    }

    private def validateExpenditure(data: Map[String, String]): ValidatedNel[FormError, Expenditure] =
      (
        validateAmountOfMoney(wagesAndSalariesKey, data),
        validateAmountOfMoney(mortgageAndRentKey, data),
        validateAmountOfMoney(billsKey, data),
        validateAmountOfMoney(materialAndStockCostsKey, data),
        validateAmountOfMoney(businessTravelKey, data),
        validateAmountOfMoney(employeeBenefitsKey, data),
        validateAmountOfMoney(otherKey, data)
      ).mapN(Expenditure.apply)

  }

  val form: Form[IncomeAndExpenditure] =
    Form(
      mapping(
        "" -> of(incomeAndExpenditureFormatter)
      )(identity)(Some(_))
    )

}
