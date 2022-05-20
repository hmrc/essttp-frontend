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

package util

import enumeratum.{Enum, EnumEntry}
import play.api.data.FormError
import play.api.data.format.Formatter

object EnumFormatter {

  def format[A <: EnumEntry](
      enum:                    Enum[A],
      errorMessageIfMissing:   String  = "missing input",
      errorMessageIfEnumError: String  = "invalid input",
      insensitive:             Boolean = false
  ): Formatter[A] = new Formatter[A] {
    val delegate = enumeratum.Forms.format(enum, insensitive)

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] = delegate.bind(key, data)
      .left
      .map(_.map(fe => fe.copy(messages = fe.messages.map {
        case "error.required" => errorMessageIfMissing
        case "error.enum"     => errorMessageIfEnumError
        case x                => x
      })))

    override def unbind(key: String, value: A): Map[String, String] = ???

  }

}
