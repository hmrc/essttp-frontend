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

import enumeratum.{ Enum, EnumEntry }
import play.api.libs.json.Format
import play.api.libs.functional.syntax._

import scala.collection.immutable

sealed trait EligibilityError extends EnumEntry

object EligibilityError extends Enum[EligibilityError] {
  object DebtIsTooLarge extends EligibilityError {
    override val entryName = "Debt is too large"
  }
  object DebtIsTooOld extends EligibilityError {
    override val entryName = "Debt is too old"
  }
  object ReturnsAreNotUpToDate extends EligibilityError {
    override val entryName = "Returns are not up to date"
  }
  object YouAlreadyHaveAPaymentPlan extends EligibilityError {
    override val entryName = "you already have a payment plan"
  }
  object PayeIsInsolvent extends EligibilityError {
    override val entryName = "Paye is insolvent"
  }
  object PayeHasDisallowedCharges extends EligibilityError {
    override val entryName = "Paye has disallowed charges"
  }
  object RLSFlagIsSet extends EligibilityError {
    override val entryName = "RLS flag is set"
  }

  override val values: immutable.IndexedSeq[EligibilityError] = findValues

  implicit val format: Format[EligibilityError] = implicitly[Format[String]].inmap(EligibilityError.withName, _.entryName)

}
