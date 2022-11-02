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
import essttp.emailverification.EmailVerificationStatus
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import util.JourneyLogger

import scala.concurrent.Future

object JourneyIncorrectStateRouter {

  def logErrorAndRouteToDefaultPageF(journey: Journey)(implicit request: Request[_]): Future[Result] = Future.successful(logErrorAndRouteToDefaultPage(journey))

  def logErrorAndRouteToDefaultPage(journey: Journey)(implicit request: Request[_]): Result = {
    val redirectTo = journey match {
      case j: Journey.Stages.Started =>
        j.taxRegime match {
          case TaxRegime.Epaye => routes.LandingController.epayeLandingPage
          case TaxRegime.Vat   => routes.LandingController.vatLandingPage
        }
      case _: Journey.Stages.ComputedTaxId      => routes.DetermineEligibilityController.determineEligibility
      case j: Journey.Stages.EligibilityChecked => EligibilityRouter.nextPage(j.eligibilityCheckResult)
      case j: Journey.Stages.AnsweredCanPayUpfront =>
        if (j.canPayUpfront.value) routes.UpfrontPaymentController.upfrontPaymentAmount
        else routes.DatesApiController.retrieveExtremeDates
      case _: Journey.Stages.EnteredUpfrontPaymentAmount  => routes.UpfrontPaymentController.upfrontPaymentSummary
      case _: Journey.Stages.RetrievedExtremeDates        => routes.DetermineAffordabilityController.determineAffordability
      case _: Journey.Stages.RetrievedAffordabilityResult => routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount
      case _: Journey.Stages.EnteredMonthlyPaymentAmount  => routes.PaymentDayController.paymentDay
      case _: Journey.Stages.EnteredDayOfMonth            => routes.DatesApiController.retrieveStartDates
      case _: Journey.Stages.RetrievedStartDates          => routes.DetermineAffordableQuotesController.retrieveAffordableQuotes
      case _: Journey.Stages.RetrievedAffordableQuotes    => routes.InstalmentsController.instalmentOptions
      case _: Journey.Stages.ChosenPaymentPlan            => routes.PaymentScheduleController.checkPaymentSchedule
      case _: Journey.Stages.CheckedPaymentPlan           => routes.BankDetailsController.detailsAboutBankAccount
      case j: Journey.Stages.EnteredDetailsAboutBankAccount =>
        if (j.detailsAboutBankAccount.isAccountHolder) routes.BankDetailsController.enterBankDetails
        else routes.BankDetailsController.cannotSetupDirectDebitOnlinePage
      case _: Journey.Stages.EnteredDirectDebitDetails   => routes.BankDetailsController.checkBankDetails
      case _: Journey.Stages.ConfirmedDirectDebitDetails => routes.TermsAndConditionsController.termsAndConditions
      // prevent accidentally submitting arrangement twice
      case j: Journey.Stages.AgreedTermsAndConditions =>
        if (j.isEmailAddressRequired) routes.EmailController.whichEmailDoYouWantToUse
        else routes.PaymentPlanSetUpController.paymentPlanSetUp
      case _: Journey.Stages.SelectedEmailToBeVerified => routes.EmailController.whichEmailDoYouWantToUse
      case j: Journey.Stages.EmailVerificationComplete =>
        j.emailVerificationStatus match {
          case EmailVerificationStatus.Verified => routes.PaymentPlanSetUpController.paymentPlanSetUp
          case EmailVerificationStatus.Locked   => routes.EmailController.tooManyPasscodeAttempts
        }

      case _: Journey.Stages.SubmittedArrangement => routes.PaymentPlanSetUpController.paymentPlanSetUp
    }

    JourneyLogger.error(
      "Journey in incorrect state. " +
        "Please investigate why. " +
        s"Sending user to the next page the user needs to provide an answer on: $redirectTo"
    )
    Redirect(redirectTo)
  }
}
