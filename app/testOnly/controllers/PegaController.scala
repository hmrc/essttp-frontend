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

package testOnly.controllers

import actions.Actions
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import essttp.utils.Errors
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.views.html.IAmPegaPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class PegaController @Inject() (
    as:          Actions,
    mcc:         MessagesControllerComponents,
    IAmPegaPage: IAmPegaPage
) extends FrontendController(mcc) {

  def dummyPegaPage(regime: TaxRegime): Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    val whyCannotPayInFullAnswers = request.journey match {
      case j: Journey.AfterWhyCannotPayInFullAnswers => j.whyCannotPayInFullAnswers
      case other                                     => Errors.throwServerErrorException(s"Could not find WhyCannotPayInFullAnswers from journey in state ${other.name}")
    }

    val upfrontPaymentAnswers = request.journey match {
      case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
      case other                                 => Errors.throwServerErrorException(s"Could not find UpfrontPaymentAnswers from journey in state ${other.name}")
    }

    val canPayWithinSixMonthsAnswers = request.journey match {
      case j: Journey.AfterCanPayWithinSixMonthsAnswers => j.canPayWithinSixMonthsAnswers
      case other                                        => Errors.throwServerErrorException(s"Could not find CanPayWithinSixMonthsAnswers from journey in state ${other.name}")
    }

    Ok(IAmPegaPage(
      regime,
      request.enrolments,
      whyCannotPayInFullAnswers,
      upfrontPaymentAnswers,
      canPayWithinSixMonthsAnswers
    ))
  }

  def dummyPegaPageContinue(regime: TaxRegime): Action[AnyContent] = as.authenticatedJourneyAction { _ =>
    Redirect(controllers.routes.PegaController.callback(regime)).withNewSession
  }

  def change(pageId: String, regime: TaxRegime): Action[AnyContent] = as.authenticatedJourneyAction { _ =>
    Redirect(controllers.routes.PaymentScheduleController.changeFromCheckPaymentSchedule(pageId, regime)).withNewSession
  }

  def backFromPegaLanding(regime: TaxRegime): Action[AnyContent] = as.authenticatedJourneyAction { _ =>
    Redirect(controllers.routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(regime)).withNewSession
  }

}
