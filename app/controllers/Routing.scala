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

import controllers.pagerouters.EligibilityRouter
import essttp.journey.model.Journey._
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, EmailVerificationAnswers, Journey, UpfrontPaymentAnswers}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import essttp.rootmodel.{CanPayUpfront, IsEmailAddressRequired, TaxRegime}
import paymentsEmailVerification.models.EmailVerificationResult
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Request, Result}
import uk.gov.hmrc.http.UpstreamErrorResponse

object Routing {

  // session key to indicate someone has just clicked a change link from a CYA page
  val clickedChangeFromSessionKey: String = "essttpClickedChangeFrom"

  def redirectToNext(current: Call, journey: Journey, submittedValueUnchanged: Boolean)(implicit request: Request[_]): Result = {
    val journeyRoutes: Map[Call, () => Call] = Map(
      routes.LandingController.epayeLandingPage -> { () =>
        routes.DetermineTaxIdController.determineTaxId
      },
      routes.LandingController.vatLandingPage -> { () =>
        routes.DetermineTaxIdController.determineTaxId
      },
      routes.DetermineTaxIdController.determineTaxId -> { () =>
        routes.DetermineEligibilityController.determineEligibility
      },
      routes.DetermineEligibilityController.determineEligibility -> { () =>
        journey match {
          case _: BeforeEligibilityChecked => throw UpstreamErrorResponse("Could not find eligibility response to determine route", INTERNAL_SERVER_ERROR)
          case j: AfterEligibilityChecked  => EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime)
        }
      },
      routes.WhyCannotPayInFullController.whyCannotPayInFull -> { () =>
        routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment
      },
      routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment -> { () =>
        journey match {
          case _: BeforeAnsweredCanPayUpfront => throw UpstreamErrorResponse("Could not find CanPayUpfront answer to determine route", INTERNAL_SERVER_ERROR)
          case j: AfterAnsweredCanPayUpfront  => canPayUpfrontRoute(j.canPayUpfront)
          case j: AfterUpfrontPaymentAnswers =>
            val canPayUpfront = j.upfrontPaymentAnswers match {
              case UpfrontPaymentAnswers.NoUpfrontPayment          => CanPayUpfront(value = false)
              case _: UpfrontPaymentAnswers.DeclaredUpfrontPayment => CanPayUpfront(value = true)
            }
            canPayUpfrontRoute(canPayUpfront)
        }
      },
      routes.UpfrontPaymentController.upfrontPaymentAmount -> { () =>
        routes.UpfrontPaymentController.upfrontPaymentSummary
      },
      routes.UpfrontPaymentController.upfrontPaymentSummary -> { () =>
        routes.DatesApiController.retrieveExtremeDates
      },
      routes.DatesApiController.retrieveExtremeDates -> { () =>
        routes.DetermineAffordabilityController.determineAffordability
      },
      routes.DetermineAffordabilityController.determineAffordability -> { () =>
        affordabilityRoute(journey)
      },
      routes.CanPayWithinSixMonthsController.canPayWithinSixMonths -> { () =>
        journey match {
          case _: BeforeCanPayWithinSixMonthsAnswers =>
            throw UpstreamErrorResponse("Could not find CanPayWithinSixMonths answer to determine route", INTERNAL_SERVER_ERROR)
          case j: AfterCanPayWithinSixMonthsAnswers =>
            canPayWithinSixMonthsRoute(j.canPayWithinSixMonthsAnswers)
        }
      },
      routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount -> { () =>
        routes.PaymentDayController.paymentDay
      },
      routes.PaymentDayController.paymentDay -> { () =>
        routes.DatesApiController.retrieveStartDates
      },
      routes.DatesApiController.retrieveStartDates -> { () =>
        routes.DetermineAffordableQuotesController.retrieveAffordableQuotes
      },
      routes.DetermineAffordableQuotesController.retrieveAffordableQuotes -> { () =>
        routes.InstalmentsController.instalmentOptions
      },
      routes.InstalmentsController.instalmentOptions -> { () =>
        routes.PaymentScheduleController.checkPaymentSchedule
      },
      routes.PaymentScheduleController.checkPaymentSchedule -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.PegaController.callback -> { () =>
        routes.BankDetailsController.detailsAboutBankAccount
      },
      routes.BankDetailsController.detailsAboutBankAccount -> { () =>
        journey match {
          case _: BeforeEnteredDetailsAboutBankAccount =>
            throw UpstreamErrorResponse("Could not find DetailsAboutBankAccount answer to determine route", INTERNAL_SERVER_ERROR)
          case j: AfterEnteredDetailsAboutBankAccount =>
            detailsAboutBankAccountRoute(j.detailsAboutBankAccount.isAccountHolder)
        }
      },
      routes.BankDetailsController.enterBankDetails -> { () =>
        routes.BankDetailsController.checkBankDetails
      },
      routes.BankDetailsController.checkBankDetails -> { () =>
        routes.TermsAndConditionsController.termsAndConditions
      },
      routes.TermsAndConditionsController.termsAndConditions -> { () =>
        journey match {
          case _: BeforeAgreedTermsAndConditions =>
            throw UpstreamErrorResponse("Could not find IsEmailAddressRequired answer to determine route", INTERNAL_SERVER_ERROR)

          case j: AfterAgreedTermsAndConditions =>
            val eligibilityCheckResult = j match {
              case e: AfterEligibilityChecked => e.eligibilityCheckResult
            }
            termsAndConditionsRoute(j.isEmailAddressRequired, eligibilityCheckResult, allowSubmitArrangement = true, j.taxRegime)
        }
      },
      routes.EmailController.enterEmail -> { () =>
        routes.EmailController.requestVerification
      },
      routes.EmailController.whichEmailDoYouWantToUse -> { () =>
        routes.EmailController.requestVerification
      },
      routes.EmailController.emailCallback -> { () =>
        journey match {
          case _: BeforeEmailAddressVerificationResult => throw UpstreamErrorResponse("Could not find EmailVerificationResult answer to determine route", INTERNAL_SERVER_ERROR)
          case j: AfterEmailAddressVerificationResult  => emailVerificationResultRoute(j.emailVerificationResult)
          case j: AfterEmailVerificationPhase =>
            j.emailVerificationAnswers match {
              case EmailVerificationAnswers.NoEmailJourney =>
                throw UpstreamErrorResponse("Trying to determine next for email callback endpoint but no email journey required in session", INTERNAL_SERVER_ERROR)
              case e: EmailVerificationAnswers.EmailVerified => emailVerificationResultRoute(e.emailVerificationResult)
            }
        }
      },
      routes.EmailController.emailAddressConfirmed -> { () =>
        routes.SubmitArrangementController.submitArrangement
      },
      routes.SubmitArrangementController.submitArrangement -> { () =>
        SubmitArrangementController.whichPaymentPlanSetupPage(journey.taxRegime)
      }
    )

