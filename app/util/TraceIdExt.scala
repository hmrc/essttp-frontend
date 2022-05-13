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
      .map(_.map(TraceId.apply))
      .map(_.toSet)
      .flatMap {
        case s if s.isEmpty   => None
        case s if s.size == 1 => Some(s.head)
        case s =>
          Logger(this.getClass).error(s"Multiple traceIds in the URL. [${s.mkString(", ")}]")
          Some(s.head)
      }
  }
}
