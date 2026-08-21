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

package models

import enumeratum.{Enum, EnumEntry}
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.{AssessmentCategory, AssessmentEligibilityRules, ChargeTypeAssessments, EligibilityRules}

import scala.collection.immutable

sealed trait EligibilityError extends EnumEntry derives CanEqual

object EligibilityErrors extends Enum[EligibilityError] {

  case object HasRlsOnAddress extends EligibilityError

  case object MarkedAsInsolvent extends EligibilityError

  case object NoDueDatesReached extends EligibilityError

  case object IsLessThanMinDebtAllowance extends EligibilityError

  case object IsMoreThanMaxDebtAllowance extends EligibilityError

  case object DisallowedChargeLockTypes extends EligibilityError

  case object ExistingTtp extends EligibilityError

  case object ChargesOverMaxDebtAge extends EligibilityError

  case object IneligibleChargeTypes extends EligibilityError

  case object MissingFiledReturns extends EligibilityError

  case object HasInvalidInterestSignals extends EligibilityError

  case object DmSpecialOfficeProcessingRequired extends EligibilityError

  case object CannotFindLockReason extends EligibilityError

  case object CreditsNotAllowed extends EligibilityError

  case object IsMoreThanMaxPaymentReference extends EligibilityError

  case object MultipleReasons extends EligibilityError

  case object ChargesBeforeMaxAccountingDate extends EligibilityError

  case object HasInvalidInterestSignalsCESA extends EligibilityError

  case object HasDisguisedRemuneration extends EligibilityError

  case object HasCapacitor extends EligibilityError

  case object DmSpecialOfficeProcessingRequiredCDCS extends EligibilityError

  case object IsAnMtdCustomer extends EligibilityError

  case object DmSpecialOfficeProcessingRequiredCESA extends EligibilityError

  case object NoMtditsaEnrollment extends EligibilityError

  case object AllChargeTypeAssessmentsFailed extends EligibilityError

