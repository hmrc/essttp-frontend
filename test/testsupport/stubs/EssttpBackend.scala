package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object EssttpBackend {
  def findJourneyAfterEligibilityCheck: StubMapping = stubFor(
    get(urlPathEqualTo("/essttp-backend/journey/find-latest-by-session-id"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(jsonBody)
      )
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
