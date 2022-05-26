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

import essttp.journey.model.ttp.{EligibilityRules, OverallEligibilityStatus}

object TdJsonBodies {

  object StartJourneyRequestBodies {
    val empty: String = "{}"
    val simple: String =
      """{
        |  "returnUrl": "http://localhost:9066/return",
        |  "backUrl":   "http://localhost:9066/back"
        |}""".stripMargin
  }

  object StartJourneyResponses {
    val bta: String =
      s"""{
         |  "nextUrl": "http://localhost:19001/set-up-a-payment-plan?traceId=33678917",
         |  "journeyId": "${TdAll.journeyId.value}"
         |}""".stripMargin
    val govUk: String = bta
    val detachedUrl: String = bta
  }

  def afterDetermineTaxIdJourneyJson(): String =
    """
      |{
      |  "ComputedTaxId": {
      |    "stage": {
      |      "ComputedTaxId": {}
      |    },
      |    "createdOn": "2022-05-17T13:28:52.261",
      |    "_id": "6284fcd33c00003d6b1f3903",
      |    "origin": "Origins.Epaye.Bta",
      |    "sjRequest": {
      |      "Simple": {
      |        "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |        "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |      }
      |    },
      |    "sessionId": "IamATestSessionId",
      |    "taxId": {
      |      "value": "123/456"
      |    }
      |  },
      |  "sessionId": "IamATestSessionId",
      |  "createdAt": "2022-05-17T13:28:52.261"
      |}
      |""".stripMargin

