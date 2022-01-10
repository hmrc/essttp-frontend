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

package moveittocor.corcommon.model

import moveittocor.corcommon.internal.ValueClassBinder._
import play.api.libs.functional.syntax._
import play.api.libs.json.Format
import play.api.mvc.{ PathBindable, QueryStringBindable }

final case class TraceId(value: String)

object TraceId {
  def apply(journeyId: JourneyId): TraceId = TraceId(eightDigitAbsoluteMod(journeyId.value.hashCode))

  private[model] def eightDigitAbsoluteMod(i: Int): String = {
    val absoluteMod = Math.abs(i % 100000000)
    f"$absoluteMod%08d"
  }

  implicit val format: Format[TraceId] = implicitly[Format[String]].inmap(TraceId(_), _.value)
  implicit val traceIdPathBinder: PathBindable[TraceId] = valueClassBinder(_.value)
  implicit val traceIdQueryStringBinder: QueryStringBindable[TraceId] = bindableA(_.value)
}