    val redirect = (request.session.get(clickedChangeFromSessionKey), submittedValueUnchanged) match {
      case (Some(url), true) =>
        Redirect(url)

      case _ =>
        val next = journeyRoutes.getOrElse(
          current,
          throw UpstreamErrorResponse(s"Could not determine next page for current call ${current.toString}", INTERNAL_SERVER_ERROR)
        )()
        Redirect(next)
    }

    redirect.removingFromSession(clickedChangeFromSessionKey)
  }

  def latestPossiblePage(journey: Journey): Call = journey match {
    case j: Journey.Stages.Started =>
      j.taxRegime match {
        case TaxRegime.Epaye => routes.LandingController.epayeLandingPage
        case TaxRegime.Vat   => routes.LandingController.vatLandingPage
        case TaxRegime.Sa    => routes.LandingController.saLandingPage
      }

    case _: Journey.Stages.ComputedTaxId =>
      routes.DetermineEligibilityController.determineEligibility

    case j: Journey.Stages.EligibilityChecked =>
      EligibilityRouter.nextPage(j.eligibilityCheckResult, j.taxRegime)

    case _: Journey.Stages.ObtainedWhyCannotPayInFullAnswers =>
      routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment

    case j: Journey.Stages.AnsweredCanPayUpfront =>
      canPayUpfrontRoute(j.canPayUpfront)

    case _: Journey.Stages.EnteredUpfrontPaymentAmount          => routes.UpfrontPaymentController.upfrontPaymentSummary
    case _: Journey.Stages.RetrievedExtremeDates                => routes.DetermineAffordabilityController.determineAffordability
    case j: Journey.Stages.RetrievedAffordabilityResult         => affordabilityRoute(j)
    case j: Journey.Stages.ObtainedCanPayWithinSixMonthsAnswers => canPayWithinSixMonthsRoute(j.canPayWithinSixMonthsAnswers)
    case _: Journey.Stages.StartedPegaCase                      => routes.CanPayWithinSixMonthsController.canPayWithinSixMonths
    case _: Journey.Stages.EnteredMonthlyPaymentAmount          => routes.PaymentDayController.paymentDay
    case _: Journey.Stages.EnteredDayOfMonth                    => routes.DatesApiController.retrieveStartDates
    case _: Journey.Stages.RetrievedStartDates                  => routes.DetermineAffordableQuotesController.retrieveAffordableQuotes
    case _: Journey.Stages.RetrievedAffordableQuotes            => routes.InstalmentsController.instalmentOptions
    case _: Journey.Stages.ChosenPaymentPlan                    => routes.PaymentScheduleController.checkPaymentSchedule
    case _: Journey.Stages.CheckedPaymentPlan                   => routes.BankDetailsController.detailsAboutBankAccount
    case j: Journey.Stages.EnteredDetailsAboutBankAccount       => detailsAboutBankAccountRoute(j.detailsAboutBankAccount.isAccountHolder)
    case _: Journey.Stages.EnteredDirectDebitDetails            => routes.BankDetailsController.checkBankDetails
    case _: Journey.Stages.ConfirmedDirectDebitDetails          => routes.TermsAndConditionsController.termsAndConditions

    case j: Journey.Stages.AgreedTermsAndConditions =>
      termsAndConditionsRoute(j.isEmailAddressRequired, j.eligibilityCheckResult, allowSubmitArrangement = false, j.taxRegime)

    case _: Journey.Stages.SelectedEmailToBeVerified =>
      routes.EmailController.requestVerification

    case j: Journey.Stages.EmailVerificationComplete =>
      emailVerificationResultRoute(j.emailVerificationResult)

    case j: Journey.Stages.SubmittedArrangement =>
      SubmitArrangementController.whichPaymentPlanSetupPage(j.taxRegime)
  }

  private def canPayUpfrontRoute(canPayUpfront: CanPayUpfront): Call =
    if (canPayUpfront.value) routes.UpfrontPaymentController.upfrontPaymentAmount
    else routes.DatesApiController.retrieveExtremeDates

  private def affordabilityRoute(journey: Journey): Call =
    if (journey.affordabilityEnabled.contains(true)) routes.CanPayWithinSixMonthsController.canPayWithinSixMonths
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

  private def canPayWithinSixMonthsRoute(canPayWithinSixMonths: CanPayWithinSixMonthsAnswers): Call =
    canPayWithinSixMonths match {
      case CanPayWithinSixMonthsAnswers.AnswerNotRequired =>
        routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount
      case CanPayWithinSixMonthsAnswers.CanPayWithinSixMonths(canPay) =>
        if (canPay) routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount
        else routes.PegaController.startPegaJourney
    }

}
