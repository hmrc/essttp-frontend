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

package models.ttp

import play.api.libs.json.{Format, Json}

/**
 * This represents response from the Eligibylity API
 * https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=DTDT&title=Eligibility+API
 */
case class EligibilityResult(
    idType:                 String,
    idNumber:               String,
    regimeType:             String,
    processingDate:         String,
    customerDetails:        CustomerDetails,
    eligibilityStatus:      EligibilityStatus,
    eligibilityRules:       EligibilityRules,
    financialLimitBreached: FinancialLimitBreached,
    chargeTypeAssessment:   List[ChargeTypeAssessment]
) {

  val isEligible: Boolean = eligibilityStatus.overallEligibilityStatus
}

object EligibilityResult {
  implicit val fmt: Format[EligibilityResult] = Json.format[EligibilityResult]
}

case class CustomerDetails(
    country:  String,
    postCode: String
)

object CustomerDetails {
  implicit val fmt: Format[CustomerDetails] = Json.format[CustomerDetails]
}

case class EligibilityStatus(
    overallEligibilityStatus: Boolean,
    minPlanLengthMonths:      Int,
    maxPlanLengthMonths:      Int
)
object EligibilityStatus {
  implicit val fmt: Format[EligibilityStatus] = Json.format[EligibilityStatus]
}

case class EligibilityRules(
    rlsOnAddress:         Boolean,
    rlsReason:            String,
    markedAsInsolvent:    Boolean,
    minimumDebtAllowance: Boolean,
    maxDebtAllowance:     Boolean,
    disallowedChargeLock: Boolean,
    existingTTP:          Boolean,
    minInstalmentAmount:  Int,
    maxInstalmentAmount:  Int,
    maxDebtAge:           Boolean,
    eligibleChargeType:   Boolean,
    returnsFiled:         Boolean
)

object EligibilityRules {
  implicit val fmt: Format[EligibilityRules] = Json.format[EligibilityRules]
}

case class FinancialLimitBreached(
    status:           Boolean,
    calculatedAmount: Int
)

object FinancialLimitBreached {
  implicit val fmt: Format[FinancialLimitBreached] = Json.format[FinancialLimitBreached]
}

case class ChargeTypeAssessment(
    taxPeriodFrom:    String,
    taxPeriodTo:      String,
    debtTotalAmount:  Int,
    taxPeriodCharges: List[TaxPeriodCharges]
)

object ChargeTypeAssessment {
  implicit val fmt: Format[ChargeTypeAssessment] = Json.format[ChargeTypeAssessment]
}

case class TaxPeriodCharges(
    chargeId:              String,
    mainTrans:             String,
    mainTransDesc:         String,
    subTrans:              String,
    subTransDesc:          String,
    outstandingDebtAmount: Int,
    interestStartDate:     String,
    accruedInterestToDate: Double,
    disallowedCharge:      Boolean,
    chargeLocks:           ChargeLocks
)

object TaxPeriodCharges {
  implicit val fmt: Format[TaxPeriodCharges] = Json.format[TaxPeriodCharges]
}

case class PaymentLock(
    status: Boolean,
    reason: String
)

object PaymentLock {
  implicit val fmt: Format[PaymentLock] = Json.format[PaymentLock]
}

case class ChargeLocks(
    paymentLock:    PaymentLock,
    clearingLock:   PaymentLock,
    interestLock:   PaymentLock,
    dunningLock:    PaymentLock,
    disallowedLock: PaymentLock
)

object ChargeLocks {
  implicit val fmt: Format[ChargeLocks] = Json.format[ChargeLocks]
}
