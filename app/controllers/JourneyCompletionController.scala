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

package controllers

import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.Inject

class JourneyCompletionController @Inject() (cc: MessagesControllerComponents) extends FrontendController(cc) with Logging {

  // just return to the test page for now.
  // abort will do the right thing depending on the origin when implemented
  def abort: Action[AnyContent] = Action {
    Redirect(testOnly.controllers.routes.TestOnlyController.testOnlyStartPage)
  }

}
