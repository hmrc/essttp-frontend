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

import essttp.crypto.CryptoFormat
import essttp.journey.model.{CanPayWithinSixMonthsAnswers, Origin, Origins}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.{EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}

import scala.concurrent.Future

class DetermineAffordabilityControllerSpec extends ItSpec {

  private val controller: DetermineAffordabilityController = app.injector.instanceOf[DetermineAffordabilityController]
  Seq[(String, Origin)](
    ("EPAYE", Origins.Epaye.Bta),
    ("VAT", Origins.Vat.Bta),
    ("SA", Origins.Sa.Bta),
    ("SIA", Origins.Sia.Pta)
  ).foreach {
      case (regime, origin) =>
        "GET /determine-affordability" - {
          s"[$regime journey] trigger call to ttp microservice affordability endpoint and update backend when" - {

            "affordability is not enabled" in {
              stubCommonActions()
              EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, origin)()
              EssttpBackend.AffordabilityMinMaxApi.stubUpdateAffordability(TdAll.journeyId, JourneyJsonTemplates.`Retrieved Affordability`(origin))
              EssttpBackend.CanPayWithinSixMonths.stubUpdateCanPayWithinSixMonths(TdAll.journeyId, JourneyJsonTemplates `Obtained Can Pay Within 6 months - not required` (origin))
              Ttp.Affordability.stubRetrieveAffordability()

              val result: Future[Result] = controller.determineAffordability(fakeRequest)

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(PageUrls.howMuchCanYouPayEachMonthUrl)
              EssttpBackend.AffordabilityMinMaxApi.verifyUpdateAffordabilityRequest(TdAll.journeyId, TdAll.instalmentAmounts)
              EssttpBackend.CanPayWithinSixMonths.verifyUpdateCanPayWithinSixMonthsRequest(TdAll.journeyId, CanPayWithinSixMonthsAnswers.AnswerNotRequired)
              Ttp.Affordability.verifyTtpAffordabilityRequest(origin.taxRegime)(CryptoFormat.NoOpCryptoFormat)
            }

            "affordability is enabled" in {
              stubCommonActions()
              EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, origin)(
                JourneyJsonTemplates.`Retrieved Extreme Dates Response`(origin, affordabilityEnabled = true)(testCrypto)
              )
              EssttpBackend.AffordabilityMinMaxApi.stubUpdateAffordability(
                TdAll.journeyId, JourneyJsonTemplates.`Retrieved Affordability`(origin, affordabilityEnabled = true)
              )
              Ttp.Affordability.stubRetrieveAffordability()

              val result: Future[Result] = controller.determineAffordability(fakeRequest)

              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(PageUrls.canPayWithinSixMonthsUrl(origin.taxRegime, None))
              EssttpBackend.AffordabilityMinMaxApi.verifyUpdateAffordabilityRequest(TdAll.journeyId, TdAll.instalmentAmounts)
              EssttpBackend.CanPayWithinSixMonths.verifyNoneUpdateCanPayWithinSixMonthsRequest(TdAll.journeyId)
              Ttp.Affordability.verifyTtpAffordabilityRequest(origin.taxRegime)(CryptoFormat.NoOpCryptoFormat)
            }

          }
        }
    }
}
