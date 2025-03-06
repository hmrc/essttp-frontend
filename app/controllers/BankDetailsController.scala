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
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.model.{Journey, JourneyStage}
import essttp.rootmodel.bank.{BankDetails, CanSetUpDirectDebit}
import models.bars.response.*
import models.enumsforforms.IsSoleSignatoryFormValue
import models.enumsforforms.TypeOfAccountFormValue.{typeOfBankAccountAsFormValue, typeOfBankAccountFromFormValue}
import models.forms.helper.FormErrorWithFieldMessageOverrides
import models.forms.{BankDetailsForm, CanSetUpDirectDebitForm}
import play.api.data.Form
import play.api.mvc.*
import requests.RequestSupport
import services.{EssttpBarsService, JourneyService}
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
  barsService:    EssttpBarsService
)(using ExecutionContext, AppConfig)
    extends FrontendController(mcc),
      Logging {

  import requestSupport.languageFromRequest

  val detailsAboutBankAccount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeCheckedPaymentPlan => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterCheckedPaymentPlan  => finalStateCheck(j, displayDetailsAboutBankAccountPage(j))
    }
  }

  private def displayDetailsAboutBankAccountPage(
    journey: JourneyStage.AfterCheckedPaymentPlan & Journey
  )(using Request[?]): Result = {
    val maybePrePoppedForm: Form[CanSetUpDirectDebitForm] =
      existingDetailsFromBankAccount(journey).fold(CanSetUpDirectDebitForm.form)(d =>
        CanSetUpDirectDebitForm.form.fill(
          CanSetUpDirectDebitForm(
            IsSoleSignatoryFormValue.booleanToIsSoleSignatoryFormValue(d.isAccountHolder)
          )
        )
      )

    Ok(views.checkYouCanSetUpDDPage(maybePrePoppedForm))
  }

  private def existingDetailsFromBankAccount(journey: Journey): Option[CanSetUpDirectDebit] =
    journey match {
      case _: JourneyStage.BeforeEnteredCanYouSetUpDirectDebit =>
        None
      case j: JourneyStage.AfterEnteredCanYouSetUpDirectDebit  =>
        Some(CanSetUpDirectDebit(j.canSetUpDirectDebitAnswer.isAccountHolder))
    }

  val detailsAboutBankAccountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    CanSetUpDirectDebitForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(views.checkYouCanSetUpDDPage(formWithErrors)),
        (detailsAboutBankAccountForm: CanSetUpDirectDebitForm) =>
          val newDetailsAboutBankAccount = CanSetUpDirectDebit(
            detailsAboutBankAccountForm.isSoleSignatory.asBoolean
          )
          journeyService
            .updateCanSetUpDirectDebit(request.journeyId, newDetailsAboutBankAccount)
            .map { updatedJourney =>
              Routing.redirectToNext(
                routes.BankDetailsController.detailsAboutBankAccount,
                updatedJourney,
                existingDetailsFromBankAccount(request.journey).contains(newDetailsAboutBankAccount)
              )
            }
      )
  }

  val enterBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEnteredCanYouSetUpDirectDebit =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterEnteredCanYouSetUpDirectDebit  =>
        if (!j.canSetUpDirectDebitAnswer.isAccountHolder) {
          JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
        } else {
          finalStateCheck(j, displayEnterBankDetailsPage(j))
        }
    }
  }

  private def displayEnterBankDetailsPage(
    journey: JourneyStage.AfterEnteredCanYouSetUpDirectDebit & Journey
  )(using Request[?]): Result = {
    val maybePrePoppedForm: Form[BankDetailsForm] = currentDirectDebitDetails(journey)
      .fold(BankDetailsForm.form) { directDebitDetails =>
        BankDetailsForm.form.fill(
          BankDetailsForm(
            typeOfBankAccount = typeOfBankAccountAsFormValue(directDebitDetails.typeOfBankAccount),
            name = directDebitDetails.name,
            sortCode = directDebitDetails.sortCode,
            accountNumber = directDebitDetails.accountNumber
          )
        )
      }
    Ok(views.enterBankDetailsPage(form = maybePrePoppedForm))
  }

  private def currentDirectDebitDetails(journey: Journey): Option[BankDetails] =
    journey match {
      case _: JourneyStage.BeforeEnteredDirectDebitDetails => None
      case j: JourneyStage.AfterEnteredDirectDebitDetails  => Some(j.directDebitDetails)
    }

  val enterBankDetailsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEnteredCanYouSetUpDirectDebit =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)

      case j: JourneyStage.AfterEnteredCanYouSetUpDirectDebit =>
        if (!j.canSetUpDirectDebitAnswer.isAccountHolder) {
          JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
        } else {
          val formFromRequest = BankDetailsForm.form.bindFromRequest()
          formFromRequest.fold(
            formWithErrors => Ok(views.enterBankDetailsPage(formWithErrors)),
            (bankDetailsForm: BankDetailsForm) => {
              val directDebitDetails: BankDetails =
                BankDetails(
                  typeOfBankAccount = typeOfBankAccountFromFormValue(bankDetailsForm.typeOfBankAccount),
                  name = bankDetailsForm.name,
                  sortCode = bankDetailsForm.sortCode,
                  accountNumber = bankDetailsForm.accountNumber
                )

              currentDirectDebitDetails(request.journey) match {
                case Some(current) if directDebitDetails == current =>
                  // nothing changed so don't call BARs and don't update journey
                  Redirect(routes.BankDetailsController.checkBankDetails)
                case _                                              =>
                  barsService
                    .verifyBankDetails(directDebitDetails, j)
                    .flatMap { barsResponse =>
                      handleBars(barsResponse, directDebitDetails, formFromRequest)
                    }
              }
            }
          )
        }
    }

  }

  private def handleBars(
    resp:               Either[BarsError, VerifyResponse],
    directDebitDetails: BankDetails,
    form:               Form[BankDetailsForm]
  )(using request: AuthenticatedJourneyRequest[?]): Future[Result] = {
    def enterBankDetailsPageWithBarsError(error: FormErrorWithFieldMessageOverrides): Future[Result] =
      Ok(
        views.enterBankDetailsPage(
          form = form.withError(error.formError),
          errorMessageOverrides = error.fieldMessageOverrides
        )
      )

    import models.forms.BankDetailsForm._
    resp.fold(
      {
        case ThirdPartyError(resp)                                                                       =>
          throw new RuntimeException(s"BARS verify third-party error - response type ${resp.getClass.getSimpleName}")
        case AccountNumberNotWellFormatted(_) | AccountNumberNotWellFormattedValidateResponse(_)         =>
          enterBankDetailsPageWithBarsError(accountNumberNotWellFormatted)
        case SortCodeDoesNotSupportDirectDebit(_) | SortCodeDoesNotSupportDirectDebitValidateResponse(_) =>
          enterBankDetailsPageWithBarsError(sortCodeDoesNotSupportsDirectDebit)
        case SortCodeNotPresentOnEiscd(_) | SortCodeNotPresentOnEiscdValidateResponse(_)                 =>
          enterBankDetailsPageWithBarsError(sortCodeNotPresentOnEiscd)
        case NameDoesNotMatch(_)                                                                         =>
          enterBankDetailsPageWithBarsError(nameDoesNotMatch)
        case AccountDoesNotExist(_)                                                                      =>
          enterBankDetailsPageWithBarsError(accountDoesNotExist)
        case SortCodeOnDenyListErrorResponse(_)                                                          =>
          enterBankDetailsPageWithBarsError(sortCodeOnDenyList)
        case OtherBarsError(_)                                                                           =>
          enterBankDetailsPageWithBarsError(otherBarsError)
        case TooManyAttempts(_, _)                                                                       =>
          Redirect(routes.BankDetailsController.barsLockout)
      },
      _ =>
        journeyService
          .updateDirectDebitDetails(request.journeyId, directDebitDetails)
          .map { updatedJourney =>
            Routing.redirectToNext(
              routes.BankDetailsController.enterBankDetails,
              updatedJourney,
              currentDirectDebitDetails(request.journey).contains(directDebitDetails)
            )
          }
    )
  }

  val checkBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEnteredDirectDebitDetails =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterEnteredDirectDebitDetails  =>
        finalStateCheck(j, Ok(views.bankDetailsSummary(j.directDebitDetails)))
    }
  }

  val checkBankDetailsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEnteredDirectDebitDetails =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j)
      case j: JourneyStage.AfterEnteredDirectDebitDetails  =>
        journeyService
          .updateHasConfirmedDirectDebitDetails(j.journeyId)
          .map(updatedJourney =>
            Routing.redirectToNext(
              routes.BankDetailsController.checkBankDetails,
              updatedJourney,
              submittedValueUnchanged = false
            )
          )
    }
  }

  val cannotSetupDirectDebitOnlinePage: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEnteredCanYouSetUpDirectDebit =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterEnteredCanYouSetUpDirectDebit  =>
        // only show this page if user has said they are not the account holder
        if (!j.canSetUpDirectDebitAnswer.isAccountHolder) {
          Ok(views.cannotSetupDirectDebitPage(request.journey.taxRegime))
        } else {
          JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
        }
    }
  }

  val barsLockout: Action[AnyContent] = as.barsLockedOutJourneyAction { implicit request =>
    Ok(views.barsLockout(request.barsLockoutExpiryTime))
  }
}

object BankDetailsController {
  val paymentScheduleUrl: Option[String]         = Some(routes.PaymentScheduleController.checkPaymentSchedule.url)
  val detailsAboutBankAccountUrl: Option[String] = Some(routes.BankDetailsController.detailsAboutBankAccount.url)
  val enterBankDetailsUrl: Option[String]        = Some(routes.BankDetailsController.enterBankDetails.url)
}
