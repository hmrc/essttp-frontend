/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import essttp.journey.model.{Origins, WhyCannotPayInFullAnswers}
import essttp.rootmodel.{CannotPayReason, TaxRegime}
import models.Languages
import models.Languages.{English, Welsh}
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.PegaRecreateSessionAssertions
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

class PegaControllerSpec extends ItSpec with PegaRecreateSessionAssertions {

  lazy val controller = app.injector.instanceOf[PegaController]

  "PegaController when" - {

    "handling requests to start a PEGA case must" - {

      "return an error when" - {

        "the user has not answered 'can you pay within 6 months' yet" in {
          stubCommonActions()
          EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)()

          val exception = intercept[Exception](await(controller.startPegaJourney(fakeRequest)))
          exception.getMessage should include("Cannot start PEGA case when journey is in state essttp.journey.model.Journey.Epaye.RetrievedAffordabilityResult")
        }

        "an answer to 'can you pay within 6 months' was not required" in {
          stubCommonActions()
          EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
            JourneyJsonTemplates.`Obtained Can Pay Within 6 months - not required`(Origins.Epaye.Bta)(testCrypto)
          )

          val exception = intercept[Exception](await(controller.startPegaJourney(fakeRequest)))
          exception.getMessage should include("Cannot start PEGA case when answer to CanPayWithinSixMonths is not required")
        }

        "the answer to 'can you pay within 6 months' is yes" in {
          stubCommonActions()
          EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
            JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(Origins.Epaye.Bta)(testCrypto)
          )

          val exception = intercept[Exception](await(controller.startPegaJourney(fakeRequest)))
          exception.getMessage should include("Cannot start PEGA case when answer to CanPayWithinSixMonths is 'yes'")
        }

