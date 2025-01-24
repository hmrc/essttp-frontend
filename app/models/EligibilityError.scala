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

import cats.Eq
import enumeratum.{Enum, EnumEntry}
import play.api.libs.functional.syntax._
import play.api.libs.json.Format
import essttp.rootmodel.ttp.eligibility.{EligibilityRules, EligibilityRulesPart1, EligibilityRulesPart2}

import scala.collection.immutable

sealed trait EligibilityError extends EnumEntry

object EligibilityError {
  implicit val eq: Eq[EligibilityError] = Eq.fromUniversalEquals
}

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

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  def toEligibilityError(eligibilityRules: EligibilityRules): Option[EligibilityError] =
    eligibilityRules match {
      case eligibilityRules if eligibilityRules.moreThanOneReasonForIneligibility => Some(MultipleReasons)
      case EligibilityRules(EligibilityRulesPart1(true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(HasRlsOnAddress)
      case EligibilityRules(EligibilityRulesPart1(_, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(MarkedAsInsolvent)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(NoDueDatesReached)
      case EligibilityRules(EligibilityRulesPart1(_, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(IsLessThanMinDebtAllowance)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(IsMoreThanMaxDebtAllowance)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(DisallowedChargeLockTypes)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(ExistingTtp)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(ChargesOverMaxDebtAge)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(IneligibleChargeTypes)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(MissingFiledReturns)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(HasInvalidInterestSignals)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(DmSpecialOfficeProcessingRequired)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(CannotFindLockReason)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(CreditsNotAllowed)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(IsMoreThanMaxPaymentReference)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _), EligibilityRulesPart2(_)) => Some(ChargesBeforeMaxAccountingDate)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _), EligibilityRulesPart2(_)) => Some(HasInvalidInterestSignalsCESA)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _), EligibilityRulesPart2(_)) => Some(HasDisguisedRemuneration)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _), EligibilityRulesPart2(_)) => Some(HasCapacitor)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _), EligibilityRulesPart2(_)) => Some(DmSpecialOfficeProcessingRequiredCDCS)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _), EligibilityRulesPart2(_)) => Some(IsAnMtdCustomer)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true)), EligibilityRulesPart2(_)) => Some(DmSpecialOfficeProcessingRequiredCESA)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(Some(true))) => Some(NoMtditsaEnrollment)
      case EligibilityRules(EligibilityRulesPart1(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), EligibilityRulesPart2(_)) => None //all false
    }

  implicit val format: Format[EligibilityError] = implicitly[Format[String]].inmap(EligibilityErrors.withName, _.entryName)

}