  def afterEligibilityCheckJourneyJson(
      overallEligibilityStatus: OverallEligibilityStatus = TdAll.eligibleOverallEligibilityStatus,
      eligibilityRules:         EligibilityRules         = TdAll.eligibleEligibilityRules
  ): String = {
    val stage: String = if (overallEligibilityStatus.value) "Eligible" else "Ineligible"
    s"""
       |{
       |  "EligibilityChecked" : {
       |    "stage" : {
       |      "$stage" : { }
       |    },
       |    "createdOn" : "2022-05-18T14:04:03.461",
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "origin" : "Origins.Epaye.Bta",
       |    "sjRequest" : {
       |      "Simple" : {
       |        "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |        "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |      }
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "eligibilityCheckResult" : {
       |      "idType" : "SSTTP",
       |      "idNumber" : "123/456",
       |      "regimeType" : "PAYE",
       |      "processingDate" : "2022-01-01",
       |      "customerDetails" : {
       |        "country" : "Narnia",
       |        "postCode" : "AA11AA"
       |      },
       |      "minPlanLengthMonths" : 1,
       |      "maxPlanLengthMonths" : 3,
       |      "eligibilityStatus" : {
       |        "overallEligibilityStatus" : ${overallEligibilityStatus.value}
       |      },
       |      "eligibilityRules" : {
       |        "hasRlsOnAddress" : ${eligibilityRules.hasRlsOnAddress},
       |        "markedAsInsolvent" : ${eligibilityRules.markedAsInsolvent},
       |        "isLessThanMinDebtAllowance" : ${eligibilityRules.isLessThanMinDebtAllowance},
       |        "isMoreThanMaxDebtAllowance" : ${eligibilityRules.isMoreThanMaxDebtAllowance},
       |        "disallowedChargeLocks" : ${eligibilityRules.disallowedChargeLocks},
       |        "existingTTP" : ${eligibilityRules.existingTTP},
       |        "exceedsMaxDebtAge" : ${eligibilityRules.exceedsMaxDebtAge},
       |        "eligibleChargeType" : ${eligibilityRules.eligibleChargeType},
       |        "missingFiledReturns" : ${eligibilityRules.missingFiledReturns}
       |      },
       |      "chargeTypeAssessment" : [ {
       |        "taxPeriodFrom" : "2020-08-13",
       |        "taxPeriodTo" : "2020-08-14",
       |        "debtTotalAmount" : 300000,
       |        "disallowedChargeLocks" : [ {
       |          "chargeId" : "A00000000001",
       |          "mainTrans" : "mainTrans",
       |          "mainTransDesc" : "mainTransDesc",
       |          "subTrans" : "subTrans",
       |          "subTransDesc" : "subTransDesc",
       |          "outstandingDebtAmount" : 100000,
       |          "interestStartDate" : "2017-03-07",
       |          "accruedInterestToDate" : 15.97,
       |          "chargeLocks" : {
       |            "paymentLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            },
       |            "clearingLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            },
       |            "interestLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            },
       |            "dunningLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            }
       |          }
       |        } ]
       |      } ]
       |    },
       |    "taxId" : {
       |      "value" : "123/456"
       |    }
       |  },
       |  "sessionId" : "IamATestSessionId",
       |  "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin
  }

  def ttpEligibilityCallJson(
      overallEligibilityStatus: OverallEligibilityStatus = TdAll.eligibleOverallEligibilityStatus,
      eligibilityRules:         EligibilityRules         = TdAll.eligibleEligibilityRules
  ): String = {
    s"""
       |{
       |  "idType" : "SSTTP",
       |  "idNumber" : "123/456",
       |  "regimeType" : "PAYE",
       |  "processingDate" : "2022-01-01",
       |  "customerDetails" : {
       |    "country" : "Narnia",
       |    "postCode" : "AA11AA"
       |  },
       |  "minPlanLengthMonths" : 1,
       |  "maxPlanLengthMonths" : 3,
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
       |    "exceedsMaxDebtAge" : ${eligibilityRules.exceedsMaxDebtAge},
       |    "eligibleChargeType" : ${eligibilityRules.eligibleChargeType},
       |    "missingFiledReturns" : ${eligibilityRules.missingFiledReturns}
       |  },
       |  "chargeTypeAssessment" : [ {
       |    "taxPeriodFrom" : "2020-08-13",
       |    "taxPeriodTo" : "2020-08-14",
       |    "debtTotalAmount" : 300000,
       |    "disallowedChargeLocks" : [ {
       |      "chargeId" : "A00000000001",
       |      "mainTrans" : "mainTrans",
       |      "mainTransDesc" : "mainTransDesc",
       |      "subTrans" : "subTrans",
       |      "subTransDesc" : "subTransDesc",
       |      "outstandingDebtAmount" : 100000,
       |      "interestStartDate" : "2017-03-07",
       |      "accruedInterestToDate" : 15.97,
       |      "chargeLocks" : {
       |        "paymentLock" : {
       |          "status" : false,
       |          "reason" : ""
       |        },
       |        "clearingLock" : {
       |          "status" : false,
       |          "reason" : ""
       |        },
       |        "interestLock" : {
       |          "status" : false,
       |          "reason" : ""
       |        },
       |        "dunningLock" : {
       |          "status" : false,
       |          "reason" : ""
       |        }
       |      }
       |    } ]
       |  } ]
       |}
       |""".stripMargin
  }

  def afterCanPayUpfrontJourneyJson(stageValue: String, canPayUpfrontValue: Boolean): String =
    s"""
       |{
       |   "AnsweredCanPayUpfront" : {
       |     "stage" : {
       |       "$stageValue" : { }
       |    },
       |    "createdOn" : "2022-05-18T14:04:03.461",
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "origin" : "Origins.Epaye.Bta",
       |    "sjRequest" : {
       |      "Simple" : {
       |        "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |        "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |      }
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "eligibilityCheckResult" : {
       |      "idType" : "SSTTP",
       |      "idNumber" : "123/456",
       |      "regimeType" : "PAYE",
       |      "processingDate" : "",
       |      "customerDetails" : {
       |        "country" : "Narnia",
       |        "postCode" : "AA11AA"
       |      },
       |      "minPlanLengthMonths" : 1,
       |      "maxPlanLengthMonths" : 3,
       |      "eligibilityStatus" : {
       |        "overallEligibilityStatus" : true
       |      },
       |      "eligibilityRules" : {
       |        "hasRlsOnAddress" : false,
       |        "markedAsInsolvent" : false,
       |        "isLessThanMinDebtAllowance" : false,
       |        "isMoreThanMaxDebtAllowance" : false,
       |        "disallowedChargeLocks" : false,
       |        "existingTTP" : false,
       |        "exceedsMaxDebtAge" : false,
       |        "eligibleChargeType" : false,
       |        "missingFiledReturns" : false
       |      },
       |      "chargeTypeAssessment" : [ {
       |        "taxPeriodFrom" : "2020-08-13",
       |        "taxPeriodTo" : "2020-08-14",
       |        "debtTotalAmount" : 300000,
       |        "disallowedChargeLocks" : [ {
       |          "chargeId" : "A00000000001",
       |          "mainTrans" : "mainTrans",
       |          "mainTransDesc" : "mainTransDesc",
       |          "subTrans" : "subTrans",
       |          "subTransDesc" : "subTransDesc",
       |          "outstandingDebtAmount" : 100000,
       |          "interestStartDate" : "2017-03-07",
       |          "accruedInterestToDate" : 15.97,
       |          "chargeLocks" : {
       |            "paymentLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            },
       |            "clearingLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            },
       |            "interestLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            },
       |            "dunningLock" : {
       |              "status" : false,
       |              "reason" : ""
       |            }
       |          }
       |        } ]
       |      } ]
       |    },
       |    "taxId" : {
       |      "value" : "123/456"
       |    },
       |    "canPayUpfront": ${canPayUpfrontValue}
       |  },
       |  "sessionId" : "IamATestSessionId",
       |  "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin
}