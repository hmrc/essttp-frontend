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
import controllers.InstalmentsController.{instalmentsForm, mockApi}
import essttp.rootmodel.AmountInPence
import models.{InstalmentOption, MockJourney, UserAnswers}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.InstalmentOptions

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class InstalmentsController @Inject() (
    as:                    Actions,
    mcc:                   MessagesControllerComponents,
    instalmentOptionsPage: InstalmentOptions
)(implicit appConfig: AppConfig)
  extends FrontendController(mcc)
  with Logging {

  val instalmentOptions: Action[AnyContent] = as.default.async { implicit request =>
    val mockJourney = MockJourney(userAnswers = UserAnswers.empty.copy(affordableAmount = Some(AmountInPence(50000L))))
    Future.successful(Ok(instalmentOptionsPage(instalmentsForm(), mockApi(mockJourney))))
  }

  val instalmentOptionsSubmit: Action[AnyContent] = as.default.async { implicit request =>
    val j: MockJourney = MockJourney()
    instalmentsForm()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(
            instalmentOptionsPage(
              formWithErrors, mockApi(j)
            )
          )),
        (option: String) => {
          Future.successful(Redirect(routes.PaymentScheduleController.checkPaymentSchedule()))
        }
      )
  }

}

object InstalmentsController {
  def mockApi(journey: MockJourney)(implicit appConfig: AppConfig): List[InstalmentOption] = {
    val monthlyInterest: BigDecimal = (appConfig.InterestRates.hmrcRate + appConfig.InterestRates.baseRate) / 12
    val interestPerMonth: BigDecimal = journey.remainingToPay.inPounds * monthlyInterest
    val offerMonths: Int = (journey.remainingToPay.value / journey.userAnswers.getAffordableAmount.value).intValue()
    val month1 = if (offerMonths > 1) offerMonths - 1 else offerMonths
    val month2 = if (offerMonths > 1) offerMonths else offerMonths + 1
    val month3 = if (offerMonths > 1) offerMonths + 1 else offerMonths + 2
    List(
      InstalmentOption(
        numberOfMonths       = month1,
        amountToPayEachMonth = AmountInPence(journey.remainingToPay.value / month1),
        interestPayment      = AmountInPence(interestPerMonth.longValue() * month1)
      ),
      InstalmentOption(
        numberOfMonths       = month2,
        amountToPayEachMonth = AmountInPence(journey.remainingToPay.value / month2),
        interestPayment      = AmountInPence(interestPerMonth.longValue() * month2)
      ),
      InstalmentOption(
        numberOfMonths       = month3,
        amountToPayEachMonth = AmountInPence(journey.remainingToPay.value / month3),
        interestPayment      = AmountInPence(interestPerMonth.longValue() * month3)
      )
    )
  }

  val key: String = "Instalments"

  def instalmentsForm(): Form[String] = Form(
    mapping(
      key -> nonEmptyText
    )(identity)(Some(_))
  )

}
