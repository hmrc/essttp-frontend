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

import actionsmodel.{AuthenticatedJourneyRequest, JourneyRequest}
import com.google.inject.Inject
import config.AppConfig
import play.api.Logger
import play.api.mvc.Results.{BadRequest, Redirect}
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, Enrolments, NoActiveSession}

import scala.concurrent.{ExecutionContext, Future}
import requests.RequestSupport._

class AuthenticateActionRefiner @Inject() (
    af:        AuthorisedFunctions,
    appConfig: AppConfig,
    cc:        MessagesControllerComponents
)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[JourneyRequest, AuthenticatedJourneyRequest] {

  private val logger = Logger(getClass)

  override protected def refine[A](request: JourneyRequest[A]): Future[Either[Result, AuthenticatedJourneyRequest[A]]] = {
    implicit val r: JourneyRequest[A] = request

    af.authorised.retrieve(
      Retrievals.allEnrolments
    ).apply {
        case enrolments =>
          Future.successful(
            Right(new AuthenticatedJourneyRequest[A](request.journey, enrolments, request))
          )
      }.recover {
        case _: NoActiveSession => Left(redirectToLoginPage)
        case e: AuthorisationException =>
          logger.error(s"Unauthorised because of ${e.reason}, please investigate why", e)
          Left(Redirect(controllers.routes.NotEnrolledController.notEnrolled()))
      }
  }

  private def hasRequiredEnrolments(enrolments: Enrolments): Boolean = {
    true
  }

  private def redirectToLoginPage(implicit request: Request[_]): Result = Redirect(
    appConfig.BaseUrl.gg,
    Map("continue" -> Seq(appConfig.BaseUrl.essttpFrontend + request.uri), "origin" -> Seq("essttp-frontend"))
  )

  override protected def executionContext: ExecutionContext = cc.executionContext

}
