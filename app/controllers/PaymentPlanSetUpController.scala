/*
 * Copyright 2023 HM Revenue & Customs
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

import actions.Actions
import actionsmodel.EligibleJourneyRequest
import config.AppConfig
import essttp.journey.model.PaymentPlanAnswers.{PaymentPlanAfterAffordability, PaymentPlanNoAffordability}
import essttp.journey.model.{Journey, JourneyStage, UpfrontPaymentAnswers}
import essttp.rootmodel.{AmountInPence, TaxRegime}
import essttp.utils.Errors
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

@Singleton
class PaymentPlanSetUpController @Inject() (
  as:    Actions,
  mcc:   MessagesControllerComponents,
  views: Views
)(using appConfig: AppConfig)
    extends FrontendController(mcc),
      Logging {

  given Ordering[LocalDate] = _.compareTo(_)

  val epayePaymentPlanSetUp: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    paymentPlanSetup(request)
  }

  val vatPaymentPlanSetUp: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    paymentPlanSetup(request)
  }

  val saPaymentPlanSetUp: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    paymentPlanSetup(request)
  }

  val simpPaymentPlanSetUp: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    paymentPlanSetup(request)
  }

  val epayeVatPrintSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    printSummarySetup(request)
  }

  val saPrintSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    printSummarySetup(request)
  }

  val simpPrintSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    printSummarySetup(request)
  }

  private def printSummarySetup(
    request: EligibleJourneyRequest[AnyContent]
  )(using EligibleJourneyRequest[?]): Result =
    request.journey match {
      case j: JourneyStage.BeforeArrangementSubmitted => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.SubmittedArrangement            => printSummary(j)
    }

  private def paymentPlanSetup(
    request: EligibleJourneyRequest[AnyContent]
  )(using EligibleJourneyRequest[?]): Result =
    request.journey match {
      case j: JourneyStage.BeforeArrangementSubmitted => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.SubmittedArrangement            => displayConfirmationPage(j)
    }

  private def wasAffordabilityJourney(journey: Journey.SubmittedArrangement): Boolean =
    journey.paymentPlanAnswers match {
      case _: PaymentPlanNoAffordability    => false
      case _: PaymentPlanAfterAffordability => true
    }

  def displayConfirmationPage(
    journey: Journey.SubmittedArrangement
  )(using request: EligibleJourneyRequest[?]): Result = {
    val firstPaymentDay = journey.paymentPlanAnswers.selectedPaymentPlan.collections.regularCollections
      .sortBy(_.dueDate.value)
      .headOption
      .getOrElse(Errors.throwServerErrorException("There are no regular collection dates, this should never happen..."))
      .dueDate

    val hasUpfrontPayment: Boolean = journey.upfrontPaymentAnswers match {
      case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => true
      case UpfrontPaymentAnswers.NoUpfrontPayment          => false
    }

    journey.taxRegime match {
      // OPS-12246 sa page differs from the others due to these changes, split off into two
      case TaxRegime.Sa                                     =>
        Ok(
          views.saPaymentPlanSetUpPage(
            customerPaymentReference = journey.arrangementResponse.customerReference.value,
            paymentDay = firstPaymentDay,
            hasUpfrontPayment = hasUpfrontPayment,
            wasEmailAddressRequired = request.isEmailAddressRequired(appConfig),
            regimeDigitalCorrespondence = request.eligibilityCheckResult.regimeDigitalCorrespondence.value,
            wasAffordabilityJourney = wasAffordabilityJourney(journey)
          )
        )
      case TaxRegime.Epaye | TaxRegime.Vat | TaxRegime.Simp =>
        Ok(
          views.paymentPlanSetUpPage(
            journey.arrangementResponse.customerReference.value,
            journey.taxRegime,
            wasAffordabilityJourney(journey)
          )
        )
    }

  }

  def printSummary(journey: Journey.SubmittedArrangement)(using EligibleJourneyRequest[?]): Result =
    journey.taxRegime match {
      case TaxRegime.Sa =>
        Ok(
          views.saPrintSummaryPage(
            paymentReference = journey.arrangementResponse.customerReference.value,
            upfrontPaymentAmount =
              PaymentPlanSetUpController.deriveUpfrontPaymentFromAnswers(journey.upfrontPaymentAnswers),
            dayOfMonth = journey.paymentPlanAnswers.dayOfMonth.value,
            paymentPlan = journey.paymentPlanAnswers.selectedPaymentPlan
          )
        )
      case _            =>
        Ok(
          views.printSummaryPage(
            paymentReference = journey.arrangementResponse.customerReference.value,
            upfrontPaymentAmount =
              PaymentPlanSetUpController.deriveUpfrontPaymentFromAnswers(journey.upfrontPaymentAnswers),
            dayOfMonth = journey.paymentPlanAnswers.dayOfMonth.value,
            paymentPlan = journey.paymentPlanAnswers.selectedPaymentPlan
          )
        )
    }

}

object PaymentPlanSetUpController {
  private def deriveUpfrontPaymentFromAnswers(upfrontPaymentAnswers: UpfrontPaymentAnswers): Option[AmountInPence] =
    upfrontPaymentAnswers match {
      case j: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(j.amount.value)
      case UpfrontPaymentAnswers.NoUpfrontPayment          => None
    }
}