        "there is a problem starting a case" in {
          stubCommonActions()
          EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
            JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
          )
          EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Left(500), recalculationNeeded = true)

          val exception = intercept[Exception](await(controller.startPegaJourney(fakeRequest)))
          exception.getMessage should include("returned 500")
        }

        "there is a problem saving the journey" in {
          stubCommonActions()
          EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
            JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
          )
          EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Right(TdAll.pegaStartCaseResponse), recalculationNeeded = true)
          EssttpBackend.StartedPegaCase.stubUpdateStartPegaCaseResponse(
            TdAll.journeyId,
            JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta)(testCrypto)
          )
          EssttpBackend.Pega.stubSaveJourneyForPega(TdAll.journeyId, Left(429))

          val exception = intercept[Exception](await(controller.startPegaJourney(fakeRequest)))
          exception.getMessage should include("returned 429")
        }

      }

      "start a case, update the journey and redirect to PEGA" in {

        stubCommonActions()
        EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
          JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
        )
        EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Right(TdAll.pegaStartCaseResponse), recalculationNeeded = true)
        EssttpBackend.StartedPegaCase.stubUpdateStartPegaCaseResponse(
          TdAll.journeyId,
          JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta, pegaCaseId = Some("case"))(testCrypto)
        )
        EssttpBackend.Pega.stubSaveJourneyForPega(TdAll.journeyId, Right(()))

        val result = controller.startPegaJourney(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/test-only/pega?regime=epaye")

        EssttpBackend.Pega.verifyStartCaseCalled(TdAll.journeyId)
        EssttpBackend.StartedPegaCase.verifyUpdateStartPegaCaseResponseRequest(TdAll.journeyId, TdAll.pegaStartCaseResponse)
        EssttpBackend.Pega.verifySaveJourneyForPegaCalled(TdAll.journeyId)

        AuditConnectorStub.verifyEventAudited(
          auditType  = "CanUserPayInSixMonths",
          auditEvent = Json.parse(
            s"""
                 |{
                 |  "regime" : "Epaye",
                 |  "taxIdentifier" : "864FZ00049",
                 |  "pegaCaseId" : "case",
                 |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                 |  "pegaCorrelationId": "testCorrelationId",
                 |   "userEnteredDetails" : {
                 |     "unableToPayReason": ["WaitingForRefund", "NoMoneySetAside"],
                 |     "payUpfront" : true,
                 |     "upfrontPaymentAmount" : 2,
                 |     "canPayInSixMonths" : false
                 |   }
                 |}
            """.stripMargin
          ).as[JsObject]
        )

      }
    }

    "handling callbacks must" - {

      behave like recreateSessionErrorBehaviour(controller.callback(_, None)(_))

      "return an error when" - {

        "the user has not started a PEGA case yet" in {
          stubCommonActions()
          EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)()

          val exception = intercept[Exception](await(controller.callback(TaxRegime.Epaye, None)(fakeRequest)))
          exception.getMessage should include("Cannot get PEGA case when journey is in state essttp.journey.model.Journey.Epaye.RetrievedAffordabilityResult")
        }

        "the user has checked their payment plan but is not on an affordability journey" in {
          stubCommonActions()
          EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, Origins.Epaye.Bta)()

          val exception = intercept[Exception](await(controller.callback(TaxRegime.Epaye, None)(fakeRequest)))
          exception.getMessage should include("Trying to get PEGA case on non-affordability journey")

        }

        "there is a problem getting the case" in {
          stubCommonActions()
          EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)()
          EssttpBackend.Pega.stubGetCase(TdAll.journeyId, Left(501))

          val exception = intercept[Exception](await(controller.callback(TaxRegime.Epaye, None)(fakeRequest)))
          exception.getMessage should include("returned 501")
        }

      }

      "get a case, update the journey and redirect to the next page when" - {

          def test(whyCannotPayReasons: Set[CannotPayReason]): Unit = {
            stubCommonActions()
            EssttpBackend.Pega.stubGetCase(TdAll.journeyId, Right(TdAll.pegaGetCaseResponse))
            EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(
              TdAll.journeyId,
              JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(Origins.Epaye.Bta)(testCrypto)
            )

            val result = controller.callback(TaxRegime.Epaye, None)(fakeRequestWithPath("/b?regime=epaye"))
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.aboutYourBankAccountUrl)

            EssttpBackend.Pega.verifyGetCaseCalled(TdAll.journeyId)
            EssttpBackend.HasCheckedPlan.verifyUpdateHasCheckedPlanRequest(TdAll.journeyId)

            AuditConnectorStub.verifyEventAudited(
              auditType  = "PlanDetails",
              auditEvent = Json.parse(
                s"""
                   |{
                   |  "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "origin": "Bta",
                   |  "canPayInSixMonths": false,
                   |  "unableToPayReason": ${Json.toJson(whyCannotPayReasons).toString()},
                   |  "schedule": {
                   |    "initialPaymentAmount" : 10,
                   |    "collectionDate" : 28,
                   |    "collectionLengthCalendarMonths" : 1,
                   |    "collections" : [ {
                   |      "collectionNumber" : 1,
                   |      "amount" : 10,
                   |      "paymentDate" : "2022-02-01"
                   |    } ],
                   |    "totalNoPayments" : 2,
                   |    "totalInterestCharged" : 10,
                   |    "totalPayable" : 20,
                   |    "totalPaymentWithoutInterest" : 10
                   |  },
                   |  "taxDetail": ${TdAll.taxDetailJsonString(TaxRegime.Epaye)},
                   |  "taxType": "Epaye"
                   |}
            """.stripMargin
              ).as[JsObject]
            )

            AuditConnectorStub.verifyEventAudited(
              auditType  = "ReturnFromAffordabilityAssessment",
              auditEvent = Json.parse(
                s"""
                   |{
                   |  "correlationId" : "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                   |  "regime" : "Epaye",
                   |  "taxIdentifier" : "864FZ00049",
                   |  "pegaCaseId" : "case",
                   |  "pegaCorrelationId" : "testCorrelationId",
                   |  "expenditure" : {
                   |    "expenditure" : 0
                   |  },
                   |  "income" : {
                   |    "income" : 1.23
                   |  },
                   |  "planDetails" : {
                   |    "debtAmount" : 10,
                   |    "initialPaymentAmount" : 10,
                   |    "initialPaymentDate" : "2022-02-01",
                   |    "customerPostcode" : "AA11AA",
                   |    "numberOfInstalments" : 1,
                   |    "planInterest" : 10,
                   |    "collections" : {
                   |      "numberOfCollections" : 1,
                   |      "regularCollectionStartDate" : "2022-02-01",
                   |      "regularCollectionAmount" : 143.23,
                   |      "finalCollectionAmount" : 143.23
                   |    }
                   |  }
                   |}
            """.stripMargin
              ).as[JsObject]
            )
          }

        "a journey can be found" in {
          val whyCannotPayReasons = Set(CannotPayReason.WaitingForRefund, CannotPayReason.NoMoneySetAside)

          EssttpBackend.StartedPegaCase.findJourney(
            testCrypto,
            Origins.Epaye.Bta,
            whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(whyCannotPayReasons)
          )()

          test(whyCannotPayReasons)
        }

        "a journey is successfully reconstructed" in {
          val whyCannotPayReasons =
            Set(CannotPayReason.LostOrReducedAbilityToEarnOrTrade, CannotPayReason.UnexpectedReductionOfIncome)

          EssttpBackend.findByLatestSessionNotFound()
          EssttpBackend.Pega.stubRecreateSession(
            TaxRegime.Epaye,
            Right(Json.parse(JourneyJsonTemplates.`Started PEGA case`(
              Origins.Epaye.Bta,
              whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(whyCannotPayReasons)
            )(testCrypto)))
          )

          test(whyCannotPayReasons)

          EssttpBackend.verifyFindByLatestSessionId()
          EssttpBackend.Pega.verifyRecreateSessionCalled(TaxRegime.Epaye)
        }

        "change the language to english if lang=en is supplied as a query parameter" in {
          stubCommonActions()
          EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)()
          EssttpBackend.Pega.stubGetCase(TdAll.journeyId, Right(TdAll.pegaGetCaseResponse))
          EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(
            TdAll.journeyId,
            JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(Origins.Epaye.Bta)(testCrypto)
          )

          val request = fakeRequestWithPath("/b?regime=epaye&lang=en").withLangWelsh().withHeaders(HeaderNames.REFERER -> "bleep")
          val result = controller.callback(TaxRegime.Epaye, Some(English))(request)
          cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("en")
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe
            Some(routes.PegaController.callback(TaxRegime.Epaye, None).url)
        }

        "change the language to welsh if lang=cy is supplied as a query parameter" in {
          stubCommonActions()
          EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)()
          EssttpBackend.Pega.stubGetCase(TdAll.journeyId, Right(TdAll.pegaGetCaseResponse))
          EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(
            TdAll.journeyId,
            JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(Origins.Epaye.Bta)(testCrypto)
          )

          val request = fakeRequestWithPath("/b?regime=epaye&lang=cy").withLangEnglish().withHeaders(HeaderNames.REFERER -> "bloop")
          val result = controller.callback(TaxRegime.Epaye, Some(Welsh))(request)
          cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("cy")
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe
            Some(routes.PegaController.callback(TaxRegime.Epaye, None).url)
        }

        "have the query parameters in the url" in {
          routes.PegaController.callback(
            TaxRegime.Sa,
            Some(Languages.English)
          ).url shouldBe "/set-up-a-payment-plan/pega-callback?regime=sa&lang=en"

          routes.PegaController.callback(
            TaxRegime.Sia,
            Some(Languages.Welsh)
          ).url shouldBe "/set-up-a-payment-plan/pega-callback?regime=sia&lang=cy"
        }

      }

    }

  }

}