  case object NoValidPlanAfterAssessments extends EligibilityError

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  def toEligibilityError(
    eligibilityRules:      EligibilityRules,
    chargeTypeAssessments: List[ChargeTypeAssessments],
    taxRegime:             TaxRegime
  ): Option[EligibilityError] = {
    def hasAssessmentEligibilityRule(
      assessmentCategory: AssessmentCategory,
      rule:               AssessmentEligibilityRules => Boolean
    ): Boolean =
      chargeTypeAssessments
        .find(_.assessmentCategory == assessmentCategory)
        .exists(c => rule(c.assessmentEligibilityRules))

    def hasAssessmentEligibilityRuleOpt(
      assessmentCategory: AssessmentCategory,
      rule:               AssessmentEligibilityRules => Option[Boolean]
    ): Boolean =
      chargeTypeAssessments
        .find(_.assessmentCategory == assessmentCategory)
        .exists(c => rule(c.assessmentEligibilityRules).contains(true))

    // N.B. be careful of the order below, the order matters
    (eligibilityRules, chargeTypeAssessments) match {
      case (_, e) if hasAssessmentEligibilityRule(AssessmentCategory.Standard, _.noDueDatesReached)          =>
        Some(NoDueDatesReached)
      case (_, e) if taxRegime == TaxRegime.Simp && e.isEmpty                                                => Some(NoDueDatesReached)
      case (_, e) if hasAssessmentEligibilityRule(AssessmentCategory.Standard, _.isLessThanMinDebtAllowance) =>
        Some(IsLessThanMinDebtAllowance)
      case (_, e) if hasAssessmentEligibilityRule(AssessmentCategory.Debts, _.isLessThanMinDebtAllowance)    =>
        Some(IsLessThanMinDebtAllowance)
      case (_, e)
          if hasAssessmentEligibilityRule(AssessmentCategory.Liabilities, _.isLessThanMinDebtAllowance) &&
            e.map(_.assessmentCategory) == List(AssessmentCategory.Liabilities) =>
        Some(IsLessThanMinDebtAllowance)
      case (e, _) if e.hasRlsOnAddress                                                                       => Some(HasRlsOnAddress)
      case (e, _) if e.moreThanOneReasonForIneligibility                                                     => Some(MultipleReasons)
      case (e1, e2)
          if e1.atLeastOneReasonForIneligibility &&
            e2.exists(_.assessmentEligibilityRules.atLeastOneReasonForIneligibility) =>
        Some(MultipleReasons)
      case (_, e) if e.exists(_.assessmentEligibilityRules.moreThanOneReasonForIneligibility)                => Some(MultipleReasons)
      case (_, e) if hasAssessmentEligibilityRule(AssessmentCategory.Standard, _.isMoreThanMaxDebtAllowance) =>
        Some(IsMoreThanMaxDebtAllowance)
      case (_, e) if hasAssessmentEligibilityRule(AssessmentCategory.Debts, _.isMoreThanMaxDebtAllowance)    =>
        Some(IsMoreThanMaxDebtAllowance)
      case (_, e)
          if hasAssessmentEligibilityRule(AssessmentCategory.Liabilities, _.isMoreThanMaxDebtAllowance) &&
            e.map(_.assessmentCategory) == List(AssessmentCategory.Liabilities) =>
        Some(IsMoreThanMaxDebtAllowance)
      case (_, e) if hasAssessmentEligibilityRuleOpt(AssessmentCategory.Standard, _.chargesOverMaxDebtAge)   =>
        Some(ChargesOverMaxDebtAge)
      case (_, e) if hasAssessmentEligibilityRuleOpt(AssessmentCategory.Debts, _.chargesOverMaxDebtAge)      =>
        Some(ChargesOverMaxDebtAge)
      case (e, _) if e.existingTTP                                                                           => Some(ExistingTtp)
      case (e, _) if e.missingFiledReturns                                                                   => Some(MissingFiledReturns)
      case (_, e) if e.exists(_.assessmentEligibilityRules.disallowedChargeLockTypes)                        => Some(DisallowedChargeLockTypes)
      case (e, _) if e.markedAsInsolvent                                                                     => Some(MarkedAsInsolvent)
      case (_, e) if e.exists(_.assessmentEligibilityRules.ineligibleChargeTypes)                            => Some(IneligibleChargeTypes)
      case (e, _) if e.hasInvalidInterestSignals.contains(true)                                              => Some(HasInvalidInterestSignals)
      case (e, _) if e.dmSpecialOfficeProcessingRequired.contains(true)                                      => Some(DmSpecialOfficeProcessingRequired)
      case (e, _) if e.cannotFindLockReason.contains(true)                                                   => Some(CannotFindLockReason)
      case (e, _) if e.creditsNotAllowed.contains(true)                                                      => Some(CreditsNotAllowed)
      case (e, _) if e.isMoreThanMaxPaymentReference.contains(true)                                          => Some(IsMoreThanMaxPaymentReference)
      case (e, _) if e.hasInvalidInterestSignalsCESA.contains(true)                                          => Some(HasInvalidInterestSignalsCESA)
      case (e, _) if e.hasDisguisedRemuneration.contains(true)                                               => Some(HasDisguisedRemuneration)
      case (e, _) if e.hasCapacitor.contains(true)                                                           => Some(HasCapacitor)
      case (e, _) if e.dmSpecialOfficeProcessingRequiredCDCS.contains(true)                                  =>
        Some(DmSpecialOfficeProcessingRequiredCDCS)
      case (e, _) if e.isAnMtdCustomer.contains(true)                                                        => Some(IsAnMtdCustomer)
      case (e, _) if e.dmSpecialOfficeProcessingRequiredCESA.contains(true)                                  =>
        Some(DmSpecialOfficeProcessingRequiredCESA)
      case (e, _) if e.noMtditsaEnrollment.contains(true)                                                    => Some(NoMtditsaEnrollment)
      case (e, _) if e.allChargeTypeAssessmentsFailed.contains(true)                                         => Some(AllChargeTypeAssessmentsFailed)
      case (e, _) if e.noValidPlanAfterAssessments.contains(true)                                            => Some(NoValidPlanAfterAssessments)
      case (_, e) if e.exists(_.assessmentEligibilityRules.chargesBeforeMaxAccountingDate.contains(true))    =>
        Some(ChargesBeforeMaxAccountingDate)
      case (_, _)                                                                                            => None // all false
    }
  }

}
