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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}

@Singleton()
class NotEnrolledController @Inject() (
  mcc:   MessagesControllerComponents,
  views: Views,
  as:    Actions
) extends FrontendController(mcc),
      Logging {

  def notEnrolled: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.notEnrolled())
  }

  def notVatRegistered: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.notVatRegistered())
  }

  def notSaEnrolled: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.notSaEnrolled())
  }

  def notMdtitsaEnrolled: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.noMtditsaEnrollement())
  }

}
