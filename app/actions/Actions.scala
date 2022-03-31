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

import play.api.mvc.{ ActionBuilder, AnyContent, DefaultActionBuilder, Request }
import requests.JourneyRequest

import javax.inject.{ Inject, Singleton }

@Singleton
class Actions @Inject() (
  payeRoleFilter: PayeRegimeEnrolmentFilter,
  actionBuilder: DefaultActionBuilder,
  authenticatedAction: AuthenticatedAction,
  getJourneyActionRefiner: GetJourneyActionRefiner) {

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  val auth = actionBuilder andThen authenticatedAction

  val authPaye = actionBuilder andThen authenticatedAction andThen payeRoleFilter

  val getJourney: ActionBuilder[JourneyRequest, AnyContent] = actionBuilder andThen getJourneyActionRefiner
}