class PegaControllerRedirectInConfigSpec extends ItSpec {

  val redirectUrl = "http://redirect-to/here"

  override lazy val configOverrides: Map[String, Any] = Map(
    "pega.start-redirect-url" -> redirectUrl
  )

  lazy val controller = app.injector.instanceOf[PegaController]

  "PegaController when" - {

    "handling requests to start a PEGA case must" - {

      for {
        (origin, expectedRegimeQueryParam) <- Seq(
          (Origins.Epaye.Bta, "PAYE"),
          (Origins.Vat.Bta, "VAT"),
          (Origins.Sa.Bta, "SA")
        )
        (lang, expectedLangQueryParam) <- Seq(
          (Languages.English, "en"),
          (Languages.Welsh, "cy")
        )
      } {

        s"[${origin.toString}, ${lang.toString}]  start a case, update the journey and redirect to the url in config if one exists" in {
          stubCommonActions()
          EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, origin)(
            JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(origin)(testCrypto)
          )
          EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Right(TdAll.pegaStartCaseResponse), recalculationNeeded = true)
          EssttpBackend.StartedPegaCase.stubUpdateStartPegaCaseResponse(
            TdAll.journeyId,
            JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta)(testCrypto)
          )
          EssttpBackend.Pega.stubSaveJourneyForPega(TdAll.journeyId, Right(()))

          val request = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId").withLang(lang)

          val result = controller.startPegaJourney(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl + s"?regime=$expectedRegimeQueryParam&lang=$expectedLangQueryParam")

          EssttpBackend.Pega.verifyStartCaseCalled(TdAll.journeyId)
          EssttpBackend.StartedPegaCase.verifyUpdateStartPegaCaseResponseRequest(TdAll.journeyId, TdAll.pegaStartCaseResponse)
          EssttpBackend.Pega.verifySaveJourneyForPegaCalled(TdAll.journeyId)
        }
      }

    }

  }

}
