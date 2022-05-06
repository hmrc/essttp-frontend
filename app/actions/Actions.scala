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
import essttp.rootmodel.TaxRegime
import play.api.mvc.Results.Redirect
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Actions @Inject() (
    actionBuilder:           DefaultActionBuilder,
    authenticatedAction:     AuthenticateActionRefiner,
    getJourneyActionRefiner: GetJourneyActionRefiner
)(implicit ec: ExecutionContext) {

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  val landingPageAction: ActionBuilder[JourneyRequest, AnyContent] =
    actionBuilder
      .andThen(getJourneyActionRefiner)

  val journeyAction: ActionBuilder[AuthenticatedJourneyRequest, AnyContent] =
    actionBuilder
      .andThen(getJourneyActionRefiner)
      .andThen(authenticatedAction)
      .andThen(filterForRequiredEnrolments)

  private def filterForRequiredEnrolments: ActionFilter[AuthenticatedJourneyRequest] = new ActionFilter[AuthenticatedJourneyRequest] {
    override protected def filter[A](request: AuthenticatedJourneyRequest[A]): Future[Option[Result]] = {
      val hasRequiredEnrolments: Boolean = request.journey.taxRegime match {
        case TaxRegime.Epaye => EnrolmentDef.Epaye.hasRequiredEnrolments(request.enrolments)
        case TaxRegime.Vat   => EnrolmentDef.Vat.hasRequiredEnrolments(request.enrolments)
      }

      if (hasRequiredEnrolments) {
        Future.successful(None)
      } else {
        Future.successful(Some(Redirect(controllers.routes.NotEnrolledController.notEnrolled())))
      }
    }
    override protected def executionContext: ExecutionContext = ec
  }
}
