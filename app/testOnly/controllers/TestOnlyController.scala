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

package testOnly.controllers

import _root_.actions.Actions
import controllers.PaymentDayController.readValue
import play.api.data.{ Form, FormError, Forms }
import play.api.data.Forms.{ mapping, nonEmptyText, text }
import play.api.data.format.Formatter
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import testOnly.controllers.TestOnlyController.{ TestOnlyForm, testOnlyForm }
import testOnly.models.Enrolment
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import testOnly.views.html.TestOnlyStart

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class TestOnlyController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  testOnlyPage: TestOnlyStart)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val testOnlyStartPage: Action[AnyContent] = as.default { implicit request =>
    val form: Form[TestOnlyForm] = testOnlyForm()
    Ok(testOnlyPage(form))
  }

  val testOnlyStartPageSubmit: Action[AnyContent] = as.default { implicit request =>
    // TODO: pattern match the combination of values posted by the form to map to BE endpoints
    /* BE endpoints:
      POST       /epaye/bta/journey/start
      POST       /epaye/gov-uk/journey/start
      POST       /epaye/detached-url/journey/start
     */
    testOnlyForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(testOnlyPage(formWithErrors)),
        (p: TestOnlyForm) => {
          p.origin
          Redirect("/essttp/")
        })
  }

}

object TestOnlyController {
  import play.api.data.Form

  case class TestOnlyForm(
    auth: String,
    enrolments: Seq[String],
    origin: String)

  def testOnlyForm(): Form[TestOnlyForm] = Form(
    mapping(
      "auth" -> nonEmptyText,
      "enrolments" -> Forms.seq(text),
      "origin" -> nonEmptyText)(TestOnlyForm.apply)(TestOnlyForm.unapply))

}
