/*
 * Copyright 2024 HM Revenue & Customs
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
import cats.syntax.eq._
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.JourneyConnector
import essttp.journey.model.CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.{AmountInPence, TaxRegime, UpfrontPaymentAmount}
import models.Language
import models.Languages.{English, Welsh}
import models.enumsforforms.CanPayWithinSixMonthsFormValue
import models.forms.CanPayWithinSixMonthsForm
import play.api.mvc.{Action, AnyContent, Cookie, MessagesControllerComponents}
import requests.RequestSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.ExecutionContext

@Singleton
class CanPayWithinSixMonthsController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    requestSupport:   RequestSupport,
    views:            Views,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  def canPayWithinSixMonths(@unused regime: TaxRegime, lang: Option[Language]): Action[AnyContent] =
    as.continueToSameEndpointAuthenticatedJourneyAction { implicit request =>
      request.journey match {
        case j: Journey.BeforeRetrievedAffordabilityResult =>
          JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)

        case j: Journey.AfterRetrievedAffordabilityResult =>
          finalStateCheck(
            j,
            {
              val previousAnswers = existingAnswersInJourney(request.journey)
              val form = previousAnswers.fold(CanPayWithinSixMonthsForm.form)(value =>
                CanPayWithinSixMonthsForm.form.fill(CanPayWithinSixMonthsFormValue.canPayWithinSixMonthsToFormValue(value)))
              val result = Ok(views.canPayWithinSixMonthsPage(form, remainingAmountToPay(j)))
              lang match {
                case Some(Welsh)   => result.withCookies(Cookie("PLAY_LANG", "cy"))
                case Some(English) => result.withCookies(Cookie("PLAY_LANG", "en"))
                case None          => result
              }
            }
          )
      }
    }

  val canPayWithinSixMonthsSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    CanPayWithinSixMonthsForm.form.bindFromRequest().fold(
      formWithErrors => Ok(views.canPayWithinSixMonthsPage(formWithErrors, remainingAmountToPay(request.journey))),
      { canPayFormValue =>
        val canPay = canPayFormValue.asCanPayWithinSixMonths
        val valueUnchanged = existingAnswersInJourney(request.journey).forall(_.value === canPay.value)

        journeyConnector.updateCanPayWithinSixMonthsAnswers(
          request.journeyId,
          canPay
        ).map(updatedJourney =>
            Routing.redirectToNext(
              routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(request.journey.taxRegime, None),
              updatedJourney,
              valueUnchanged
            ))
      }
    )
  }

  private def existingAnswersInJourney(journey: Journey): Option[CanPayWithinSixMonths] = journey match {
    case _: Journey.BeforeCanPayWithinSixMonthsAnswers => None
    case j: Journey.AfterCanPayWithinSixMonthsAnswers => j.canPayWithinSixMonthsAnswers match {
      case CanPayWithinSixMonthsAnswers.AnswerNotRequired        => None
      case c: CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths => Some(c)
    }
  }

  private def remainingAmountToPay(journey: Journey): AmountInPence = {
    val eligibilityCheckResult = journey match {
      case _: Journey.BeforeEligibilityChecked => sys.error("Could not find eligbility check result to calculate remaining amount to pay")
      case j: Journey.AfterEligibilityChecked  => j.eligibilityCheckResult
    }

    val upfrontPaymentAmount = journey match {
      case _: Journey.BeforeUpfrontPaymentAnswers => sys.error("Could not find upfront payment answers to calculate remaining amount to pay")
      case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers match {
        case UpfrontPaymentAnswers.NoUpfrontPayment               => UpfrontPaymentAmount(AmountInPence.zero)
        case UpfrontPaymentAnswers.DeclaredUpfrontPayment(amount) => amount
      }
    }

    UpfrontPaymentController.deriveRemainingAmountToPay(
      UpfrontPaymentController.determineTotalAmountToPayWithoutInterest(eligibilityCheckResult),
      upfrontPaymentAmount
    )
  }

}

