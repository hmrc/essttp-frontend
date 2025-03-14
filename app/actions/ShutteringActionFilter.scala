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

import actionsmodel.AuthenticatedJourneyRequest
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import play.api.Logging
import play.api.mvc.{ActionFilter, Result, Results}
import views.Views

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShutteringActionFilter @Inject() (
  appConfig: AppConfig,
  views:     Views
)(using ec: ExecutionContext)
    extends ActionFilter[AuthenticatedJourneyRequest],
      Logging,
      Results {

  override protected def filter[A](request: AuthenticatedJourneyRequest[A]): Future[Option[Result]] = {
    val result =
      if (appConfig.shutteredTaxRegimes.contains(request.journey.taxRegime)) Some(Ok(views.shuttered()(request)))
      else None
    Future.successful(result)
  }

  override protected def executionContext: ExecutionContext = ec
}
