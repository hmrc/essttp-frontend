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

package actions

import actionsmodel.AuthenticatedRequest
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import controllers.routes
import essttp.rootmodel.{GGCredId, Nino}
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import requests.RequestSupport
import requests.RequestSupport.hc
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._

import scala.concurrent.{ExecutionContext, Future}

trait AuthenticatedActionRefiner { this: ActionRefiner[Request, AuthenticatedRequest] & AuthorisedFunctions =>
  val authConnector: AuthConnector
  val appConfig: AppConfig
  val cc: MessagesControllerComponents
  def loginContinueToUrl(request: Request[?]): String
  val requestSupport: RequestSupport

  private val logger = Logger(getClass)

  private given ExecutionContext = cc.executionContext

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    given Request[A] = request

    authorised(AuthProviders(GovernmentGateway))
      .retrieve(
        Retrievals.allEnrolments and Retrievals.credentials and Retrievals.nino
      ) { case enrolments ~ credentials ~ nino =>
        credentials match {
          case None =>
            Future.failed(new RuntimeException(s"Could not find credentials"))

          case Some(ggCredId) =>
            Future.successful(
              Right(
                new AuthenticatedRequest[A](
                  request,
                  enrolments,
                  GGCredId(ggCredId.providerId),
                  nino.map(nino => Nino(nino)),
                  requestSupport.languageFromRequest
                )
              )
            )
        }

      }
      .recover {
        case e: NoActiveSession        =>
          logger.info(
            s"Authorising but found NoActiveSession exception of type ${e.getClass.getSimpleName} with reason: ${e.reason}. Redirecting to login"
          )
          Left(redirectToLoginPage(request))
        case e: AuthorisationException =>
          logger.warn(s"Unauthorised because of ${e.reason}, please investigate why", e)
          Left(Redirect(controllers.routes.NotEnrolledController.notEnrolled))
      }
  }

  private def redirectToLoginPage(request: Request[?]): Result =
    Redirect(
      appConfig.BaseUrl.gg,
      Map(
        "continue" -> Seq(appConfig.BaseUrl.essttpFrontend + loginContinueToUrl(request)),
        "origin"   -> Seq("essttp-frontend")
      )
    )

  override protected def executionContext: ExecutionContext = cc.executionContext

}

@Singleton
class ContinueToLandingPagesAuthenticatedActionRefiner @Inject() (
  val authConnector:  AuthConnector,
  val appConfig:      AppConfig,
  val cc:             MessagesControllerComponents,
  val requestSupport: RequestSupport
) extends ActionRefiner[Request, AuthenticatedRequest],
      AuthorisedFunctions,
      AuthenticatedActionRefiner {

  override def loginContinueToUrl(request: Request[?]): String =
    routes.WhichTaxRegimeController.whichTaxRegime.url

}

@Singleton
class ContinueToSameEndpointAuthenticatedActionRefiner @Inject() (
  val authConnector:  AuthConnector,
  val appConfig:      AppConfig,
  val cc:             MessagesControllerComponents,
  val requestSupport: RequestSupport
) extends ActionRefiner[Request, AuthenticatedRequest],
      AuthorisedFunctions,
      AuthenticatedActionRefiner {

  override def loginContinueToUrl(request: Request[?]): String =
    request.uri

}
