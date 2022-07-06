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
       |  "customerPostcodes": [
       |        {
       |          "addressPostcode": "AA11AA",
       |          "postcodeDate": "2022-01-31"
       |        }
       |  ],
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
       |      "accruedInterestToDate" : 1597,
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
       |      "customerPostcodes": [
       |        {
       |          "addressPostcode": "AA11AA",
       |          "postcodeDate": "2022-01-31"
       |        }
       |      ],
       |      "minPlanLengthMonths" : 1,
       |      "maxPlanLengthMonths" : 6,
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

  def afterUpfrontPaymentAmountJourneyJson(upfrontPaymentAmount: UpfrontPaymentAmount): String =
    s"""
       |{
       |   "EnteredUpfrontPaymentAmount" : {
       |     "stage" : {
       |       "EnteredUpfrontPaymentAmount" : { }
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
       |      "customerPostcodes": [
       |        {
       |          "addressPostcode": "AA11AA",
       |          "postcodeDate": "2022-01-31"
       |        }
       |      ],
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
       |    },
       |    "taxId" : {
       |      "value" : "123/456"
       |    },
       |    "canPayUpfront": true,
       |    "upfrontPaymentAmount": ${upfrontPaymentAmount.value.value}
       |  },
       |  "sessionId" : "IamATestSessionId",
       |  "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin

  def afterExtremeDatesJourneyJson(): String =
    s"""{
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "RetrievedExtremeDates" : {
       |        "stage" : {
       |            "ExtremeDatesResponseRetrieved" : {
       |
       |            }
       |        },
       |        "createdOn" : "2022-06-14T10:28:40.3",
       |        "_id" : "6284fcd33c00003d6b1f3903",
       |        "extremeDatesResponse" : {
       |            "initialPaymentDate" : "2022-06-24",
       |            "earliestPlanStartDate" : "2022-07-14",
       |            "latestPlanStartDate" : "2022-08-13"
       |        },
       |        "origin" : "Origins.Epaye.Bta",
       |        "sjRequest" : {
       |            "Simple" : {
       |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |            }
       |        },
       |        "sessionId" : "IamATestSessionId",
       |        "eligibilityCheckResult" : {
       |            "idType" : "SSTTP",
       |            "idNumber" : "868/GZ93987",
       |            "regimeType" : "PAYE",
       |            "processingDate" : "2022-01-31",
       |            "customerPostcodes" : [
       |                {
       |                    "addressPostcode" : "AA11AA",
       |                    "postcodeDate" : "2022-01-01"
       |                }
       |            ],
       |            "minPlanLengthMonths" : 1,
       |            "maxPlanLengthMonths" : 3,
       |            "eligibilityStatus" : {
       |                "overallEligibilityStatus" : true
       |            },
       |            "eligibilityRules" : {
       |                "hasRlsOnAddress" : false,
       |                "markedAsInsolvent" : false,
       |                "isLessThanMinDebtAllowance" : false,
       |                "isMoreThanMaxDebtAllowance" : false,
       |                "disallowedChargeLocks" : false,
       |                "existingTTP" : false,
       |                "exceedsMaxDebtAge" : false,
       |                "eligibleChargeType" : false,
       |                "missingFiledReturns" : false
       |            },
       |            "chargeTypeAssessment" : [
       |                {
       |                    "taxPeriodFrom" : "2020-08-13",
       |                    "taxPeriodTo" : "2020-08-14",
       |                    "debtTotalAmount" : 12301,
       |                    "disallowedChargeLocks" : [
       |                        {
       |                            "chargeId" : "A00000000001",
       |                            "mainTrans" : "mainTrans",
       |                            "mainTransDesc" : "mainTransDesc",
       |                            "subTrans" : "subTrans",
       |                            "subTransDesc" : "subTransDesc",
       |                            "outstandingDebtAmount" : 100000,
       |                            "interestStartDate" : "2017-03-07",
       |                            "accruedInterestToDate" : 1597,
       |                            "chargeLocks" : {
       |                                "paymentLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "clearingLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "interestLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "dunningLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                }
       |                            }
       |                        }
       |                    ]
       |                }
       |            ]
       |        },
       |        "upfrontPaymentAnswers" : {
       |            "DeclaredUpfrontPayment" : {
       |                "amount" : 2000
       |            }
       |        },
       |        "taxId" : {
       |            "value" : "868/GZ93987"
       |        }
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "createdAt" : "2022-06-14T10:28:40.3"
       |}""".stripMargin

  def ttpAffordabilityResponseJson(): String = {
    s"""
       |{
       |    "minimumInstalmentAmount": 33333,
       |    "maximumInstalmentAmount": 100000
       |}
       |""".stripMargin
  }

  def afterAffordabilityCheckJourneyJson(minimumInstalmentAmount: Int = 29997): String =
    s"""
      |{
      |    "_id" : "6284fcd33c00003d6b1f3903",
      |    "RetrievedAffordabilityResult" : {
      |        "stage" : {
      |            "RetrievedAffordabilityResult" : {
      |
      |            }
      |        },
      |        "createdOn" : "2022-05-18T14:04:03.461",
      |        "instalmentAmounts" : {
      |            "minimumInstalmentAmount" : ${minimumInstalmentAmount},
      |            "maximumInstalmentAmount" : 87944
      |        },
      |        "_id" : "6284fcd33c00003d6b1f3903",
      |        "extremeDatesResponse" : {
      |            "initialPaymentDate" : "2022-06-23",
      |            "earliestPlanStartDate" : "2022-07-13",
      |            "latestPlanStartDate" : "2022-08-12"
      |        },
      |        "origin" : "Origins.Epaye.Bta",
      |        "sjRequest" : {
      |            "Simple" : {
      |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |            }
      |        },
      |        "sessionId" : "IamATestSessionId",
      |        "eligibilityCheckResult" : {
      |            "idType" : "SSTTP",
      |            "idNumber" : "840/GZ00064",
      |            "regimeType" : "PAYE",
      |            "processingDate" : "2022-01-31",
      |            "customerPostcodes" : [
      |                {
      |                    "addressPostcode" : "AA11AA",
      |                    "postcodeDate" : "2022-01-01"
      |                }
      |            ],
      |            "minPlanLengthMonths" : 1,
      |            "maxPlanLengthMonths" : 3,
      |            "eligibilityStatus" : {
      |                "overallEligibilityStatus" : true
      |            },
      |            "eligibilityRules" : {
      |                "hasRlsOnAddress" : false,
      |                "markedAsInsolvent" : false,
      |                "isLessThanMinDebtAllowance" : false,
      |                "isMoreThanMaxDebtAllowance" : false,
      |                "disallowedChargeLocks" : false,
      |                "existingTTP" : false,
      |                "exceedsMaxDebtAge" : false,
      |                "eligibleChargeType" : false,
      |                "missingFiledReturns" : false
      |            },
      |            "chargeTypeAssessment" : [
      |                {
      |                    "taxPeriodFrom" : "2020-08-13",
      |                    "taxPeriodTo" : "2020-08-14",
      |                    "debtTotalAmount" : 1000000,
      |                    "disallowedChargeLocks" : [
      |                        {
      |                            "chargeId" : "A00000000001",
      |                            "mainTrans" : "mainTrans",
      |                            "mainTransDesc" : "mainTransDesc",
      |                            "subTrans" : "subTrans",
      |                            "subTransDesc" : "subTransDesc",
      |                            "outstandingDebtAmount" : 100000,
      |                            "interestStartDate" : "2017-03-07",
      |                            "accruedInterestToDate" : 1597,
      |                            "chargeLocks" : {
      |                                "paymentLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "clearingLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "interestLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "dunningLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                }
      |                            }
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        "upfrontPaymentAnswers" : {
      |            "DeclaredUpfrontPayment" : {
      |                "amount" : 12312
      |            }
      |        },
      |        "taxId" : {
      |            "value" : "840/GZ00064"
      |        }
      |    },
      |    "sessionId" : "IamATestSessionId",
      |    "createdAt" : "2022-05-18T14:04:03.461"
      |}
      |""".stripMargin

  def afterMonthlyPaymentAmountJourneyJson(): String =
    """
      |{
      |    "_id" : "6284fcd33c00003d6b1f3903",
      |    "EnteredMonthlyPaymentAmount" : {
      |        "stage" : {
      |            "EnteredMonthlyPaymentAmount" : {}
      |        },
      |        "createdOn" : "2022-05-18T14:04:03.461",
      |        "instalmentAmounts" : {
      |            "minimumInstalmentAmount" : 29997,
      |            "maximumInstalmentAmount" : 87944
      |        },
      |        "_id" : "6284fcd33c00003d6b1f3903",
      |        "extremeDatesResponse" : {
      |            "initialPaymentDate" : "2022-06-23",
      |            "earliestPlanStartDate" : "2022-07-13",
      |            "latestPlanStartDate" : "2022-08-12"
      |        },
      |        "origin" : "Origins.Epaye.Bta",
      |        "sjRequest" : {
      |            "Simple" : {
      |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |            }
      |        },
      |        "sessionId" : "IamATestSessionId",
      |        "eligibilityCheckResult" : {
      |            "idType" : "SSTTP",
      |            "idNumber" : "840/GZ00064",
      |            "regimeType" : "PAYE",
      |            "processingDate" : "2022-01-31",
      |            "customerPostcodes" : [
      |                {
      |                    "addressPostcode" : "AA11AA",
      |                    "postcodeDate" : "2022-01-01"
      |                }
      |            ],
      |            "minPlanLengthMonths" : 1,
      |            "maxPlanLengthMonths" : 3,
      |            "eligibilityStatus" : {
      |                "overallEligibilityStatus" : true
      |            },
      |            "eligibilityRules" : {
      |                "hasRlsOnAddress" : false,
      |                "markedAsInsolvent" : false,
      |                "isLessThanMinDebtAllowance" : false,
      |                "isMoreThanMaxDebtAllowance" : false,
      |                "disallowedChargeLocks" : false,
      |                "existingTTP" : false,
      |                "exceedsMaxDebtAge" : false,
      |                "eligibleChargeType" : false,
      |                "missingFiledReturns" : false
      |            },
      |            "chargeTypeAssessment" : [
      |                {
      |                    "taxPeriodFrom" : "2020-08-13",
      |                    "taxPeriodTo" : "2020-08-14",
      |                    "debtTotalAmount" : 1000000,
      |                    "disallowedChargeLocks" : [
      |                        {
      |                            "chargeId" : "A00000000001",
      |                            "mainTrans" : "mainTrans",
      |                            "mainTransDesc" : "mainTransDesc",
      |                            "subTrans" : "subTrans",
      |                            "subTransDesc" : "subTransDesc",
      |                            "outstandingDebtAmount" : 100000,
      |                            "interestStartDate" : "2017-03-07",
      |                            "accruedInterestToDate" : 1597,
      |                            "chargeLocks" : {
      |                                "paymentLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "clearingLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "interestLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "dunningLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                }
      |                            }
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        "upfrontPaymentAnswers" : {
      |            "DeclaredUpfrontPayment" : {
      |                "amount" : 12312
      |            }
      |        },
      |        "taxId" : {
      |            "value" : "840/GZ00064"
      |        },
      |        "monthlyPaymentAmount": 30000
      |    },
      |    "sessionId" : "IamATestSessionId",
      |    "createdAt" : "2022-05-18T14:04:03.461"
      |}
      |""".stripMargin

  def afterDayOfMonthJourneyJson(dayOfMonth: DayOfMonth): String =
    s"""
      |{
      |    "_id" : "6284fcd33c00003d6b1f3903",
      |    "EnteredDayOfMonth" : {
      |        "stage" : {
      |            "EnteredDayOfMonth" : {}
      |        },
      |        "createdOn" : "2022-05-18T14:04:03.461",
      |        "instalmentAmounts" : {
      |            "minimumInstalmentAmount" : 29997,
      |            "maximumInstalmentAmount" : 87944
      |        },
      |        "_id" : "6284fcd33c00003d6b1f3903",
      |        "extremeDatesResponse" : {
      |            "initialPaymentDate" : "2022-06-23",
      |            "earliestPlanStartDate" : "2022-07-13",
      |            "latestPlanStartDate" : "2022-08-12"
      |        },
      |        "origin" : "Origins.Epaye.Bta",
      |        "sjRequest" : {
      |            "Simple" : {
      |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |            }
      |        },
      |        "sessionId" : "IamATestSessionId",
      |        "eligibilityCheckResult" : {
      |            "idType" : "SSTTP",
      |            "idNumber" : "840/GZ00064",
      |            "regimeType" : "PAYE",
      |            "processingDate" : "2022-01-31",
      |            "customerPostcodes" : [
      |                {
      |                    "addressPostcode" : "AA11AA",
      |                    "postcodeDate" : "2022-01-01"
      |                }
      |            ],
      |            "minPlanLengthMonths" : 1,
      |            "maxPlanLengthMonths" : 3,
      |            "eligibilityStatus" : {
      |                "overallEligibilityStatus" : true
      |            },
      |            "eligibilityRules" : {
      |                "hasRlsOnAddress" : false,
      |                "markedAsInsolvent" : false,
      |                "isLessThanMinDebtAllowance" : false,
      |                "isMoreThanMaxDebtAllowance" : false,
      |                "disallowedChargeLocks" : false,
      |                "existingTTP" : false,
      |                "exceedsMaxDebtAge" : false,
      |                "eligibleChargeType" : false,
      |                "missingFiledReturns" : false
      |            },
      |            "chargeTypeAssessment" : [
      |                {
      |                    "taxPeriodFrom" : "2020-08-13",
      |                    "taxPeriodTo" : "2020-08-14",
      |                    "debtTotalAmount" : 1000000,
      |                    "disallowedChargeLocks" : [
      |                        {
      |                            "chargeId" : "A00000000001",
      |                            "mainTrans" : "mainTrans",
      |                            "mainTransDesc" : "mainTransDesc",
      |                            "subTrans" : "subTrans",
      |                            "subTransDesc" : "subTransDesc",
      |                            "outstandingDebtAmount" : 100000,
      |                            "interestStartDate" : "2017-03-07",
      |                            "accruedInterestToDate" : 1597,
      |                            "chargeLocks" : {
      |                                "paymentLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "clearingLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "interestLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "dunningLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                }
      |                            }
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        "upfrontPaymentAnswers" : {
      |            "DeclaredUpfrontPayment" : {
      |                "amount" : 12312
      |            }
      |        },
      |        "taxId" : {
      |            "value" : "840/GZ00064"
      |        },
      |        "monthlyPaymentAmount": 30000,
      |        "dayOfMonth": ${dayOfMonth.value}
      |    },
      |    "sessionId" : "IamATestSessionId",
      |    "createdAt" : "2022-05-18T14:04:03.461"
      |}
      |""".stripMargin

  def afterStartDatesJourneyJson(): String =
    s"""
      |{
      |    "_id" : "6284fcd33c00003d6b1f3903",
      |    "RetrievedStartDates" : {
      |        "stage" : {
      |            "StartDatesResponseRetrieved" : {}
      |        },
      |        "createdOn" : "2022-05-18T14:04:03.461",
      |        "instalmentAmounts" : {
      |            "minimumInstalmentAmount" : 29997,
      |            "maximumInstalmentAmount" : 87944
      |        },
      |        "_id" : "6284fcd33c00003d6b1f3903",
      |        "extremeDatesResponse" : {
      |            "initialPaymentDate" : "2022-06-23",
      |            "earliestPlanStartDate" : "2022-07-13",
      |            "latestPlanStartDate" : "2022-08-12"
      |        },
      |        "origin" : "Origins.Epaye.Bta",
      |        "sjRequest" : {
      |            "Simple" : {
      |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |            }
      |        },
      |        "sessionId" : "IamATestSessionId",
      |        "eligibilityCheckResult" : {
      |            "idType" : "SSTTP",
      |            "idNumber" : "840/GZ00064",
      |            "regimeType" : "PAYE",
      |            "processingDate" : "2022-01-31",
      |            "customerPostcodes" : [
      |                {
      |                    "addressPostcode" : "AA11AA",
      |                    "postcodeDate" : "2022-01-01"
      |                }
      |            ],
      |            "minPlanLengthMonths" : 1,
      |            "maxPlanLengthMonths" : 3,
      |            "eligibilityStatus" : {
      |                "overallEligibilityStatus" : true
      |            },
      |            "eligibilityRules" : {
      |                "hasRlsOnAddress" : false,
      |                "markedAsInsolvent" : false,
      |                "isLessThanMinDebtAllowance" : false,
      |                "isMoreThanMaxDebtAllowance" : false,
      |                "disallowedChargeLocks" : false,
      |                "existingTTP" : false,
      |                "exceedsMaxDebtAge" : false,
      |                "eligibleChargeType" : false,
      |                "missingFiledReturns" : false
      |            },
      |            "chargeTypeAssessment" : [
      |                {
      |                    "taxPeriodFrom" : "2020-08-13",
      |                    "taxPeriodTo" : "2020-08-14",
      |                    "debtTotalAmount" : 1000000,
      |                    "disallowedChargeLocks" : [
      |                        {
      |                            "chargeId" : "A00000000001",
      |                            "mainTrans" : "mainTrans",
      |                            "mainTransDesc" : "mainTransDesc",
      |                            "subTrans" : "subTrans",
      |                            "subTransDesc" : "subTransDesc",
      |                            "outstandingDebtAmount" : 100000,
      |                            "interestStartDate" : "2017-03-07",
      |                            "accruedInterestToDate" : 1597,
      |                            "chargeLocks" : {
      |                                "paymentLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "clearingLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "interestLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "dunningLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                }
      |                            }
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        "upfrontPaymentAnswers" : {
      |            "DeclaredUpfrontPayment" : {
      |                "amount" : 12312
      |            }
      |        },
      |        "taxId" : {
      |            "value" : "840/GZ00064"
      |        },
      |        "monthlyPaymentAmount": 30000,
      |        "dayOfMonth": 1,
      |         "startDatesResponse" : {
      |            "initialPaymentDate" : "2022-07-03",
      |            "instalmentStartDate" : "2022-07-28"
      |         }
      |    },
      |    "sessionId" : "IamATestSessionId",
      |    "createdAt" : "2022-05-18T14:04:03.461"
      |}
      |""".stripMargin

  def ttpAffordableQuotesResponseJson: String =
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

  def afterAffordableQuotesJourneyJson(): String =
    s"""
      |{
      |    "_id" : "6284fcd33c00003d6b1f3903",
      |    "RetrievedAffordableQuotes" : {
      |        "stage" : {
      |            "AffordableQuotesRetrieved" : {}
      |        },
      |        "createdOn" : "2022-05-18T14:04:03.461",
      |        "instalmentAmounts" : {
      |            "minimumInstalmentAmount" : 29997,
      |            "maximumInstalmentAmount" : 87944
      |        },
      |        "_id" : "6284fcd33c00003d6b1f3903",
      |        "extremeDatesResponse" : {
      |            "initialPaymentDate" : "2022-06-23",
      |            "earliestPlanStartDate" : "2022-07-13",
      |            "latestPlanStartDate" : "2022-08-12"
      |        },
      |        "origin" : "Origins.Epaye.Bta",
      |        "sjRequest" : {
      |            "Simple" : {
      |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
      |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
      |            }
      |        },
      |        "sessionId" : "IamATestSessionId",
      |        "eligibilityCheckResult" : {
      |            "idType" : "SSTTP",
      |            "idNumber" : "840/GZ00064",
      |            "regimeType" : "PAYE",
      |            "processingDate" : "2022-01-31",
      |            "customerPostcodes" : [
      |                {
      |                    "addressPostcode" : "AA11AA",
      |                    "postcodeDate" : "2022-01-01"
      |                }
      |            ],
      |            "minPlanLengthMonths" : 1,
      |            "maxPlanLengthMonths" : 3,
      |            "eligibilityStatus" : {
      |                "overallEligibilityStatus" : true
      |            },
      |            "eligibilityRules" : {
      |                "hasRlsOnAddress" : false,
      |                "markedAsInsolvent" : false,
      |                "isLessThanMinDebtAllowance" : false,
      |                "isMoreThanMaxDebtAllowance" : false,
      |                "disallowedChargeLocks" : false,
      |                "existingTTP" : false,
      |                "exceedsMaxDebtAge" : false,
      |                "eligibleChargeType" : false,
      |                "missingFiledReturns" : false
      |            },
      |            "chargeTypeAssessment" : [
      |                {
      |                    "taxPeriodFrom" : "2020-08-13",
      |                    "taxPeriodTo" : "2020-08-14",
      |                    "debtTotalAmount" : 1000000,
      |                    "disallowedChargeLocks" : [
      |                        {
      |                            "chargeId" : "A00000000001",
      |                            "mainTrans" : "mainTrans",
      |                            "mainTransDesc" : "mainTransDesc",
      |                            "subTrans" : "subTrans",
      |                            "subTransDesc" : "subTransDesc",
      |                            "outstandingDebtAmount" : 100000,
      |                            "interestStartDate" : "2017-03-07",
      |                            "accruedInterestToDate" : 1597,
      |                            "chargeLocks" : {
      |                                "paymentLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "clearingLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "interestLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                },
      |                                "dunningLock" : {
      |                                    "status" : false,
      |                                    "reason" : ""
      |                                }
      |                            }
      |                        }
      |                    ]
      |                }
      |            ]
      |        },
      |        "upfrontPaymentAnswers" : {
      |            "DeclaredUpfrontPayment" : {
      |                "amount" : 12312
      |            }
      |        },
      |        "taxId" : {
      |            "value" : "840/GZ00064"
      |        },
      |        "monthlyPaymentAmount": 30000,
      |        "dayOfMonth": 1,
      |         "startDatesResponse" : {
      |            "initialPaymentDate" : "2022-07-03",
      |            "instalmentStartDate" : "2022-07-28"
      |         },
      |         "affordableQuotesResponse" : {
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
      |    },
      |    "sessionId" : "IamATestSessionId",
      |    "createdAt" : "2022-05-18T14:04:03.461"
      |}
      |""".stripMargin

  def afterSelectedPlanJourneyJson(
      upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}"""
  ): String =
    s"""
       |{
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "ChosenPaymentPlan" : {
       |        "stage" : {
       |            "SelectedPlan" : {}
       |        },
       |        "createdOn" : "2022-05-18T14:04:03.461",
       |        "instalmentAmounts" : {
       |            "minimumInstalmentAmount" : 29997,
       |            "maximumInstalmentAmount" : 87944
       |        },
       |        "_id" : "6284fcd33c00003d6b1f3903",
       |        "extremeDatesResponse" : {
       |            "initialPaymentDate" : "2022-06-23",
       |            "earliestPlanStartDate" : "2022-07-13",
       |            "latestPlanStartDate" : "2022-08-12"
       |        },
       |        "origin" : "Origins.Epaye.Bta",
       |        "sjRequest" : {
       |            "Simple" : {
       |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |            }
       |        },
       |        "sessionId" : "IamATestSessionId",
       |        "eligibilityCheckResult" : {
       |            "idType" : "SSTTP",
       |            "idNumber" : "840/GZ00064",
       |            "regimeType" : "PAYE",
       |            "processingDate" : "2022-01-31",
       |            "customerPostcodes" : [
       |                {
       |                    "addressPostcode" : "AA11AA",
       |                    "postcodeDate" : "2022-01-01"
       |                }
       |            ],
       |            "minPlanLengthMonths" : 1,
       |            "maxPlanLengthMonths" : 3,
       |            "eligibilityStatus" : {
       |                "overallEligibilityStatus" : true
       |            },
       |            "eligibilityRules" : {
       |                "hasRlsOnAddress" : false,
       |                "markedAsInsolvent" : false,
       |                "isLessThanMinDebtAllowance" : false,
       |                "isMoreThanMaxDebtAllowance" : false,
       |                "disallowedChargeLocks" : false,
       |                "existingTTP" : false,
       |                "exceedsMaxDebtAge" : false,
       |                "eligibleChargeType" : false,
       |                "missingFiledReturns" : false
       |            },
       |            "chargeTypeAssessment" : [
       |                {
       |                    "taxPeriodFrom" : "2020-08-13",
       |                    "taxPeriodTo" : "2020-08-14",
       |                    "debtTotalAmount" : 1000000,
       |                    "disallowedChargeLocks" : [
       |                        {
       |                            "chargeId" : "A00000000001",
       |                            "mainTrans" : "mainTrans",
       |                            "mainTransDesc" : "mainTransDesc",
       |                            "subTrans" : "subTrans",
       |                            "subTransDesc" : "subTransDesc",
       |                            "outstandingDebtAmount" : 100000,
       |                            "interestStartDate" : "2017-03-07",
       |                            "accruedInterestToDate" : 1597,
       |                            "chargeLocks" : {
       |                                "paymentLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "clearingLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "interestLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "dunningLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                }
       |                            }
       |                        }
       |                    ]
       |                }
       |            ]
       |        },
       |        "upfrontPaymentAnswers" : $upfrontPaymentAmountJsonString,
       |        "taxId" : {
       |            "value" : "840/GZ00064"
       |        },
       |        "monthlyPaymentAmount": 30000,
       |        "dayOfMonth": 1,
       |        "startDatesResponse" : {
       |           "initialPaymentDate" : "2022-07-03",
       |           "instalmentStartDate" : "2022-07-28"
       |        },
       |        "affordableQuotesResponse" : {
       |           "paymentPlans" : [
       |               {
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
       |               },
       |               {
       |                   "numberOfInstalments" : 3,
       |                   "planDuration" : 3,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111150,
       |                   "planInterest" : 9,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 37050
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 37047,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 74094,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               },
       |               {
       |                   "numberOfInstalments" : 4,
       |                   "planDuration" : 4,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111153,
       |                   "planInterest" : 12,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-11-28",
       |                               "amountDue" : 27788
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 4,
       |                           "dueDate" : "2022-11-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 27786,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 55571,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 83356,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               }
       |           ]
       |        },
       |        "selectedPaymentPlan": {
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
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin

  def afterHasCheckedPlanJourneyJson(
      upfrontPaymentAmountJsonString: String = """{"DeclaredUpfrontPayment": {"amount": 12312}}"""
  ): String =
    s"""
       |{
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "CheckedPaymentPlan" : {
       |        "stage" : {
       |            "AcceptedPlan" : {}
       |        },
       |        "createdOn" : "2022-05-18T14:04:03.461",
       |        "instalmentAmounts" : {
       |            "minimumInstalmentAmount" : 29997,
       |            "maximumInstalmentAmount" : 87944
       |        },
       |        "_id" : "6284fcd33c00003d6b1f3903",
       |        "extremeDatesResponse" : {
       |            "initialPaymentDate" : "2022-06-23",
       |            "earliestPlanStartDate" : "2022-07-13",
       |            "latestPlanStartDate" : "2022-08-12"
       |        },
       |        "origin" : "Origins.Epaye.Bta",
       |        "sjRequest" : {
       |            "Simple" : {
       |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |            }
       |        },
       |        "sessionId" : "IamATestSessionId",
       |        "eligibilityCheckResult" : {
       |            "idType" : "SSTTP",
       |            "idNumber" : "840/GZ00064",
       |            "regimeType" : "PAYE",
       |            "processingDate" : "2022-01-31",
       |            "customerPostcodes" : [
       |                {
       |                    "addressPostcode" : "AA11AA",
       |                    "postcodeDate" : "2022-01-01"
       |                }
       |            ],
       |            "minPlanLengthMonths" : 1,
       |            "maxPlanLengthMonths" : 3,
       |            "eligibilityStatus" : {
       |                "overallEligibilityStatus" : true
       |            },
       |            "eligibilityRules" : {
       |                "hasRlsOnAddress" : false,
       |                "markedAsInsolvent" : false,
       |                "isLessThanMinDebtAllowance" : false,
       |                "isMoreThanMaxDebtAllowance" : false,
       |                "disallowedChargeLocks" : false,
       |                "existingTTP" : false,
       |                "exceedsMaxDebtAge" : false,
       |                "eligibleChargeType" : false,
       |                "missingFiledReturns" : false
       |            },
       |            "chargeTypeAssessment" : [
       |                {
       |                    "taxPeriodFrom" : "2020-08-13",
       |                    "taxPeriodTo" : "2020-08-14",
       |                    "debtTotalAmount" : 1000000,
       |                    "disallowedChargeLocks" : [
       |                        {
       |                            "chargeId" : "A00000000001",
       |                            "mainTrans" : "mainTrans",
       |                            "mainTransDesc" : "mainTransDesc",
       |                            "subTrans" : "subTrans",
       |                            "subTransDesc" : "subTransDesc",
       |                            "outstandingDebtAmount" : 100000,
       |                            "interestStartDate" : "2017-03-07",
       |                            "accruedInterestToDate" : 1597,
       |                            "chargeLocks" : {
       |                                "paymentLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "clearingLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "interestLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "dunningLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                }
       |                            }
       |                        }
       |                    ]
       |                }
       |            ]
       |        },
       |        "upfrontPaymentAnswers" : $upfrontPaymentAmountJsonString,
       |        "taxId" : {
       |            "value" : "840/GZ00064"
       |        },
       |        "monthlyPaymentAmount": 30000,
       |        "dayOfMonth": 1,
       |        "startDatesResponse" : {
       |           "initialPaymentDate" : "2022-07-03",
       |           "instalmentStartDate" : "2022-07-28"
       |        },
       |        "affordableQuotesResponse" : {
       |           "paymentPlans" : [
       |               {
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
       |               },
       |               {
       |                   "numberOfInstalments" : 3,
       |                   "planDuration" : 3,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111150,
       |                   "planInterest" : 9,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 37050
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 37047,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 74094,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               },
       |               {
       |                   "numberOfInstalments" : 4,
       |                   "planDuration" : 4,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111153,
       |                   "planInterest" : 12,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-11-28",
       |                               "amountDue" : 27788
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 4,
       |                           "dueDate" : "2022-11-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 27786,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 55571,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 83356,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               }
       |           ]
       |        },
       |        "selectedPaymentPlan": {
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
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin

  def afterDirectDebitDetailsJourneyJson(
      upfrontPaymentAmountJsonString: String  = """{"DeclaredUpfrontPayment": {"amount": 12312}}""",
      isAccountHolder:                Boolean = true
  ): String =
    s"""
       |{
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "EnteredDirectDebitDetails" : {
       |        "stage" : {
       |            "IsAccountHolder" : {}
       |        },
       |        "createdOn" : "2022-05-18T14:04:03.461",
       |        "instalmentAmounts" : {
       |            "minimumInstalmentAmount" : 29997,
       |            "maximumInstalmentAmount" : 87944
       |        },
       |        "_id" : "6284fcd33c00003d6b1f3903",
       |        "extremeDatesResponse" : {
       |            "initialPaymentDate" : "2022-06-23",
       |            "earliestPlanStartDate" : "2022-07-13",
       |            "latestPlanStartDate" : "2022-08-12"
       |        },
       |        "origin" : "Origins.Epaye.Bta",
       |        "sjRequest" : {
       |            "Simple" : {
       |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |            }
       |        },
       |        "sessionId" : "IamATestSessionId",
       |        "eligibilityCheckResult" : {
       |            "idType" : "SSTTP",
       |            "idNumber" : "840/GZ00064",
       |            "regimeType" : "PAYE",
       |            "processingDate" : "2022-01-31",
       |            "customerPostcodes" : [
       |                {
       |                    "addressPostcode" : "AA11AA",
       |                    "postcodeDate" : "2022-01-01"
       |                }
       |            ],
       |            "minPlanLengthMonths" : 1,
       |            "maxPlanLengthMonths" : 3,
       |            "eligibilityStatus" : {
       |                "overallEligibilityStatus" : true
       |            },
       |            "eligibilityRules" : {
       |                "hasRlsOnAddress" : false,
       |                "markedAsInsolvent" : false,
       |                "isLessThanMinDebtAllowance" : false,
       |                "isMoreThanMaxDebtAllowance" : false,
       |                "disallowedChargeLocks" : false,
       |                "existingTTP" : false,
       |                "exceedsMaxDebtAge" : false,
       |                "eligibleChargeType" : false,
       |                "missingFiledReturns" : false
       |            },
       |            "chargeTypeAssessment" : [
       |                {
       |                    "taxPeriodFrom" : "2020-08-13",
       |                    "taxPeriodTo" : "2020-08-14",
       |                    "debtTotalAmount" : 1000000,
       |                    "disallowedChargeLocks" : [
       |                        {
       |                            "chargeId" : "A00000000001",
       |                            "mainTrans" : "mainTrans",
       |                            "mainTransDesc" : "mainTransDesc",
       |                            "subTrans" : "subTrans",
       |                            "subTransDesc" : "subTransDesc",
       |                            "outstandingDebtAmount" : 100000,
       |                            "interestStartDate" : "2017-03-07",
       |                            "accruedInterestToDate" : 1597,
       |                            "chargeLocks" : {
       |                                "paymentLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "clearingLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "interestLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "dunningLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                }
       |                            }
       |                        }
       |                    ]
       |                }
       |            ]
       |        },
       |        "upfrontPaymentAnswers" : $upfrontPaymentAmountJsonString,
       |        "taxId" : {
       |            "value" : "840/GZ00064"
       |        },
       |        "directDebitDetails" : {
       |          "bankDetails" : {
       |            "name" : "Bob Ross",
       |            "sortCode" : "123456",
       |            "accountNumber" : "12345678"
       |          },
       |          "isAccountHolder" : ${isAccountHolder}
       |        },
       |        "monthlyPaymentAmount": 30000,
       |        "dayOfMonth": 1,
       |        "startDatesResponse" : {
       |           "initialPaymentDate" : "2022-07-03",
       |           "instalmentStartDate" : "2022-07-28"
       |        },
       |        "affordableQuotesResponse" : {
       |           "paymentPlans" : [
       |               {
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
       |               },
       |               {
       |                   "numberOfInstalments" : 3,
       |                   "planDuration" : 3,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111150,
       |                   "planInterest" : 9,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 37050
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 37047,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 74094,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               },
       |               {
       |                   "numberOfInstalments" : 4,
       |                   "planDuration" : 4,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111153,
       |                   "planInterest" : 12,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-11-28",
       |                               "amountDue" : 27788
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 4,
       |                           "dueDate" : "2022-11-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 27786,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 55571,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 83356,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               }
       |           ]
       |        },
       |        "selectedPaymentPlan": {
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
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin

  def afterConfirmDirectDebitDetailsJourneyJson(): String =
    s"""
       |{
       |    "_id" : "6284fcd33c00003d6b1f3903",
       |    "ConfirmedDirectDebitDetails" : {
       |        "stage" : {
       |            "ConfirmedDetails" : {}
       |        },
       |        "createdOn" : "2022-05-18T14:04:03.461",
       |        "instalmentAmounts" : {
       |            "minimumInstalmentAmount" : 29997,
       |            "maximumInstalmentAmount" : 87944
       |        },
       |        "_id" : "6284fcd33c00003d6b1f3903",
       |        "extremeDatesResponse" : {
       |            "initialPaymentDate" : "2022-06-23",
       |            "earliestPlanStartDate" : "2022-07-13",
       |            "latestPlanStartDate" : "2022-08-12"
       |        },
       |        "origin" : "Origins.Epaye.Bta",
       |        "sjRequest" : {
       |            "Simple" : {
       |                "returnUrl" : "/set-up-a-payment-plan/test-only/bta-page?return-page",
       |                "backUrl" : "/set-up-a-payment-plan/test-only/bta-page?starting-page"
       |            }
       |        },
       |        "sessionId" : "IamATestSessionId",
       |        "eligibilityCheckResult" : {
       |            "idType" : "SSTTP",
       |            "idNumber" : "840/GZ00064",
       |            "regimeType" : "PAYE",
       |            "processingDate" : "2022-01-31",
       |            "customerPostcodes" : [
       |                {
       |                    "addressPostcode" : "AA11AA",
       |                    "postcodeDate" : "2022-01-01"
       |                }
       |            ],
       |            "minPlanLengthMonths" : 1,
       |            "maxPlanLengthMonths" : 3,
       |            "eligibilityStatus" : {
       |                "overallEligibilityStatus" : true
       |            },
       |            "eligibilityRules" : {
       |                "hasRlsOnAddress" : false,
       |                "markedAsInsolvent" : false,
       |                "isLessThanMinDebtAllowance" : false,
       |                "isMoreThanMaxDebtAllowance" : false,
       |                "disallowedChargeLocks" : false,
       |                "existingTTP" : false,
       |                "exceedsMaxDebtAge" : false,
       |                "eligibleChargeType" : false,
       |                "missingFiledReturns" : false
       |            },
       |            "chargeTypeAssessment" : [
       |                {
       |                    "taxPeriodFrom" : "2020-08-13",
       |                    "taxPeriodTo" : "2020-08-14",
       |                    "debtTotalAmount" : 1000000,
       |                    "disallowedChargeLocks" : [
       |                        {
       |                            "chargeId" : "A00000000001",
       |                            "mainTrans" : "mainTrans",
       |                            "mainTransDesc" : "mainTransDesc",
       |                            "subTrans" : "subTrans",
       |                            "subTransDesc" : "subTransDesc",
       |                            "outstandingDebtAmount" : 100000,
       |                            "interestStartDate" : "2017-03-07",
       |                            "accruedInterestToDate" : 1597,
       |                            "chargeLocks" : {
       |                                "paymentLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "clearingLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "interestLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                },
       |                                "dunningLock" : {
       |                                    "status" : false,
       |                                    "reason" : ""
       |                                }
       |                            }
       |                        }
       |                    ]
       |                }
       |            ]
       |        },
       |        "upfrontPaymentAnswers" : {"DeclaredUpfrontPayment": {"amount": 12312}},
       |        "taxId" : {
       |            "value" : "840/GZ00064"
       |        },
       |        "directDebitDetails" : {
       |          "bankDetails" : {
       |            "name" : "Bob Ross",
       |            "sortCode" : "123456",
       |            "accountNumber" : "12345678"
       |          },
       |          "isAccountHolder" : true
       |        },
       |        "monthlyPaymentAmount": 30000,
       |        "dayOfMonth": 1,
       |        "startDatesResponse" : {
       |           "initialPaymentDate" : "2022-07-03",
       |           "instalmentStartDate" : "2022-07-28"
       |        },
       |        "affordableQuotesResponse" : {
       |           "paymentPlans" : [
       |               {
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
       |               },
       |               {
       |                   "numberOfInstalments" : 3,
       |                   "planDuration" : 3,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111150,
       |                   "planInterest" : 9,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 37050
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 37050
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 37047,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 74094,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 37047,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               },
       |               {
       |                   "numberOfInstalments" : 4,
       |                   "planDuration" : 4,
       |                   "totalDebt" : 111141,
       |                   "totalDebtIncInt" : 111153,
       |                   "planInterest" : 12,
       |                   "collections" : {
       |                       "initialCollection" : {
       |                           "dueDate" : "2022-07-03",
       |                           "amountDue" : 12312
       |                       },
       |                       "regularCollections" : [
       |                           {
       |                               "dueDate" : "2022-08-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-09-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-10-28",
       |                               "amountDue" : 27788
       |                           },
       |                           {
       |                               "dueDate" : "2022-11-28",
       |                               "amountDue" : 27788
       |                           }
       |                       ]
       |                   },
       |                   "instalments" : [
       |                       {
       |                           "instalmentNumber" : 4,
       |                           "dueDate" : "2022-11-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 27786,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 3,
       |                           "dueDate" : "2022-10-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 55571,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 2,
       |                           "dueDate" : "2022-09-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 83356,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       },
       |                       {
       |                           "instalmentNumber" : 1,
       |                           "dueDate" : "2022-08-28",
       |                           "instalmentInterestAccrued" : 3,
       |                           "instalmentBalance" : 111141,
       |                           "debtItemChargeId" : "A00000000001",
       |                           "amountDue" : 27785,
       |                           "debtItemOriginalDueDate" : "2021-07-28"
       |                       }
       |                   ]
       |               }
       |           ]
       |        },
       |        "selectedPaymentPlan": {
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
       |    },
       |    "sessionId" : "IamATestSessionId",
       |    "createdAt" : "2022-05-18T14:04:03.461"
       |}
       |""".stripMargin
}
