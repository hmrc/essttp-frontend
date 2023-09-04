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
import actionsmodel.AuthenticatedJourneyRequest
import config.AppConfig
import essttp.journey.model.SjRequest
import messages.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}

@Singleton()
class IneligibleController @Inject() (
    mcc:       MessagesControllerComponents,
    views:     Views,
    as:        Actions,
    appConfig: AppConfig
) extends FrontendController(mcc) with Logging {

  def genericIneligiblePage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.genericIneligiblePartial()
    ))

  val payeGenericIneligiblePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericIneligiblePage }

  val vatGenericIneligiblePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericIneligiblePage }

  val epayeDebtTooLargePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooLargePartial(appConfig.PolicyParameters.EPAYE.maxAmountOfDebt)
    ))
  }

  val vatDebtTooLargePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooLargePartial(appConfig.PolicyParameters.VAT.maxAmountOfDebt)
    ))
  }

  val epayeDebtTooSmallPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`You cannot use this service`,
      leadingContent = views.partials.debtTooSmallPartial(appConfig.PolicyParameters.EPAYE.govukPayLink)
    ))
  }

  val vatDebtTooSmallPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`You cannot use this service`,
      leadingContent = views.partials.debtTooSmallPartial(appConfig.PolicyParameters.VAT.govukPayLink)
    ))
  }

  val epayeDebtTooOldPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooOldPartial(appConfig.PolicyParameters.EPAYE.maxAgeOfDebtInDays)
    ))
  }

  val vatDebtTooOldPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooOldPartial(appConfig.PolicyParameters.VAT.maxAgeOfDebtInDays)
    ))
  }

  def genericFileReturnPage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`File your return to use this service`,
      leadingContent = views.partials.returnsNotUpToDatePartial(determineBtaReturnUrl, request.journey.taxRegime)
    ))

  val epayeFileYourReturnPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericFileReturnPage }

  val vatFileYourReturnPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericFileReturnPage }

  def genericAlreadyHaveAPaymentPlanPage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(
      views.partials.ineligibleTemplatePage(
        Messages.NotEligible.`You already have a payment plan with HMRC`,
        views.partials.existingPaymentPlanPartial(request.journey.taxRegime)
      )
    )

  val epayeAlreadyHaveAPaymentPlanPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    genericAlreadyHaveAPaymentPlanPage
  }
  val vatAlreadyHaveAPaymentPlanPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    genericAlreadyHaveAPaymentPlanPage
  }

  private def determineBtaReturnUrl(implicit request: AuthenticatedJourneyRequest[AnyContent]): String = request.journey.sjRequest match {
    case SjRequest.Epaye.Simple(returnUrl, _) => returnUrl.value
    case SjRequest.Epaye.Empty()              => s"${appConfig.Urls.businessTaxAccountUrl}"
    case SjRequest.Vat.Simple(returnUrl, _)   => returnUrl.value
    case SjRequest.Vat.Empty()                => s"${appConfig.Urls.businessTaxAccountUrl}"
  }
}
