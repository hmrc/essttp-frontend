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
import controllers.JourneyFinalStateCheck.finalStateCheck
import controllers.JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage
import essttp.journey.JourneyConnector
import essttp.journey.model.JourneyStage.BeforeAssessmentCategoryDetermined
import essttp.journey.model.{Journey, JourneyStage, WhyCannotPayInFullAnswers}
import essttp.rootmodel.AmountInPence
import essttp.rootmodel.ttp.eligibility.{AssessmentCategory, ChargeTypeAssessment, ChargeTypeAssessments, Charges, EligibilityCheckResult, MainTrans}
import essttp.rootmodel.ttp.{DdInProgress, IsInterestBearingCharge}
import models.forms.{AddLiabilitiesForm, AddLiabilitiesFormValue}
import models.{InvoicePeriod, OverDuePayments, OverduePayment}
import play.api.mvc.*
import play.twirl.api.Html
import requests.RequestSupport
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourBillController @Inject() (
  as:               Actions,
  mcc:              MessagesControllerComponents,
  views:            Views,
  auditService:     AuditService,
  journeyConnector: JourneyConnector,
  requestSupport:   RequestSupport
)(using ExecutionContext)
    extends FrontendController(mcc),
      Logging {

  import requestSupport.languageFromRequest

  val yourBill: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    finalStateCheck(
      request.journey,
      request.eligibilityCheckResult.foldOnAssessmentCategory(
        standard =>
          displayPage(views.yourBillIs(YourBillController.overDuePayments(standard), request.journey.taxRegime)),
        debts => displayPage(views.yourBillIs(YourBillController.overDuePayments(debts), request.journey.taxRegime)),
        _ => Redirect(routes.YourBillController.yourUpcomingBill),
        (debts, _, _) =>
          ensureDebtsEligible(debts) {
            displayPage(views.yourBillIs(YourBillController.overDuePayments(debts), request.journey.taxRegime))
          }
      )
    )
  }

  val yourBillSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit eligibilityRequest =>
    val next = eligibilityRequest.eligibilityCheckResult.foldOnAssessmentCategory(
      _ => computeNext(eligibilityRequest.journey, eligibilityRequest.eligibilityCheckResult),
      _ => computeNext(eligibilityRequest.journey, eligibilityRequest.eligibilityCheckResult),
      _ => throw new Exception("Not expecting submit on YourBill for Liabilities only assessment category"),
      (debts, _, _) =>
        ensureDebtsEligible(debts) {
          eligibilityRequest.journey match {
            case _: JourneyStage.BeforeAssessmentCategoryDetermined =>
              Future.successful(routes.YourBillController.advancePayment)
            case _: JourneyStage.AfterAssessmentCategoryDetermined  =>
              computeNext(eligibilityRequest.journey, eligibilityRequest.eligibilityCheckResult)
          }
        }
    )

    next.map(Redirect(_))
  }

  val yourUpcomingBill: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    finalStateCheck(
      request.journey,
      request.eligibilityCheckResult.foldOnAssessmentCategory(
        _ => logErrorAndRouteToDefaultPage(request.journey),
        _ => logErrorAndRouteToDefaultPage(request.journey),
        liabilities =>
          displayPage(
            views.yourUpcomingBill(YourBillController.overDuePayments(liabilities), request.journey.taxRegime)
          ),
        (_, _, _) => logErrorAndRouteToDefaultPage(request.journey)
      )
    )
  }

  val yourUpcomingBillSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit eligibilityRequest =>
    val next = eligibilityRequest.eligibilityCheckResult.foldOnAssessmentCategory(
      _ => throw new Exception("Not expecting submit on YourUpcomingBill for Standard assessment category"),
      _ => throw new Exception("Not expecting submit on YourUpcomingBill for Debts only assessment category"),
      _ => computeNext(eligibilityRequest.journey, eligibilityRequest.eligibilityCheckResult),
      (_, _, _) =>
        throw new Exception("Not expecting submit on YourUpcomingBill for DebtAndLiabilities assessment category")
    )

    next.map(Redirect(_))
  }

  val yourBillCombined: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    finalStateCheck(
      request.journey,
      request.eligibilityCheckResult.foldOnAssessmentCategory(
        _ => logErrorAndRouteToDefaultPage(request.journey),
        _ => logErrorAndRouteToDefaultPage(request.journey),
        _ => logErrorAndRouteToDefaultPage(request.journey),
        (debts, liabilities, _) =>
          displayPage(
            views.yourBillCombined(
              YourBillController.overDuePayments(debts),
              YourBillController.overDuePayments(liabilities),
              request.journey.taxRegime,
              showRemoveLiabilities = debts.assessmentEligibilityStatus
            )
          )
      )
    )
  }

  val yourBillCombinedSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit eligibilityRequest =>
    val next = eligibilityRequest.eligibilityCheckResult.foldOnAssessmentCategory(
      _ => throw new Exception("Not expecting submit on YourBillCombined for Standard assessment category"),
      _ => throw new Exception("Not expecting submit on YourBillCombined for Debts only assessment category"),
      _ => throw new Exception("Not expecting submit on YourBillCombined for Liabilities only assessment category"),
      (_, _, _) => computeNext(eligibilityRequest.journey, eligibilityRequest.eligibilityCheckResult)
    )

    next.map(Redirect(_))
  }

  val removeAdvancePayments: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.journey match {
      case j: BeforeAssessmentCategoryDetermined             =>
        logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterAssessmentCategoryDetermined =>
        j.assessmentCategory match {
          case AssessmentCategory.DebtsAndLiabilities =>
            val debts = request.eligibilityCheckResult.chargeTypeAssessments
              .find(_.assessmentCategory == AssessmentCategory.Debts)
              .getOrElse(throw new Exception("Could not find chargeTypeAssessments with category Debts"))

            if (debts.assessmentEligibilityStatus) {
              journeyConnector.updateAssessmentCategory(j.journeyId, AssessmentCategory.Debts).map { _ =>
                Redirect(routes.YourBillController.yourBill)
              }
            } else {
              throw new Exception("Cannot remove liabilities when debts not eligible")
            }

          case other =>
            logger.warn(s"Unexpected assessment category $other for removeAdvancePayments")
            logErrorAndRouteToDefaultPage(j)
        }
    }
  }

  val advancePayment: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    finalStateCheck(
      request.journey,
      request.eligibilityCheckResult.foldOnAssessmentCategory(
        _ => logErrorAndRouteToDefaultPage(request.journey),
        _ => logErrorAndRouteToDefaultPage(request.journey),
        _ => logErrorAndRouteToDefaultPage(request.journey),
        (debts, liabilities, _) =>
          ensureDebtsEligible(debts) {
            val form = request.journey match {
              case j: JourneyStage.BeforeAssessmentCategoryDetermined =>
                AddLiabilitiesForm.form
              case j: JourneyStage.AfterAssessmentCategoryDetermined  =>
                j.assessmentCategory match {
                  case AssessmentCategory.DebtsAndLiabilities =>
                    AddLiabilitiesForm.form.fill(AddLiabilitiesFormValue.Yes)
                  case AssessmentCategory.Debts               => AddLiabilitiesForm.form.fill(AddLiabilitiesFormValue.No)
                  case _                                      => AddLiabilitiesForm.form
                }
            }

            displayPage(
              views.advancePayments(form, YourBillController.overDuePayments(liabilities), request.journey.taxRegime)
            )
          }
      )
    )
  }

  val advancePaymentSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    request.eligibilityCheckResult.foldOnAssessmentCategory[Future[Result]](
      _ => logErrorAndRouteToDefaultPage(request.journey),
      _ => logErrorAndRouteToDefaultPage(request.journey),
      _ => logErrorAndRouteToDefaultPage(request.journey),
      (debts, liabilities, _) =>
        ensureDebtsEligible(debts) {
          AddLiabilitiesForm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    views.advancePayments(
                      formWithErrors,
                      YourBillController.overDuePayments(liabilities),
                      request.journey.taxRegime
                    )
                  )
                ),
              addLiabilities => {
                val (assessmentCategory, next) = addLiabilities match {
                  case AddLiabilitiesFormValue.Yes =>
                    AssessmentCategory.DebtsAndLiabilities -> routes.YourBillController.yourBillCombined
                  case AddLiabilitiesFormValue.No  =>
                    AssessmentCategory.Debts -> routes.YourBillController.yourBill
                }

                journeyConnector.updateAssessmentCategory(request.journey.journeyId, assessmentCategory).map { _ =>
                  Redirect(next)
                }
              }
            )
        }
    )
  }

  private def ensureDebtsEligible[A](debts: ChargeTypeAssessments)(f: => A): A =
    if (debts.assessmentEligibilityStatus) f
    else throw new Exception("eligibility status for assessment category 'debts' must be eligible but was not")

  private def displayPage(page: => Html)(using Request[?]): Result =
    try
      Ok(page)
    catch {
      case e: MainTrans.UnknownMainTransException                      =>
        logger.warn(s"${e.getClass.getName}: MainTrans with no corresponding charge type: ${e.mTrans.value}")
        Redirect(routes.IneligibleController.saGenericIneligiblePage)
      case e: ChargeTypeAssessment.ChargesWithDifferentMTransException =>
        logger.warn(
          s"${e.getClass.getName}: ChargeTypeAssessment has charges with different MainTrans: ${e.charges.map(_.mainTrans).toString}"
        )
        Redirect(routes.IneligibleController.saGenericIneligiblePage)
    }

  val youAlreadyHaveDirectDebit: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeAssessmentCategoryDetermined => logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterAssessmentCategoryDetermined  =>
        finalStateCheck(j, displayYouAlreadyHaveDirectDebitPage(j, request.eligibilityCheckResult))
    }
  }

  private def displayYouAlreadyHaveDirectDebitPage(
    journey:                JourneyStage.AfterAssessmentCategoryDetermined & Journey,
    eligibilityCheckResult: EligibilityCheckResult
  )(using Request[?]): Result =
    Ok(
      views.youAlreadyHaveDirectDebit(
        YourBillController.overDuePaymentsWithDdInProgress(
          eligibilityCheckResult.relevantChargeTypeAssessments(journey)
        ),
        journey.taxRegime
      )
    )

  val youAlreadyHaveDirectDebitSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    auditService.auditDdInProgress(request.journey, hasChosenToContinue = true)
    computeNext(request.journey, request.eligibilityCheckResult, checkIfHasAnyChargesWithDdInProgress = false).map(
      Redirect(_)
    )
  }

  private def computeNext(
    journey:                              Journey,
    eligibilityCheckResult:               EligibilityCheckResult,
    checkIfHasAnyChargesWithDdInProgress: Boolean = true
  )(using RequestHeader): Future[Call] =
    if (
      checkIfHasAnyChargesWithDdInProgress && YourBillController.hasAnyChargesWithDdInProgress(
        eligibilityCheckResult,
        journey
      )
    )
      Future.successful(routes.YourBillController.youAlreadyHaveDirectDebit)
    else if (journey.affordabilityEnabled.contains(true))
      Future.successful(routes.WhyCannotPayInFullController.whyCannotPayInFull)
    else
      journeyConnector
        .updateWhyCannotPayInFullAnswers(journey.journeyId, WhyCannotPayInFullAnswers.AnswerNotRequired)
        .map(_ => routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment)

}

