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

package testOnly.models

import enumeratum.{Enum, EnumEntry}
import essttp.journey.model.ttp.EligibilityRules
import play.api.libs.functional.syntax._
import play.api.libs.json.Format

import scala.collection.immutable

sealed trait EligibilityError extends EnumEntry

object EligibilityErrors extends Enum[EligibilityError] {

  case object HasRlsOnAddress extends EligibilityError

  case object MarkedAsInsolvent extends EligibilityError

  case object IsLessThanMinDebtAllowance extends EligibilityError

  case object IsMoreThanMaxDebtAllowance extends EligibilityError

  case object DisallowedChargeLocks extends EligibilityError

  case object ExistingTTP extends EligibilityError

  case object ExceedsMaxDebtAge extends EligibilityError

  case object EligibleChargeType extends EligibilityError

  case object MissingFiledReturns extends EligibilityError

  case object MultipleReasons extends EligibilityError

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  //TODO: try to refactor it and move those from the testonly package
  def toEligibilityError(eligibilityRules: EligibilityRules): EligibilityError = eligibilityRules match {
    case eligibilityRules if eligibilityRules.moreThanOneReasonForIneligibility          => MultipleReasons
    case EligibilityRules(true, false, false, false, false, false, false, false, false)  => HasRlsOnAddress
    case EligibilityRules(false, true, false, false, false, false, false, false, false)  => MarkedAsInsolvent
    case EligibilityRules(false, false, true, false, false, false, false, false, false)  => IsLessThanMinDebtAllowance
    case EligibilityRules(false, false, false, true, false, false, false, false, false)  => IsMoreThanMaxDebtAllowance
    case EligibilityRules(false, false, false, false, true, false, false, false, false)  => DisallowedChargeLocks
    case EligibilityRules(false, false, false, false, false, true, false, false, false)  => ExistingTTP
    case EligibilityRules(false, false, false, false, false, false, true, false, false)  => ExceedsMaxDebtAge
    case EligibilityRules(false, false, false, false, false, false, false, true, false)  => EligibleChargeType
    case EligibilityRules(false, false, false, false, false, false, false, false, true)  => MissingFiledReturns
    case EligibilityRules(false, false, false, false, false, false, false, false, false) => throw new UnsupportedOperationException("should not happen")
  }

  implicit val format: Format[EligibilityError] = implicitly[Format[String]].inmap(EligibilityErrors.withName, _.entryName)

}
