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

import enumeratum.EnumEntry
import play.api.libs.functional.syntax._
import enumeratum.Enum
import play.api.libs.json.Format
import cats.Eq
import essttp.rootmodel.ttp.eligibility.EligibilityRules

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

  case object HasInvalidInterestSignalsCESA extends EligibilityError

  case object DmSpecialOfficeProcessingRequired extends EligibilityError

  case object CannotFindLockReason extends EligibilityError

  case object CreditsNotAllowed extends EligibilityError

  case object IsMoreThanMaxPaymentReference extends EligibilityError

  case object MultipleReasons extends EligibilityError

  case object ChargesBeforeMaxAccountingDate extends EligibilityError

  case object HasDisguisedRemuneration extends EligibilityError

  case object HasCapacitor extends EligibilityError

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  def toEligibilityError(eligibilityRules: EligibilityRules): Option[EligibilityError] = {

    val normalisedEligibilityRules: EligibilityRules = eligibilityRulesWithoutNone(eligibilityRules)

    normalisedEligibilityRules match {
      case eligibilityRules if eligibilityRules.moreThanOneReasonForIneligibility             => Some(MultipleReasons)
      case EligibilityRules(true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)       => Some(HasRlsOnAddress)
      case EligibilityRules(_, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)       => Some(MarkedAsInsolvent)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, true, _, _, _, _, _, _)       => Some(NoDueDatesReached)
      case EligibilityRules(_, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)       => Some(IsLessThanMinDebtAllowance)
      case EligibilityRules(_, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)       => Some(IsMoreThanMaxDebtAllowance)
      case EligibilityRules(_, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _, _)       => Some(DisallowedChargeLockTypes)
      case EligibilityRules(_, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _, _, _)       => Some(ExistingTtp)
      case EligibilityRules(_, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _, _, _, _) => Some(ChargesOverMaxDebtAge)
      case EligibilityRules(_, _, _, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _, _)       => Some(IneligibleChargeTypes)
      case EligibilityRules(_, _, _, _, _, _, _, _, true, _, _, _, _, _, _, _, _, _, _)       => Some(MissingFiledReturns)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _, _) => Some(HasInvalidInterestSignals)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _, _) => Some(HasInvalidInterestSignalsCESA)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _, _, _) => Some(DmSpecialOfficeProcessingRequired)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _, _) => Some(CannotFindLockReason)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _, _) => Some(CreditsNotAllowed)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _, _) => Some(IsMoreThanMaxPaymentReference)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _, _) => Some(ChargesBeforeMaxAccountingDate)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true), _) => Some(HasDisguisedRemuneration)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, Some(true)) => Some(HasCapacitor)
      case EligibilityRules(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)          => None //all false
    }
  }

  private def eligibilityRulesWithoutNone(eligibilityRules: EligibilityRules): EligibilityRules = {
    val hasInvalidInterestSignalsAsSomeBoolean =
      eligibilityRules.hasInvalidInterestSignals.fold(Some(false))(isDefined => Some(isDefined))
    val dmSpecialOfficeProcessingRequiredAsSomeBoolean =
      eligibilityRules.dmSpecialOfficeProcessingRequired.fold(Some(false))(isDefined => Some(isDefined))

    eligibilityRules
      .copy(hasInvalidInterestSignals = hasInvalidInterestSignalsAsSomeBoolean)
      .copy(dmSpecialOfficeProcessingRequired = dmSpecialOfficeProcessingRequiredAsSomeBoolean)
  }

  implicit val format: Format[EligibilityError] = implicitly[Format[String]].inmap(EligibilityErrors.withName, _.entryName)

}
