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

package testsupport.testdata

import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.eligibility.{EligibilityPass, EligibilityRules}

object TtpJsonResponses {

  def ttpEligibilityCallJson(
      taxRegime:                          TaxRegime,
      eligibilityPass:                    EligibilityPass  = TdAll.eligibleEligibilityPass,
      eligibilityRules:                   EligibilityRules = TdAll.eligibleEligibilityRules,
      poundsInsteadOfPence:               Boolean          = false,
      regimeDigitalCorrespondence:        Boolean          = false,
      maybeChargeIsInterestBearingCharge: Option[Boolean]  = None,
      maybeChargeUseChargeReference:      Option[Boolean]  = None,
      maybeChargeBeforeMaxAccountingDate: Option[Boolean]  = None
  ): String = {

    val isInterestBearingChargeValue = maybeChargeIsInterestBearingCharge match {
      case Some(bool) => s""""isInterestBearingCharge":${bool.toString},"""
      case None       => ""
    }

    val useChargeReferenceValue = maybeChargeUseChargeReference match {
      case Some(bool) => s""""useChargeReference":${bool.toString},"""
      case None       => ""
    }

    val chargeBeforeMaxAccountingDateValue = maybeChargeBeforeMaxAccountingDate match {
      case Some(bool) => s""""chargeBeforeMaxAccountingDate":${bool.toString},"""
      case None       => ""
    }

    s"""
       |{
       |  "processingDateTime": "2022-03-23T13:49:51.141Z",
       |  "identification": ${TdAll.identificationJsonString(taxRegime)},
       |  "customerPostcodes": [
       |        {
       |          "addressPostcode": "AA11AA",
       |          "postcodeDate": "2022-01-31"
       |        }
       |  ],
       |  "regimePaymentFrequency": "Monthly",
       |  "paymentPlanFrequency": "Monthly",
       |  "paymentPlanMinLength": 1,
       |  "paymentPlanMaxLength": 12,
       |  "eligibilityStatus" : {
       |    "eligibilityPass" : ${eligibilityPass.value.toString}
       |  },
       |  "eligibilityRules" : {
       |    "hasRlsOnAddress" : ${eligibilityRules.part1.hasRlsOnAddress.toString},
       |    "markedAsInsolvent" : ${eligibilityRules.part1.markedAsInsolvent.toString},
       |    "isLessThanMinDebtAllowance" : ${eligibilityRules.part1.isLessThanMinDebtAllowance.toString},
       |    "isMoreThanMaxDebtAllowance" : ${eligibilityRules.part1.isMoreThanMaxDebtAllowance.toString},
       |    "disallowedChargeLockTypes" : ${eligibilityRules.part1.disallowedChargeLockTypes.toString},
       |    "existingTTP" : ${eligibilityRules.part1.existingTTP.toString},
       |    "ineligibleChargeTypes" : ${eligibilityRules.part1.ineligibleChargeTypes.toString},
       |    "missingFiledReturns" : ${eligibilityRules.part1.missingFiledReturns.toString},
       |    "noDueDatesReached": ${eligibilityRules.part1.noDueDatesReached.toString}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.hasInvalidInterestSignals, "hasInvalidInterestSignals")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.hasInvalidInterestSignalsCESA, "hasInvalidInterestSignalsCESA")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.dmSpecialOfficeProcessingRequired, "dmSpecialOfficeProcessingRequired")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.cannotFindLockReason, "cannotFindLockReason")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.creditsNotAllowed, "creditsNotAllowed")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.isMoreThanMaxPaymentReference, "isMoreThanMaxPaymentReference")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.chargesBeforeMaxAccountingDate, "chargesBeforeMaxAccountingDate")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.chargesOverMaxDebtAge, "chargesOverMaxDebtAge")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.hasDisguisedRemuneration, "hasDisguisedRemuneration")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.hasCapacitor, "hasCapacitor")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.dmSpecialOfficeProcessingRequiredCDCS, "dmSpecialOfficeProcessingRequiredCDCS")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part1.dmSpecialOfficeProcessingRequiredCESA, "dmSpecialOfficeProcessingRequiredCESA")}
       |  ${optionalEligibilityResponsesJson(eligibilityRules.part2.noMtditsaEnrollment, "noMtditsaEnrollment")}
       |  },
       |  "chargeTypeAssessment" : [ {
       |    "taxPeriodFrom" : "2020-08-13",
       |    "taxPeriodTo" : "2020-08-14",
       |    "debtTotalAmount" : ${if (poundsInsteadOfPence) "3000.00" else "300000"},
       |    "chargeReference" : "A00000000001",
       |    "charges" : [ {
       |      "chargeType": "InYearRTICharge-Tax",
       |      "mainType": "InYearRTICharge(FPS)",
       |      "mainTrans" : "mainTrans",
       |      "subTrans" : "subTrans",
       |      "outstandingAmount" : ${if (poundsInsteadOfPence) "1000.00" else "100000"},
       |      "interestStartDate" : "2017-03-07",
       |      "dueDate" : "2017-03-07",
       |      "accruedInterest" : ${if (poundsInsteadOfPence) "15.97" else "1597"},
       |      "ineligibleChargeType": false,
       |      "chargeOverMaxDebtAge": false,
       |       "dueDateNotReached": false,
       |       $isInterestBearingChargeValue
       |       $useChargeReferenceValue
       |       $chargeBeforeMaxAccountingDateValue
       |       "locks": [ {
       |          "lockType": "Payment",
       |          "lockReason": "Risk/Fraud",
       |          "disallowedChargeLockType": false
       |       } ]
       |    } ]
       |  } ],
       |  ${if (regimeDigitalCorrespondence) { s""""regimeDigitalCorrespondence":true,""" } else ""}
       |  "futureChargeLiabilitiesExcluded": false
       |}
       |""".stripMargin
  }

