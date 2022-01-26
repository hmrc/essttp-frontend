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

import java.time.LocalDate

object DateMessages {

  //the same as dd MMMM yy
  def january(day: Int, year: String): Message = Message(
    english = s"$day January $year",
    welsh = s"$day Ionawr $year")

  def february(day: Int, year: String): Message = Message(
    english = s"$day February $year",
    welsh = s"$day Chwefror $year")

  def march(day: Int, year: String): Message = Message(
    english = s"$day March $year",
    welsh = s"$day Mawrth $year")

  def april(day: Int, year: String): Message = Message(
    english = s"$day April $year",
    welsh = s"$day Ebrill $year")

  def may(day: Int, year: String): Message = Message(
    english = s"$day May $year",
    welsh = s"$day Mai $year")

  def june(day: Int, year: String): Message = Message(
    english = s"$day June $year",
    welsh = s"$day Mehefin $year")

  def july(day: Int, year: String): Message = Message(
    english = s"$day July $year",
    welsh = s"$day Gorffenn $year")

  def august(day: Int, year: String): Message = Message(
    english = s"$day August $year",
    welsh = s"$day Awst $year")

  def september(day: Int, year: String): Message = Message(
    english = s"$day September $year",
    welsh = s"$day Medi $year")

  def october(day: Int, year: String): Message = Message(
    english = s"$day October $year",
    welsh = s"$day Hydref $year")

  def november(day: Int, year: String): Message = Message(
    english = s"$day November $year",
    welsh = s"$day Tachwedd $year")

  def december(day: Int, year: String): Message = Message(
    english = s"$day December $year",
    welsh = s"$day Rhagfyr $year")

  def getMonthName(month: Int): Message = month match {
    case 1 => Message("January")
    case 2 => Message("February")
    case 3 => Message("March")
    case 4 => Message("April")
    case 5 => Message("May")
    case 6 => Message("June")
    case 7 => Message("July")
    case 8 => Message("August")
    case 9 => Message("September")
    case 10 => Message("October")
    case 11 => Message("November")
    case 12 => Message("December")
    case _ => Message("Invalid")
  }

  def getShortMonthName(month: Int): Message = month match {
    case 1 => Message("Jan")
    case 2 => Message("Feb")
    case 3 => Message("Mar")
    case 4 => Message("Apr")
    case 5 => Message("May")
    case 6 => Message("Jun")
    case 7 => Message("Jul")
    case 8 => Message("Aug")
    case 9 => Message("Sep")
    case 10 => Message("Oct")
    case 11 => Message("Nov")
    case 12 => Message("Dec")
    case _ => Message("Invalid")
  }

}
