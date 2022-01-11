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

package messages

import langswitch.{ Language, Languages }

final case class Message(
  english: String,
  welsh: Option[String]) {

  def show(implicit language: Language): String = language match {
    case Languages.English => english
    case Languages.Welsh => welsh.getOrElse(english)
  }

  def ++(other: Message): Message = Message(
    english = s"$english${other.english}",
    welsh = for { thisWelsh <- welsh; thatWelsh <- other.welsh } yield s"$thisWelsh$thatWelsh")
}

object Message {
  def apply(english: String, welsh: String): Message = Message(english, Option(welsh))
  def apply(english: String): Message = Message(english, None)
}
