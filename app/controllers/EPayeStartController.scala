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
import essttp.rootmodel.TaxRegime
import messages.{Message, Messages}
import models.{EligibilityData, ttp}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{EligibilityDataService, JourneyService}
import testOnly.models.EligibilityError._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.EPaye.ineligible.IneligibleTemplatePage
import views.html.EPaye.{EPayeLandingPage, EPayeStartPage}
import views.html.partials.{DebtTooLargePartial, DebtTooOldPartial, ExistingPaymentPlanPartial, GenericIneligiblePartial, ReturnsNotUpToDatePartial}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EPayeStartController @Inject() (
    as:                         Actions,
    mcc:                        MessagesControllerComponents,
    journeyService:             JourneyService,
    eligibilityDataService:     EligibilityDataService,
    ineligibleTemplatePage:     IneligibleTemplatePage,
    genericIneligiblePartial:   GenericIneligiblePartial,
    debtTooLargePartial:        DebtTooLargePartial,
    debtTooOldPartial:          DebtTooOldPartial,
    existingPaymentPlanPartial: ExistingPaymentPlanPartial,
    returnsNotUpToDatePartial:  ReturnsNotUpToDatePartial,
    ePayeLandingPage:           EPayeLandingPage,
    ePayeStartPage:             EPayeStartPage
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging with I18nSupport {

  val ePayeLanding: Action[AnyContent] = as.default { implicit request =>
    Ok(ePayeLandingPage())
  }

  val ePayeLandingSubmit: Action[AnyContent] = as.default { _ =>
    Redirect(routes.EPayeStartController.ePayeStart())
  }

  val ePayeStart: Action[AnyContent] = as.default.async { implicit request =>
    request.session.data.get("JourneyId") match {
      case Some(_: String) => for {
        data <- eligibilityDataService.data(
          idType         = "AOR",
          regime         = TaxRegime.Epaye,
          id             = ttp.DefaultTaxId,
          showFinancials = true
        )
      } yield routeResponse(data)
      case _ => throw new IllegalStateException("missing journey")
    }
  }

  val ePayeStartSubmit: Action[AnyContent] = as.default { _ =>
    Redirect(routes.UpfrontPaymentController.upfrontPayment())
  }

  private def genericIneligiblePageInfo(implicit r: Request[_]): (Message, Html) =
    (Messages.NotEligible.`Call us`, genericIneligiblePartial())

  def routeResponse(data: EligibilityData)(implicit R: Request[_]): Result = {
    if (data.hasRejections) {
      val (pageHeading, leadingContentToShow): (Message, Html) = {
        if (data.hasMultipleRejections) {
          genericIneligiblePageInfo
        } else {
          data.rejections.head match {
            case YouAlreadyHaveAPaymentPlan => (Messages.NotEligible.`You already have a payment plan with HMRC`, existingPaymentPlanPartial())
            case DebtIsTooLarge             => (Messages.NotEligible.`Call us`, debtTooLargePartial())
            case DebtIsTooOld               => (Messages.NotEligible.`Call us`, debtTooOldPartial())
            case ReturnsAreNotUpToDate      => (Messages.NotEligible.`File your return to use this service`, returnsNotUpToDatePartial())
            case OutstandingPenalty | PayeIsInsolvent | PayeHasDisallowedCharges |
              RLSFlagIsSet => genericIneligiblePageInfo
          }
        }
      }
      Ok(ineligibleTemplatePage(pageh1         = pageHeading, leadingContent = leadingContentToShow)(implicitly[Request[_]]))
    } else {
      Ok(ePayeStartPage(data.overduePayments, Option(controllers.routes.JourneyCompletionController.abort())))
    }

  }

}
