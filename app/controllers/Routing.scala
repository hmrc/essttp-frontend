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

import actionsmodel.AuthenticatedRequest
import config.AppConfig
import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.Journey.*
import essttp.journey.model.*
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import essttp.rootmodel.{CanPayUpfront, IsEmailAddressRequired, TaxRegime}
import essttp.utils.Errors
import models.Languages.{English, Welsh}
import paymentsEmailVerification.models.EmailVerificationResult
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.UpstreamErrorResponse

object Routing {

  // session key to indicate someone has just clicked a change link from a CYA page
  val clickedChangeFromSessionKey: String = "essttpClickedChangeFrom"

  def redirectToNext(
    current:                 Call,
    journey:                 Journey,
    submittedValueUnchanged: Boolean
  )(using request: AuthenticatedRequest[?], config: AppConfig): Result = {
    val journeyRoutes: Map[Call, () => Call] = Map(
      routes.LandingController.epayeLandingPage                                                      -> { () =>
        routes.DetermineTaxIdController.determineTaxId
      },
      routes.LandingController.vatLandingPage                                                        -> { () =>
        routes.DetermineTaxIdController.determineTaxId
      },
      routes.DetermineTaxIdController.determineTaxId                                                 -> { () =>
        routes.DetermineEligibilityController.determineEligibility
      },
      routes.DetermineEligibilityController.determineEligibility                                     -> { () =>
        journey match {
          case _: JourneyStage.BeforeEligibilityChecked =>
            throw UpstreamErrorResponse("Could not find eligibility response to determine route", INTERNAL_SERVER_ERROR)
          case j: JourneyStage.AfterEligibilityChecked  =>
            EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime)
        }
      },
      routes.WhyCannotPayInFullController.whyCannotPayInFull                                         -> { () =>
        routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment
      },
      routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment                                     -> { () =>
        journey match {
          case _: JourneyStage.BeforeAnsweredCanPayUpfront =>
            throw UpstreamErrorResponse("Could not find CanPayUpfront answer to determine route", INTERNAL_SERVER_ERROR)
          case j: JourneyStage.AfterAnsweredCanPayUpfront  => canPayUpfrontRoute(j.canPayUpfront)
          case j: JourneyStage.AfterUpfrontPaymentAnswers  =>
            val canPayUpfront = j.upfrontPaymentAnswers match {
              case UpfrontPaymentAnswers.NoUpfrontPayment          => CanPayUpfront(value = false)
              case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => CanPayUpfront(value = true)
            }
            canPayUpfrontRoute(canPayUpfront)
        }
      },
      routes.UpfrontPaymentController.upfrontPaymentAmount                                           -> { () =>
        routes.UpfrontPaymentController.upfrontPaymentSummary
      },
      routes.UpfrontPaymentController.upfrontPaymentSummary                                          -> { () =>
        routes.DatesApiController.retrieveExtremeDates
      },
      routes.DatesApiController.retrieveExtremeDates                                                 -> { () =>
        routes.DetermineAffordabilityController.determineAffordability
      },
      routes.DetermineAffordabilityController.determineAffordability                                 -> { () =>
        affordabilityRoute(journey)
      },
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(journey.taxRegime, Some(English)) -> { () =>
        canPayWithinSixMonthsRoute(journey, submittedValueUnchanged)
      },
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(journey.taxRegime, Some(Welsh))   -> { () =>
        canPayWithinSixMonthsRoute(journey, submittedValueUnchanged)
      },
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(journey.taxRegime, None)          -> { () =>
        canPayWithinSixMonthsRoute(journey, submittedValueUnchanged)
      },
      routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount                              -> { () =>
        routes.PaymentDayController.paymentDay
      },
      routes.PaymentDayController.paymentDay                                                         -> { () =>
        routes.DatesApiController.retrieveStartDates
      },
      routes.DatesApiController.retrieveStartDates                                                   -> { () =>
        routes.DetermineAffordableQuotesController.retrieveAffordableQuotes
      },
      routes.DetermineAffordableQuotesController.retrieveAffordableQuotes                            -> { () =>
        routes.InstalmentsController.instalmentOptions
      },
      routes.InstalmentsController.instalmentOptions                                                 -> { () =>
        routes.PaymentScheduleController.checkPaymentSchedule
      },
      routes.PaymentScheduleController.checkPaymentSchedule                                          -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.PegaController.callback(TaxRegime.Epaye, None)                                          -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.PegaController.callback(TaxRegime.Vat, None)                                            -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.PegaController.callback(TaxRegime.Sa, None)                                             -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.PegaController.callback(TaxRegime.Simp, None)                                           -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.BankDetailsController.detailsAboutBankAccount                                           -> { () =>
        journey match {
          case _: JourneyStage.BeforeEnteredCanYouSetUpDirectDebit =>
            throw UpstreamErrorResponse(
              "Could not find DetailsAboutBankAccount answer to determine route",
              INTERNAL_SERVER_ERROR
            )
          case j: JourneyStage.AfterEnteredCanYouSetUpDirectDebit  =>
            detailsAboutBankAccountRoute(j.canSetUpDirectDebitAnswer.isAccountHolder)
        }
      },
      routes.BankDetailsController.enterBankDetails                                                  -> { () =>
        routes.BankDetailsController.checkBankDetails
      },
      routes.BankDetailsController.checkBankDetails                                                  -> { () =>
        routes.TermsAndConditionsController.termsAndConditions
      },
      routes.TermsAndConditionsController.termsAndConditions                                         -> { () =>
        journey match {
          case _: JourneyStage.BeforeAgreedTermsAndConditions =>
            throw UpstreamErrorResponse(
              "Could not find IsEmailAddressRequired answer to determine route",
              INTERNAL_SERVER_ERROR
            )

          case j: JourneyStage.AfterAgreedTermsAndConditions =>
            val eligibilityCheckResult = j match {
              case e: JourneyStage.AfterEligibilityChecked => e.eligibilityCheckResult
            }
            termsAndConditionsRoute(
              j.isEmailAddressRequired,
              eligibilityCheckResult,
              allowSubmitArrangement = true,
              j.taxRegime
            )
        }
      },
      routes.EmailController.enterEmail                                                              -> { () =>
        routes.EmailController.requestVerification
      },
      routes.EmailController.whichEmailDoYouWantToUse                                                -> { () =>
        routes.EmailController.requestVerification
      },
      routes.EmailController.emailCallback                                                           -> { () =>
        journey match {
          case _: JourneyStage.BeforeEmailAddressVerificationResult =>
            throw UpstreamErrorResponse(
              "Could not find EmailVerificationResult answer to determine route",
              INTERNAL_SERVER_ERROR
            )
          case j: JourneyStage.AfterEmailAddressVerificationResult  =>
            emailVerificationResultRoute(j.emailVerificationResult)
          case j: JourneyStage.AfterEmailVerificationPhase          =>
            j.emailVerificationAnswers match {
              case EmailVerificationAnswers.NoEmailJourney   =>
                throw UpstreamErrorResponse(
                  "Trying to determine next for email callback endpoint but no email journey required in session",
                  INTERNAL_SERVER_ERROR
                )
              case e: EmailVerificationAnswers.EmailVerified => emailVerificationResultRoute(e.emailVerificationResult)
            }
        }
      },
      routes.EmailController.emailAddressConfirmed                                                   -> { () =>
        routes.SubmitArrangementController.submitArrangement
      },
      routes.SubmitArrangementController.submitArrangement                                           -> { () =>
        SubmitArrangementController.whichPaymentPlanSetupPage(journey.taxRegime)
      }
    )

