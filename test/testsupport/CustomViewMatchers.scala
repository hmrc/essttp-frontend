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

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.Assertions
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

trait CustomViewMatchers extends Matchers {
  type MessagesProvider = Messages

  def containMessages(withMessageKeys: String*)(expectHtmlEscaped: Boolean = true)(implicit messagesProvider: MessagesProvider): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        withMessageKeys.foreach(checkMessageIsDefined)

        val htmlText = html.toString
        val (msgsPresent, msgsMissing) = withMessageKeys.partition { messageKey =>
          val expectedMessage =
            if (expectHtmlEscaped)
              htmlEscapedMessage(messageKey)
            else
              messagesProvider(messageKey)

          htmlText.contains(expectedMessage)
        }
        MatchResult(
          msgsMissing.isEmpty,
          s"Content is missing in the html for message keys: ${msgsMissing.mkString(", ")}",
          s"Content is present in the html for message keys: ${msgsPresent.mkString(", ")}"
        )
      }
    }

  def containElement(
      withId:    String,
      withTag:   String,
      withAttrs: Map[String, String]
  ): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        val doc = Jsoup.parse(html.toString)
        val foundElement = doc.getElementById(withId)
        val isAsExpected = Option(foundElement) match {
          case None => false
          case Some(elFound) => {
            val isExpectedTag = elFound.tagName() == withTag
            val hasExpectedAttributes = hasAttributes(elFound, withAttrs)
            isExpectedTag && hasExpectedAttributes
          }
        }
        MatchResult(
          isAsExpected,
          s"""Html does not contain a "$withTag" element with id of "$withId" with matching attributes $withAttrs""",
          s"""Html contains a "$withTag" element with id of "$withId" with matching attributes $withAttrs"""
        )
      }
    }

  def containElementWithMessage(
      withTag:           String,
      withMessageKey:    String,
      withMessageParams: String*
  )(implicit messagesProvider: MessagesProvider): Matcher[Html] = {
    containElementWithMessage(withTag, withAttrs = Map.empty, withMessageKey, withMessageParams: _*)
  }

  def containElementWithMessage(
      withTag:           String,
      withAttrs:         Map[String, String],
      withMessageKey:    String,
      withMessageParams: String*
  )(implicit messagesProvider: MessagesProvider): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        checkMessageIsDefined(withMessageKey)
        containElementWithContent(withTag, Messages(withMessageKey, withMessageParams: _*), withAttrs)(html)
      }
    }

  def containElementWithContent(
      withTag:     String,
      withContent: String,
      withAttrs:   Map[String, String] = Map.empty
  ): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        val doc = Jsoup.parse(html.toString)

        import collection.JavaConverters._
        val foundMatchingElements = doc.getElementsByTag(withTag).asScala
          .filter(hasAttributes(_, withAttrs))
          .filter(_.text().contains(withContent))
        val hasMatchingElement = foundMatchingElements.nonEmpty

        MatchResult(
          hasMatchingElement,
          s"""Html does not contain a "$withTag" element with content "$withContent" and with attributes "$withAttrs"""",
          s"""Html contains a "$withTag" element with content "$withContent" and with attributes "$withAttrs""""
        )
      }
    }

  def containForm(
      withAction: String,
      withMethod: String = "POST"
  ): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        val doc = Jsoup.parse(html.toString)
        import collection.JavaConverters._
        val formElements = doc.getElementsByTag("form").asScala.toList

        val hasMatchingForm = formElements.exists(form =>
          withAction == form.attr("action") &&
            withMethod == form.attr("method"))

        MatchResult(
          hasMatchingForm,
          s"""Html does not contain a $withMethod form element with action of "$withAction"""",
          s"""Html contains a $withMethod element with action of "$withAction""""
        )
      }
    }

  def containSubmitButton(
      withMessageKey: String,
      withId:         String,
      withTagName:    String = "button",
      withType:       String = "submit"
  )(implicit messagesProvider: MessagesProvider): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        val doc = Jsoup.parse(html.toString)
        checkMessageIsDefined(withMessageKey)
        val foundElement = doc.getElementById(withId)
        val isAsExpected = Option(foundElement) match {
          case None => false
          case Some(element) => {
            val isExpectedTag = element.tagName() == withTagName
            val isExpectedType = element.attr("type") == withType
            val hasExpectedMsg = element.text() == messagesProvider.messages(withMessageKey)
            isExpectedTag && isExpectedType && hasExpectedMsg
          }
        }
        MatchResult(
          isAsExpected,
          s"""Html does not contain a submit button with id "$withId" and type "$withType" with content for message key "$withMessageKey" """,
          s"""Html contains a submit button with id "$withId" and type "$withType" with content for message key "$withMessageKey" """
        )
      }
    }

  def containLink(
      withMessageKey:       String,
      withHref:             String,
      withClasses:          Set[String]    = Set.empty,
      withMessageParams:    Seq[String]    = Seq.empty,
      withHiddenHintMsgKey: Option[String] = None
  )(
      implicit
      messagesProvider: MessagesProvider
  ): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        val doc = Jsoup.parse(html.toString)

        checkMessageIsDefined(withMessageKey)
        withHiddenHintMsgKey.foreach(hintMsgKey => checkMessageIsDefined(hintMsgKey))

        import collection.JavaConverters._
        val foundElements = doc.select(s"a[href=$withHref]").iterator().asScala

        val wasFound = foundElements.exists { element =>
          val expectedMsg = htmlEscapedMessage(withMessageKey, withMessageParams: _*)
          val expectedHintMsg = withHiddenHintMsgKey.fold("") { hintMsgKey => htmlEscapedMessage(hintMsgKey, withMessageParams: _*) }
          val fullExpectedContent = s"$expectedMsg $expectedHintMsg".trim
          val hasCorrectMessage = element.text() == fullExpectedContent
          val hasExpectedClasses = withClasses.forall(element.hasClass)

          hasCorrectMessage && hasExpectedClasses
        }
        MatchResult(
          wasFound,
          s"""Html does not contain a link to "$withHref" with content for message key "$withMessageKey" and with classes: "${
            withClasses
              .mkString(", ")
          }" """,
          s"""Html contains a link to "$withHref" with content for message key "$withMessageKey" and with classes: "${
            withClasses
              .mkString(", ")
          } """
        )
      }
    }

  def containLink(withHref: String): Matcher[Html] =
    new Matcher[Html] {
      override def apply(html: Html): MatchResult = {
        val doc = Jsoup.parse(html.toString)
        import collection.JavaConverters._
        val foundElements = doc.select(s"a[href=$withHref]").iterator().asScala

        val wasFound = foundElements.exists { element =>
          Set.empty.forall(element.hasClass)
        }
        MatchResult(
          wasFound,
          s"""Html does not contain a link to "$withHref" """,
          s"""Html contains a link to "$withHref" """
        )
      }
    }

  protected def htmlEscapedMessage(key: String, params: String*)(implicit messagesProvider: MessagesProvider): String =
    HtmlFormat.escape(Messages(key, params: _*)).toString

  protected def checkMessageIsDefined(messageKey: String)(implicit messagesProvider: MessagesProvider) =
    Assertions.withClue(s"Message key ($messageKey) should be defined: ") {
      Messages.isDefinedAt(messageKey) shouldBe true
    }

  private def hasAttributes(element: Element, expectedAttrs: Map[String, String]): Boolean = {
    expectedAttrs.forall {
      case (expectedAttr, expectedValue) =>
        element.attr(expectedAttr) == expectedValue
    }
  }
}
