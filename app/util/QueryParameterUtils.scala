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

import java.nio.charset.Charset
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.Base64

object QueryParameterUtils {

  implicit class InstantOps(private val instant: Instant) extends AnyVal {
    def encodedLongFormat: String = encode(formatted(instant))
  }

  private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, h:mma")

  private def formatted(expiresAt: Instant): String =
    fmt.format(expiresAt.atZone(ZoneId.of("Europe/London")))
      .replaceAll("AM$", "am")
      .replaceAll("PM$", "pm")

  private def encode(expiry: String): String =
    Base64.getEncoder.encodeToString(expiry.getBytes(Charset.forName("UTF-8")))
}
