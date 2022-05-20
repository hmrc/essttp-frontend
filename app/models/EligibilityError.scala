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
import _root_.essttp.journey.model.ttp._

import scala.collection.immutable

sealed trait EligibilityError extends EnumEntry

object EligibilityErrors extends Enum[EligibilityError] {

  case object HasRlsOnAddress extends EligibilityError

  case object MarkedAsInsolvent extends EligibilityError

  case object IsLessThanMinDebtAllowance extends EligibilityError

  case object IsMoreThanMaxDebtAllowance extends EligibilityError

  case object DisallowedChargeLocks extends EligibilityError

  case object ExistingTtp extends EligibilityError

  case object ExceedsMaxDebtAge extends EligibilityError

  case object EligibleChargeType extends EligibilityError

  case object MissingFiledReturns extends EligibilityError

  case object MultipleReasons extends EligibilityError

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  def toEligibilityError(eligibilityRules: EligibilityRules): Option[EligibilityError] = eligibilityRules match {
    case eligibilityRules if eligibilityRules.moreThanOneReasonForIneligibility          => Some(MultipleReasons)
    case EligibilityRules(true, false, false, false, false, false, false, false, false)  => Some(HasRlsOnAddress)
    case EligibilityRules(false, true, false, false, false, false, false, false, false)  => Some(MarkedAsInsolvent)
    case EligibilityRules(false, false, true, false, false, false, false, false, false)  => Some(IsLessThanMinDebtAllowance)
    case EligibilityRules(false, false, false, true, false, false, false, false, false)  => Some(IsMoreThanMaxDebtAllowance)
    case EligibilityRules(false, false, false, false, true, false, false, false, false)  => Some(DisallowedChargeLocks)
    case EligibilityRules(false, false, false, false, false, true, false, false, false)  => Some(ExistingTtp)
    case EligibilityRules(false, false, false, false, false, false, true, false, false)  => Some(ExceedsMaxDebtAge)
    case EligibilityRules(false, false, false, false, false, false, false, true, false)  => Some(EligibleChargeType)
    case EligibilityRules(false, false, false, false, false, false, false, false, true)  => Some(MissingFiledReturns)
    case EligibilityRules(false, false, false, false, false, false, false, false, false) => None //all false
  }

  implicit val format: Format[EligibilityError] = implicitly[Format[String]].inmap(EligibilityErrors.withName, _.entryName)

}