object YourBillController {
  def chargeDueDate(chargeTypeAssessments: List[ChargeTypeAssessment]): LocalDate =
    chargeTypeAssessments.headOption
      .map { (chargeTypeAssessment: ChargeTypeAssessment) =>
        chargeTypeAssessment.charges.headOption.map { (charges: Charges) =>
          parseLocalDate(charges.dueDate.value.toString)
        }
      }
      .getOrElse(throw new IllegalArgumentException("missing charge list"))
      .getOrElse(throw new IllegalArgumentException("missing charge list"))

  val LocalDateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def parseLocalDate(s: String): LocalDate = LocalDateTimeFmt.parse(s, LocalDate.from)

  def invoicePeriod(ass: ChargeTypeAssessment): InvoicePeriod = {
    val dueDate: LocalDate   = chargeDueDate(List(ass))
    val startDate: LocalDate = parseLocalDate(ass.taxPeriodFrom.value)
    val endDate: LocalDate   = parseLocalDate(ass.taxPeriodTo.value)
    InvoicePeriod(monthNumberInTaxYear(startDate), startDate, endDate, dueDate)
  }

  private def chargeBearsInterest(ass: ChargeTypeAssessment): Option[IsInterestBearingCharge] =
    ass.charges.headOption.flatMap(_.isInterestBearingCharge)