  def optionalEligibilityResponsesJson(flag: Option[Boolean], flagName: String): String =
    flag.fold(""){ f => s""",  "$flagName": ${f.toString} """ }

  def ttpAffordabilityResponseJson(): String = {
    s"""
       |{
       |    "processingDateTime": "2022-03-23T13:49:51.141Z",
       |    "minimumInstalmentAmount": 33333,
       |    "maximumInstalmentAmount": 100000
       |}
       |""".stripMargin
  }

  def ttpAffordableQuotesResponseJson(): String =
    """
      |{
      |	"paymentPlans": [{
      |			"numberOfInstalments": 2,
      |			"planDuration": 2,
      |			"totalDebt": 111141,
      |			"totalDebtIncInt": 111147,
      |			"planInterest": 6,
      |			"collections": {
      |				"initialCollection": {
      |					"dueDate": "2022-07-03",
      |					"amountDue": 12312
      |				},
      |				"regularCollections": [{
      |						"dueDate": "2022-08-28",
      |						"amountDue": 55573
      |					},
      |					{
      |						"dueDate": "2022-09-28",
      |						"amountDue": 55573
      |					}
      |				]
      |			},
      |			"instalments": [{
      |					"instalmentNumber": 2,
      |					"dueDate": "2022-09-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 55571,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 55570,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				},
      |				{
      |					"instalmentNumber": 1,
      |					"dueDate": "2022-08-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 111141,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 55570,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				}
      |			]
      |		},
      |		{
      |			"numberOfInstalments": 3,
      |			"planDuration": 3,
      |			"totalDebt": 111141,
      |			"totalDebtIncInt": 111150,
      |			"planInterest": 9,
      |			"collections": {
      |				"initialCollection": {
      |					"dueDate": "2022-07-03",
      |					"amountDue": 12312
      |				},
      |				"regularCollections": [{
      |						"dueDate": "2022-08-28",
      |						"amountDue": 37050
      |					},
      |					{
      |						"dueDate": "2022-09-28",
      |						"amountDue": 37050
      |					},
      |					{
      |						"dueDate": "2022-10-28",
      |						"amountDue": 37050
      |					}
      |				]
      |			},
      |			"instalments": [{
      |					"instalmentNumber": 3,
      |					"dueDate": "2022-10-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 37047,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 37047,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				},
      |				{
      |					"instalmentNumber": 2,
      |					"dueDate": "2022-09-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 74094,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 37047,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				},
      |				{
      |					"instalmentNumber": 1,
      |					"dueDate": "2022-08-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 111141,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 37047,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				}
      |			]
      |		},
      |		{
      |			"numberOfInstalments": 4,
      |			"planDuration": 4,
      |			"totalDebt": 111141,
      |			"totalDebtIncInt": 111153,
      |			"planInterest": 12,
      |			"collections": {
      |				"initialCollection": {
      |					"dueDate": "2022-07-03",
      |					"amountDue": 12312
      |				},
      |				"regularCollections": [{
      |						"dueDate": "2022-08-28",
      |						"amountDue": 27788
      |					},
      |					{
      |						"dueDate": "2022-09-28",
      |						"amountDue": 27788
      |					},
      |					{
      |						"dueDate": "2022-10-28",
      |						"amountDue": 27788
      |					},
      |					{
      |						"dueDate": "2022-11-28",
      |						"amountDue": 27788
      |					}
      |				]
      |			},
      |			"instalments": [{
      |					"instalmentNumber": 4,
      |					"dueDate": "2022-11-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 27786,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 27785,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				},
      |				{
      |					"instalmentNumber": 3,
      |					"dueDate": "2022-10-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 55571,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 27785,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				},
      |				{
      |					"instalmentNumber": 2,
      |					"dueDate": "2022-09-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 83356,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 27785,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				},
      |				{
      |					"instalmentNumber": 1,
      |					"dueDate": "2022-08-28",
      |					"instalmentInterestAccrued": 3,
      |					"instalmentBalance": 111141,
      |					"debtItemChargeId": "A00000000001",
      |					"amountDue": 27785,
      |					"debtItemOriginalDueDate": "2021-07-28"
      |				}
      |			]
      |		}
      |	]
      |}
      |""".stripMargin

  def ttpEnactArrangementResponseJson(taxRegime: TaxRegime): String =
    s"""
       |{
       |  "processingDateTime": "2022-03-23T13:49:51.141Z",
       |  "customerReference": "${TdAll.customerReference(taxRegime).value}"
       |}
       |""".stripMargin

}
