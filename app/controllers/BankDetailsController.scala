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
import actionsmodel.AuthenticatedJourneyRequest
import cats.syntax.eq._
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.model.Journey
import essttp.journey.model.Journey.{AfterChosenTypeOfBankAccount, BeforeChosenTypeOfBankAccount}
import essttp.rootmodel.bank.{BankDetails, DirectDebitDetails}
import models.bars.response._
import models.enumsforforms.{IsSoleSignatoryFormValue, TypeOfAccountFormValue}
import models.forms.{BankDetailsForm, TypeOfAccountForm}
import play.api.data.{Form, FormError}
import play.api.mvc._
import requests.RequestSupport
import services.{EssttpBarsService, JourneyService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import util.QueryParameterUtils.InstantOps
import views.Views

import java.nio.charset.Charset
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
)(
    implicit
    executionContext: ExecutionContext
) extends FrontendController(mcc) with Logging {

  import requestSupport._

  val typeOfAccount: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeCheckedPaymentPlan => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterCheckedPaymentPlan  => finalStateCheck(j, displayTypeOfBankAccountPage(j))
    }
  }

  private def displayTypeOfBankAccountPage(journey: Journey.AfterCheckedPaymentPlan)(
      implicit
      request: Request[_]
  ): Result = {
    val maybePrePoppedForm: Form[TypeOfAccountForm] = journey match {
      case _: Journey.BeforeChosenTypeOfBankAccount => TypeOfAccountForm.form
      case j: Journey.AfterChosenTypeOfBankAccount =>
        TypeOfAccountForm.form.fill(
          TypeOfAccountForm(TypeOfAccountFormValue.typeOfBankAccountAsFormValue(j.typeOfBankAccount))
        )
    }
    Ok(views.chooseTypeOfAccountPage(form    = maybePrePoppedForm, backUrl = BankDetailsController.paymentScheduleUrl))
  }

  val typeOfAccountSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    TypeOfAccountForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            Ok(views.chooseTypeOfAccountPage(formWithErrors, BankDetailsController.paymentScheduleUrl))
          ),
        (typeOfBankAccountForm: TypeOfAccountForm) =>
          journeyService
            .updateChosenTypeOfBankAccount(
              journeyId         = request.journeyId,
              typeOfBankAccount =
                TypeOfAccountFormValue.typeOfBankAccountFromFormValue(typeOfBankAccountForm.typeOfAccount)
            )
            .map(_ => Redirect(routes.BankDetailsController.enterBankDetails()))
      )
  }

  val enterBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeChosenTypeOfBankAccount => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterChosenTypeOfBankAccount  => finalStateCheck(j, displayEnterBankDetailsPage(j))
    }
  }

  private def displayEnterBankDetailsPage(journey: Journey.AfterChosenTypeOfBankAccount)(
      implicit
      request: Request[_]
  ): Result = {
    val maybePrePoppedForm: Form[BankDetailsForm] = currentDirectDebitDetails(journey)
      .fold(BankDetailsForm.form) { directDebitDetails =>
        BankDetailsForm.form.fill(
          BankDetailsForm(
            name            = directDebitDetails.bankDetails.name,
            sortCode        = directDebitDetails.bankDetails.sortCode,
            accountNumber   = directDebitDetails.bankDetails.accountNumber,
            isSoleSignatory =
              IsSoleSignatoryFormValue.booleanToIsSoleSignatoryFormValue(directDebitDetails.isAccountHolder)
          )
        )
      }
    Ok(views.enterBankDetailsPage(form    = maybePrePoppedForm, backUrl = BankDetailsController.chooseTypeOfAccountUrl))
  }

  private def currentDirectDebitDetails(journey: Journey): Option[DirectDebitDetails] =
    journey match {
      case _: Journey.BeforeEnteredDirectDebitDetails => None
      case j: Journey.AfterEnteredDirectDebitDetails  => Some(j.directDebitDetails)
    }

  val enterBankDetailsSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    val formFromRequest = BankDetailsForm.form.bindFromRequest()
    formFromRequest.fold(
      formWithErrors =>
        Future.successful(
          Ok(views.enterBankDetailsPage(formWithErrors, BankDetailsController.chooseTypeOfAccountUrl))
        ),
      (bankDetailsForm: BankDetailsForm) => {

        val directDebitDetails: DirectDebitDetails = DirectDebitDetails(
          BankDetails(
            name          = bankDetailsForm.name,
            sortCode      = bankDetailsForm.sortCode,
            accountNumber = bankDetailsForm.accountNumber
          ),
          bankDetailsForm.isSoleSignatory.asBoolean
        )

        request.journey match {
          case j: BeforeChosenTypeOfBankAccount =>
            Future.successful(JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j))
          case j: AfterChosenTypeOfBankAccount =>
            bankDetailsForm.isSoleSignatory match {
              case IsSoleSignatoryFormValue.No =>
                journeyService.updateDirectDebitDetails(request.journeyId, directDebitDetails).map { _ =>
                  Redirect(routes.BankDetailsController.cannotSetupDirectDebitOnlinePage)
                }
              case IsSoleSignatoryFormValue.Yes =>
                currentDirectDebitDetails(request.journey) match {
                  case Some(current) if directDebitDetails === current =>
                    // nothing changed so don't call BARs and don't update journey
                    Future.successful(Redirect(routes.BankDetailsController.checkBankDetails))
                  case _ =>
                    barsService
                      .verifyBankDetails(directDebitDetails.bankDetails, j.typeOfBankAccount, j)
                      .flatMap { barsResponse =>
                        handleBars(barsResponse, directDebitDetails, formFromRequest)
                      }
                }
            }
        }
      }
    )
  }

  private def handleBars(
      resp:               Either[BarsError, VerifyResponse],
      directDebitDetails: DirectDebitDetails,
      form:               Form[BankDetailsForm]
  )(
      implicit
      request: AuthenticatedJourneyRequest[_]
  ): Future[Result] = {
      def enterBankDetailsPageWithBarsError(error: FormError): Future[Result] =
        Future.successful(
          Ok(
            views.enterBankDetailsPage(
              form    = form.withError(error),
              backUrl = BankDetailsController.chooseTypeOfAccountUrl
            )
          )
        )

    import models.forms.BankDetailsForm._
    resp.fold(
      {
        case ThirdPartyError(_) =>
          Future.successful(Redirect(routes.BankDetailsController.barsErrorPlaceholder))
        case AccountNumberNotWellFormatted(_) =>
          enterBankDetailsPageWithBarsError(accountNumberNotWellFormatted)
        case SortCodeDoesNotSupportDirectDebit(_) =>
          enterBankDetailsPageWithBarsError(sortCodeDoesNotSupportsDirectDebit)
        case SortCodeNotPresentOnEiscd(_) =>
          enterBankDetailsPageWithBarsError(sortCodeNotPresentOnEiscd)
        case NameDoesNotMatch(_) =>
          enterBankDetailsPageWithBarsError(nameDoesNotMatch)
        case AccountDoesNotExist(_) =>
          enterBankDetailsPageWithBarsError(accountDoesNotExist)
        case SortCodeOnDenyListError(_) =>
          enterBankDetailsPageWithBarsError(sortCodeOnDenyList)
        case OtherBarsError(_) =>
          enterBankDetailsPageWithBarsError(otherBarsError)
        case TooManyAttempts(_, expiry) =>
          Future.successful(Redirect(routes.BankDetailsController.barsLockout(expiry.encodedLongFormat)))
      },
      _ =>
        journeyService
          .updateDirectDebitDetails(request.journeyId, directDebitDetails)
          .map { _ =>
            Redirect(routes.BankDetailsController.checkBankDetails)
          }
    )
  }

  val checkBankDetails: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEnteredDirectDebitDetails => JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)
      case j: Journey.AfterEnteredDirectDebitDetails =>
        if (j.directDebitDetails.isAccountHolder) {
          finalStateCheck(
            j,
            Ok(views.bankDetailsSummary(j.directDebitDetails, BankDetailsController.enterBankDetailsUrl))
          )
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
          journeyService
            .updateHasConfirmedDirectDebitDetails(j.journeyId)
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
        journeyService
          .updateAgreedTermsAndConditions(request.journeyId)
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

  // TODO in a future ticket
  val barsErrorPlaceholder: Action[AnyContent] = as.default { implicit request =>
    Ok(views.barsErrorPlaceHolder())
  }

  def barsLockout(p: String): Action[AnyContent] = as.default { implicit request =>
    val charset = Charset.forName("UTF-8")
    val expiry = new String(java.util.Base64.getDecoder.decode(p.getBytes(charset)), charset)
    Ok(views.barsLockout(expiry))
  }
}

object BankDetailsController {
  val paymentScheduleUrl: Option[String] = Some(routes.PaymentScheduleController.checkPaymentSchedule.url)
  val chooseTypeOfAccountUrl: Option[String] = Some(routes.BankDetailsController.typeOfAccount.url)
  val enterBankDetailsUrl: Option[String] = Some(routes.BankDetailsController.enterBankDetails.url)
}
