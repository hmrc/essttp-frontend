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
import controllers.UpfrontPaymentController.upfrontPaymentAmountForm
import essttp.journey.model.Journey
import essttp.rootmodel.{AmountInPence, CanPayUpfront}
import models.MoneyUtil.amountOfMoneyFormatter
import models.enumsforforms.CanPayUpfrontFormValue
import models.forms.CanPayUpfrontForm
import models.{MockJourney, UserAnswers}
import play.api.data.Forms.mapping
import play.api.data.{Form, Forms}
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

  val canYouMakeAnUpfrontPayment: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.Stages.AfterStarted       => logErrorAndRouteToDefaultPage(j)
      case j: Journey.Stages.AfterComputedTaxId => logErrorAndRouteToDefaultPage(j)
      case _: Journey.HasEligibilityCheckResult => displayPage()
    }
  }

  private def displayPage()(implicit request: Request[_]): Result = {
    val backUrl: Option[String] = Some(routes.YourBillController.yourBill().url)
    Ok(views.canYouMakeAnUpFrontPayment(CanPayUpfrontForm.form, backUrl))
  }

  val canYouMakeAnUpfrontPaymentSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    CanPayUpfrontForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(views.canYouMakeAnUpFrontPayment(formWithErrors))),
        (canPayUpfrontForm: CanPayUpfrontFormValue) => {
          val canPayUpfront: CanPayUpfront = canPayUpfrontForm.asCanPayUpfront
          val pageToRedirectTo: Call =
            if (canPayUpfront.value) {
              routes.UpfrontPaymentController.upfrontPaymentAmount()
            } else {
              routes.MonthlyPaymentAmountController.monthlyPaymentAmount()
            }
          journeyService.updateCanPayUpfront(request.journeyId, canPayUpfront)
            .map(_ => Redirect(pageToRedirectTo))
        }
      )
  }

  val upfrontPaymentAmount: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val mockJourney = MockJourney(userAnswers = UserAnswers.empty.copy(hasUpfrontPayment = Some(true)))
    Future.successful(Ok(views.upfrontPaymentAmountPage(upfrontPaymentAmountForm(mockJourney), mockJourney.qualifyingDebt, AmountInPence(100L))))
  }

  val upfrontPaymentAmountSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val mockJourney = MockJourney(userAnswers = UserAnswers.empty)
    upfrontPaymentAmountForm(mockJourney)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(views.upfrontPaymentAmountPage(formWithErrors, mockJourney.qualifyingDebt, AmountInPence(100L)))),
        (s: BigDecimal) => {
          /* TODO: compute what is remaining to pay by subtracting "s" from the initial qualifying debt amount
             and write to session store
           */
          Future(Redirect(routes.UpfrontPaymentController.upfrontSummary()))
        }
      )
  }

  val upfrontSummary: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val mockUserAnswers = UserAnswers.empty.copy(
      hasUpfrontPayment = Some(true),
      upfrontAmount     = Some(AmountInPence(10000L))
    )
    Future.successful(Ok(views.upfrontSummaryPage(mockUserAnswers, AmountInPence(200000L))))
  }
}

object UpfrontPaymentController {

  def upfrontPaymentAmountForm(journey: MockJourney): Form[BigDecimal] = Form(
    mapping(
      "UpfrontPaymentAmount" -> Forms.of(amountOfMoneyFormatter(AmountInPence(100L).inPounds > _, journey.qualifyingDebt.inPounds < _))
    )(identity)(Some(_))
  )
}
