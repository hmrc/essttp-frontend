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

import actionsmodel.{AuthenticatedJourneyRequest, AuthenticatedRequest, BarsLockedOutRequest, EligibleJourneyRequest}
import play.api.mvc._

import javax.inject.{Inject, Singleton}

@Singleton
class Actions @Inject() (
    actionBuilder:                                    DefaultActionBuilder,
    continueToLandingPagesAuthenticatedActionRefiner: ContinueToLandingPagesAuthenticatedActionRefiner,
    continueToSameEndpointAuthenticatedActionRefiner: ContinueToSameEndpointAuthenticatedActionRefiner,
    getJourneyActionRefiner:                          GetJourneyActionRefiner,
    barsLockoutActionFilter:                          BarsLockoutActionRefiner,
    barsLockedOutJourneyRefiner:                      BarsLockedOutJourneyRefiner,
    eligibleJourneyRefiner:                           EligibleJourneyRefiner,
    shutteringActionFilter:                           ShutteringActionFilter
) {

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val continueToSameEndpointAuthenticatedAction: ActionBuilder[AuthenticatedRequest, AnyContent] =
    actionBuilder
      .andThen(continueToSameEndpointAuthenticatedActionRefiner)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val continueToSameEndpointAuthenticatedJourneyAction: ActionBuilder[AuthenticatedJourneyRequest, AnyContent] =
    actionBuilder
      .andThen(continueToSameEndpointAuthenticatedActionRefiner)
      .andThen(getJourneyActionRefiner)
      .andThen(shutteringActionFilter)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val authenticatedAction: ActionBuilder[AuthenticatedRequest, AnyContent] =
    actionBuilder
      .andThen(continueToLandingPagesAuthenticatedActionRefiner)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val authenticatedJourneyAction: ActionBuilder[AuthenticatedJourneyRequest, AnyContent] =
    actionBuilder
      .andThen(continueToLandingPagesAuthenticatedActionRefiner)
      .andThen(getJourneyActionRefiner)
      .andThen(shutteringActionFilter)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val eligibleJourneyAction: ActionBuilder[EligibleJourneyRequest, AnyContent] =
    actionBuilder
      .andThen(continueToLandingPagesAuthenticatedActionRefiner)
      .andThen(getJourneyActionRefiner)
      .andThen(shutteringActionFilter)
      .andThen(barsLockoutActionFilter)
      .andThen(eligibleJourneyRefiner)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val barsLockedOutJourneyAction: ActionBuilder[BarsLockedOutRequest, AnyContent] =
    actionBuilder
      .andThen(continueToLandingPagesAuthenticatedActionRefiner)
      .andThen(getJourneyActionRefiner)
      .andThen(shutteringActionFilter)
      .andThen(barsLockedOutJourneyRefiner)

}