    val redirect = (request.session.get(clickedChangeFromSessionKey), submittedValueUnchanged) match {
      case (Some(_), true) =>
        Redirect(redirectToAfterUnchangedAnswerFromCYA(journey))

      case _ =>
        val next = journeyRoutes.getOrElse(
          current,
          throw UpstreamErrorResponse(
            s"Could not determine next page for current call ${current.toString}",
            INTERNAL_SERVER_ERROR
          )
        )()
        Redirect(next)
    }

    redirect.removingFromSession(clickedChangeFromSessionKey)
  }

  private def redirectToAfterUnchangedAnswerFromCYA(
    journey: Journey
  )(using request: AuthenticatedRequest[?], config: AppConfig): String =
    journey match {
      case _: JourneyStage.AfterStartedPegaCase =>
        config.pegaChangeLinkReturnUrl(journey.taxRegime, request.lang)

      case _: JourneyStage.AfterSelectedPaymentPlan =>
        routes.PaymentScheduleController.checkPaymentSchedule.url

      case j: JourneyStage.AfterCheckedPaymentPlan =>
        j.paymentPlanAnswers match {
          case _: PaymentPlanAnswers.PaymentPlanNoAffordability    =>
            routes.PaymentScheduleController.checkPaymentSchedule.url
          case _: PaymentPlanAnswers.PaymentPlanAfterAffordability =>
            config.pegaChangeLinkReturnUrl(journey.taxRegime, request.lang)
        }

      case j: JourneyStage.AfterUpfrontPaymentAnswers if hasDeclaredUpfrontPaymentAmount(j) =>
        routes.UpfrontPaymentController.upfrontPaymentSummary.url

      case other =>
        Errors.throwServerErrorException(
          s"Cannot change answer from check your payment plan page in journey state ${other.name}"
        )
    }

  private def hasDeclaredUpfrontPaymentAmount(j: JourneyStage.AfterUpfrontPaymentAnswers) =
    j.upfrontPaymentAnswers match {
      case UpfrontPaymentAnswers.NoUpfrontPayment               => false
      case UpfrontPaymentAnswers.DeclaredUpfrontPayment(amount) => true
    }

  def latestPossiblePage(journey: Journey): Call = journey match {
    case j: Journey.Started =>
      j.taxRegime match {
        case TaxRegime.Epaye => routes.LandingController.epayeLandingPage
        case TaxRegime.Vat   => routes.LandingController.vatLandingPage
        case TaxRegime.Sa    => routes.LandingController.saLandingPage
        case TaxRegime.Simp  => routes.LandingController.simpLandingPage
      }

    case _: Journey.ComputedTaxId =>
      routes.DetermineEligibilityController.determineEligibility

    case j: Journey.EligibilityChecked =>
      EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime)

    case _: Journey.ObtainedWhyCannotPayInFullAnswers =>
      routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment

    case j: Journey.AnsweredCanPayUpfront =>
      canPayUpfrontRoute(j.canPayUpfront)

    case _: Journey.EnteredUpfrontPaymentAmount          => routes.UpfrontPaymentController.upfrontPaymentSummary
    case _: Journey.RetrievedExtremeDates                => routes.DetermineAffordabilityController.determineAffordability
    case j: Journey.RetrievedAffordabilityResult         => affordabilityRoute(j)
    case _: Journey.ObtainedCanPayWithinSixMonthsAnswers =>
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(journey.taxRegime, None)
    case _: Journey.StartedPegaCase                      =>
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(journey.taxRegime, None)
    case _: Journey.EnteredMonthlyPaymentAmount          => routes.PaymentDayController.paymentDay
    case _: Journey.EnteredDayOfMonth                    => routes.DatesApiController.retrieveStartDates
    case _: Journey.RetrievedStartDates                  => routes.DetermineAffordableQuotesController.retrieveAffordableQuotes
    case _: Journey.RetrievedAffordableQuotes            => routes.InstalmentsController.instalmentOptions
    case _: Journey.ChosenPaymentPlan                    => routes.PaymentScheduleController.checkPaymentSchedule
    case _: Journey.CheckedPaymentPlan                   => routes.BankDetailsController.detailsAboutBankAccount
    case j: Journey.EnteredCanYouSetUpDirectDebit        =>
      detailsAboutBankAccountRoute(j.canSetUpDirectDebitAnswer.isAccountHolder)
    case _: Journey.EnteredDirectDebitDetails            => routes.BankDetailsController.checkBankDetails
    case _: Journey.ConfirmedDirectDebitDetails          => routes.TermsAndConditionsController.termsAndConditions

    case j: Journey.AgreedTermsAndConditions =>
      termsAndConditionsRoute(
        j.isEmailAddressRequired,
        j.eligibilityCheckResult,
        allowSubmitArrangement = false,
        j.taxRegime
      )

    case _: Journey.SelectedEmailToBeVerified =>
      routes.EmailController.requestVerification

    case j: Journey.EmailVerificationComplete =>
      emailVerificationResultRoute(j.emailVerificationResult)

    case j: Journey.SubmittedArrangement =>
      SubmitArrangementController.whichPaymentPlanSetupPage(j.taxRegime)
  }

  private def canPayWithinSixMonthsRoute(journey: Journey, submittedValueUnchanged: Boolean)(using
    AuthenticatedRequest[?],
    AppConfig
  ): Call =
    journey match {
      case _: JourneyStage.BeforeCanPayWithinSixMonthsAnswers =>
        throw UpstreamErrorResponse(
          "Could not find CanPayWithinSixMonths answer to determine route",
          INTERNAL_SERVER_ERROR
        )
      case j: JourneyStage.AfterCanPayWithinSixMonthsAnswers  =>
        canPayWithinSixMonthsRoute(j.canPayWithinSixMonthsAnswers, submittedValueUnchanged, journey)
    }

  private def canPayUpfrontRoute(canPayUpfront: CanPayUpfront): Call =
    if (canPayUpfront.value) routes.UpfrontPaymentController.upfrontPaymentAmount
    else routes.DatesApiController.retrieveExtremeDates

  private def affordabilityRoute(journey: Journey): Call =
    if (journey.affordabilityEnabled.contains(true))
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(journey.taxRegime, None)
    else routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount

  private def detailsAboutBankAccountRoute(isAccountHolder: Boolean): Call =
    if (isAccountHolder) routes.BankDetailsController.enterBankDetails
    else routes.BankDetailsController.cannotSetupDirectDebitOnlinePage

  // prevent accidentally submitting arrangement twice
  private def termsAndConditionsRoute(
    isEmailAddressRequired: IsEmailAddressRequired,
    eligibilityCheckResult: EligibilityCheckResult,
    allowSubmitArrangement: Boolean,
    taxRegime:              TaxRegime
  ): Call =
    if (isEmailAddressRequired) {
      if (eligibilityCheckResult.email.isDefined) routes.EmailController.whichEmailDoYouWantToUse
      else routes.EmailController.enterEmail
    } else if (allowSubmitArrangement) {
      routes.SubmitArrangementController.submitArrangement
    } else {
      SubmitArrangementController.whichPaymentPlanSetupPage(taxRegime)
    }

  private def emailVerificationResultRoute(emailVerificationResult: EmailVerificationResult): Call =
    emailVerificationResult match {
      case EmailVerificationResult.Verified => routes.EmailController.emailAddressConfirmed
      case EmailVerificationResult.Locked   => routes.EmailController.tooManyPasscodeAttempts
    }

  private def canPayWithinSixMonthsRoute(
    canPayWithinSixMonths:   CanPayWithinSixMonthsAnswers,
    submittedValueUnchanged: Boolean,
    journey:                 Journey
  )(using request: AuthenticatedRequest[?], config: AppConfig): Call =
    canPayWithinSixMonths match {
      case CanPayWithinSixMonthsAnswers.AnswerNotRequired             =>
        routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount
      case CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(canPay) =>
        if (canPay) routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount
        else {
          if (submittedValueUnchanged) {
            Call("GET", config.pegaStartRedirectUrl(journey.taxRegime, request.lang))
          } else routes.PegaController.startPegaJourney
        }
    }

}
