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
import config.AppConfig
import controllers.InstalmentsController.{ calculateOptions, instalmentsForm }
import models.Journey
import moveittocor.corcommon.model.AmountInPence
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.data.Form
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.InstalmentOptions

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class InstalmentsController @Inject() (
  as: Actions,
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  instalmentOptionsPage: InstalmentOptions)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc)
  with Logging {

  val instalmentOptions: Action[AnyContent] = as.getJourney.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        Future.successful(Ok(instalmentOptionsPage(instalmentsForm(), calculateOptions(j))))
      case _ => sys.error("no journey to update")
    }
  }

  val instalmentOptionsSubmit: Action[AnyContent] = as.getJourney.async { implicit request =>
    val journey: Future[Journey] = journeyService.get()
    journey.flatMap {
      case j: Journey =>
        instalmentsForm()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(Ok(
                instalmentOptionsPage(
                  formWithErrors, calculateOptions(j)))),
            _ => Future.successful(Ok("this is as fas as we go for now...")))
      case _ => sys.error("no journey to update")
    }

  }

}

object InstalmentsController {
  case class InstalmentOption(
    numberOfMonths: Int,
    amountToPayEachMonth: AmountInPence,
    interestPayment: AmountInPence)

  def calculateOptions(journey: Journey)(implicit appConfig: AppConfig): List[InstalmentOption] = {
    val monthlyInterest: BigDecimal = (appConfig.InterestRates.hmrcRate + appConfig.InterestRates.baseRate) / 12
    val interestPerMonth: BigDecimal = journey.remainingToPay.inPounds * monthlyInterest
    val offerMonths: Int = (journey.remainingToPay.value / journey.userAnswers.getAffordableAmount.value).intValue()
    val month1 = if (offerMonths > 1) offerMonths - 1 else offerMonths
    val month2 = if (offerMonths > 1) offerMonths else offerMonths + 1
    val month3 = if (offerMonths > 1) offerMonths + 1 else offerMonths + 2
    List(
      InstalmentOption(
        numberOfMonths = month1,
        amountToPayEachMonth = AmountInPence(journey.remainingToPay.value / month1),
        interestPayment = AmountInPence(interestPerMonth.longValue() * month1)),
      InstalmentOption(
        numberOfMonths = month2,
        amountToPayEachMonth = AmountInPence(journey.remainingToPay.value / month2),
        interestPayment = AmountInPence(interestPerMonth.longValue() * month2)),
      InstalmentOption(
        numberOfMonths = month3,
        amountToPayEachMonth = AmountInPence(journey.remainingToPay.value / month3),
        interestPayment = AmountInPence(interestPerMonth.longValue() * month3)))
  }

  val key: String = "Instalments"

  def instalmentsForm(): Form[String] = Form(
    mapping(
      key -> nonEmptyText)(identity)(Some(_)))

}