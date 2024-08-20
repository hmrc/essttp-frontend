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

import essttp.journey.model.Origins
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}

class PegaControllerSpec extends ItSpec {

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
          EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Left(500))

          val exception = intercept[Exception](await(controller.startPegaJourney(fakeRequest)))
          exception.getMessage should include("returned 500")
        }
      }

      "start a case, update the journey and redirect to PEGA" in {
        stubCommonActions()
        EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
          JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
        )
        EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Right(TdAll.pegaStartCaseResponse))
        EssttpBackend.StartedPegaCase.stubUpdateStartPegaCaseResponse(
          TdAll.journeyId,
          JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta)(testCrypto)
        )

        val result = controller.startPegaJourney(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/set-up-a-payment-plan/test-only/pega")

        EssttpBackend.Pega.verifyStartCaseCalled(TdAll.journeyId)
        EssttpBackend.StartedPegaCase.verifyUpdateStartPegaCaseResponseRequest(TdAll.journeyId, TdAll.pegaStartCaseResponse)
      }

    }

    "handling callbacks must" - {

      "return an error when" - {

        "the user has not started a PEGA case yet" in {
          stubCommonActions()
          EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)()

          val exception = intercept[Exception](await(controller.callback(fakeRequest)))
          exception.getMessage should include("Cannot get PEGA case when journey is in state essttp.journey.model.Journey.Epaye.RetrievedAffordabilityResult")
        }

        "the user has checked their payment plan but is not on an affordability journey" in {
          stubCommonActions()
          EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, Origins.Epaye.Bta)()

          val exception = intercept[Exception](await(controller.callback(fakeRequest)))
          exception.getMessage should include("Trying to get PEGA case on non-affordability journey")

        }

        "there is a problem getting the case" in {
          stubCommonActions()
          EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)()
          EssttpBackend.Pega.stubGetCase(TdAll.journeyId, Left(501))

          val exception = intercept[Exception](await(controller.callback(fakeRequest)))
          exception.getMessage should include("returned 501")
        }
      }

      "get a case, update the journey and redirect to the next page" in {
        stubCommonActions()
        EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)()
        EssttpBackend.Pega.stubGetCase(TdAll.journeyId, Right(TdAll.pegaGetCaseResponse))
        EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(
          TdAll.journeyId,
          JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(Origins.Epaye.Bta)(testCrypto)
        )

        val result = controller.callback(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(PageUrls.aboutYourBankAccountUrl)

        EssttpBackend.Pega.verifyGetCaseCalled(TdAll.journeyId)
        EssttpBackend.HasCheckedPlan.verifyUpdateHasCheckedPlanRequest(TdAll.journeyId)
      }

    }

  }

}

class PegaControllerRedirectInConfigSpec extends ItSpec {

  val redirectUrl = "http://redirect-to/here"

  override lazy val configOverrides: Map[String, Any] = Map(
    "pega.redirect-url" -> redirectUrl
  )

  lazy val controller = app.injector.instanceOf[PegaController]

  "PegaController when" - {

    "handling requests to start a PEGA case must" - {

      "start a case, update the journey and redirect to the url in config if one exists" in {
        stubCommonActions()
        EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
          JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(testCrypto)
        )
        EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Right(TdAll.pegaStartCaseResponse))
        EssttpBackend.StartedPegaCase.stubUpdateStartPegaCaseResponse(
          TdAll.journeyId,
          JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta)(testCrypto)
        )

        val result = controller.startPegaJourney(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(redirectUrl)

        EssttpBackend.Pega.verifyStartCaseCalled(TdAll.journeyId)
        EssttpBackend.StartedPegaCase.verifyUpdateStartPegaCaseResponseRequest(TdAll.journeyId, TdAll.pegaStartCaseResponse)
      }

    }

  }

}
