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
import actionsmodel.EligibleJourneyRequest
import config.AppConfig
import essttp.journey.model.Journey.Stages
import essttp.journey.model.{Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.AmountInPence
import essttp.utils.Errors
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import java.time.LocalDate

@Singleton
class PaymentPlanSetUpController @Inject() (
    as:        Actions,
    mcc:       MessagesControllerComponents,
    views:     Views,
    appConfig: AppConfig
) extends FrontendController(mcc)
  with Logging {

  implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _

  val epayePaymentPlanSetUp: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    paymentPlanSetup(request)
  }

  val vatPaymentPlanSetUp: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    paymentPlanSetup(request)
  }

  private def paymentPlanSetup(request: EligibleJourneyRequest[AnyContent])(implicit eligibleJourneyRequest: EligibleJourneyRequest[_]): Result = {
    request.journey match {
      case j: Journey.BeforeArrangementSubmitted => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Stages.SubmittedArrangement        => displayConfirmationPage(j)
    }
  }

  def displayConfirmationPage(journey: Journey.Stages.SubmittedArrangement)(implicit request: EligibleJourneyRequest[_]): Result = {
    val firstPaymentDay = journey.selectedPaymentPlan.collections.regularCollections
      .sortBy(_.dueDate.value)
      .headOption.getOrElse(Errors.throwServerErrorException("There are no regular collection dates, this should never happen..."))
      .dueDate

    val hasUpfrontPayment: Boolean = journey.upfrontPaymentAnswers match {
      case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => true
      case UpfrontPaymentAnswers.NoUpfrontPayment          => false
    }

    Ok(
      views.paymentPlanSetUpPage(
        customerPaymentReference = journey.arrangementResponse.customerReference.value,
        paymentDay               = firstPaymentDay,
        hasUpfrontPayment        = hasUpfrontPayment,
        taxRegime                = journey.taxRegime,
        wasEmailAddressRequired  = request.isEmailAddressRequired(appConfig)
      )
    )
  }

  val printSummary: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeArrangementSubmitted => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.Stages.SubmittedArrangement =>
        Ok(views.printSummaryPage(
          paymentReference     = j.arrangementResponse.customerReference.value,
          upfrontPaymentAmount = PaymentPlanSetUpController.deriveUpfrontPaymentFromAnswers(j.upfrontPaymentAnswers),
          dayOfMonth           = j.dayOfMonth.value,
          paymentPlan          = j.selectedPaymentPlan
        ))
    }
  }

}

object PaymentPlanSetUpController {
  private def deriveUpfrontPaymentFromAnswers(upfrontPaymentAnswers: UpfrontPaymentAnswers): Option[AmountInPence] = upfrontPaymentAnswers match {
    case j: UpfrontPaymentAnswers.DeclaredUpfrontPayment => Some(j.amount.value)
    case UpfrontPaymentAnswers.NoUpfrontPayment          => None
  }
}
