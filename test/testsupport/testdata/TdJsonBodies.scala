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
import essttp.rootmodel.{DayOfMonth, UpfrontPaymentAmount}
import testsupport.testdata.JourneyInfo.JourneyInfoAsJson

import scala.annotation.tailrec

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

  def createJourneyJson(stageInfo: (String, String), journeyInfo: List[String]): String = {
      @tailrec
      def createJourneyInfoJson(remainingJsonInfo: List[String], jsonAsString: String): String = {
        remainingJsonInfo match {
          case Nil      => jsonAsString
          case h :: Nil => s"$jsonAsString, $h"
          case h :: t   => createJourneyInfoJson(t, s"$jsonAsString, $h")
        }
      }
    s"""
      |{
      |  "${stageInfo._1}": {
      |    "stage": {
      |      "${stageInfo._2}": {}
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
      |    "sessionId": "IamATestSessionId"
      |    ${createJourneyInfoJson(journeyInfo, "")}
      |  },
      |  "sessionId": "IamATestSessionId",
      |  "createdAt": "2022-05-17T13:28:52.261"
      |}
      |""".stripMargin
  }

  def taxIdJourneyInfo(taxId: String = "123/456"): JourneyInfoAsJson =
    s"""
      |"taxId": {
      |      "value": "$taxId"
      |}
      |""".stripMargin

  def eligibilityCheckJourneyInfo(
      overallEligibilityStatus: OverallEligibilityStatus = TdAll.eligibleOverallEligibilityStatus,
      eligibilityRules:         EligibilityRules         = TdAll.eligibleEligibilityRules
  ): JourneyInfoAsJson = {
    s"""
      |"eligibilityCheckResult" : {
      |      "idType" : "SSTTP",
      |      "idNumber" : "123/456",
      |      "regimeType" : "PAYE",
      |      "processingDate" : "2022-01-01",
      |      "customerPostcodes": [
      |        {
      |          "addressPostcode": "AA11AA",
      |          "postcodeDate": "2022-01-31"
      |        }
      |      ],
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
      |          "accruedInterestToDate" : 1597,
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
      |    }
      |""".stripMargin
  }

  def canPayUpfrontJourneyInfo(canPayUpfront: Boolean): String = s""""canPayUpfront": $canPayUpfront"""

  def upfrontPaymentAmountJourneyInfo(upfrontPaymentAmount: UpfrontPaymentAmount): String = s""""upfrontPaymentAmount": ${upfrontPaymentAmount.value.value}"""

  def upfrontPaymentAnswersJourneyInfo(upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 200}}"""): String =
    s""""upfrontPaymentAnswers" : $upfrontPaymentAmountJsonString"""

  def extremeDatesJourneyInfo(): String =
    s"""
        |"extremeDatesResponse": {
        |  "initialPaymentDate": "2022-06-24",
        |  "earliestPlanStartDate": "2022-07-14",
        |  "latestPlanStartDate": "2022-08-13"
        |}""".stripMargin

  def affordabilityResultJourneyInfo(minimumInstalmentAmount: Int = 29997): String =
    s"""
       |"instalmentAmounts": {
       |   "minimumInstalmentAmount": $minimumInstalmentAmount,
       |   "maximumInstalmentAmount": 87944
       |}
       |""".stripMargin

  def monthlyPaymentAmountJourneyInfo: String = """"monthlyPaymentAmount": 30000"""

  def dayOfMonthJourneyInfo(dayOfMonth: DayOfMonth): String = s""""dayOfMonth": ${dayOfMonth.value}"""

  def startDatesJourneyInfo: String =
    s"""
       |"startDatesResponse" : {
       |   "initialPaymentDate" : "2022-07-03",
       |   "instalmentStartDate" : "2022-07-28"
       |}
       |""".stripMargin

  def affordableQuotesJourneyInfo: String =
    s"""
       |"affordableQuotesResponse" : {
       |            "paymentPlans" : [
       |                {
       |                    "numberOfInstalments" : 2,
       |                    "planDuration" : 2,
       |                    "totalDebt" : 111141,
       |                    "totalDebtIncInt" : 111147,
       |                    "planInterest" : 6,
       |                    "collections" : {
       |                        "initialCollection" : {
       |                            "dueDate" : "2022-07-03",
       |                            "amountDue" : 12312
       |                        },
       |                        "regularCollections" : [
       |                            {
       |                                "dueDate" : "2022-08-28",
       |                                "amountDue" : 55573
       |                            },
       |                            {
       |                                "dueDate" : "2022-09-28",
       |                                "amountDue" : 55573
       |                            }
       |                        ]
       |                    },
       |                    "instalments" : [
       |                        {
       |                            "instalmentNumber" : 2,
       |                            "dueDate" : "2022-09-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 55571,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 55570,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        },
       |                        {
       |                            "instalmentNumber" : 1,
       |                            "dueDate" : "2022-08-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 111141,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 55570,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        }
       |                    ]
       |                },
       |                {
       |                    "numberOfInstalments" : 3,
       |                    "planDuration" : 3,
       |                    "totalDebt" : 111141,
       |                    "totalDebtIncInt" : 111150,
       |                    "planInterest" : 9,
       |                    "collections" : {
       |                        "initialCollection" : {
       |                            "dueDate" : "2022-07-03",
       |                            "amountDue" : 12312
       |                        },
       |                        "regularCollections" : [
       |                            {
       |                                "dueDate" : "2022-08-28",
       |                                "amountDue" : 37050
       |                            },
       |                            {
       |                                "dueDate" : "2022-09-28",
       |                                "amountDue" : 37050
       |                            },
       |                            {
       |                                "dueDate" : "2022-10-28",
       |                                "amountDue" : 37050
       |                            }
       |                        ]
       |                    },
       |                    "instalments" : [
       |                        {
       |                            "instalmentNumber" : 3,
       |                            "dueDate" : "2022-10-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 37047,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 37047,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        },
       |                        {
       |                            "instalmentNumber" : 2,
       |                            "dueDate" : "2022-09-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 74094,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 37047,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        },
       |                        {
       |                            "instalmentNumber" : 1,
       |                            "dueDate" : "2022-08-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 111141,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 37047,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        }
       |                    ]
       |                },
       |                {
       |                    "numberOfInstalments" : 4,
       |                    "planDuration" : 4,
       |                    "totalDebt" : 111141,
       |                    "totalDebtIncInt" : 111153,
       |                    "planInterest" : 12,
       |                    "collections" : {
       |                        "initialCollection" : {
       |                            "dueDate" : "2022-07-03",
       |                            "amountDue" : 12312
       |                        },
       |                        "regularCollections" : [
       |                            {
       |                                "dueDate" : "2022-08-28",
       |                                "amountDue" : 27788
       |                            },
       |                            {
       |                                "dueDate" : "2022-09-28",
       |                                "amountDue" : 27788
       |                            },
       |                            {
       |                                "dueDate" : "2022-10-28",
       |                                "amountDue" : 27788
       |                            },
       |                            {
       |                                "dueDate" : "2022-11-28",
       |                                "amountDue" : 27788
       |                            }
       |                        ]
       |                    },
       |                    "instalments" : [
       |                        {
       |                            "instalmentNumber" : 4,
       |                            "dueDate" : "2022-11-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 27786,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 27785,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        },
       |                        {
       |                            "instalmentNumber" : 3,
       |                            "dueDate" : "2022-10-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 55571,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 27785,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        },
       |                        {
       |                            "instalmentNumber" : 2,
       |                            "dueDate" : "2022-09-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 83356,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 27785,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        },
       |                        {
       |                            "instalmentNumber" : 1,
       |                            "dueDate" : "2022-08-28",
       |                            "instalmentInterestAccrued" : 3,
       |                            "instalmentBalance" : 111141,
       |                            "debtItemChargeId" : "A00000000001",
       |                            "amountDue" : 27785,
       |                            "debtItemOriginalDueDate" : "2021-07-28"
       |                        }
       |                    ]
       |                }
       |            ]
       |        }
       |""".stripMargin

  def selectedPlanJourneyInfo: String =
    s"""
       |"selectedPaymentPlan": {
       |                   "numberOfInstalments" : 2,
       |                   "planDuration" : 2,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111147,
       |                   "planInterest" : 6,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 55573
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 55573
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 55571,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 55570,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 55570,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               }
       |""".stripMargin

  def directDebitDetailsJourneyInfo(isAccountHolder: Boolean = true): String =
    s"""
       |"directDebitDetails" : {
       |  "bankDetails" : {
       |    "name" : "Bob Ross",
       |    "sortCode" : "123456",
       |    "accountNumber" : "12345678"
       |  },
       |  "isAccountHolder" : $isAccountHolder
       |}""".stripMargin

}
