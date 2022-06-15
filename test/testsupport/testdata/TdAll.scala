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

package testsupport.testdata

import actions.EnrolmentDef
import essttp.journey.model.JourneyId
import essttp.journey.model.ttp.{EligibilityRules, OverallEligibilityStatus}
import essttp.rootmodel.{AmountInPence, CanPayUpfront, DayOfMonth, UpfrontPaymentAmount}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

object TdAll {

  val expectedServiceNamePaye: String = "Set up an Employers’ PAYE payment plan"

  val journeyId: JourneyId = JourneyId("6284fcd33c00003d6b1f3903")

  private val `IR-PAYE-TaxOfficeNumber`: EnrolmentDef = EnrolmentDef(enrolmentKey  = "IR-PAYE", identifierKey = "TaxOfficeNumber")
  private val `IR-PAYE-TaxOfficeReference`: EnrolmentDef = EnrolmentDef(enrolmentKey  = "IR-PAYE", identifierKey = "TaxOfficeReference")

  val payeEnrolment: Enrolment = Enrolment(
    key               = "IR-PAYE",
    identifiers       = List(
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeNumber`.identifierKey, "123"),
      EnrolmentIdentifier(`IR-PAYE-TaxOfficeReference`.identifierKey, "456")
    ),
    state             = "Activated",
    delegatedAuthRule = None
  )

  val unactivePayeEnrolment: Enrolment = payeEnrolment.copy(state = "Not Activated")

  val canPayUpfront: CanPayUpfront = CanPayUpfront(true)
  val canNotPayUpfront: CanPayUpfront = canPayUpfront.copy(false)

  val amountInPence: AmountInPence = AmountInPence(1000)
  val upfrontPaymentAmount: UpfrontPaymentAmount = UpfrontPaymentAmount(amountInPence)

  val eligibleOverallEligibilityStatus: OverallEligibilityStatus = OverallEligibilityStatus(true)
  val notEligibleOverallEligibilityStatus: OverallEligibilityStatus = eligibleOverallEligibilityStatus.copy(value = false)
  val eligibleEligibilityRules: EligibilityRules = EligibilityRules(
    hasRlsOnAddress            = false,
    markedAsInsolvent          = false,
    isLessThanMinDebtAllowance = false,
    isMoreThanMaxDebtAllowance = false,
    disallowedChargeLocks      = false,
    existingTTP                = false,
    exceedsMaxDebtAge          = false,
    eligibleChargeType         = false,
    missingFiledReturns        = false
  )
  val notEligibleHasRlsOnAddress: EligibilityRules = eligibleEligibilityRules.copy(hasRlsOnAddress = true)
  val notEligibleMarkedAsInsolvent: EligibilityRules = eligibleEligibilityRules.copy(markedAsInsolvent = true)
  val notEligibleIsLessThanMinDebtAllowance: EligibilityRules = eligibleEligibilityRules.copy(isLessThanMinDebtAllowance = true)
  val notEligibleIsMoreThanMaxDebtAllowance: EligibilityRules = eligibleEligibilityRules.copy(isMoreThanMaxDebtAllowance = true)
  val notEligibleDisallowedChargeLocks: EligibilityRules = eligibleEligibilityRules.copy(disallowedChargeLocks = true)
  val notEligibleExistingTTP: EligibilityRules = eligibleEligibilityRules.copy(existingTTP = true)
  val notEligibleExceedsMaxDebtAge: EligibilityRules = eligibleEligibilityRules.copy(exceedsMaxDebtAge = true)
  val notEligibleEligibleChargeType: EligibilityRules = eligibleEligibilityRules.copy(eligibleChargeType = true)
  val notEligibleMissingFiledReturns: EligibilityRules = eligibleEligibilityRules.copy(missingFiledReturns = true)
  def dayOfMonth(day: Int = 28): DayOfMonth = DayOfMonth(day)
}
