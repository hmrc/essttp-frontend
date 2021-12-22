/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.actions

import com.google.inject.{ Inject, Singleton }
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisationException, AuthorisedFunctions, NoActiveSession }
import config.AppConfig
import util.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ ExecutionContext, Future }

final case class AuthenticatedRequest[A](
  request: MessagesRequest[A]) extends WrappedRequest[A](request)

@Singleton
class AuthAction @Inject() (
  val authConnector: AuthConnector,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig)(implicit val executionContext: ExecutionContext)
  extends ActionFunction[MessagesRequest, AuthenticatedRequest]
  with ActionBuilder[AuthenticatedRequest, AnyContent]
  with AuthorisedFunctions
  with Logging {

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised()(block(AuthenticatedRequest(new MessagesRequest(request, mcc.messagesApi))))
      .recover {
        case _: NoActiveSession =>
          Redirect(appConfig.Urls.loginUrl)

        case e: AuthorisationException =>
          sys.error(s"Could not authorise: ${e.getMessage}")

      }
  }

  override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

}

