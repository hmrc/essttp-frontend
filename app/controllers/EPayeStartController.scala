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
import moveittocor.corcommon.model.AmountInPence
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.{ EPayeStartPage, EPayeLandingPage }

import java.time.LocalDate
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext }
import controllers.EPayeStartController._
import models.Journey
import services.JourneyService

@Singleton
class EPayeStartController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  ePayeLandingPage: EPayeLandingPage,
  ePayeStartPage: EPayeStartPage)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val ePayeLanding: Action[AnyContent] = as.default { implicit request =>
    Ok(ePayeLandingPage())
  }

  val ePayeLandingSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.EPayeStartController.ePayeStart())
  }

  val ePayeStart: Action[AnyContent] = as.default { implicit request =>
    val qualifyingDebt: AmountInPence = AmountInPence(296345)
    val overduePayments = OverduePayments(
      total = qualifyingDebt,
      payments = List(
        OverduePayment(
          InvoicePeriod(
            monthNumber = 7,
            dueDate = LocalDate.of(2022, 1, 22),
            start = LocalDate.of(2021, 11, 6),
            end = LocalDate.of(2021, 12, 5)),
          amount = AmountInPence((qualifyingDebt.value * 0.4).longValue())),
        OverduePayment(
          InvoicePeriod(
            monthNumber = 8,
            dueDate = LocalDate.of(2021, 12, 22),
            start = LocalDate.of(2021, 10, 6),
            end = LocalDate.of(2021, 11, 5)),
          amount = AmountInPence((qualifyingDebt.value * 0.6).longValue()))))
    request.session.data.get("JourneyId") match {
      case Some(_: String) => Ok(ePayeStartPage(overduePayments))
      case _ =>
        val journey: Journey = journeyService.newJourney(qualifyingDebt)
        journeyService.upsert(journey)
        Ok(ePayeStartPage(overduePayments)).withSession("JourneyId" -> journey._id.value)
    }
  }

  val ePayeStartSubmit: Action[AnyContent] = as.default { implicit request =>
    Redirect(routes.UpfrontPaymentController.upfrontPayment())
  }

}

object EPayeStartController {
  case class OverduePayments(
    total: AmountInPence,
    payments: List[OverduePayment])
  case class OverduePayment(
    invoicePeriod: InvoicePeriod,
    amount: AmountInPence)
  case class InvoicePeriod(
    monthNumber: Int,
    start: LocalDate,
    end: LocalDate,
    dueDate: LocalDate)

}
