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

import actionsmodel.{AuthenticatedRequest, EligibleJourneyRequest, NewAuthenticatedJourneyRequest}
import controllers.JourneyIncorrectStateRouter
import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import play.api.mvc.Results.Redirect
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Actions @Inject() (
    actionBuilder:                 DefaultActionBuilder,
    authenticateAction:            AuthenticateActionRefiner,
    getJourneyActionRefiner:       GetJourneyActionRefiner,
    newAuthenticatedActionRefiner: AuthenticatedActionRefiner,
    newGetJourneyActionRefiner:    NewGetJourneyActionRefiner
)(implicit ec: ExecutionContext) {

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  val landingPageAction: ActionBuilder[AuthenticatedRequest, AnyContent] =
    actionBuilder
      .andThen(newAuthenticatedActionRefiner)

  //  val notEnrolledAction: ActionBuilder[JourneyRequest, AnyContent] =
  //    actionBuilder
  //      .andThen(getJourneyActionRefiner)
  //      .andThen(authenticateAction)

  val notEnrolledAction: ActionBuilder[AuthenticatedRequest, AnyContent] =
    actionBuilder
      .andThen(newAuthenticatedActionRefiner)

  val authenticatedJourneyAction: ActionBuilder[NewAuthenticatedJourneyRequest, AnyContent] =
    actionBuilder
      .andThen(newAuthenticatedActionRefiner)
      .andThen(newGetJourneyActionRefiner)
      .andThen(filterForRequiredEnrolments)

  //  val authenticatedJourneyAction: ActionBuilder[AuthenticatedJourneyRequest, AnyContent] =
  //    actionBuilder
  //      .andThen(getJourneyActionRefiner)
  //      .andThen(authenticateAction)
  //      .andThen(filterForRequiredEnrolments)

  val eligibleJourneyAction: ActionBuilder[NewAuthenticatedJourneyRequest, AnyContent] =
    actionBuilder
      .andThen(newAuthenticatedActionRefiner)
      .andThen(newGetJourneyActionRefiner)
      .andThen(filterForRequiredEnrolments)
      .andThen(filterForEligibleJourney)

  private def filterForEligibleJourney: ActionRefiner[NewAuthenticatedJourneyRequest, EligibleJourneyRequest] = new ActionRefiner[NewAuthenticatedJourneyRequest, EligibleJourneyRequest] {

    override protected def refine[A](request: NewAuthenticatedJourneyRequest[A]): Future[Either[Result, EligibleJourneyRequest[A]]] = {
      implicit val r: Request[A] = request
      val result: Either[Result, EligibleJourneyRequest[A]] = request.journey match {
        case j: Journey.Stages.Started       => Left(JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j))
        case j: Journey.Stages.ComputedTaxId => Left(JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j))
        case j: Journey.AfterEligibilityChecked =>
          if (j.eligibilityCheckResult.isEligible) {
            Right(new EligibleJourneyRequest[A](
              journey    = j,
              enrolments = request.enrolments,
              request    = request
            ))
          } else {
            Left(EligibilityRouter.nextPage(j.eligibilityCheckResult))
          }
      }
      Future.successful(result)
    }

    override protected def executionContext: ExecutionContext = ec

  }

  private def filterForRequiredEnrolments: ActionFilter[NewAuthenticatedJourneyRequest] = new ActionFilter[NewAuthenticatedJourneyRequest] {
    override protected def filter[A](request: NewAuthenticatedJourneyRequest[A]): Future[Option[Result]] = {
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