  private def ddInProgress(ass: ChargeTypeAssessment): Option[DdInProgress] =
    ass.charges.headOption.flatMap(_.ddInProgress)

  private def hasAnyChargesWithDdInProgress(eligibilityResult: EligibilityCheckResult, journey: Journey) =
    eligibilityResult
      .relevantChargeTypeAssessments(journey)
      .chargeTypeAssessment
      .map(overDuePaymentOf)
      .exists(_.ddInProgress.contains(DdInProgress(value = true)))

  private val taxMonthStartDay: Int = 6

  def monthNumberInTaxYear(date: LocalDate): Int = {
    val day: Int   = date.getDayOfMonth
    val month: Int =
      if (day >= taxMonthStartDay) date.getMonthValue
      else {
        date.getMonthValue - 1
      }
    if (month >= 4) month - 3 else month + 9
  }

  private def overDuePaymentOf(ass: ChargeTypeAssessment): OverduePayment = {
    val maybeMainTrans = ass.charges
      .map(_.mainTrans)
      .reduceOption((a, b) =>
        if (a == b) a else throw ChargeTypeAssessment.ChargesWithDifferentMTransException(ass.charges)
      )

    val mainTrans = maybeMainTrans.getOrElse(
      throw new RuntimeException("This should not be possible: A charge did not have a MainTrans")
    )

    OverduePayment(
      invoicePeriod(ass),
      ass.debtTotalAmount.value,
      chargeBearsInterest(ass),
      ddInProgress(ass),
      mainTrans
    )
  }

  private def qualifyingDebt(chargeTypeAssessments: ChargeTypeAssessments): AmountInPence =
    chargeTypeAssessments.chargeTypeAssessment
      .map(_.debtTotalAmount.value)
      .fold(AmountInPence.zero)(_ + _)

  private def overDuePayments(chargeTypeAssessments: ChargeTypeAssessments): OverDuePayments = {
    val payments = chargeTypeAssessments.chargeTypeAssessment.map(overDuePaymentOf)
    OverDuePayments(qualifyingDebt(chargeTypeAssessments), payments)
  }

  private def overDuePaymentsWithDdInProgress(chargeTypeAssessments: ChargeTypeAssessments): OverDuePayments = {
    val paymentsWithDdInProgress =
      chargeTypeAssessments.chargeTypeAssessment
        .map(overDuePaymentOf)
        .filter(_.ddInProgress.contains(DdInProgress(value = true)))

    OverDuePayments(qualifyingDebt(chargeTypeAssessments), paymentsWithDdInProgress)
  }
}
