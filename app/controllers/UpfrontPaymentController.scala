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
import controllers.UpfrontPaymentController.{CanUpfrontPaymentFormValue, form, upfrontPaymentAmountForm}
import enumeratum.Enum
import essttp.journey.model.{Journey, Origins}
import essttp.rootmodel.{AmountInPence, CanPayUpfront}
import langswitch.Language
import models.MoneyUtil.amountOfMoneyFormatter
import models.{EligibilityError, MockJourney, UserAnswers}
import play.api.data.Forms.{boolean, mapping, nonEmptyText}
import play.api.data.{Form, Forms, Mapping}
import play.api.mvc._
import services.JourneyService
import testOnly.formsmodel.SignInAsFormValue
import messages.Messages
import requests.RequestSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{EnumFormatter, Logging}
import views.Views

import javax.inject.{Inject, Singleton}
import scala.collection.immutable
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
      case j: Journey.HasEligibilityCheckResult => displayPage(j)
    }
  }

  private def displayPage(journey: Journey.HasEligibilityCheckResult)(implicit request: Request[_]): Result = {
    val backUrl = journey.origin match {
      case Origins.Epaye.Bta         => Some(routes.YourBillController.yourBill().url)
      case Origins.Epaye.DetachedUrl => Some(routes.YourBillController.yourBill().url)
      case Origins.Epaye.GovUk       => Some(routes.YourBillController.yourBill().url)
    }
    Ok(views.canYouMakeAnUpFrontPayment(form, backUrl))
  }

  val canYouMakeAnUpfrontPaymentSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(views.canYouMakeAnUpFrontPayment(formWithErrors))),
        (canMakeUpfrontPayment: CanUpfrontPaymentFormValue) => {
          val canPayUpfront: CanPayUpfront = canMakeUpfrontPayment.asCanPayUpfront
          val pageToRedirectTo: Call =
            if (canPayUpfront.value) {
              routes.UpfrontPaymentController.upfrontPaymentAmount()
            } else {
              routes.MonthlyPaymentAmountController.monthlyPaymentAmount()
            }
          journeyService.updateCanPayUpfront(request.journeyId, canPayUpfront)
            .flatMap(_ => Future.successful(Redirect(pageToRedirectTo)))
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

  sealed trait CanUpfrontPaymentFormValue extends enumeratum.EnumEntry {
    def asCanPayUpfront: CanPayUpfront = this match {
      case CanUpfrontPaymentFormValue.Yes => CanPayUpfront(true)
      case CanUpfrontPaymentFormValue.No  => CanPayUpfront(false)
    }
  }
  object CanUpfrontPaymentFormValue extends Enum[CanUpfrontPaymentFormValue] {
    case object Yes extends CanUpfrontPaymentFormValue
    case object No extends CanUpfrontPaymentFormValue
    override def values: immutable.IndexedSeq[CanUpfrontPaymentFormValue] = findValues
  }

  def form(implicit language: Language): Form[CanUpfrontPaymentFormValue] = {

    val canMakeUpfrontPaymentMapping: Mapping[CanUpfrontPaymentFormValue] = Forms.of(EnumFormatter.format(
      enum                    = CanUpfrontPaymentFormValue,
      errorMessageIfMissing   = Messages.UpfrontPayment.`Select yes if you can make an upfront payment`.show,
      errorMessageIfEnumError = Messages.UpfrontPayment.`Select yes if you can make an upfront payment`.show
    ))

    Form(
      mapping(
        "CanYouMakeAnUpFrontPayment" -> canMakeUpfrontPaymentMapping
      )(identity)(Some(_))
    )
  }

  def upfrontPaymentAmountForm(journey: MockJourney): Form[BigDecimal] = Form(
    mapping(
      "UpfrontPaymentAmount" -> Forms.of(amountOfMoneyFormatter(AmountInPence(100L).inPounds > _, journey.qualifyingDebt.inPounds < _))
    )(identity)(Some(_))
  )

}
