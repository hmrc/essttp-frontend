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
import actionsmodel.EligibleJourneyRequest
import cats.implicits.catsSyntaxEq
import config.AppConfig
import controllers.EmailController.{ChooseEmailForm, chooseEmailForm}
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.{logErrorAndRouteToDefaultPage, logErrorAndRouteToDefaultPageF}
import essttp.emailverification.EmailVerificationStatus
import essttp.journey.model.Journey
import essttp.rootmodel.Email
import essttp.utils.Errors
import io.scalaland.chimney.dsl.TransformerOps
import models.emailverification.RequestEmailVerificationResponse
import play.api.data.Form
import play.api.mvc._
import services.{EmailVerificationService, JourneyService}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailController @Inject() (
    as:                       Actions,
    mcc:                      MessagesControllerComponents,
    views:                    Views,
    emailVerificationService: EmailVerificationService,
    journeyService:           JourneyService,
    appConfig:                AppConfig
)(implicit execution: ExecutionContext) extends FrontendController(mcc) with Logging {

  //todo get email from eligibilityCheckResponse by pattern matching, pass into chooseEmailPage -- it's not available in eligibilityCheckResponse yet
  val temporaryStaticEmail: SensitiveString = SensitiveString("bobross@joyOfPainting.com")

  private def withEmailEnabled(action: Action[AnyContent]): Action[AnyContent] =
    if (!appConfig.emailJourneyEnabled) {
      as.default{ _ => NotImplemented }
    } else {
      action
    }

  val whichEmailDoYouWantToUse: Action[AnyContent] =
    withEmailEnabled {
      as.eligibleJourneyAction { implicit request =>
        request.journey match {
          case j: Journey.BeforeAgreedTermsAndConditions => logErrorAndRouteToDefaultPage(j)
          case j: Journey.AfterAgreedTermsAndConditions =>
            if (!j.isEmailAddressRequired) {
              logErrorAndRouteToDefaultPage(j)
            } else {
              finalStateCheck(j, displayWhichEmailDoYouWantToUse(j))
            }
        }
      }
    }

  private def displayWhichEmailDoYouWantToUse(journey: Journey.AfterAgreedTermsAndConditions)(implicit request: Request[_]): Result = {
    val maybePrePopForm: Form[ChooseEmailForm] = journey match {
      case _: Journey.BeforeEmailAddressSelectedToBeVerified => chooseEmailForm()
      case j: Journey.AfterEmailAddressSelectedToBeVerified =>
        if (j.emailToBeVerified.value.decryptedValue === temporaryStaticEmail.decryptedValue) {
          chooseEmailForm().fill(ChooseEmailForm(j.emailToBeVerified.value.decryptedValue, None))
        } else {
          chooseEmailForm().fill(ChooseEmailForm(temporaryStaticEmail.decryptedValue, Some(j.emailToBeVerified.value.decryptedValue)))
        }
      case _: Journey.Stages.SubmittedArrangement =>
        Errors.throwServerErrorException("Can't render form for page when submission is submitted, this should never happen")
    }
    Ok(views.chooseEmailPage(temporaryStaticEmail.decryptedValue, maybePrePopForm))
  }

  val whichEmailDoYouWantToUseSubmit: Action[AnyContent] =
    withEmailEnabled {
      as.eligibleJourneyAction.async { implicit request =>
        EmailController.chooseEmailForm()
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(
              Ok(views.chooseEmailPage(temporaryStaticEmail.decryptedValue, form = formWithErrors))
            ),
            (form: ChooseEmailForm) => {
              val emailAddress: Email = form.differentEmail match {
                case Some(email) => Email(SensitiveString(email))
                case None        => Email(temporaryStaticEmail)
              }
              journeyService
                .updateSelectedEmailToBeVerified(
                  journeyId = request.journeyId,
                  email     = emailAddress
                )
                .map(updatedJourney => Redirect(Routing.next(updatedJourney)))
            }
          )
      }
    }

  val requestVerification: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      request.journey match {
        case j: Journey.BeforeEmailAddressSelectedToBeVerified =>
          logErrorAndRouteToDefaultPageF(j)

        case j: Journey.AfterArrangementSubmitted =>
          logErrorAndRouteToDefaultPageF(j)

        case j: Journey.AfterEmailAddressSelectedToBeVerified =>
          emailVerificationService.requestEmailVerification(j.emailToBeVerified).map {
            case RequestEmailVerificationResponse.Success(redirectUri) =>
              logger.info(s"Email verification journey successfully started. Redirecting to $redirectUri")
              Redirect(redirectUri)
            case RequestEmailVerificationResponse.LockedOut =>
              Redirect(routes.EmailController.tooManyEmailAddresses)
          }
      }

    }
  }

  val emailCallback: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      request.journey match {
        case j: Journey.BeforeEmailAddressSelectedToBeVerified =>
          logErrorAndRouteToDefaultPageF(j)

        case j: Journey.AfterArrangementSubmitted =>
          logErrorAndRouteToDefaultPageF(j)

        case j: Journey.AfterEmailAddressSelectedToBeVerified =>
          for {
            status <- emailVerificationService.getVerificationStatus(j.emailToBeVerified)
            updatedJourney <- journeyService.updateEmailVerificationStatus(j.journeyId, status)
          } yield Redirect(Routing.next(updatedJourney, allowSubmitArrangement = false))
      }
    }
  }

  val tooManyEmailAddresses: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      Ok(views.tooManyEmails())
    }
  }

  val tooManyPasscodeAttempts: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      Ok(views.tooManyPasscodes())
    }
  }

  val emailAddressConfirmed: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      withEmailAddressVerified(journey =>
        Ok(views.emailAddressConfirmed(journey.into[Journey.AfterEmailAddressSelectedToBeVerified].transform.emailToBeVerified)))
    }
  }

  val emailAddressConfirmedSubmit: Action[AnyContent] = withEmailEnabled{
    as.eligibleJourneyAction { implicit request =>
      withEmailAddressVerified(_ => Redirect(routes.SubmitArrangementController.submitArrangement))
    }
  }

  private def withEmailAddressVerified(f: Journey.AfterEmailAddressVerificationResult => Result)(implicit request: EligibleJourneyRequest[_]): Result =
    request.journey match {
      case j: Journey.BeforeEmailAddressVerificationResult => logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterArrangementSubmitted =>
        logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterEmailAddressVerificationResult =>
        j.emailVerificationStatus match {
          case EmailVerificationStatus.Verified =>
            f(j)

          case EmailVerificationStatus.Locked =>
            logErrorAndRouteToDefaultPage(j)
        }
    }

}

object EmailController {
  import play.api.data.Forms.{mapping, nonEmptyText}
  import play.api.data.validation.{Constraint, Invalid, Valid}
  import play.api.data.{Form, Mapping}
  import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

  final case class ChooseEmailForm(email: String, differentEmail: Option[String])

  def chooseEmailForm(): Form[ChooseEmailForm] = Form(
    mapping(
      "selectAnEmailToUseRadio" -> nonEmptyText,
      "newEmailInput" -> mandatoryIfEqual("selectAnEmailToUseRadio", "new", differentEmailAddressMapping)
    )(ChooseEmailForm.apply)(ChooseEmailForm.unapply)
  )

  val differentEmailAddressMapping: Mapping[String] = nonEmptyText
    .transform[String](email => email.toLowerCase(Locale.UK), _.toLowerCase(Locale.UK))
    .verifying(
      Constraint[String]((email: String) =>
        if (email.length > 256) Invalid("error.tooManyChar")
        else if (EmailAddress.isValid(email)) Valid
        else Invalid("error.invalidFormat"))
    )

}
