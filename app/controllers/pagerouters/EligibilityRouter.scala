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

package controllers.pagerouters

import cats.syntax.eq._
import controllers.routes
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.TaxRegime.Sa
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import models.EligibilityErrors._
import models.{EligibilityError, EligibilityErrors}
import play.api.mvc.Call

object EligibilityRouter {

  def nextPage(eligibilityResult: EligibilityCheckResult, taxRegime: TaxRegime): Call = {
    if (eligibilityResult.isEligible) {
      routes.YourBillController.yourBill
    } else {
      EligibilityErrors.toEligibilityError(eligibilityResult.eligibilityRules) match {
        case ee @ Some(MultipleReasons) =>
          determineWhetherToGoToDebtTooSmall(ee, eligibilityResult.eligibilityRules.isLessThanMinDebtAllowance, eligibilityResult.eligibilityRules.noDueDatesReached, taxRegime)
        case None                                    => whichGenericIneligiblePage(taxRegime)
        case Some(HasRlsOnAddress)                   => whichGenericIneligiblePage(taxRegime)
        case Some(MarkedAsInsolvent)                 => whichGenericIneligiblePage(taxRegime)
        case Some(IsLessThanMinDebtAllowance)        => whichDebtTooSmallPage(taxRegime)
        case Some(IsMoreThanMaxDebtAllowance)        => whichDebtTooLargePage(taxRegime)
        case Some(DisallowedChargeLockTypes)         => whichGenericIneligiblePage(taxRegime)
        case Some(ChargesOverMaxDebtAge)             => whichDebtTooOldPage(taxRegime)
        case Some(ExistingTtp)                       => whichExistingPlanPage(taxRegime)
        case Some(IneligibleChargeTypes)             => whichGenericIneligiblePage(taxRegime)
        case Some(MissingFiledReturns)               => whichFileYourReturnsPage(taxRegime)
        case Some(HasInvalidInterestSignals)         => whichGenericIneligiblePage(taxRegime)
        case Some(HasInvalidInterestSignalsCESA)     => whichGenericIneligiblePage(taxRegime)
        case Some(DmSpecialOfficeProcessingRequired) => whichGenericIneligiblePage(taxRegime)
        case Some(NoDueDatesReached) => if (taxRegime =!= Sa) {
          whichNoDueDatesReachedPage(taxRegime)
        } else { whichGenericIneligiblePage(taxRegime) }
        case Some(CannotFindLockReason)           => whichGenericIneligiblePage(taxRegime)
        case Some(CreditsNotAllowed)              => whichGenericIneligiblePage(taxRegime)
        case Some(IsMoreThanMaxPaymentReference)  => whichGenericIneligiblePage(taxRegime)
        case Some(ChargesBeforeMaxAccountingDate) => whichDebtBeforeAccountingDatePage(taxRegime)
        case Some(HasDisguisedRemuneration)       => whichGenericIneligiblePage(taxRegime)
        case Some(HasCapacitor)                   => whichGenericIneligiblePage(taxRegime)
      }
    }
  }

  /**
   * To be used when there are more than one 'flavour' of a lockout page. Design want them to have different urls.
   */
  private def whichGenericIneligiblePage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.payeGenericIneligiblePage
    case TaxRegime.Vat   => routes.IneligibleController.vatGenericIneligiblePage
    case TaxRegime.Sa    => routes.IneligibleController.saGenericIneligiblePage
  }

  private def whichDebtTooLargePage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeDebtTooLargePage
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtTooLargePage
    case TaxRegime.Sa    => routes.IneligibleController.saDebtTooLargePage
  }

  private def whichDebtTooSmallPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeDebtTooSmallPage
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtTooSmallPage
    case TaxRegime.Sa    => routes.IneligibleController.saDebtTooSmallPage
  }

  private def whichExistingPlanPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeAlreadyHaveAPaymentPlanPage
    case TaxRegime.Vat   => routes.IneligibleController.vatAlreadyHaveAPaymentPlanPage
    case TaxRegime.Sa    => routes.IneligibleController.saAlreadyHaveAPaymentPlanPage
  }

  private def whichDebtTooOldPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeDebtTooOldPage
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtTooOldPage
    case TaxRegime.Sa    => routes.IneligibleController.saDebtTooOldPage
  }

  private def whichFileYourReturnsPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeFileYourReturnPage
    case TaxRegime.Vat   => routes.IneligibleController.vatFileYourReturnPage
    case TaxRegime.Sa    => routes.IneligibleController.saFileYourReturnPage
  }

  private def whichDebtBeforeAccountingDatePage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => throw new NotImplementedError("Ineligibility reason not relevant to EPAYE")
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtBeforeAccountingDatePage
    case TaxRegime.Sa    => routes.IneligibleController.saDebtTooOldPage
  }

  /*
  requirement from business that if multiple reasons exist but any of them are isLessThanMinDebtAllowance,
  go to debt too small page, unless reason is NoDueDatesReached
  */
  private def determineWhetherToGoToDebtTooSmall(
      maybeEligibilityError:      Option[EligibilityError],
      isLessThanMinDebtAllowance: Boolean,
      dueDatesReached:            Boolean,
      taxRegime:                  TaxRegime
  ): Call = (maybeEligibilityError, isLessThanMinDebtAllowance, dueDatesReached, taxRegime) match {
    case (Some(MultipleReasons), true, true, _) => whichNoDueDatesReachedPage(taxRegime)
    case (Some(MultipleReasons), true, _, _)    => whichDebtTooSmallPage(taxRegime)
    case _                                      => whichGenericIneligiblePage(taxRegime)
  }

  private def whichNoDueDatesReachedPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeNoDueDatesReachedPage
    case TaxRegime.Vat   => routes.IneligibleController.vatNoDueDatesReachedPage
    case TaxRegime.Sa    => throw new NotImplementedError("Ineligibility reason not relevant to SA")
  }
}
