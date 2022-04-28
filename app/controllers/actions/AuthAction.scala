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

package controllers.actions

import com.google.inject.{Inject, Singleton}
import config.AppConfig
import error.ErrorResponses
import login.LoginSupport
import play.api.Environment
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, Enrolments, NoActiveSession}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.Logging

import scala.concurrent.{ExecutionContext, Future}
import requests.RequestSupport._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

final case class AuthenticatedRequest[A](
    request: MessagesRequest[A]
) extends WrappedRequest[A](request)

import play.api.mvc.{Request, WrappedRequest}

class EnrollmentsRequest[A](
    val request:    Request[A],
    val enrolments: Enrolments
) extends WrappedRequest[A](request) {
}

class AuthActionRefiner @Inject() (
    af:             AuthorisedFunctions,
    appConfig:      AppConfig,
    env:            Environment,
    errorResponses: ErrorResponses
)(implicit ec: ExecutionContext)
  extends ActionRefiner[Request, EnrollmentsRequest]
  with Logging {

  override protected def refine[A](request: Request[A]): Future[Either[Result, EnrollmentsRequest[A]]] = {
    implicit val implicitRequest: Request[A] = request
    af.authorised.retrieve(Retrievals.allEnrolments) { enrolments: Enrolments =>
      Future.successful(Right(
        new EnrollmentsRequest[A](request, enrolments)
      ))
    }.recover {
      case _: NoActiveSession =>
        val loginUrl = LoginSupport.getLoginUrlStr(RedirectUrl(appConfig.BaseUrl.essttpFrontend + request.uri))(appConfig, env)
        Left(Redirect(loginUrl))
      case e: AuthorisationException =>
        logger.debug(s"Unauthorised because of ${e.reason}, $e")
        Left(errorResponses.unauthorised)
    }
  }
  override protected def executionContext: ExecutionContext = ec
}

@Singleton
class AuthAction @Inject() (
    af:             AuthorisedFunctions,
    mcc:            MessagesControllerComponents,
    appConfig:      AppConfig,
    errorResponses: ErrorResponses
)(implicit val executionContext: ExecutionContext)
  extends ActionFunction[MessagesRequest, AuthenticatedRequest]
  with ActionBuilder[AuthenticatedRequest, AnyContent]
  with Logging {

  override def invokeBlock[A](
      request: Request[A],
      block:   AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val implicitRequest = request

    af.authorised()(block(AuthenticatedRequest(new MessagesRequest(request, mcc.messagesApi))))
      .recover {
        case _: NoActiveSession =>
          Redirect(appConfig.Urls.loginUrl)

        case e: AuthorisationException =>
          logger.debug(s"Unauthorised because of ${e.reason}, $e")
          errorResponses.unauthorised()

      }
  }

  override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

}

