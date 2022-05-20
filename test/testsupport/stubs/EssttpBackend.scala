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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.journey.model.JourneyId
import essttp.rootmodel.CanPayUpfront

object EssttpBackend {

  private val findByLatestSessionIdUrl: String = "/essttp-backend/journey/find-latest-by-session-id"

  object EligibilityCheck {
    def findJourneyAfterEligibilityCheck: StubMapping = stubFor(
      get(urlPathEqualTo(findByLatestSessionIdUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(jsonBody))
    )

    val jsonBody: String =
      """
        |{
        |  "AfterEligibilityCheck" : {
        |    "stage" : {
        |      "Eligible" : { }
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
        |    }
        |  },
        |  "sessionId" : "IamATestSessionId",
        |  "createdAt" : "2022-05-18T14:04:03.461"
        |}
        |""".stripMargin
  }

  object CanPayUpfront {

    def updateCanPayUpfront(journeyId: JourneyId, canPayUpfrontScenario: Boolean): StubMapping =
      stubFor(
        post(urlPathEqualTo(s"/essttp-backend/journey/${journeyId.value}/update-can-pay-upfront"))
          .withRequestBody(equalTo(canPayUpfrontScenario.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
          )
      )

    def findJourneyAfterUpdateCanPayUpfront(canPayUpfront: CanPayUpfront): StubMapping = {
      val additionalPayloadInfo: (String, Boolean) =
        if (canPayUpfront.value) {
          ("Yes", true)
        } else {
          ("No", false)
        }
      stubFor(
        get(urlPathEqualTo(findByLatestSessionIdUrl))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(jsonBody(additionalPayloadInfo)))
      )
    }

    val jsonBody: ((String, Boolean)) => String = (tuple: (String, Boolean)) =>
      s"""
         |{
         |   "AfterCanPayUpfront" : {
         |     "stage" : {
         |       "${tuple._1}" : { }
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
         |    }
         |    "canPayUpfront": ${tuple._2}
         |  },
         |  "sessionId" : "IamATestSessionId",
         |  "createdAt" : "2022-05-18T14:04:03.461"
         |}
         |""".stripMargin
  }
}
