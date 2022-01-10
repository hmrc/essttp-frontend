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

package langswitch

import actions.Actions
import config.AppConfig
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.mvc.Http.HeaderNames

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class LanguageSwitchController @Inject() (
  cc: ControllerComponents,
  as: Actions,
  appConfig: AppConfig)(implicit ec: ExecutionContext) extends AbstractController(cc) with Logging {

  def switchToLanguage(language: Language): Action[AnyContent] = cc.actionBuilder { implicit request =>

    val result: Result = request.headers.get(HeaderNames.REFERER) match {
      case Some(referrer) =>
        if (referrer.contains(appConfig.BaseUrl.essttpFrontend)) Redirect(referrer)
        else {
          logger.error(s"Suspicious behaviour during language swtiching. Referrer contains external URL [referrer:$referrer]")
          Redirect(controllers.routes.LandingController.landingPage)
        }
      case None => Redirect(controllers.routes.LandingController.landingPage)
    }
    messagesApi.setLang(result, language.toPlayLang)
  }
}

