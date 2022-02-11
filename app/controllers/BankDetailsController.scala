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

import _root_.actions.Actions
import controllers.BankDetailsController.bankDetailsForm
import models.Journey
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.SetUpBankDetails

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class BankDetailsController @Inject() (
  as: Actions,
  bankDetailsPage: SetUpBankDetails,
  journeyService: JourneyService,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val setUpBankDetails: Action[AnyContent] = as.default { implicit request =>
    Ok(bankDetailsPage(bankDetailsForm()))
  }

  val setUpBankDetailsSubmit: Action[AnyContent] = as.getJourney.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        bankDetailsForm()
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(Ok(bankDetailsPage(formWithErrors))),
            _ => {
              Future.successful(Ok("this is as far as we go for now..."))
            })
      case _ => sys.error("journey not found to update")
    }
  }
}

object BankDetailsController {
  import play.api.data.Form

  case class BankDetailsForm(
    name: String,
    sortCode: String,
    accountNumber: String)

  def bankDetailsForm(): Form[BankDetailsForm] = Form(
    mapping(
      "name" -> nonEmptyText,
      "sortCode" -> nonEmptyText,
      "accountNumber" -> nonEmptyText)(BankDetailsForm.apply)(BankDetailsForm.unapply))
}