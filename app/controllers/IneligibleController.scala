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

import actions.Actions
import config.AppConfig
import essttp.journey.model.SjRequest
import messages.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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

  val genericIneligiblePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(Messages.NotEligible.`Call us`, views.partials.genericIneligiblePartial()))
  }

  val debtTooLargePage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(Messages.NotEligible.`Call us`, views.partials.debtTooLargePartial()))
  }

  val debtTooOldPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(Messages.NotEligible.`Call us`, views.partials.debtTooOldPartial()))
  }

  val fileYourReturnPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    val btaReturnUrl: String = request.journey.sjRequest match {
      case SjRequest.Epaye.Simple(returnUrl, _) => returnUrl.value
      case SjRequest.Epaye.Empty()              => s"${appConfig.Urls.businessTaxAccountUrl}"
      case SjRequest.Vat.Simple(returnUrl, _)   => returnUrl.value
    }
    Ok(views.partials.ineligibleTemplatePage(
      pageh1         = Messages.NotEligible.`File your return to use this service`,
      leadingContent = views.partials.returnsNotUpToDatePartial(btaReturnUrl)
    ))
  }

  val alreadyHaveAPaymentPlanPage: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    Ok(views.partials.ineligibleTemplatePage(Messages.NotEligible.`You already have a payment plan with HMRC`, views.partials.existingPaymentPlanPartial()))
  }
}
