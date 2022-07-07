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
import essttp.journey.model.Journey
import essttp.rootmodel.bank.{BankDetails, DirectDebitDetails}
import models.enumsforforms.IsSoleSignatoryFormValue
import models.forms.BankDetailsForm
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import requests.RequestSupport
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankDetailsController @Inject() (
    as:             Actions,
    views:          Views,
    mcc:            MessagesControllerComponents,
    requestSupport: RequestSupport,
    journeyService: JourneyService
)(
    implicit
    executionContext: ExecutionContext
) extends FrontendController(mcc)
  with Logging {

  val enterBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeCheckedPaymentPlan => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterCheckedPaymentPlan  => displayEnterBankDetailsPage(j)
    }
  }

  private def displayEnterBankDetailsPage(journey: Journey.AfterCheckedPaymentPlan)(implicit request: Request[_]): Result = {
    val maybePrePoppedForm: Form[BankDetailsForm] = journey match {
      case _: Journey.BeforeEnteredDirectDebitDetails => BankDetailsForm.form
      case j: Journey.AfterEnteredDirectDebitDetails =>
        BankDetailsForm.form.fill(BankDetailsForm(
          name            = j.directDebitDetails.bankDetails.name,
          sortCode        = j.directDebitDetails.bankDetails.sortCode,
          accountNumber   = j.directDebitDetails.bankDetails.accountNumber,
          isSoleSignatory = IsSoleSignatoryFormValue.booleanToIsSoleSignatoryFormValue(j.directDebitDetails.isAccountHolder)
        ))
    }
    Ok(views.enterBankDetailsPage(form    = maybePrePoppedForm, backUrl = BankDetailsController.paymentScheduleUrl))
  }

  val enterBankDetailsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    BankDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(views.enterBankDetailsPage(formWithErrors))),
        (bankDetailsForm: BankDetailsForm) => {

          val directDebitDetails: DirectDebitDetails = DirectDebitDetails(
            BankDetails(
              name          = bankDetailsForm.name,
              sortCode      = bankDetailsForm.sortCode,
              accountNumber = bankDetailsForm.accountNumber
            ),
            bankDetailsForm.isSoleSignatory.asBoolean
          )

          journeyService.updateDirectDebitDetails(request.journeyId, directDebitDetails)
            .map { _ =>
              bankDetailsForm.isSoleSignatory match {
                case IsSoleSignatoryFormValue.Yes => Redirect(routes.BankDetailsController.checkBankDetails())
                case IsSoleSignatoryFormValue.No  => Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage())
              }
            }
        }
      )
  }

  val checkBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterEnteredDirectDebitDetails =>
        if (j.directDebitDetails.isAccountHolder) {
          Ok(views.bankDetailsSummary(j.directDebitDetails, BankDetailsController.enterBankDetailsUrl))
        } else {
          Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage().url)
        }
    }
  }

  val checkBankDetailsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j)
      case j: Journey.AfterEnteredDirectDebitDetails =>
        if (j.directDebitDetails.isAccountHolder) {
          journeyService.updateHasConfirmedDirectDebitDetails(j.journeyId)
            .map(_ => Redirect(routes.BankDetailsController.termsAndConditions()))
        } else {
          Future.successful(Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage().url))
        }
    }
  }

  val termsAndConditions: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    Ok(views.termsAndConditions())
  }

  val termsAndConditionsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    journeyService.updateAgreedTermsAndConditions(request.journeyId)
      .map(_ => Redirect(routes.ConfirmationController.confirmation()))
  }

  val cannotSetupDirectDebitOnlinePage: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    Ok(s"This is where the not eligible to setup a dd page will go, for now, here's the journey data:\n\n\n" +
      Json.prettyPrint(Json.toJson(request.journey)))
  }
}

object BankDetailsController {
  val paymentScheduleUrl: Option[String] = Some(routes.PaymentScheduleController.checkPaymentSchedule().url)
  val enterBankDetailsUrl: Option[String] = Some(routes.BankDetailsController.enterBankDetails().url)
}
