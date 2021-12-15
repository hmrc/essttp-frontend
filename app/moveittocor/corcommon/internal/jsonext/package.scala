/*
 * Copyright 2021 HM Revenue & Customs
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

package moveittocor.corcommon.internal

import java.time.{ LocalDate, LocalDateTime }

import play.api.libs.json.{ JsError, _ }

package object jsonext {

  implicit class ReadsOps[A](val r: Reads[A]) extends AnyVal {

    def jsrFlatMap[B](f: A => JsResult[B]): Reads[B] = Reads[B](json =>
      r.reads(json).flatMap(a => f(a)))

    def mapJsError(f: JsError => JsError): Reads[A] = Reads[A] { json =>
      r.reads(json) match {
        case s => s
      }
    }

    def castUp[B >: A]: Reads[B] = r.map(obj => obj: B)
  }

  implicit val isoLocalDateTimeFormat: Format[LocalDateTime] = Format(
    Reads.DefaultLocalDateTimeReads,
    Writes.DefaultLocalDateTimeWrites)

  implicit val isoLocalDateFormat: Format[LocalDate] = Format(
    Reads.DefaultLocalDateReads,
    Writes.DefaultLocalDateWrites)

}
