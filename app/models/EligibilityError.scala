/*
 * Copyright 2022 HM Revenue & Customs
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
import essttp.rootmodel.ttp.EligibilityRules

import scala.collection.immutable

sealed trait EligibilityError extends EnumEntry

object EligibilityError {
  implicit val eq: Eq[EligibilityError] = Eq.fromUniversalEquals
}

object EligibilityErrors extends Enum[EligibilityError] {

  case object HasRlsOnAddress extends EligibilityError

  case object MarkedAsInsolvent extends EligibilityError

  case object IsLessThanMinDebtAllowance extends EligibilityError

  case object IsMoreThanMaxDebtAllowance extends EligibilityError

  case object DisallowedChargeLockTypes extends EligibilityError

  case object ExistingTtp extends EligibilityError

  case object ChargesOverMaxDebtAge extends EligibilityError

  case object IneligibleChargeTypes extends EligibilityError

  case object MissingFiledReturns extends EligibilityError

  case object HasInvalidInterestSignals extends EligibilityError

  case object DmSpecialOfficeProcessingRequired extends EligibilityError

  case object MultipleReasons extends EligibilityError

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  def toEligibilityError(eligibilityRules: EligibilityRules): Option[EligibilityError] = {

    val normalisedEligibilityRules: EligibilityRules = eligibilityRulesWithoutNone(eligibilityRules)

    normalisedEligibilityRules match {
      case eligibilityRules if eligibilityRules.moreThanOneReasonForIneligibility                                    => Some(MultipleReasons)
      case EligibilityRules(true, false, false, false, false, false, false, false, false, Some(false), Some(false))  => Some(HasRlsOnAddress)
      case EligibilityRules(false, true, false, false, false, false, false, false, false, Some(false), Some(false))  => Some(MarkedAsInsolvent)
      case EligibilityRules(false, false, true, false, false, false, false, false, false, Some(false), Some(false))  => Some(IsLessThanMinDebtAllowance)
      case EligibilityRules(false, false, false, true, false, false, false, false, false, Some(false), Some(false))  => Some(IsMoreThanMaxDebtAllowance)
      case EligibilityRules(false, false, false, false, true, false, false, false, false, Some(false), Some(false))  => Some(DisallowedChargeLockTypes)
      case EligibilityRules(false, false, false, false, false, true, false, false, false, Some(false), Some(false))  => Some(ExistingTtp)
      case EligibilityRules(false, false, false, false, false, false, true, false, false, Some(false), Some(false))  => Some(ChargesOverMaxDebtAge)
      case EligibilityRules(false, false, false, false, false, false, false, true, false, Some(false), Some(false))  => Some(IneligibleChargeTypes)
      case EligibilityRules(false, false, false, false, false, false, false, false, true, Some(false), Some(false))  => Some(MissingFiledReturns)
      case EligibilityRules(false, false, false, false, false, false, false, false, false, Some(true), Some(false))  => Some(HasInvalidInterestSignals)
      case EligibilityRules(false, false, false, false, false, false, false, false, false, Some(false), Some(true))  => Some(DmSpecialOfficeProcessingRequired)
      case EligibilityRules(false, false, false, false, false, false, false, false, false, Some(false), Some(false)) => None //all false
    }
  }

  def eligibilityRulesWithoutNone(eligibilityRules: EligibilityRules): EligibilityRules = {
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
