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
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.ttp.{PaymentPlanMaxLength, PaymentPlanMinLength}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{EssttpBackend, Ttp}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class DetermineAffordableQuotesControllerSpec extends ItSpec {

  private val controller: DetermineAffordableQuotesController = app.injector.instanceOf[DetermineAffordableQuotesController]

  Seq[(String, Origin)](
    ("EPAYE", Origins.Epaye.Bta),
    ("VAT", Origins.Vat.Bta),
    ("SA", Origins.Sa.Bta)
  ).foreach {
      case (regime, origin) =>
        "GET /determine-affordable-quotes" - {
          s"[$regime journey] trigger call to ttp microservice affordable quotes endpoint and update backend" in {
            val journeyJson =
              JourneyJsonTemplates.`Retrieved Start Dates`(origin, eligibilityMinPlanLength = 2, eligibilityMaxPlanLength = 24)(testCrypto)

            val expectedAffordableQuotesRequest = TdAll.affordableQuotesRequest(origin.taxRegime).copy(
              paymentPlanMinLength = PaymentPlanMinLength(2),
              paymentPlanMaxLength = PaymentPlanMaxLength(24)
            )

            stubCommonActions()
            EssttpBackend.Dates.findJourneyStartDates(testCrypto, origin)(journeyJson)
            Ttp.AffordableQuotes.stubRetrieveAffordableQuotes()
            EssttpBackend.AffordableQuotes.stubUpdateAffordableQuotes(TdAll.journeyId, JourneyJsonTemplates.`Retrieved Affordable Quotes`(origin))

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result: Future[Result] = controller.retrieveAffordableQuotes(fakeRequest)

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.instalmentsUrl)
            Ttp.AffordableQuotes.verifyTtpAffordableQuotesRequest(origin.taxRegime)(expectedAffordableQuotesRequest)(CryptoFormat.NoOpCryptoFormat)
            EssttpBackend.AffordableQuotes.verifyUpdateAffordableQuotesRequest(TdAll.journeyId)
          }
        }
    }
}
