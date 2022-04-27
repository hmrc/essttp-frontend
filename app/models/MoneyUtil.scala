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

import play.api.data.FormError
import play.api.data.format.Formatter
import cats.syntax.either._
import cats.syntax.eq._

import scala.util.Try

object MoneyUtil {
  def cleanupAmountOfMoneyString(s: String): String = {
    s.trim().filter(c => c =!= ',' && c =!= '£')
  }
  def formatAmountOfMoneyWithoutPoundSign(d: BigDecimal): String =
    d.toString().replaceAllLiterally("£", "")
  def amountOfMoneyFormatter(
    isTooSmall: BigDecimal => Boolean,
    isTooLarge: BigDecimal => Boolean): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      override def bind(
        key: String,
        data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        validateAmountOfMoney(
          key,
          isTooSmall,
          isTooLarge)(data(key)).leftMap(Seq(_))

      def validateAmountOfMoney(
        key: String,
        isTooSmall: BigDecimal => Boolean,
        isTooLarge: BigDecimal => Boolean)(
        s: String): Either[FormError, BigDecimal] =
        Try(BigDecimal(cleanupAmountOfMoneyString(s))).toEither
          .leftMap(_ => FormError(key, "error.pattern"))
          .flatMap { d: BigDecimal =>
            if (isTooSmall(d)) {
              Left(FormError(key, "error.tooSmall"))
            } else if (isTooLarge(d)) {
              Left(FormError(key, "error.tooLarge"))
            } else {
              Right(d)
            }
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        Map(key -> formatAmountOfMoneyWithoutPoundSign(value))
    }
}
