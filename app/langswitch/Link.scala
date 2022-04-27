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

package langswitch

//shamelessly copied from play-ui
//TODO: remove what it not used

import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

trait Target {
  protected val targetName: String

  def toAttr: String = Link.attr("target", targetName)

  def hiddenInfo: Option[String] = None
}

case object SameWindow extends Target {
  override val targetName = "_self"
}

case object NewWindow extends Target {
  override val targetName = "_blank"
  override val hiddenInfo: Option[String] = Some("link opens in a new window")
}

trait PossibleSso {
  protected val value: String

  def toAttr: String = Link.attr("data-sso", value)
}

case object NoSso extends PossibleSso {
  override val value = "false"
}

case object ClientSso extends PossibleSso {
  override val value = "client"
}

case object ServerSso extends PossibleSso {
  override val value = "server"
}

case class Link(
    url:            String,
    value:          Option[String],
    id:             Option[String]              = None,
    target:         Target                      = SameWindow,
    sso:            PossibleSso                 = NoSso,
    cssClasses:     Option[String]              = None,
    dataAttributes: Option[Map[String, String]] = None,
    hiddenInfo:     Option[String]              = None
)(implicit messages: Messages) {

  import Link._

  val hiddenLink: String = hiddenSpanFor(hiddenInfo.orElse(target.hiddenInfo))

  def toHtml: Html = Html(s"<a$idAttr$hrefAttr${target.toAttr}${sso.toAttr}$cssAttr$dataAttr$relAttr>$text$hiddenLink</a>")

  private def hrefAttr = attr("href", url)

  private def idAttr = id.map(attr("id", _)).getOrElse("")

  private def text = value.map(v => Messages(v)).getOrElse("")

  private def cssAttr = cssClasses.map(attr("class", _)).getOrElse("")

  private def dataAttr = buildAttributeString(dataAttributes)

  def buildAttributeString(attributes: Option[Map[String, String]]): String = {
    attributes match {
      case Some(attributeMap) => attributeMap.foldLeft("") {
        (result, attr) =>
          result + " data-" + attr._1 + "=" + s""""${attr._2}""""
      }
      case None => ""
    }
  }

  private def relAttr = if (target == NewWindow) attr("rel", "external noopener noreferrer") else ""

  private def hiddenSpanFor(txt: Option[String]) = txt.map(t => s"""<span class="visuallyhidden">${Messages(t)}</span>""").getOrElse("")
}

object Link {

  def attr(name: String, value: String): String = s""" $name="${escape(value)}""""

  private def escape(str: String) = HtmlFormat.escape(str).toString()

  def toInternalPage: PreconfiguredLink = PreconfiguredLink(NoSso, SameWindow)

  def toExternalPage: PreconfiguredLink = PreconfiguredLink(NoSso, NewWindow)

  def toPortalPage: PreconfiguredLink = PreconfiguredLink(ClientSso, SameWindow)

  def toInternalPageWithSso: PreconfiguredLink = PreconfiguredLink(ServerSso, SameWindow)

  case class PreconfiguredLink(sso: PossibleSso, target: Target) {
    def apply(
        url:            String,
        value:          Option[String],
        id:             Option[String]              = None,
        cssClasses:     Option[String]              = None,
        dataAttributes: Option[Map[String, String]] = None,
        hiddenInfo:     Option[String]              = None
    )(implicit messages: Messages): Link =
      Link(url, value, id, target, sso, cssClasses, dataAttributes, hiddenInfo)
  }

}
