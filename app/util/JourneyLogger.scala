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

import actionsmodel.JourneyRequest
import essttp.journey.model.JourneyId
import essttp.rootmodel.TraceId
import essttp.utils.RequestSupport._
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.CookieNames

/**
 * Journey Logger is a contextual logger. It will append to the message some extra bits of information
 * like journeyId origin, path method, etc.
 * Use it everywhere
 */
object JourneyLogger {

  private val log: Logger = Logger("journey")

  def debug(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Debug)

  def info(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Info)

  def warn(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Warn)

  def error(message: => String)(implicit request: RequestHeader): Unit = logMessage(message, Error)

  def debug(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Debug)

  def info(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Info)

  def warn(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Warn)

  def error(message: => String, ex: Throwable)(implicit request: RequestHeader): Unit = logMessage(message, ex, Error)

  private def context(implicit request: RequestHeader) = s"[context: ${request.method} ${request.path}] $sessionId $referer $deviceId"

  private def sessionId(implicit request: RequestHeader) = s"[${hc.sessionId}]"

  private def referer(implicit r: RequestHeader) = s"[Referer: ${r.headers.headers.find(_._1 == "Referer").map(_._2).getOrElse("")}]"

  private def deviceId(implicit r: RequestHeader) = s"[deviceId: ${r.cookies.find(_.name == CookieNames.deviceID).map(_.value).getOrElse("")}]"

  private def origin(implicit r: JourneyRequest[_]) = s"[${r.journey.origin}]"

  private def journeyId(implicit r: JourneyRequest[_]) = s"[${r.journey.id}]"

  private def traceId(implicit r: JourneyRequest[_]) = {
    val traceIdsFromUrlIfDifferentThanInJourney: String = TraceIdExt
      .traceIdStringsFromQueryParameter()
      .map(_.filterNot(_ == r.journey.traceId.value))
      .map(_.mkString("[traceId different:", "", "]"))
      .getOrElse("")

    s"[${TraceId(r.journeyId)}] $traceIdsFromUrlIfDifferentThanInJourney"
  }

  private def taxRegime(implicit r: JourneyRequest[_]) = s"[${r.journey.taxRegime}]"

  private def stage(implicit r: JourneyRequest[_]) = s"[${r.journey.stage}]"

  private def journeyName(implicit r: JourneyRequest[_]) = s"[${r.journey.name}]"

  private def makeRichMessage(message: String)(implicit request: RequestHeader): String = {
    request match {
      case r: JourneyRequest[_] =>
        implicit val req: JourneyRequest[_] = r
        s"$message $taxRegime $origin $journeyName $stage $journeyId $context $traceId"
      case _ =>
        s"$message $context "
    }
  }

  private def makeRichMessage(message: => String, journeyId: JourneyId)(implicit request: RequestHeader): String = {
    val traceId: TraceId = TraceId(journeyId)
    s"$message $traceId $journeyId $context "
  }

  private sealed trait LogLevel

  private case object Debug extends LogLevel

  private case object Info extends LogLevel

  private case object Warn extends LogLevel

  private case object Error extends LogLevel

  private def logMessage(message: => String, ex: Throwable, level: LogLevel, journeyId: JourneyId)(implicit request: RequestHeader): Unit = {
    level match {
      case Debug => log.debug(makeRichMessage(message, journeyId), ex)
      case Info  => log.info(makeRichMessage(message, journeyId), ex)
      case Warn  => log.warn(makeRichMessage(message, journeyId), ex)
      case Error => log.error(makeRichMessage(message, journeyId), ex)
    }
  }

  private def logMessage(message: => String, level: LogLevel, journeyId: JourneyId)(implicit request: RequestHeader): Unit = {
    level match {
      case Debug => log.debug(makeRichMessage(message, journeyId))
      case Info  => log.info(makeRichMessage(message, journeyId))
      case Warn  => log.warn(makeRichMessage(message, journeyId))
      case Error => log.error(makeRichMessage(message, journeyId))
    }
  }

  private def logMessage(message: => String, level: LogLevel)(implicit request: RequestHeader): Unit = {
    lazy val richMessage = makeRichMessage(message)
    level match {
      case Debug => log.debug(richMessage)
      case Info  => log.info(richMessage)
      case Warn  => log.warn(richMessage)
      case Error => log.error(richMessage)
    }
  }

  private def logMessage(message: => String, ex: Throwable, level: LogLevel)(implicit request: RequestHeader): Unit = {
    lazy val richMessage = makeRichMessage(message)
    level match {
      case Debug => log.debug(richMessage, ex)
      case Info  => log.info(richMessage, ex)
      case Warn  => log.warn(richMessage, ex)
      case Error => log.error(richMessage, ex)
    }
  }

}
