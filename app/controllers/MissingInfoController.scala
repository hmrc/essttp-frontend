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

package controllers

import actions.Actions
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{JourneyLogger, Logging}
import views.Views

import javax.inject.{Inject, Singleton}

@Singleton
class MissingInfoController @Inject() (
    as:    Actions,
    mcc:   MessagesControllerComponents,
    views: Views
) extends FrontendController(mcc)
  with Logging {

  val missingInfo: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.missingInfoPage())
  }

  val determineNextPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(request.journey)
  }

}

object MissingInfoController {

  def redirectToMissingInfoPage()(implicit r: Request[_]): Result = {
    JourneyLogger.warn(s"Not enough information in session to show ${r.uri}. " +
      "Redirecting to missing info page")
    Redirect(routes.MissingInfoController.missingInfo)
  }

}
