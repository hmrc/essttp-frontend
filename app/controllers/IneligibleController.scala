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
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}

@Singleton()
class IneligibleController @Inject() (
    mcc:          MessagesControllerComponents,
    views:        Views,
    as:           Actions,
    appConfig:    AppConfig,
    auditService: AuditService
) extends FrontendController(mcc) with Logging {

  def genericIneligiblePage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.genericIneligiblePartial()
    ))

  val payeGenericIneligiblePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericIneligiblePage }

  val vatGenericIneligiblePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericIneligiblePage }

  val saGenericIneligiblePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericIneligiblePage }

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

  val saDebtTooLargePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooLargePartial(appConfig.PolicyParameters.SA.maxAmountOfDebt)
    ))
  }

  val epayeDebtTooSmallPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Pay your ... bill in full`(request.journey.taxRegime),
      leadingContent = views.partials.debtTooSmallPartial(appConfig.PolicyParameters.EPAYE.payOnlineLink)
    ))
  }

  val vatDebtTooSmallPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Pay your ... bill in full`(request.journey.taxRegime),
      leadingContent = views.partials.debtTooSmallPartial(appConfig.PolicyParameters.VAT.payOnlineLink)
    ))
  }

  val saDebtTooSmallPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1                      = Messages.NotEligible.`Pay your ... bill in full`(request.journey.taxRegime),
      leadingContent              = views.partials.debtTooSmallPartial(appConfig.PolicyParameters.SA.payOnlineLink),
      showFullListPreparationTips = false
    ))
  }

  val epayeDebtTooOldPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooOldPartial(appConfig.PolicyParameters.EPAYE.maxAgeOfDebtInYears)
    ))
  }

  val vatDebtTooOldPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooOldPartial(appConfig.PolicyParameters.VAT.maxAgeOfDebtInDays)
    ))
  }

  val saDebtTooOldPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.debtTooOldPartial(appConfig.PolicyParameters.SA.maxAgeOfDebtInDays)
    ))
  }

  val vatDebtBeforeAccountingDatePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Call us about a payment plan`,
      leadingContent = views.partials.vatDebtBeforeAccountingDatePartial(appConfig.PolicyParameters.VAT.vatAccountingPeriodStart)
    ))
  }

  def genericFileReturnPage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(views.partials.ineligibleTemplatePage(
      pageh1                  = Messages.NotEligible.`File your return to use this service`(request.journey.taxRegime),
      leadingContent          = views.partials.returnsNotUpToDatePartial(determineFileYourReturnUrl, request.journey.taxRegime),
      showCallPreparationTips = false
    ))

  val epayeFileYourReturnPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericFileReturnPage }

  val vatFileYourReturnPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericFileReturnPage }

  val saFileYourReturnPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericFileReturnPage }

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
  val saAlreadyHaveAPaymentPlanPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    genericAlreadyHaveAPaymentPlanPage
  }

  private def genericNoDueDatesReachedPage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(
      views.partials.noDueDatesTemplatePage(
        Messages.NotEligible.`You cannot use this service`,
        views.partials.noDueDatesReachedPartial(request.journey.taxRegime)
      )
    )

  val epayeNoDueDatesReachedPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    genericNoDueDatesReachedPage
  }

  val vatNoDueDatesReachedPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    genericNoDueDatesReachedPage
  }

  private def determineFileYourReturnUrl(implicit request: AuthenticatedJourneyRequest[AnyContent]): String = request.journey.sjRequest match {
    case SjRequest.Epaye.Simple(returnUrl, _) => returnUrl.value
    case SjRequest.Epaye.Empty()              => s"${appConfig.Urls.businessTaxAccountUrl}"
    case SjRequest.Vat.Simple(returnUrl, _)   => returnUrl.value
    case SjRequest.Vat.Empty()                => s"${appConfig.Urls.businessTaxAccountUrl}"
    case SjRequest.Sa.Simple(returnUrl, _)    => returnUrl.value
    case SjRequest.Sa.Empty()                 => s"${appConfig.Urls.enrolForSaUrl}"
  }

  private def genericYouHaveChosenNotToSetUpPage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(
      views.partials.ineligibleTemplatePage(
        Messages.NotEligible.`Call us about a payment plan`,
        views.partials.youAlreadyHaveDirectDebitPartial(request.journey.taxRegime),
        hasBackLink = true
      )
    )

  val epayeYouHaveChosenNotToSetUpPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    auditService.auditDdInProgress(request.journey, hasChosenToContinue = false)
    genericYouHaveChosenNotToSetUpPage
  }

  val vatYouHaveChosenNotToSetUpPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    auditService.auditDdInProgress(request.journey, hasChosenToContinue = false)
    genericYouHaveChosenNotToSetUpPage
  }

  def genericRLSIneligiblePage(implicit request: AuthenticatedJourneyRequest[AnyContent]): Result =
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`Update your personal details to use this service`,
      leadingContent = views.partials.genericRLSPartial(appConfig.Urls.tellHMRCChangeDetailsUrl)
    ))

  val epayeRLSPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericRLSIneligiblePage }

  val vatRLSPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericRLSIneligiblePage }

  val saRLSPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request => genericRLSIneligiblePage }

}
