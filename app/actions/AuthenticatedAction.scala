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

package actions

import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import play.api.mvc.Results.{ BadRequest, Ok, Redirect }
import play.api.mvc.{ ActionRefiner, MessagesControllerComponents, Request, Result, WrappedRequest }
import uk.gov.hmrc.auth.core.{ AuthorisationException, AuthorisedFunctions, Enrolments, NoActiveSession }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ Credentials, ~ }

import scala.concurrent.{ ExecutionContext, Future }

final class AuthenticatedRequest[A](
  val request: Request[A],
  val enrolments: Enrolments,
  val credentials: Option[Credentials]) extends WrappedRequest[A](request) {

  lazy val hasActiveSaEnrolment: Boolean = enrolments.enrolments.exists(e => e.key == "IR-SA" && e.isActivated)
}

class AuthenticatedAction @Inject() (
  af: AuthorisedFunctions,
  appConfig: AppConfig,
  cc: MessagesControllerComponents)(
  implicit
  ec: ExecutionContext) extends ActionRefiner[Request, AuthenticatedRequest] {

  private val logger = Logger(getClass)

  import requests.RequestSupport._

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val r: Request[A] = request

    af.authorised.retrieve(
      Retrievals.allEnrolments and Retrievals.credentials).apply {
        case enrolments ~ credentials =>
          Future.successful(
            Right(new AuthenticatedRequest[A](request, enrolments, credentials)))
      }.recover {
        case _: NoActiveSession =>
          //TODO: what is a proper value to origin
          Left(Redirect(appConfig.loginUrl, Map("continue" -> Seq(appConfig.frontendBaseUrl + request.uri), "origin" -> Seq("pay-online"))))
        case e: AuthorisationException =>
          logger.debug(s"Unauthorised because of ${e.reason}, $e")
          Left(BadRequest("bad"))
      }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}
