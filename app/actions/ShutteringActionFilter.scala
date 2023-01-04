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
import com.typesafe.config.Config
import play.api.Logging
import play.api.mvc.{ActionFilter, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import configs.syntax._
import essttp.rootmodel.TaxRegime
import views.Views

@Singleton
class ShutteringActionFilter @Inject() (
    config: Config,
    views:  Views
)(implicit ec: ExecutionContext) extends ActionFilter[AuthenticatedJourneyRequest] with Logging with Results {

  val shutteredTaxRegime: List[TaxRegime] =
    config.get[List[String]]("shuttering.shuttered-tax-regimes").value.map(TaxRegime.withNameInsensitive)

  override protected def filter[A](request: AuthenticatedJourneyRequest[A]): Future[Option[Result]] = {
    val result = if (shutteredTaxRegime.contains(request.journey.taxRegime)) Some(Ok(views.shuttered()(request))) else None
    Future.successful(result)
  }

  override protected def executionContext: ExecutionContext = ec
}

