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

import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.Journey
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import util.JourneyLogger

import scala.concurrent.Future

object JourneyIncorrectStateRouter {

  def logErrorAndRouteToDefaultPageF(journey: Journey)(implicit request: Request[_]): Future[Result] = Future.successful(logErrorAndRouteToDefaultPage(journey))

  def logErrorAndRouteToDefaultPage(journey: Journey)(implicit request: Request[_]): Result = {
    val defaultEndpoint = journey match {
      case _: Journey.Stages.Started                      => Redirect(routes.LandingController.landingPage())
      case _: Journey.Stages.ComputedTaxId                => Redirect(routes.DetermineEligibilityController.determineEligibility())
      case j: Journey.Stages.EligibilityChecked           => EligibilityRouter.nextPage(j.eligibilityCheckResult)
      case _: Journey.Stages.AnsweredCanPayUpfront        => Redirect(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment())
      case _: Journey.Stages.EnteredUpfrontPaymentAmount  => Redirect(routes.UpfrontPaymentController.upfrontPaymentAmount())
      case _: Journey.Stages.RetrievedExtremeDates        => Redirect(routes.DatesApiController.retrieveExtremeDates())
      case _: Journey.Stages.RetrievedAffordabilityResult => Redirect(routes.DetermineAffordabilityController.determineAffordability())
      case _: Journey.Stages.EnteredMonthlyPaymentAmount  => Redirect(routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount())
      case _: Journey.Stages.EnteredDayOfMonth            => Redirect(routes.PaymentDayController.paymentDay())
      case _: Journey.Stages.RetrievedStartDates          => Redirect(routes.DatesApiController.retrieveStartDates())
      case _: Journey.Stages.RetrievedAffordableQuotes    => Redirect(routes.DetermineAffordableQuotesController.retrieveAffordableQuotes())
      case _: Journey.Stages.ChosenPaymentPlan            => Redirect(routes.InstalmentsController.instalmentOptions())
      case _: Journey.Stages.CheckedPaymentPlan           => Redirect(routes.PaymentScheduleController.checkPaymentSchedule())
      case _: Journey.Stages.ChosenTypeOfBankAccount      => Redirect(routes.BankDetailsController.typeOfAccount())
      case _: Journey.Stages.EnteredDirectDebitDetails    => Redirect(routes.BankDetailsController.enterBankDetails())
      case _: Journey.Stages.ConfirmedDirectDebitDetails  => Redirect(routes.BankDetailsController.enterBankDetails())
      case _: Journey.Stages.AgreedTermsAndConditions     => Redirect(routes.BankDetailsController.termsAndConditions())
    }

    JourneyLogger.error(
      "Journey in incorrect state. " +
        "Please investigate why. " +
        s"Sending user to the first page which supports that journey state ${defaultEndpoint}"
    )
    defaultEndpoint
  }
}
