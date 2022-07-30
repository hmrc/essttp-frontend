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
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.model.Journey
import essttp.rootmodel.bank.{BankDetails, DirectDebitDetails}
import messages.Messages.BankDetails.{accountNumberIsWellFormattedNoError, sortCodeIsPresentOnEiscdNoError, sortCodeSupportsDirectDebitNoError}
import models.bars.BarsModel.BarsResponse._
import models.enumsforforms.{IsSoleSignatoryFormValue, TypeOfAccountFormValue}
import models.forms.{BankDetailsForm, TypeOfAccountForm}
import play.api.data.{Form, FormError}
import play.api.mvc._
import requests.RequestSupport
import services.{BarsService, JourneyService}
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
    journeyService: JourneyService,
    barsService:    BarsService
)(
    implicit
    executionContext: ExecutionContext
) extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val typeOfAccount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeCheckedPaymentPlan => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterCheckedPaymentPlan  => finalStateCheck(j, displayTypeOfBankAccountPage(j))
    }
  }

  private def displayTypeOfBankAccountPage(journey: Journey.AfterCheckedPaymentPlan)(implicit request: Request[_]): Result = {
    val maybePrePoppedForm: Form[TypeOfAccountForm] = journey match {
      case _: Journey.BeforeChosenTypeOfBankAccount => TypeOfAccountForm.form
      case j: Journey.AfterChosenTypeOfBankAccount =>
        TypeOfAccountForm.form.fill(TypeOfAccountForm(TypeOfAccountFormValue.typeOfBankAccountAsFormValue(j.typeOfBankAccount)))
    }
    Ok(views.chooseTypeOfAccountPage(form    = maybePrePoppedForm, backUrl = BankDetailsController.paymentScheduleUrl))
  }

  val typeOfAccountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    TypeOfAccountForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(views.chooseTypeOfAccountPage(formWithErrors, BankDetailsController.paymentScheduleUrl))),
        (typeOfBankAccountForm: TypeOfAccountForm) =>
          journeyService.updateChosenTypeOfBankAccount(
            journeyId         = request.journeyId,
            typeOfBankAccount = TypeOfAccountFormValue.typeOfBankAccountFromFormValue(typeOfBankAccountForm.typeOfAccount)
          ).map(_ => Redirect(routes.BankDetailsController.enterBankDetails()))
      )
  }

  val enterBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeChosenTypeOfBankAccount => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterChosenTypeOfBankAccount  => finalStateCheck(j, displayEnterBankDetailsPage(j))
    }
  }

  private def displayEnterBankDetailsPage(journey: Journey.AfterChosenTypeOfBankAccount)(implicit request: Request[_]): Result = {
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
    Ok(views.enterBankDetailsPage(form    = maybePrePoppedForm, backUrl = BankDetailsController.chooseTypeOfAccountUrl))
  }

  val enterBankDetailsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val validForm = BankDetailsForm.form.bindFromRequest()
    validForm.fold(
      hasErrors = (formWithErrors: Form[BankDetailsForm]) =>
        Future.successful(Ok(views.enterBankDetailsPage(formWithErrors, BankDetailsController.chooseTypeOfAccountUrl))),

      success = (bankDetailsForm: BankDetailsForm) => {

        val directDebitDetails: DirectDebitDetails = DirectDebitDetails(
          BankDetails(
            name          = bankDetailsForm.name,
            sortCode      = bankDetailsForm.sortCode,
            accountNumber = bankDetailsForm.accountNumber
          ),
          bankDetailsForm.isSoleSignatory.asBoolean
        )

          def enterBankDetailsPageWithBarsError(error: FormError): Result = {
            Ok(views.enterBankDetailsPage(
              validForm.withError(error),
              BankDetailsController.chooseTypeOfAccountUrl
            ))
          }

        journeyService.updateDirectDebitDetails(request.journeyId, directDebitDetails)
          .flatMap { _ =>
            bankDetailsForm.isSoleSignatory match {
              case IsSoleSignatoryFormValue.No =>
                Future.successful(Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage))

              case IsSoleSignatoryFormValue.Yes =>
                barsService.assessBankAccountReputation(directDebitDetails.bankDetails).map {
                  case sortCodeIsPresentOnEiscdError()  => Ok(views.errorPlaceholder()) // TODO Error page?
                  case accountNumberIsWellFormattedNo() => enterBankDetailsPageWithBarsError(accountNumberIsWellFormattedNoError)
                  case sortCodeSupportsDirectDebitNo()  => enterBankDetailsPageWithBarsError(sortCodeSupportsDirectDebitNoError)
                  case sortCodeIsPresentOnEiscdNo()     => enterBankDetailsPageWithBarsError(sortCodeIsPresentOnEiscdNoError)
                  // BARs check was successful
                  case _                                => Redirect(routes.BankDetailsController.checkBankDetails())
                }
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
          finalStateCheck(j, Ok(views.bankDetailsSummary(j.directDebitDetails, BankDetailsController.enterBankDetailsUrl)))
        } else {
          Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage.url)
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
          Future.successful(Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage.url))
        }
    }
  }

  val termsAndConditions: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeConfirmedDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterConfirmedDirectDebitDetails  => finalStateCheck(j, Ok(views.termsAndConditions()))
    }
  }

  val termsAndConditionsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: Journey.BeforeConfirmedDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j)
      case _: Journey.AfterConfirmedDirectDebitDetails =>
        journeyService.updateAgreedTermsAndConditions(request.journeyId)
          .map(_ => Redirect(routes.SubmitArrangementController.submitArrangement))
    }
  }

  val cannotSetupDirectDebitOnlinePage: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterEnteredDirectDebitDetails =>
        //only show this page if user has said they are not the account holder
        if (!j.directDebitDetails.isAccountHolder) {
          Ok(views.cannotSetupDirectDebitPage())
        } else {
          JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
        }
    }

  }
}

object BankDetailsController {
  val paymentScheduleUrl: Option[String] = Some(routes.PaymentScheduleController.checkPaymentSchedule.url)
  val chooseTypeOfAccountUrl: Option[String] = Some(routes.BankDetailsController.typeOfAccount.url)
  val enterBankDetailsUrl: Option[String] = Some(routes.BankDetailsController.enterBankDetails.url)
}
