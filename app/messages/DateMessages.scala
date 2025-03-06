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

package messages
import cats.syntax.eq._
import models.{Language, Languages}

object DateMessages {

  val monthName: Map[Int, Message] = Map(
    1  -> Message(english = "January", welsh = "Ionawr"),
    2  -> Message(english = "February", welsh = "Chwefror"),
    3  -> Message(english = "March", welsh = "Mawrth"),
    4  -> Message(english = "April", welsh = "Ebrill"),
    5  -> Message(english = "May", welsh = "Mai"),
    6  -> Message(english = "June", welsh = "Mehefin"),
    7  -> Message(english = "July", welsh = "Gorffennaf"),
    8  -> Message(english = "August", welsh = "Awst"),
    9  -> Message(english = "September", welsh = "Medi"),
    10 -> Message(english = "October", welsh = "Hydref"),
    11 -> Message(english = "November", welsh = "Tachwedd"),
    12 -> Message(english = "December", welsh = "Rhagfyr")
  )

  val shortMonthName: Map[Int, Message] = Map(
    1  -> Message(english = "Jan", welsh = "Ion"),
    2  -> Message(english = "Feb", welsh = "Chwef"),
    3  -> Message(english = "Mar", welsh = "Maw"),
    4  -> Message(english = "Apr", welsh = "Ebr"),
    5  -> Message(english = "May", welsh = "Mai"),
    6  -> Message(english = "Jun", welsh = "Meh"),
    7  -> Message(english = "Jul", welsh = "Gorff"),
    8  -> Message(english = "Aug", welsh = "Awst"),
    9  -> Message(english = "Sep", welsh = "Medi"),
    10 -> Message(english = "Oct", welsh = "Hyd"),
    11 -> Message(english = "Nov", welsh = "Tach"),
    12 -> Message(english = "Dec", welsh = "Rhag")
  )

  def getSuffix(day: Int)(using lang: Language): String =
    if (day > 31)
      throw new IllegalArgumentException("day cannot be greater than 31")
    else if (day < 1)
      throw new IllegalArgumentException("day cannot be less than 1")
    else {
      lang match {
        case Languages.English =>
          val j = day % 10
          val k = day % 100
          if (j === 1 && k =!= 11) "st"
          else if (j === 2 && k =!= 12) "nd"
          else if (j === 3 && k =!= 13) "rd"
          else "th"

        case Languages.Welsh =>
          if (day > 20) "ain"
          else if (day === 1) "af"
          else if (day === 2) "il"
          else if (day === 3 || day === 4) "ydd"
          else if (day === 5 || day === 6) "ed"
          else if (List(7, 8, 9, 10, 12, 15, 18, 20).exists(day === _)) "fed"
          else "eg"
      }
    }

}
