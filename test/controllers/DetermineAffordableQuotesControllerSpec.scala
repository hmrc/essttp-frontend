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

package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.crypto.CryptoFormat
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.ttp.{PaymentPlanMaxLength, PaymentPlanMinLength}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.{EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class DetermineAffordableQuotesControllerSpec extends ItSpec {

  private val controller: DetermineAffordableQuotesController =
    app.injector.instanceOf[DetermineAffordableQuotesController]

  Seq[(String, Origin)](
    ("EPAYE", Origins.Epaye.Bta),
    ("VAT", Origins.Vat.Bta),
    ("SA", Origins.Sa.Bta),
    ("SIMP", Origins.Simp.Pta)
  ).foreach { case (regime, origin) =>
    "GET /determine-affordable-quotes" - {

      s"[regime $regime] return an error when" - {

        "the journey is in state" - {

          "AfterStartedPegaCase" in {
            stubCommonActions()
            EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)()

            val exception = intercept[UpstreamErrorResponse](await(controller.retrieveAffordableQuotes(fakeRequest)))

            exception.statusCode shouldBe INTERNAL_SERVER_ERROR
            exception.message shouldBe "Not expecting to retrieve affordable quotes when started PEGA case"
          }

          "AfterCheckedPaymentPlan on an affordability journey" in {
            stubCommonActions()
            EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)()

            val exception = intercept[UpstreamErrorResponse](await(controller.retrieveAffordableQuotes(fakeRequest)))

            exception.statusCode shouldBe INTERNAL_SERVER_ERROR
            exception.message shouldBe "Not expecting to retrieve affordable quotes when payment plan has been checked on affordability journey"
          }

        }

      }

      s"[$regime journey] trigger call to ttp microservice affordable quotes endpoint and update backend when" - {

        def test(stubFindJourney: () => StubMapping, expectedMaxPlanLength: Int = 24): Unit = {
          val expectedAffordableQuotesRequest = TdAll
            .affordableQuotesRequest(origin.taxRegime)
            .copy(
              paymentPlanMinLength = PaymentPlanMinLength(2),
              paymentPlanMaxLength = PaymentPlanMaxLength(expectedMaxPlanLength)
            )

          stubCommonActions()
          stubFindJourney()
          Ttp.AffordableQuotes.stubRetrieveAffordableQuotes()
          EssttpBackend.AffordableQuotes
            .stubUpdateAffordableQuotes(TdAll.journeyId, JourneyJsonTemplates.`Retrieved Affordable Quotes`(origin))

          val result: Future[Result] = controller.retrieveAffordableQuotes(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.instalmentsUrl)
          Ttp.AffordableQuotes.verifyTtpAffordableQuotesRequest(origin.taxRegime)(expectedAffordableQuotesRequest)(using
            CryptoFormat.NoOpCryptoFormat
          )
          EssttpBackend.AffordableQuotes.verifyUpdateAffordableQuotesRequest(TdAll.journeyId)
        }

        "the user has not checked their payment plan yet" in {
          val journeyJson =
            JourneyJsonTemplates.`Retrieved Start Dates`(
              origin,
              eligibilityMinPlanLength = 2,
              eligibilityMaxPlanLength = 24
            )(using testCrypto)

          test(() => EssttpBackend.Dates.findJourneyStartDates(testCrypto, origin)(journeyJson))
        }

        "the user has checked their payment plan on a non-affordability journey" in {
          test(() =>
            EssttpBackend.HasCheckedPlan.findJourney(
              withAffordability = false,
              testCrypto,
              origin,
              eligibilityMinPlanLength = 2,
              eligibilityMaxPlanLength = 24
            )()
          )
        }

        "the user has affordability enabled on their journey but isn't on an affordability journey" in {
          test(
            () =>
              EssttpBackend.HasCheckedPlan.findJourney(
                withAffordability = false,
                testCrypto,
                origin,
                eligibilityMinPlanLength = 2,
                eligibilityMaxPlanLength = 24,
                affordabilityEnabled = true
              )(),
            expectedMaxPlanLength = 6
          )
        }

      }
    }
  }
}
