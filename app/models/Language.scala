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

package models

import cats.Eq
import enumeratum.{Enum, EnumEntry}
import essttp.utils.EnumFormat
import essttp.utils.ValueClassBinder.valueClassBinder
import models.Languages.{English, Welsh}
import play.api.i18n.Lang
import play.api.libs.json.Format
import play.api.mvc.{PathBindable, QueryStringBindable}

import scala.collection.immutable

sealed trait Language extends EnumEntry with Product with Serializable {
  val toPlayLang: Lang = Lang(code)

  def code: String

  def label: String
}

object Language {
  implicit val eq: Eq[Language] = Eq.fromUniversalEquals

  implicit val format: Format[Language] = EnumFormat(Languages)
  implicit val languagePathBinder: PathBindable[Language] = valueClassBinder(_.toString)
  implicit val optionLanguagePathBinder: PathBindable[Option[Language]] = new PathBindable[Option[Language]] {
    override def bind(key: String, value: String): Either[String, Option[Language]] =
      implicitly[PathBindable[Language]]
        .bind(key, value)
        .map(Some(_))

    override def unbind(key: String, value: Option[Language]): String = value map (_.code) getOrElse ""
  }

  implicit val languageQueryStringBindable: QueryStringBindable[Language] =
    new QueryStringBindable[Language] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Language]] = {
        params.get(key).map(_.toList match {
          case "en" :: Nil => Right(English)
          case "cy" :: Nil => Right(Welsh)
          case error       => Left(s"invalid query parameters for language ${error.toString()}")
        })
      }

      override def unbind(key: String, language: Language): String = language.code
    }

  def apply(lang: Lang): Language = lang.code match {
    case "en" => English
    case "cy" => Welsh
    case _    => English //default language is English
  }
}

object Languages extends Enum[Language] {

  val availableLanguages: List[Language] = List(English, Welsh)

  override def values: immutable.IndexedSeq[Language] = findValues

  case object English extends Language {
    override def code: String = "en"

    override def label: String = "English"
  }

  case object Welsh extends Language {
    override def code: String = "cy"

    override def label: String = "Cymraeg"
  }
}
