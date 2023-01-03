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

package testsupport

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html

trait PageElements {
  //see https://jsoup.org/cookbook/extracting-data/selector-syntax
  //for advanced selections

  /**
   * Html representig rendered view.
   * Override is as a `val`
   */
  def html: Html

  protected lazy val document: Document = Jsoup.parse(html.toString())

  def headerTitle: String = {
    val nav = document.getElementById("proposition-menu")
    val span = nav.children.first
    span.text
  }

  def homeNavTitle: String = {
    val elem = document.getElementById("homeNavHref")
    elem.text
  }
}
