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

package controllers

import config.AppConfig
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Status
import play.api.mvc.{ Request, RequestHeader, Result }
import play.twirl.api.Html
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.error.{ page_not_found, standard_error_template, there_is_a_problem_with_this_service }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  errorTemplate: standard_error_template,
  thereIsAProblem: there_is_a_problem_with_this_service,
  pageNotFound: page_not_found)(implicit val appConfig: AppConfig) extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    errorTemplate(pageTitle, heading, message)

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    exception match {
      case Upstream5xxResponse(response) if response.message.contains("UPSTREAM_FAILURE_ECOSPEND") =>
        implicit val rq: Request[String] = Request(request, "")
        Logger(getClass).error("Frontend received UPSTREAM_FAILURE_ECOSPEND: showing user thereIsAProblem page")
        Future.successful(Status(response.statusCode)(thereIsAProblem()))
      case _ => super.onServerError(request, exception)
    }

  override def notFoundTemplate(implicit request: Request[_]): Html = pageNotFound()

}
