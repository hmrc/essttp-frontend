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

import actionsmodel.AuthenticatedRequest
import cats.syntax.eq._
import com.google.inject.Inject
import config.AppConfig
import models.GGCredId
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Request, Result}
import requests.RequestSupport._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, NoActiveSession}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionRefiner @Inject() (
    af:        AuthorisedFunctions,
    appConfig: AppConfig,
    cc:        MessagesControllerComponents
)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[Request, AuthenticatedRequest] {

  private val logger = Logger(getClass)

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val r: Request[A] = request

    af.authorised().retrieve(
      Retrievals.allEnrolments and Retrievals.credentials
    ) {
        case enrolments ~ credentials =>
          credentials.filter(_.providerType === "GovernmentGateway") match {
            case None =>
              Future.failed(
                new RuntimeException(s"Found unsupported provider type '${credentials.map(_.providerType).getOrElse("")}'. " +
                  "Expected provider type 'GovernmentGateway'")
              )

            case Some(ggCredId) =>
              Future.successful(
                Right(new AuthenticatedRequest[A](request, enrolments, GGCredId(ggCredId.providerId)))
              )
          }

      }.recover {
        case _: NoActiveSession => Left(redirectToLoginPage)
        case e: AuthorisationException =>
          logger.error(s"Unauthorised because of ${e.reason}, please investigate why", e)
          Left(Redirect(controllers.routes.NotEnrolledController.notEnrolled))
      }
  }

  private def redirectToLoginPage(implicit request: Request[_]): Result = {
    val returnUrl =
      if (request.uri.endsWith(controllers.routes.EpayeGovUkController.startJourney.url)) appConfig.Urls.epayeGovUkJourneyLoginContinueUrl
      else request.uri
    Redirect(
      appConfig.BaseUrl.gg,
      Map("continue" -> Seq(appConfig.BaseUrl.essttpFrontend + returnUrl), "origin" -> Seq("essttp-frontend"))
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}
