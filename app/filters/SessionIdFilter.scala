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

package filters

import akka.stream.Materializer
import play.api.http.Status
import play.api.mvc
import play.api.mvc.Results._
import play.api.mvc.{Filter, RequestHeader}
import uk.gov.hmrc.http.SessionKeys
import util.JourneyLogger

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future

/**
 * This filter adds "sessionId" entry to the session.
 * The "sessionId" is required to make SPJ call in pay-api.
 */
class SessionIdFilter @Inject() (implicit val mat: Materializer) extends Filter {

  private def requiresSession(rh: RequestHeader): Boolean = SessionIdFilter.requiresSession(rh)

  override def apply(f: RequestHeader => Future[mvc.Result])(rh: RequestHeader): Future[mvc.Result] = {

    if (requiresSession(rh)) {
      val sid = s"session-${UUID.randomUUID()}"
      logger.info(s"There was missing sessionId. Adding new one and redirecting to the same endpoint [newSid:$sid]")(rh)
      Future.successful(Redirect(rh.uri, Status.SEE_OTHER).addingToSession(SessionKeys.sessionId -> sid)(rh))
    } else {
      f(rh)
    }
  }

  val logger: JourneyLogger.type = JourneyLogger
}

object SessionIdFilter {
  def requiresSession(rh: RequestHeader): Boolean = {
    val hasSessionId = rh.session.get(SessionKeys.sessionId).isDefined
    //we require sessionId...
    !hasSessionId
  }
}
