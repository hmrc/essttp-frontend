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

package error

import essttp.journey.JourneyConnector
import essttp.rootmodel.TaxRegime
import play.api.i18n.MessagesApi
import play.api.mvc.RequestHeader
import play.api.mvc.Results.Ok
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future, Promise}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter

@Singleton
class ErrorHandler @Inject() (
  errorTemplate:             ErrorTemplate,
  val messagesApi:           MessagesApi,
  journeyConnector:          JourneyConnector,
  val authConnector:         AuthConnector,
  sessionCookieCryptoFilter: SessionCookieCryptoFilter
)(using
  val ec:                    ExecutionContext
) extends FrontendErrorHandler,
      AuthorisedFunctions,
      FrontendHeaderCarrierProvider {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(using
    RequestHeader
  ): Future[Html] =
    taxRegime().map { maybeTaxRegime =>
      errorTemplate(pageTitle, heading, message, maybeTaxRegime)
    }

  private def taxRegime()(using r: RequestHeader): Future[Option[TaxRegime]] = {
    // constrained to return Future[Result] in sessionCookieCryptoFilter below, so create a promise
    // that we can write into to observe the tax regime outside of it
    val promise = Promise[Option[TaxRegime]]

    // session in request seems to be missing in the incoming RequestHeader - use sessionCookieCryptoFilter
    // to make sure call to `authorised` can find bearer token in the resulting session it creates in the RequestHeader
    val _ = sessionCookieCryptoFilter { implicit rh: RequestHeader =>
      val result = authorised() {
        journeyConnector
          .findLatestJourneyBySessionId()
          .map(_.map(_.taxRegime))
      }

      promise.completeWith(result)

      result.map(_ => Ok)
    }(r)

    promise.future
  }.recover(_ => None)

}
