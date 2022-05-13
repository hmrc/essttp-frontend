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
import play.api.mvc._
import play.mvc.Http.HeaderNames

import javax.inject.Inject

class LanguageSwitchController @Inject() (
    cc:        ControllerComponents,
    as:        Actions,
    appConfig: AppConfig
) extends AbstractController(cc) with Logging {

  def switchToLanguage(language: Language): Action[AnyContent] = cc.actionBuilder { implicit request =>

    val result: Result = request.headers.get(HeaderNames.REFERER) match {
      case Some(referer) =>
        if (referer.contains(appConfig.BaseUrl.essttpFrontend)) Redirect(referer)
        else {
          logger.error(s"Suspicious behaviour during language switching. Referer contains external URL [referrer:$referer]")
          Redirect(controllers.routes.EpayeGovUkController.startJourney)
        }
      case None => Redirect(controllers.routes.EpayeGovUkController.startJourney)
    }
    messagesApi.setLang(result, language.toPlayLang)
  }
}

