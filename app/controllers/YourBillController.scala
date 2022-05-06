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
import essttp.rootmodel.AmountInPence
import models.OverDuePayments
import models.ttp.EligibilityResult
import play.api.mvc._
import services.TtpService
import services.TtpService.overDuePaymentOf
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class YourBillController @Inject() (
    as:         Actions,
    mcc:        MessagesControllerComponents,
    ttpService: TtpService,
    views:      Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val yourBill: Action[AnyContent] = as.journeyAction { implicit request =>
    //TODO: this will be in journey
    val eligibilityResult: EligibilityResult = sys.error("TODO")
    Ok(views.yourBillIs(overDuePayments(eligibilityResult), None /*TODO*/ ))
  }

  private def overDuePayments(eligibilityResult: EligibilityResult): OverDuePayments = {
    val qualifyingDebt: AmountInPence = AmountInPence(eligibilityResult.chargeTypeAssessment.map(_.debtTotalAmount).sum)
    val payments = eligibilityResult.chargeTypeAssessment.map(overDuePaymentOf)
    OverDuePayments(qualifyingDebt, payments)
  }

  val yourBillSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.UpfrontPaymentController.upfrontPayment())
  }

}
