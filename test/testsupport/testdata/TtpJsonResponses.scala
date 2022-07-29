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

import essttp.rootmodel.ttp.{EligibilityRules, OverallEligibilityStatus}

object TtpJsonResponses {

  def ttpEligibilityCallJson(
      overallEligibilityStatus: OverallEligibilityStatus = TdAll.eligibleOverallEligibilityStatus,
      eligibilityRules:         EligibilityRules         = TdAll.eligibleEligibilityRules
  ): String = {
    s"""
       |{
       |  "processingDateTime": "2022-03-23T13:49:51.141Z",
       |  "identification": [
       |    {
       |      "idType": "EMPREF",
       |      "idValue": "864FZ00049"
       |    },
       |    {
       |      "idType": "BROCS",
       |      "idValue": "123PA44545546"
       |    }
       |  ],
       |  "customerPostcodes": [
       |        {
       |          "addressPostcode": "AA11AA",
       |          "postcodeDate": "2022-01-31"
       |        }
       |  ],
       |  "regimePaymentFrequency": "Monthly",
       |  "paymentPlanFrequency": "Monthly",
       |  "paymentPlanMinLength": 1,
       |  "paymentPlanMaxLength": 6,
       |  "eligibilityStatus" : {
       |    "overallEligibilityStatus" : ${overallEligibilityStatus.value}
       |  },
       |  "eligibilityRules" : {
       |    "hasRlsOnAddress" : ${eligibilityRules.hasRlsOnAddress},
       |    "markedAsInsolvent" : ${eligibilityRules.markedAsInsolvent},
       |    "isLessThanMinDebtAllowance" : ${eligibilityRules.isLessThanMinDebtAllowance},
       |    "isMoreThanMaxDebtAllowance" : ${eligibilityRules.isMoreThanMaxDebtAllowance},
       |    "disallowedChargeLocks" : ${eligibilityRules.disallowedChargeLocks},
       |    "existingTTP" : ${eligibilityRules.existingTTP},
       |    "chargesOverMaxDebtAge" : ${eligibilityRules.chargesOverMaxDebtAge},
       |    "ineligibleChargeTypes" : ${eligibilityRules.ineligibleChargeTypes},
       |    "missingFiledReturns" : ${eligibilityRules.missingFiledReturns}
       |  },
       |  "chargeTypeAssessment" : [ {
       |    "taxPeriodFrom" : "2020-08-13",
       |    "taxPeriodTo" : "2020-08-14",
       |    "debtTotalAmount" : 300000,
       |    "charges" : [ {
       |      "chargeType": "InYearRTICharge-Tax",
       |      "mainType": "InYearRTICharge(FPS)",
       |      "chargeReference" : "A00000000001",
       |      "mainTrans" : "mainTrans",
       |      "subTrans" : "subTrans",
       |      "outstandingAmount" : 100000,
       |      "interestStartDate" : "2017-03-07",
       |      "dueDate" : "2017-03-07",
       |      "accruedInterest" : 1597,
       |      "ineligibleChargeType": false,
       |      "chargeOverMaxDebtAge": false,
       |      "locks": [ {
       |          "lockType": "Payment",
       |          "lockReason": "Risk/Fraud",
       |          "disallowedChargeLockType": false
       |       } ]
       |    } ]
       |  } ]
       |}
       |""".stripMargin
  }

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

  def ttpEnactArrangementResponseJson(): String =
    s"""
       |{
       |  "processingDateTime": "2022-03-23T13:49:51.141Z",
       |  "customerReference": "123PA44545546"
       |}
       |""".stripMargin

}
