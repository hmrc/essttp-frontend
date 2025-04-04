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
import cats.implicits.catsSyntaxOptionId
import config.AppConfig
import controllers.EmailController.{ChooseEmailForm, chooseEmailForm, enterEmailForm}
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.{logErrorAndRouteToDefaultPage, logErrorAndRouteToDefaultPageF}
import essttp.journey.model.{Journey, JourneyStage}
import essttp.journey.model.JourneyStage.AfterEmailAddressSelectedToBeVerified
import essttp.rootmodel.Email
import essttp.utils.Errors
import paymentsEmailVerification.models.api.StartEmailVerificationJourneyResponse
import paymentsEmailVerification.models.{EmailVerificationResult, EmailVerificationState}
import play.api.data.Form
import play.api.mvc.*
import services.{EmailVerificationService, JourneyService}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class EmailController @Inject() (
  as:                       Actions,
  mcc:                      MessagesControllerComponents,
  views:                    Views,
  emailVerificationService: EmailVerificationService,
  journeyService:           JourneyService
)(using ec: ExecutionContext, appConfig: AppConfig)
    extends FrontendController(mcc),
      Logging {

  private def withEmailEnabled(action: Action[AnyContent]): Action[AnyContent] =
    if (!appConfig.emailJourneyEnabled) {
      as.default(_ => NotImplemented)
    } else {
      action
    }

  val whichEmailDoYouWantToUse: Action[AnyContent] =
    withEmailEnabled {
      as.eligibleJourneyAction { implicit request =>
        withEmailAddressFromEligibilityResponse { emailFromEligibilityResponse =>
          request.journey match {
            case j: JourneyStage.BeforeAgreedTermsAndConditions => logErrorAndRouteToDefaultPage(j)
            case j: JourneyStage.AfterAgreedTermsAndConditions  =>
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
    journey:                      JourneyStage.AfterAgreedTermsAndConditions & Journey,
    emailFromEligibilityResponse: Email
  )(using Request[?]): Result = {
    val maybePrePopForm: Form[ChooseEmailForm] =
      existingEmailToBeVerified(journey).fold(chooseEmailForm()) { existingEmail =>
        if (existingEmail == emailFromEligibilityResponse) {
          chooseEmailForm().fill(ChooseEmailForm(existingEmail.value.decryptedValue, None))
        } else {
          chooseEmailForm().fill(
            ChooseEmailForm(emailFromEligibilityResponse.value.decryptedValue, Some(existingEmail.value.decryptedValue))
          )
        }
      }
    Ok(views.chooseEmailPage(emailFromEligibilityResponse.value.decryptedValue, maybePrePopForm))
  }

  private def existingEmailToBeVerified(journey: Journey): Option[Email] = journey match {
    case _: JourneyStage.BeforeEmailAddressSelectedToBeVerified => None
    case j: JourneyStage.AfterEmailAddressSelectedToBeVerified  => Some(j.emailToBeVerified)
    case _: Journey.SubmittedArrangement                        =>
      Errors.throwServerErrorException(
        "Shouldn't be trying to find email address in session when submission is submitted"
      )
  }

  val whichEmailDoYouWantToUseSubmit: Action[AnyContent] =
    withEmailEnabled {
      as.eligibleJourneyAction.async { implicit request =>
        withEmailAddressFromEligibilityResponse { emailFromEligibilityResponse =>
          EmailController
            .chooseEmailForm()
            .bindFromRequest()
            .fold[Future[Result]](
              formWithErrors =>
                Ok(views.chooseEmailPage(emailFromEligibilityResponse.value.decryptedValue, formWithErrors)),
              (form: ChooseEmailForm) => {
                val emailAddress: Email = form.differentEmail match {
                  case Some(email) => Email(SensitiveString(email))
                  case None        => Email(emailFromEligibilityResponse.value)
                }

                journeyService
                  .updateSelectedEmailToBeVerified(
                    journeyId = request.journeyId,
                    email = emailAddress
                  )
                  .map(updatedJourney =>
                    Routing.redirectToNext(
                      routes.EmailController.whichEmailDoYouWantToUse,
                      updatedJourney,
                      existingEmailToBeVerified(request.journey).contains(emailAddress)
                    )
                  )
              }
            )
        }
      }
    }

  val enterEmail: Action[AnyContent] = {
    def displayEnterEmailPage(journey: JourneyStage.AfterAgreedTermsAndConditions)(using Request[?]): Result = {
      val form: Form[Email] = journey match {
        case _: JourneyStage.BeforeEmailAddressSelectedToBeVerified => enterEmailForm
        case j: JourneyStage.AfterEmailAddressSelectedToBeVerified  => enterEmailForm.fill(j.emailToBeVerified)
        case _: Journey.SubmittedArrangement                        =>
          Errors.throwServerErrorException(
            "Can't render form for page when submission is submitted, this should never happen"
          )
      }

      Ok(views.enterEmailPage(form))
    }

    withEmailEnabled {
      as.eligibleJourneyAction { implicit request =>
        request.journey match {
          case j: JourneyStage.BeforeAgreedTermsAndConditions => logErrorAndRouteToDefaultPage(j)
          case j: JourneyStage.AfterAgreedTermsAndConditions  =>
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
          formWithErrors => Ok(views.enterEmailPage(formWithErrors)),
          email =>
            journeyService
              .updateSelectedEmailToBeVerified(
                journeyId = request.journeyId,
                email = email
              )
              .map(updatedJourney =>
                Routing.redirectToNext(
                  routes.EmailController.enterEmail,
                  updatedJourney,
                  existingEmailToBeVerified(request.journey).contains(email)
                )
              )
        )

    }
  }

  val requestVerification: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      request.journey match {
        case j: JourneyStage.BeforeEmailAddressSelectedToBeVerified =>
          logErrorAndRouteToDefaultPageF(j)

        case j: JourneyStage.AfterArrangementSubmitted =>
          logErrorAndRouteToDefaultPageF(j)

        case j: JourneyStage.AfterEmailAddressSelectedToBeVerified =>
          emailVerificationService.requestEmailVerification(j.emailToBeVerified).flatMap {
            case StartEmailVerificationJourneyResponse.Success(redirectUri) =>
              logger.info(s"Email verification journey successfully started. Redirecting to $redirectUri")
              Redirect(redirectUri)
            case StartEmailVerificationJourneyResponse.Error(reason)        =>
              reason match {
                case EmailVerificationState.TooManyPasscodeAttempts        =>
                  Redirect(routes.EmailController.tooManyPasscodeAttempts)
                case EmailVerificationState.TooManyPasscodeJourneysStarted =>
                  Redirect(routes.EmailController.tooManyPasscodeJourneysStarted)
                case EmailVerificationState.TooManyDifferentEmailAddresses =>
                  Redirect(routes.EmailController.tooManyDifferentEmailAddresses)
                case EmailVerificationState.AlreadyVerified                =>
                  journeyService
                    .updateEmailVerificationResult(
                      j.journeyId,
                      EmailVerificationResult.Verified
                    )
                    .map(_ => Redirect(routes.EmailController.emailAddressConfirmed))
              }
          }
      }

    }
  }

  val emailCallback: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      request.journey match {
        case j: JourneyStage.BeforeEmailAddressSelectedToBeVerified =>
          logErrorAndRouteToDefaultPageF(j)

        case j: JourneyStage.AfterArrangementSubmitted =>
          logErrorAndRouteToDefaultPageF(j)

        case j: JourneyStage.AfterEmailAddressSelectedToBeVerified =>
          for {
            result         <- emailVerificationService.getEmailVerificationResult(j.emailToBeVerified)
            updatedJourney <- journeyService.updateEmailVerificationResult(j.journeyId, result)
          } yield Routing.redirectToNext(
            routes.EmailController.emailCallback,
            updatedJourney,
            submittedValueUnchanged = false
          )
      }
    }
  }

  val tooManyPasscodeAttempts: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      val maybeEmail: Option[Email] = request.eligibilityCheckResult.email

      val emailEntryEndpoint: Call = maybeEmail.fold(
        routes.EmailController.enterEmail
      ) { _ =>
        routes.EmailController.whichEmailDoYouWantToUse
      }

      Ok(views.tooManyPasscodes(emailEntryEndpoint))
    }
  }

  val tooManyPasscodeJourneysStarted: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction { implicit request =>
      val email: Email = request.journey match {
        case _: JourneyStage.BeforeEmailAddressSelectedToBeVerified =>
          Errors.throwServerErrorException("Trying to get email before one has been entered.")
        case j: JourneyStage.AfterEmailAddressSelectedToBeVerified  => j.emailToBeVerified
        case _: Journey.SubmittedArrangement                        => Errors.throwServerErrorException("Journey is in finished state.")
      }
      Ok(
        views.tooManyPasscodeJourneysStarted(
          email = email.value.decryptedValue,
          newEmailLink = request.eligibilityCheckResult.email.fold(routes.EmailController.enterEmail.url)(_ =>
            routes.EmailController.whichEmailDoYouWantToUse.url
          )
        )
      )
    }
  }

  val tooManyDifferentEmailAddresses: Action[AnyContent] = withEmailEnabled {
    as.eligibleJourneyAction.async { implicit request =>
      emailVerificationService.getLockoutCreatedAt().map {
        _.earliestCreatedAtTime.fold(Errors.throwServerErrorException("Could not find earliest created at time")) {
          dateTime => Ok(views.tooManyEmails(dateTime.plusDays(1L)))
        }
      }
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
        Routing.redirectToNext(
          routes.EmailController.emailAddressConfirmed,
          request.journey,
          submittedValueUnchanged = false
        )
      )
    }
  }

  private def withEmailAddressVerified(
    f: JourneyStage.AfterEmailAddressVerificationResult => Result
  )(implicit request: EligibleJourneyRequest[?]): Result =
    request.journey match {
      case j: JourneyStage.BeforeEmailAddressVerificationResult => logErrorAndRouteToDefaultPage(j)

      case j: JourneyStage.AfterArrangementSubmitted =>
        logErrorAndRouteToDefaultPage(j)

      case j: JourneyStage.AfterEmailAddressVerificationResult =>
        j.emailVerificationResult match {
          case EmailVerificationResult.Verified =>
            f(j)

          case EmailVerificationResult.Locked =>
            logErrorAndRouteToDefaultPage(j)
        }
    }

  private def withEmailAddressFromEligibilityResponse(f: Email => Result)(implicit
    r: EligibleJourneyRequest[?]
  ): Result =
    f(emailFromEligibilityResponse(r))

  private def withEmailAddressFromEligibilityResponse(f: Email => Future[Result])(implicit
    r: EligibleJourneyRequest[?]
  ): Future[Result] =
    f(emailFromEligibilityResponse(r))

  private def emailFromEligibilityResponse(r: EligibleJourneyRequest[?]): Email =
    r.eligibilityCheckResult.email.getOrElse(
      Errors.throwServerErrorException("Could not find email address in eligibility response")
    )

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
      "newEmailInput"           -> mandatoryIfEqual("selectAnEmailToUseRadio", "new", differentEmailAddressMapping)
    )(ChooseEmailForm.apply)(Tuple.fromProductTyped(_).some)
  )

  private val emailRegex: Regex = "^([a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)$".r

  val differentEmailAddressMapping: Mapping[String] = nonEmptyText
    .transform[String](email => email.toLowerCase(Locale.UK), _.toLowerCase(Locale.UK))
    .verifying(
      Constraint[String]((email: String) =>
        if (email.length > 256) Invalid("error.tooManyChar")
        else if (emailRegex.matches(email)) Valid
        else Invalid("error.invalidFormat")
      )
    )

  val enterEmailForm: Form[Email] = Form(
    mapping(
      "newEmailInput" -> differentEmailAddressMapping
    )(s => Email.apply(SensitiveString(s)))(Email.unapply(_).value.decryptedValue.some)
  )

}
