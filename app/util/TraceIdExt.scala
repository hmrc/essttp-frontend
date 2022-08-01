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

import cats.syntax.eq._
import essttp.rootmodel.TraceId
import play.api.Logger
import play.api.mvc.RequestHeader

object TraceIdExt {

  def traceIdStringsFromQueryParameter()(implicit request: RequestHeader): Option[Seq[String]] = request
    .queryString
    .get("traceId")

  /**
   * Returns first traceId found in a URL if any. If multiple found, it reports them as an error and returns first.
   */
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def traceIdFromQueryParameter()(implicit request: RequestHeader): Option[TraceId] = {
    traceIdStringsFromQueryParameter()
      .flatMap { seq =>
        val set = seq.map(TraceId.apply).toSet
        if (set.size > 1) Logger(this.getClass).error(s"Multiple traceIds in the URL. [${set.mkString(", ")}]")
        set.headOption
      }
  }
}
