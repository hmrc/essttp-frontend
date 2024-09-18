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
import essttp.journey.model.{Origin, Origins}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.{EssttpBackend, EssttpDates}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class DatesApiControllerSpec extends ItSpec {

  private val controller: DatesApiController = app.injector.instanceOf[DatesApiController]

  Seq[(String, Origin)](
    ("EPAYE", Origins.Epaye.Bta),
    ("VAT", Origins.Vat.Bta),
    ("SA", Origins.Sa.Bta),
    ("SIA", Origins.Sia.Pta)
  ).foreach {
      case (regime, origin) =>
        "GET /retrieve-extreme-dates" - {
          s"[$regime journey] should trigger call to essttp-dates microservice extreme dates endpoint and update backend" in {
            stubCommonActions()
            EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto, origin)()
            EssttpDates.stubExtremeDatesCall()
            EssttpBackend.Dates.stubUpdateExtremeDates(TdAll.journeyId, JourneyJsonTemplates.`Retrieved Extreme Dates Response`(origin))

            val result: Future[Result] = controller.retrieveExtremeDates(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.determineAffordabilityUrl)
            EssttpBackend.Dates.verifyUpdateExtremeDates(TdAll.journeyId, TdAll.extremeDatesResponse())
            EssttpDates.verifyExtremeDates(TdAll.extremeDatesRequest(initialPayment = true))
          }
        }

        "GET /retrieve-start-dates" - {

          s"[regime $regime] return an error when" - {

            "the journey is in state" - {

              "AfterStartedPegaCase" in {
                stubCommonActions()
                EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)()

                val exception = intercept[UpstreamErrorResponse](await(controller.retrieveStartDates(fakeRequest)))

                exception.statusCode shouldBe INTERNAL_SERVER_ERROR
                exception.message shouldBe "Not expecting to retrieve start dates when started PEGA case"
              }

              "AfterCheckedPaymentPlan on an affordability journey" in {
                stubCommonActions()
                EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)()

                val exception = intercept[UpstreamErrorResponse](await(controller.retrieveStartDates(fakeRequest)))

                exception.statusCode shouldBe INTERNAL_SERVER_ERROR
                exception.message shouldBe "Not expecting to retrieve start dates after checked payment plan on affordability journey"
              }

            }

          }

          s"[$regime journey] should trigger call to essttp-dates microservice start dates endpoint and update backend when" - {

              def test(stubFindJourney: () => StubMapping): Unit = {
                stubCommonActions()
                stubFindJourney()
                EssttpDates.stubStartDatesCall()
                EssttpBackend.Dates.stubUpdateStartDates(TdAll.journeyId, JourneyJsonTemplates.`Retrieved Start Dates`(origin))

                val result: Future[Result] = controller.retrieveStartDates(fakeRequest)

                status(result) shouldBe Status.SEE_OTHER
                redirectLocation(result) shouldBe Some(PageUrls.determineAffordableQuotesUrl)
                EssttpBackend.Dates.verifyUpdateStartDates(TdAll.journeyId, TdAll.startDatesResponse())
                EssttpDates.verifyStartDates(TdAll.startDatesRequest(initialPayment = true, day = 28))
                ()
              }

            "the user has entered the day of month but has not checked their payment plan yet" in {
              test(
                () => EssttpBackend.DayOfMonth.findJourney(TdAll.dayOfMonth(), testCrypto, origin)()
              )
            }

            "the user has their checked payment plan on a non-affordability journey" in {
              test(
                () => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)()
              )
            }

          }
        }
    }

}
