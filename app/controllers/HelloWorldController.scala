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

import controllers.actions.AuthAction
import play.api.i18n.I18nSupport
import views.html.{ HelloWorldPage, UpfrontPayment }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import util.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class HelloWorldController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage,
  upfrontPaymentPage: UpfrontPayment)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with I18nSupport
  with Logging {

  val helloWorld: Action[AnyContent] = authAction.async { implicit request =>
    Future.successful(Ok(helloWorldPage()))
  }

  val helloWorldSubmit: Action[AnyContent] = authAction { implicit request =>
    Redirect(routes.HelloWorldController.upfrontPayment())
  }

  val upfrontPayment: Action[AnyContent] = authAction.async { implicit request =>
    Future.successful(Ok(upfrontPaymentPage()))
  }

}