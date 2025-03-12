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

package util

import actionsmodel.JourneyRequest
import cats.syntax.eq._
import essttp.utils.RequestSupport.hc
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.CookieNames

/** Journey Logger is a contextual logger. It will append to the message some extra bits of information like journeyId
  * origin, path method, etc. Use it everywhere
  */
object JourneyLogger {

  private val log: Logger = Logger("journey")

  def debug(message: => String)(using RequestHeader): Unit = logMessage(message, Debug)

  def info(message: => String)(using RequestHeader): Unit = logMessage(message, Info)

  def warn(message: => String)(using RequestHeader): Unit = logMessage(message, Warn)

  def error(message: => String)(using RequestHeader): Unit = logMessage(message, Error)

  def debug(message: => String, ex: Throwable)(using RequestHeader): Unit = logMessage(message, ex, Debug)

  def info(message: => String, ex: Throwable)(using RequestHeader): Unit = logMessage(message, ex, Info)

  def warn(message: => String, ex: Throwable)(using RequestHeader): Unit = logMessage(message, ex, Warn)

  def error(message: => String, ex: Throwable)(using RequestHeader): Unit = logMessage(message, ex, Error)

  private def context(using request: RequestHeader) =
    s"[context: ${request.method} ${request.path}] $sessionId $requestId $referer $deviceId"

  private def sessionId(using RequestHeader) = s"[${hc.sessionId.toString}]"

  private def requestId(using RequestHeader) = s"[${hc.requestId.toString}]"

  private def referer(using r: RequestHeader) =
    s"[Referer: ${r.headers.headers.find(_._1 === "Referer").map(_._2).getOrElse("")}]"

  private def deviceId(using r: RequestHeader) =
    s"[deviceId: ${r.cookies.find(_.name === CookieNames.deviceID).map(_.value).getOrElse("")}]"

  private def origin(using r: JourneyRequest[?]) = s"[${r.journey.origin.toString}]"

  private def journeyId(using r: JourneyRequest[?]) = s"[${r.journey.id.toString}]"

  private def taxRegime(using r: JourneyRequest[?]) = s"[${r.journey.taxRegime.toString}]"

  private def stage(using r: JourneyRequest[?]) = s"[${r.journey.stage}]"

  private def journeyName(using r: JourneyRequest[?]) = s"[${r.journey.name}]"

  private def makeRichMessage(message: String)(using request: RequestHeader): String =
    request match {
      case r: JourneyRequest[?] =>
        given JourneyRequest[?] = r
        // Warn, don't log whole journey as it might contain sensitive data (PII)
        s"$message $taxRegime $origin $journeyName $stage $journeyId $context"
      case _                    =>
        s"$message $context "
    }

  private sealed trait LogLevel derives CanEqual

  private case object Debug extends LogLevel

  private case object Info extends LogLevel

  private case object Warn extends LogLevel

  private case object Error extends LogLevel

  private def logMessage(message: => String, level: LogLevel)(using RequestHeader): Unit = {
    lazy val richMessage = makeRichMessage(message)
    level match {
      case Debug => log.debug(richMessage)
      case Info  => log.info(richMessage)
      case Warn  => log.warn(richMessage)
      case Error => log.error(richMessage)
    }
  }

  private def logMessage(message: => String, ex: Throwable, level: LogLevel)(using RequestHeader): Unit = {
    lazy val richMessage = makeRichMessage(message)
    level match {
      case Debug => log.debug(richMessage, ex)
      case Info  => log.info(richMessage, ex)
      case Warn  => log.warn(richMessage, ex)
      case Error => log.error(richMessage, ex)
    }
  }

}
