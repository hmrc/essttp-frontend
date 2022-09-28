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

  val monthName: Map[Int, Message] = Map(
    1 -> Message(english = "January"),
    2 -> Message(english = "February"),
    3 -> Message(english = "March"),
    4 -> Message(english = "April"),
    5 -> Message(english = "May"),
    6 -> Message(english = "June"),
    7 -> Message(english = "July"),
    8 -> Message(english = "August"),
    9 -> Message(english = "September"),
    10 -> Message(english = "October"),
    11 -> Message(english = "November"),
    12 -> Message(english = "December")
  )

  val shortMonthName: Map[Int, Message] = Map(
    1 -> Message(english = "Jan"),
    2 -> Message(english = "Feb"),
    3 -> Message(english = "Mar"),
    4 -> Message(english = "Apr"),
    5 -> Message(english = "May"),
    6 -> Message(english = "Jun"),
    7 -> Message(english = "Jul"),
    8 -> Message(english = "Aug"),
    9 -> Message(english = "Sep"),
    10 -> Message(english = "Oct"),
    11 -> Message(english = "Nov"),
    12 -> Message(english = "Dec")
  )

  def getSuffix(day: Int): Message = {
    val j = day % 10
    val k = day % 100
    if (j === 1 && k =!= 11) {
      Message("st")
    } else if (j === 2 && k =!= 12) {
      Message("nd")
    } else if (j === 3 && k =!= 13) {
      Message("rd")
    } else {
      Message("th")
    }
  }

}
