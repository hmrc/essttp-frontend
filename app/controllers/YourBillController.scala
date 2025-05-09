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
import essttp.journey.model.{Journey, JourneyStage, WhyCannotPayInFullAnswers}
import essttp.rootmodel.AmountInPence
import essttp.rootmodel.ttp.eligibility.{ChargeTypeAssessment, Charges, EligibilityCheckResult, MainTrans}
import essttp.rootmodel.ttp.{DdInProgress, IsInterestBearingCharge}
import models.{InvoicePeriod, OverDuePayments, OverduePayment}
import play.api.mvc.*
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
  journeyConnector: JourneyConnector
)(using ExecutionContext)
    extends FrontendController(mcc),
      Logging {

  val yourBill: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEligibilityChecked => logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterEligibilityChecked  => finalStateCheck(j, displayPage(j))
    }
  }

  private def displayPage(
    journey: JourneyStage.AfterEligibilityChecked & Journey
  )(using Request[?]): Result =
    try
      Ok(
        views.yourBillIs(
          YourBillController.overDuePayments(journey.eligibilityCheckResult),
          journey.taxRegime
        )
      )
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

  val yourBillSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit eligibilityRequest =>
    if (YourBillController.hasAnyChargesWithDdInProgress(eligibilityRequest.eligibilityCheckResult))
      Redirect(routes.YourBillController.youAlreadyHaveDirectDebit)
    else
      computeNext(eligibilityRequest.journey).map(Redirect(_))
  }

  val youAlreadyHaveDirectDebit: Action[AnyContent] = as.eligibleJourneyAction { implicit request =>
    request.journey match {
      case j: JourneyStage.BeforeEligibilityChecked => logErrorAndRouteToDefaultPage(j)
      case j: JourneyStage.AfterEligibilityChecked  => finalStateCheck(j, displayYouAlreadyHaveDirectDebitPage(j))
    }
  }

  private def displayYouAlreadyHaveDirectDebitPage(
    journey: JourneyStage.AfterEligibilityChecked & Journey
  )(using Request[?]): Result =
    Ok(
      views.youAlreadyHaveDirectDebit(
        YourBillController.overDuePaymentsWithDdInProgress(journey.eligibilityCheckResult),
        journey.taxRegime
      )
    )

  val youAlreadyHaveDirectDebitSubmit: Action[AnyContent] = as.eligibleJourneyAction.async { implicit request =>
    auditService.auditDdInProgress(request.journey, hasChosenToContinue = true)
    computeNext(request.journey).map(Redirect(_))
  }

  private def computeNext(journey: Journey)(using RequestHeader): Future[Call] =
    if (journey.affordabilityEnabled.contains(true))
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

  private def hasAnyChargesWithDdInProgress(eligibilityResult: EligibilityCheckResult) =
    eligibilityResult.chargeTypeAssessment
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

  private def qualifyingDebt(eligibilityResult: EligibilityCheckResult): AmountInPence =
    eligibilityResult.chargeTypeAssessment.map(_.debtTotalAmount.value).fold(AmountInPence.zero)(_ + _)

  private def overDuePayments(eligibilityResult: EligibilityCheckResult): OverDuePayments = {
    val payments = eligibilityResult.chargeTypeAssessment.map(overDuePaymentOf)
    OverDuePayments(qualifyingDebt(eligibilityResult), payments)
  }

  private def overDuePaymentsWithDdInProgress(eligibilityResult: EligibilityCheckResult): OverDuePayments = {
    val paymentsWithDdInProgress =
      eligibilityResult.chargeTypeAssessment
        .map(overDuePaymentOf)
        .filter(_.ddInProgress.contains(DdInProgress(value = true)))

    OverDuePayments(qualifyingDebt(eligibilityResult), paymentsWithDdInProgress)
  }
}
