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
import cats.syntax.eq._

object DateMessages {

  //the same as dd MMMM yy
  def january(day: Int, year: String): Message = Message(
    english = s"$day January $year",
    welsh   = s"$day Ionawr $year"
  )

  def february(day: Int, year: String): Message = Message(
    english = s"$day February $year",
    welsh   = s"$day Chwefror $year"
  )

  def march(day: Int, year: String): Message = Message(
    english = s"$day March $year",
    welsh   = s"$day Mawrth $year"
  )

  def april(day: Int, year: String): Message = Message(
    english = s"$day April $year",
    welsh   = s"$day Ebrill $year"
  )

  def may(day: Int, year: String): Message = Message(
    english = s"$day May $year",
    welsh   = s"$day Mai $year"
  )

  def june(day: Int, year: String): Message = Message(
    english = s"$day June $year",
    welsh   = s"$day Mehefin $year"
  )

  def july(day: Int, year: String): Message = Message(
    english = s"$day July $year",
    welsh   = s"$day Gorffenn $year"
  )

  def august(day: Int, year: String): Message = Message(
    english = s"$day August $year",
    welsh   = s"$day Awst $year"
  )

  def september(day: Int, year: String): Message = Message(
    english = s"$day September $year",
    welsh   = s"$day Medi $year"
  )

  def october(day: Int, year: String): Message = Message(
    english = s"$day October $year",
    welsh   = s"$day Hydref $year"
  )

  def november(day: Int, year: String): Message = Message(
    english = s"$day November $year",
    welsh   = s"$day Tachwedd $year"
  )

  def december(day: Int, year: String): Message = Message(
    english = s"$day December $year",
    welsh   = s"$day Rhagfyr $year"
  )

  val monthName: Map[Int, Message] = Map(
    1 -> Message("January"),
    2 -> Message("February"),
    3 -> Message("March"),
    4 -> Message("April"),
    5 -> Message("May"),
    6 -> Message("June"),
    7 -> Message("July"),
    8 -> Message("August"),
    9 -> Message("September"),
    10 -> Message("October"),
    11 -> Message("November"),
    12 -> Message("December")
  )

  val shortMonthName: Map[Int, Message] = Map(
    1 -> Message("Jan"),
    2 -> Message("Feb"),
    3 -> Message("Mar"),
    4 -> Message("Apr"),
    5 -> Message("May"),
    6 -> Message("Jun"),
    7 -> Message("Jul"),
    8 -> Message("Aug"),
    9 -> Message("Sep"),
    10 -> Message("Oct"),
    11 -> Message("Nov"),
    12 -> Message("Dec")
  )

  def getSuffix(day: Int): Message = {
    val j = day % 10
    val k = day % 100
    if (j === 1 && k != 11) {
      Message("st")
    } else if (j === 2 && k != 12) {
      Message("nd")
    } else if (j === 3 && k != 13) {
      Message("rd")
    } else {
      Message("th")
    }
  }

}
