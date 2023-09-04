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

import controllers.routes
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import models.EligibilityErrors
import models.EligibilityErrors._
import play.api.mvc.Call

object EligibilityRouter {

  def nextPage(eligibilityResult: EligibilityCheckResult, taxRegime: TaxRegime): Call = {
    if (eligibilityResult.isEligible) {
      routes.YourBillController.yourBill
    } else {
      EligibilityErrors.toEligibilityError(eligibilityResult.eligibilityRules) match {
        case Some(MultipleReasons)                   => whichGenericEligibilityPage(taxRegime)
        case None                                    => whichGenericEligibilityPage(taxRegime)
        case Some(HasRlsOnAddress)                   => whichGenericEligibilityPage(taxRegime)
        case Some(MarkedAsInsolvent)                 => whichGenericEligibilityPage(taxRegime)
        case Some(IsLessThanMinDebtAllowance)        => whichDebtTooSmallPage(taxRegime)
        case Some(IsMoreThanMaxDebtAllowance)        => whichDebtTooLargePage(taxRegime)
        case Some(DisallowedChargeLockTypes)         => whichGenericEligibilityPage(taxRegime)
        case Some(ChargesOverMaxDebtAge)             => whichDebtTooOldPage(taxRegime)
        case Some(ExistingTtp)                       => whichExistingPlanPage(taxRegime)
        case Some(IneligibleChargeTypes)             => whichGenericEligibilityPage(taxRegime)
        case Some(MissingFiledReturns)               => whichFileYourReturnsPage(taxRegime)
        case Some(HasInvalidInterestSignals)         => whichGenericEligibilityPage(taxRegime)
        case Some(DmSpecialOfficeProcessingRequired) => whichGenericEligibilityPage(taxRegime)
        case Some(NoDueDatesReached)                 => whichGenericEligibilityPage(taxRegime)
        case Some(CannotFindLockReason)              => whichGenericEligibilityPage(taxRegime)
        case Some(CreditsNotAllowed)                 => whichGenericEligibilityPage(taxRegime)
        case Some(IsMoreThanMaxPaymentReference)     => whichGenericEligibilityPage(taxRegime)
      }
    }
  }

  /**
   * To be used when there are more than one 'flavour' of a lockout page. Design want them to have different urls.
   */
  def whichGenericEligibilityPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.payeGenericIneligiblePage
    case TaxRegime.Vat   => routes.IneligibleController.vatGenericIneligiblePage
  }

  def whichDebtTooLargePage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeDebtTooLargePage
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtTooLargePage
  }

  def whichDebtTooSmallPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeDebtTooSmallPage
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtTooSmallPage
  }

  def whichExistingPlanPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeAlreadyHaveAPaymentPlanPage
    case TaxRegime.Vat   => routes.IneligibleController.vatAlreadyHaveAPaymentPlanPage
  }

  def whichDebtTooOldPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeDebtTooOldPage
    case TaxRegime.Vat   => routes.IneligibleController.vatDebtTooOldPage
  }

  def whichFileYourReturnsPage(taxRegime: TaxRegime): Call = taxRegime match {
    case TaxRegime.Epaye => routes.IneligibleController.epayeFileYourReturnPage
    case TaxRegime.Vat   => routes.IneligibleController.vatFileYourReturnPage
  }

}
