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
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import essttp.journey.model.ttp.affordability.InstalmentAmounts
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.{AmountInPence, MonthlyPaymentAmount}
import essttp.utils.Errors
import models.MoneyUtil._
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms}
import play.api.mvc._
import requests.RequestSupport
import services.{JourneyService, TtpService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MonthlyPaymentAmountController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    views:          Views,
    journeyService: JourneyService,
    ttpService:     TtpService,
    requestSupport: RequestSupport,
    appConfig:      AppConfig
)(
    implicit
    executionContext: ExecutionContext
) extends FrontendController(mcc)
  with Logging {

  val displayMonthlyPaymentAmount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeUpfrontPaymentAnswers => logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterUpfrontPaymentAnswers  => displayMonthlyPaymentAmountPage(j)
    }
  }

  private def displayMonthlyPaymentAmountPage(journey: Journey.AfterUpfrontPaymentAnswers)(implicit request: Request[_]): Result = {
    val backUrl: Option[String] = journey.upfrontPaymentAnswers match {
      case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(routes.UpfrontPaymentController.upfrontPaymentSummary().url)
      case UpfrontPaymentAnswers.NoUpfrontPayment          => Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url)
    }

    val minMaxResponse: InstalmentAmounts = journey match {
      case _: Journey.BeforeRetrievedAffordabilityResult => Errors.throwServerErrorException("We don't have the affordability api response...")
      case j: Journey.AfterRetrievedAffordabilityResult  => j.instalmentAmounts
    }

    Ok(views.monthlyPaymentAmountPage(
      form           = MonthlyPaymentAmountController.monthlyPaymentAmountForm(minMaxResponse.minimumInstalmentAmount, minMaxResponse.maximumInstalmentAmount),
      maximumPayment = minMaxResponse.maximumInstalmentAmount,
      minimumPayment = minMaxResponse.minimumInstalmentAmount,
      backUrl        = backUrl
    ))
  }

  val monthlyPaymentAmountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val backUrl: Option[String] = request.journey match {
      case _: Journey.BeforeUpfrontPaymentAnswers => Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url)
      case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers match {
        case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(routes.UpfrontPaymentController.upfrontPaymentSummary().url)
        case UpfrontPaymentAnswers.NoUpfrontPayment          => Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url)
      }
    }
    val minMaxResponse: InstalmentAmounts = request.journey match {
      case _: Journey.BeforeRetrievedAffordabilityResult => Errors.throwServerErrorException("We don't have the affordability api response...")
      case j: Journey.AfterRetrievedAffordabilityResult  => j.instalmentAmounts
    }
    MonthlyPaymentAmountController.monthlyPaymentAmountForm(minMaxResponse.minimumInstalmentAmount, minMaxResponse.maximumInstalmentAmount)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(views.monthlyPaymentAmountPage(
            form           = formWithErrors,
            maximumPayment = minMaxResponse.maximumInstalmentAmount,
            minimumPayment = minMaxResponse.minimumInstalmentAmount,
            backUrl        = backUrl
          ))),
        (validForm: BigDecimal) => {
          val monthlyPaymentAmount: MonthlyPaymentAmount = MonthlyPaymentAmount(AmountInPence(validForm))
          journeyService.updateMonthlyPaymentAmount(request.journeyId, monthlyPaymentAmount)
            .map(_ => Redirect(routes.PaymentDayController.paymentDay()))
        }
      )
  }
}

object MonthlyPaymentAmountController {
  val key: String = "MonthlyPaymentAmount"

  def monthlyPaymentAmountForm(minimumAmount: AmountInPence, maximumAmount: AmountInPence): Form[BigDecimal] = Form(
    mapping(
      key -> Forms.of(amountOfMoneyFormatter(minimumAmount.inPounds > _, maximumAmount.inPounds < _))
    )(identity)(Some(_))
  )
}
