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

package testsupport

import cats.syntax.eq._
import play.api.libs.json.{JsObject, JsValue}

object JsonUtils {

  /**
   * Find the value at the specified `fieldPath` in the given `json` and replace the value with `replaceWith`.
   * Key names in `fieldPath` should have outer keys first and inner keys last. If the specified path cannot be
   * found, this method throws an error.
   */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def replace(fieldPath: List[String], replaceWith: JsValue)(json: JsObject): JsObject = {

      def loop(path: List[String], j: JsObject): JsObject = {
        path match {
          case Nil => j
          case head :: tail =>
            j.value.get(head) match {
              case None =>
                sys.error(s"Could not find value for field $head in path ${fieldPath.mkString(".")}. " +
                  s"Found fields [${j.value.keys.mkString(", ")}]")

              case Some(_) =>
                tail match {
                  case Nil =>
                    JsObject(j.value.toMap.updated(head, replaceWith))
                  case remainder =>
                    JsObject(j.value.map{
                      case (key, jsObject: JsObject) if (key === head) => key -> loop(remainder, jsObject)
                      case (key, value)                                => key -> value
                    })
                }
            }
        }
      }

    loop(fieldPath, json)
  }

}
