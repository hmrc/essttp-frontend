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
import models.enumsforforms.CanPayWithinSixMonthsFormValue
import models.forms.CanPayWithinSixMonthsForm
import play.api.http.HeaderNames
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import requests.RequestSupport
import services.AuditService
import uk.gov.hmrc.hmrcfrontend.controllers.LanguageController
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.ExecutionContext

@Singleton
class CanPayWithinSixMonthsController @Inject() (
    as:                 Actions,
    mcc:                MessagesControllerComponents,
    requestSupport:     RequestSupport,
    views:              Views,
    journeyConnector:   JourneyConnector,
    languageController: LanguageController,
    auditService:       AuditService
)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport
  with Logging {

  import requestSupport._

  def canPayWithinSixMonths(@unused regime: TaxRegime, lang: Option[Language]): Action[AnyContent] =
    as.continueToSameEndpointAuthenticatedJourneyAction.async { implicit request =>
      lang match {
        case None =>
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
                  Ok(views.canPayWithinSixMonthsPage(form, remainingAmountToPay(j)))
                }
              )
          }

        case Some(language) =>
          languageController.switchToLanguage(language.code)(
            request.withHeaders(
              request.headers
                .remove(HeaderNames.REFERER)
                .add(HeaderNames.REFERER -> routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(regime, None).url)
            )
          )

      }
    }

  val canPayWithinSixMonthsSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    CanPayWithinSixMonthsForm.form.bindFromRequest().fold(
      formWithErrors => Ok(views.canPayWithinSixMonthsPage(formWithErrors, remainingAmountToPay(request.journey))),
      { canPayFormValue =>
        val canPay = canPayFormValue.asCanPayWithinSixMonths
        val valueUnchanged = existingAnswersInJourney(request.journey).exists(_.value === canPay.value)

        if (canPay.value) auditService.auditCanUserPayInSixMonths(request.journey, canPay, maybeStartCaseResponse = None)

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
      UpfrontPaymentController.determineTotalAmountToPayWithInterest(eligibilityCheckResult),
      upfrontPaymentAmount
    )
  }

}

