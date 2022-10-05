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

import essttp.rootmodel.ttp.{EligibilityPass, EligibilityRules}
import essttp.rootmodel.{DayOfMonth, UpfrontPaymentAmount}
import testsupport.testdata.JourneyInfo.JourneyInfoAsJson
import uk.gov.hmrc.crypto.{Encrypter, PlainText}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

object TdJsonBodies {

  def encryptString(s: String, encrypter: Encrypter): String =
    encrypter.encrypt(
      PlainText("\"" + SensitiveString(s).decryptedValue + "\"")
    ).value

  object StartJourneyRequestBodies {
    val empty: String =
      """{
        |   "Empty": {}
        |}""".stripMargin
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

  def createJourneyJson(stageInfo: StageInfo, journeyInfo: List[String]): String = {
    val jsonFormatted: String = if (journeyInfo.isEmpty) "" else s",\n${journeyInfo.mkString(",")}"
    s"""
      |{
      |  "_id": "6284fcd33c00003d6b1f3903",
      |  "${stageInfo.stage}": {
      |    "stage": {
      |      "${stageInfo.stageValue}": {}
      |    },
      |    "createdOn": "2022-07-22T14:01:06.629Z",
      |    "_id": "6284fcd33c00003d6b1f3903",
      |    "origin": "Origins.Epaye.Bta",
      |    "sjRequest": {
      |      "Simple": {
      |        "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |        "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |      }
      |    },
      |    "sessionId": "IamATestSessionId",
      |    "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059"$jsonFormatted
      |  },
      |  "sessionId": "IamATestSessionId",
      |  "createdAt": "2022-07-22T14:01:06.629Z",
      |  "lastUpdated": "2022-07-22T14:01:06.629Z"
      |}
      |""".stripMargin
  }

  def taxIdJourneyInfo(taxId: String = "864FZ00049"): JourneyInfoAsJson =
    s"""
      |"taxId": {
      |      "value": "$taxId"
      |}
      |""".stripMargin

  def eligibilityCheckJourneyInfo(
      eligibilityPass:  EligibilityPass  = TdAll.eligibleEligibilityPass,
      eligibilityRules: EligibilityRules = TdAll.eligibleEligibilityRules,
      encrypter:        Encrypter
  ): JourneyInfoAsJson = {
    s"""
      |"eligibilityCheckResult" : {
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
      |          "addressPostcode": "${encryptString("AA11AA", encrypter)}",
      |          "postcodeDate": "2022-01-31"
      |        }
      |  ],
      |  "regimePaymentFrequency": "monthly",
      |  "paymentPlanFrequency": "monthly",
      |  "paymentPlanMinLength": 1,
      |  "paymentPlanMaxLength": 6,
      |  "eligibilityStatus" : {
      |    "eligibilityPass" : ${eligibilityPass.value}
      |  },
      |  "eligibilityRules" : {
      |    "hasRlsOnAddress" : ${eligibilityRules.hasRlsOnAddress},
      |    "markedAsInsolvent" : ${eligibilityRules.markedAsInsolvent},
      |    "isLessThanMinDebtAllowance" : ${eligibilityRules.isLessThanMinDebtAllowance},
      |    "isMoreThanMaxDebtAllowance" : ${eligibilityRules.isMoreThanMaxDebtAllowance},
      |    "disallowedChargeLockTypes" : ${eligibilityRules.disallowedChargeLockTypes},
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
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 55573
       |                           },
       |                           {
       |                               "dueDate" : "2022-08-28",
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

  def detailsAboutBankAccountJourneyInfo(typeOfAccount: String = "Business", isAccountHolder: Boolean = true): String =
    s"""
       |"detailsAboutBankAccount": {
       |  "typeOfBankAccount" : "$typeOfAccount",
       |  "isAccountHolder": $isAccountHolder
       |}""".stripMargin

  def directDebitDetailsJourneyInfo(encrypter: Encrypter): String =
    s"""
       |"directDebitDetails" : {
       |  "name" : "${encryptString("Bob Ross", encrypter)}",
       |  "sortCode" : "${encryptString("123456", encrypter)}",
       |  "accountNumber" : "${encryptString("12345678", encrypter)}"
       |}""".stripMargin

  def isEmailAddressRequiredJourneyInfo(isEmailAddressRequired: Boolean): String =
    s"""
       |"isEmailAddressRequired": $isEmailAddressRequired
       |""".stripMargin

  def arrangementResponseJourneyInfo(): String =
    s"""
       |"arrangementResponse" : {
       |  "processingDateTime": "2022-03-23T13:49:51.141Z",
       |  "customerReference": "123PA44545546"
       |}
       |""".stripMargin
}
