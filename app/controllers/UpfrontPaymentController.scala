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
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import essttp.journey.model.Journey
import essttp.journey.model.ttp.DebtTotalAmount
import essttp.rootmodel.{AmountInPence, CanPayUpfront, UpfrontPaymentAmount}
import models.enumsforforms.CanPayUpfrontFormValue
import models.forms.{CanPayUpfrontForm, UpfrontPaymentAmountForm}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import requests.RequestSupport
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpfrontPaymentController @Inject() (
    as:             Actions,
    mcc:            MessagesControllerComponents,
    views:          Views,
    journeyService: JourneyService,
    requestSupport: RequestSupport
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val canYouMakeAnUpfrontPayment: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.Stages.AfterStarted       => logErrorAndRouteToDefaultPage(j)
      case j: Journey.Stages.AfterComputedTaxId => logErrorAndRouteToDefaultPage(j)
      case _: Journey.HasEligibilityCheckResult => displayCanYouPayUpfrontPage()
    }
  }

  private def displayCanYouPayUpfrontPage()(implicit request: Request[_]): Result = {
    val backUrl: Option[String] = Some(routes.YourBillController.yourBill().url)
    Ok(views.canYouMakeAnUpFrontPayment(CanPayUpfrontForm.form, backUrl))
  }

  val canYouMakeAnUpfrontPaymentSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    CanPayUpfrontForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(views.canYouMakeAnUpFrontPayment(
            form    = formWithErrors,
            backUrl = Some(routes.YourBillController.yourBill().url)
          ))),
        (canPayUpfrontForm: CanPayUpfrontFormValue) => {
          val canPayUpfront: CanPayUpfront = canPayUpfrontForm.asCanPayUpfront
          val pageToRedirectTo: Call =
            if (canPayUpfront.value) {
              routes.UpfrontPaymentController.upfrontPaymentAmount()
            } else {
              routes.MonthlyPaymentAmountController.monthlyPaymentAmount()
            }
          journeyService.updateCanPayUpfront(request.journeyId, canPayUpfront)
            .map(_ => Redirect(pageToRedirectTo.url))
        }
      )
  }

  val upfrontPaymentAmount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.Stages.AfterStarted                                        => logErrorAndRouteToDefaultPage(j)
      case j: Journey.Stages.AfterComputedTaxId                                  => logErrorAndRouteToDefaultPage(j)
      case j: Journey.Stages.AfterEligibilityCheck                               => logErrorAndRouteToDefaultPage(j)
      case j: Journey.HasCanPayUpfront if !j.canPayUpfront.userWantsToPayUpFront => logErrorAndRouteToDefaultPage(j)
      case j: Journey.HasCanPayUpfront if j.canPayUpfront.userWantsToPayUpFront  => displayUpfrontPageAmountPage(j)
    }
  }

  private def displayUpfrontPageAmountPage(journey: Journey.HasCanPayUpfront)(implicit request: Request[_]): Result = {
    val backUrl: Option[String] = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url)
    val totalDebtAmount: DebtTotalAmount = UpfrontPaymentController.determineMaxDebt(journey)
    Ok(views.upfrontPaymentAmountPage(
      form = UpfrontPaymentAmountForm.form(journey),
      maximumPayment = AmountInPence(totalDebtAmount.value),
      minimumPayment = UpfrontPaymentController.minimumPaymentAmount,
      backUrl = backUrl
    ))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    // TODO change the totalDebtAmount calculated here - I think ttp are changing their api to have total debt on the top level, not in the chargeTypeAssessment array
    val totalDebtAmount: DebtTotalAmount = UpfrontPaymentController.determineMaxDebt(request.journey)
    UpfrontPaymentAmountForm.form(request.journey)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[BigDecimal]) =>
          Future.successful(Ok(
            views.upfrontPaymentAmountPage(
              form           = formWithErrors,
              maximumPayment = AmountInPence(totalDebtAmount.value),
              minimumPayment = UpfrontPaymentController.minimumPaymentAmount,
              backUrl        = Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment().url)
            )
          )),
        (validForm: BigDecimal) => {
          //amount in pence case class apply method converts big decimal to pennies
          val amountInPence: AmountInPence = AmountInPence(validForm)
          journeyService.updateUpfrontPaymentAmount(request.journeyId, UpfrontPaymentAmount(amountInPence))
            .map(_ => Redirect(routes.UpfrontPaymentController.upfrontPaymentSummary().url))
        }
      )
  }

  val upfrontPaymentSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    Ok("Submitted the upfront payment amount, page is still a wip and needs wiring up...\n" +
      s"For testing purposes for now, here's the journey:\n ${Json.prettyPrint(Json.toJson(request.journey))}")
  }
}

object UpfrontPaymentController {
  def determineMaxDebt(journey: Journey): DebtTotalAmount = journey match {
    case j: Journey.Epaye.AfterCanPayUpfront =>
      j.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount)
        .headOption.getOrElse(throw new RuntimeException("Total debt not found, there's nothing we can do in this situation?"))
    case j: Journey.Epaye.AfterUpfrontPaymentAmount =>
      j.eligibilityCheckResult.chargeTypeAssessment.map(_.debtTotalAmount)
        .headOption.getOrElse(throw new RuntimeException("Total debt not found, there's nothing we can do in this situation?"))
  }

  val minimumPaymentAmount: AmountInPence = AmountInPence(100L) // todo, find out what the minimum upfront payment amount can be, for now £1
}
