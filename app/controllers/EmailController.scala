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
import actionsmodel.EligibleJourneyRequest
import cats.implicits.catsSyntaxEq
import config.AppConfig
import controllers.EmailController.{ChooseEmailForm, chooseEmailForm, enterEmailForm}
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.{logErrorAndRouteToDefaultPage, logErrorAndRouteToDefaultPageF}
import essttp.emailverification.{EmailVerificationResult, EmailVerificationState, StartEmailVerificationJourneyResponse}
import essttp.journey.model.Journey.AfterEmailAddressSelectedToBeVerified
import essttp.journey.model.{EmailVerificationAnswers, Journey}
import essttp.rootmodel.Email
import essttp.utils.Errors
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

  private def withEmailEnabled(action: Action[AnyContent]): Action[AnyContent] =
    if (!appConfig.emailJourneyEnabled) {
      as.default { _ => NotImplemented }
    } else {
      action
    }

  val whichEmailDoYouWantToUse: Action[AnyContent] =
    withEmailEnabled {
      as.eligibleJourneyAction { implicit request =>
        withEmailAddressFromEligibilityResponse { emailFromEligibilityResponse =>
          request.journey match {
            case j: Journey.BeforeAgreedTermsAndConditions => logErrorAndRouteToDefaultPage(j)
            case j: Journey.AfterAgreedTermsAndConditions =>
              if (!j.isEmailAddressRequired) {
                logErrorAndRouteToDefaultPage(j)
              } else {
                finalStateCheck(j, displayWhichEmailDoYouWantToUse(j, emailFromEligibilityResponse))
              }
          }
        }
      }
    }

  private def displayWhichEmailDoYouWantToUse(
      journey:                      Journey.AfterAgreedTermsAndConditions,
      emailFromEligibilityResponse: Email
  )(implicit request: Request[_]): Result = {
    val maybePrePopForm: Form[ChooseEmailForm] = {
      existingEmailToBeVerified(journey).fold(chooseEmailForm()){ existingEmail =>
        if (existingEmail === emailFromEligibilityResponse) {
          chooseEmailForm().fill(ChooseEmailForm(existingEmail.value.decryptedValue, None))
        } else {
          chooseEmailForm().fill(ChooseEmailForm(emailFromEligibilityResponse.value.decryptedValue, Some(existingEmail.value.decryptedValue)))
        }
      }
    }
    Ok(views.chooseEmailPage(emailFromEligibilityResponse.value.decryptedValue, maybePrePopForm))
  }

  private def existingEmailToBeVerified(journey: Journey): Option[Email] = journey match {
    case _: Journey.BeforeEmailAddressSelectedToBeVerified => None
    case j: Journey.AfterEmailAddressSelectedToBeVerified  => Some(j.emailToBeVerified)
    case _: Journey.Stages.SubmittedArrangement =>
      Errors.throwServerErrorException("Shouldn't be trying to find email address in session when submission is submitted")
  }

  def checkIfAlreadyVerified(journey: Journey, emailSubmitted: Email): Option[EmailVerificationResult] = journey match {
    case j: Journey.AfterEmailVerificationPhase => j.emailVerificationAnswers match {
      case EmailVerificationAnswers.NoEmailJourney =>
        Errors.throwServerErrorException("We should not be submitting when there is no email journey.")
      case EmailVerificationAnswers.EmailVerified(email, emailVerificationResult) =>
        if (email === emailSubmitted) Some(emailVerificationResult)
        else None
    }
    case _: Journey.BeforeEmailAddressVerificationResult => None
  }

  val whichEmailDoYouWantToUseSubmit: Action[AnyContent] =
    withEmailEnabled {
      as.eligibleJourneyAction.async { implicit request =>
        withEmailAddressFromEligibilityResponse { emailFromEligibilityResponse =>
          EmailController.chooseEmailForm()
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(
                Ok(views.chooseEmailPage(emailFromEligibilityResponse.value.decryptedValue, formWithErrors))
              ),
              (form: ChooseEmailForm) => {
                val emailAddress: Email = form.differentEmail match {
                  case Some(email) => Email(SensitiveString(email))
                  case None        => Email(emailFromEligibilityResponse.value)
                }
                checkIfAlreadyVerified(request.journey, emailAddress).fold {
                  journeyService.updateSelectedEmailToBeVerified(
                    journeyId = request.journeyId,
                    email     = emailAddress
                  ).map(updatedJourney =>
                    Routing.redirectToNext(
                      routes.EmailController.whichEmailDoYouWantToUse,
                      updatedJourney,
                      existingEmailToBeVerified(request.journey).contains(emailAddress)
                    ))
                } {
                  case EmailVerificationResult.Verified => Future.successful(Redirect(routes.EmailController.emailAddressConfirmed))
                  case EmailVerificationResult.Locked   => Future.successful(Routing.redirectToNext(routes.EmailController.emailCallback, request.journey, false))
                }

              }
            )
        }
      }
    }

  val enterEmail: Action[AnyContent] = {
      def displayEnterEmailPage(journey: Journey.AfterAgreedTermsAndConditions)(implicit request: Request[_]): Result = {
        val form: Form[Email] = journey match {
          case _: Journey.BeforeEmailAddressSelectedToBeVerified => enterEmailForm
          case j: Journey.AfterEmailAddressSelectedToBeVerified  => enterEmailForm.fill(j.emailToBeVerified)
          case _: Journey.Stages.SubmittedArrangement =>
            Errors.throwServerErrorException("Can't render form for page when submission is submitted, this should never happen")
        }

        Ok(views.enterEmailPage(form))
      }

    withEmailEnabled {
      as.eligibleJourneyAction { implicit request =>
        request.journey match {
          case j: Journey.BeforeAgreedTermsAndConditions => logErrorAndRouteToDefaultPage(j)
          case j: Journey.AfterAgreedTermsAndConditions =>
            if (!j.isEmailAddressRequired) {
              logErrorAndRouteToDefaultPage(j)
            } else {
              finalStateCheck(j, displayEnterEmailPage(j))
            }
        }
      }
    }
  }

  val enterEmailSubmit: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      EmailController.enterEmailForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(views.enterEmailPage(formWithErrors))),
          email => {
            checkIfAlreadyVerified(request.journey, email).fold {
              journeyService.updateSelectedEmailToBeVerified(
                journeyId = request.journeyId,
                email     = email
              ).map(updatedJourney =>
                Routing.redirectToNext(
                  routes.EmailController.enterEmail,
                  updatedJourney,
                  existingEmailToBeVerified(request.journey).contains(email)
                ))
            } {
              case EmailVerificationResult.Verified => Future.successful(Redirect(routes.EmailController.emailAddressConfirmed))
              case EmailVerificationResult.Locked   => Future.successful(Routing.redirectToNext(routes.EmailController.emailCallback, request.journey, false))
            }
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
            case StartEmailVerificationJourneyResponse.Success(redirectUri) =>
              logger.info(s"Email verification journey successfully started. Redirecting to $redirectUri")
              Redirect(redirectUri)
            case StartEmailVerificationJourneyResponse.AlreadyVerified => Redirect(routes.EmailController.emailAddressConfirmed)
            case StartEmailVerificationJourneyResponse.Error(reason) => reason match {
              case EmailVerificationState.TooManyPasscodeAttempts        => Redirect(routes.EmailController.tooManyPasscodeAttempts)
              case EmailVerificationState.TooManyPasscodeJourneysStarted => Redirect(routes.EmailController.tooManyPasscodeJourneysStarted)
              case EmailVerificationState.TooManyDifferentEmailAddresses => Redirect(routes.EmailController.tooManyDifferentEmailAddresses)
              case EmailVerificationState.AlreadyVerified =>
                Errors.throwServerErrorException("Fallen into case wiith AlreadyVerified, this should never happen.")
            }
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
            result <- emailVerificationService.getEmailVerificationResult(j.emailToBeVerified)
            updatedJourney <- journeyService.updateEmailVerificationResult(j.journeyId, result)
          } yield Routing.redirectToNext(routes.EmailController.emailCallback, updatedJourney, submittedValueUnchanged = false)
      }
    }
  }

  val tooManyPasscodeAttempts: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      val emailEntryEndpoint = request.eligibilityCheckResult.email.fold(
        routes.EmailController.enterEmail
      )(_ =>
          routes.EmailController.whichEmailDoYouWantToUse)

      Ok(views.tooManyPasscodes(emailEntryEndpoint))
    }
  }

  val tooManyPasscodeJourneysStarted: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      Ok(views.tooManyPasscodeJourneysStarted())
    }
  }

  val tooManyDifferentEmailAddresses: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      emailVerificationService.getLockoutCreatedAt()
        .map(dateTime => Ok(views.tooManyEmails(dateTime)))
    }
  }

  val emailAddressConfirmed: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      withEmailAddressVerified { journey =>
        val email = journey match {
          case j: AfterEmailAddressSelectedToBeVerified => j.emailToBeVerified
        }
        Ok(views.emailAddressConfirmed(email))
      }
    }
  }

  val emailAddressConfirmedSubmit: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      withEmailAddressVerified(_ =>
        Routing.redirectToNext(routes.EmailController.emailAddressConfirmed, request.journey, submittedValueUnchanged = false))
    }
  }

  private def withEmailAddressVerified(f: Journey.AfterEmailAddressVerificationResult => Result)(implicit request: EligibleJourneyRequest[_]): Result =
    request.journey match {
      case j: Journey.BeforeEmailAddressVerificationResult => logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterArrangementSubmitted =>
        logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterEmailAddressVerificationResult =>
        j.emailVerificationResult match {
          case EmailVerificationResult.Verified =>
            f(j)

          case EmailVerificationResult.Locked =>
            logErrorAndRouteToDefaultPage(j)
        }
    }

  private def withEmailAddressFromEligibilityResponse(f: Email => Result)(implicit r: EligibleJourneyRequest[_]): Result =
    f(emailFromEligibilityResponse(r))

  private def withEmailAddressFromEligibilityResponse(f: Email => Future[Result])(implicit r: EligibleJourneyRequest[_]): Future[Result] =
    f(emailFromEligibilityResponse(r))

  private def emailFromEligibilityResponse(r: EligibleJourneyRequest[_]): Email =
    r.eligibilityCheckResult.email.getOrElse(Errors.throwServerErrorException("Could not find email address in eligibility response"))

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

  val enterEmailForm: Form[Email] = Form(
    mapping(
      "newEmailInput" -> differentEmailAddressMapping
    )(s => Email.apply(SensitiveString(s)))(Email.unapply(_).map(_.decryptedValue))
  )

}
