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

package controllers

import essttp.crypto.CryptoFormat
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{EssttpBackend, Ttp}
import testsupport.testdata.{PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class DetermineAffordabilityControllerSpec extends ItSpec {

  private val controller: DetermineAffordabilityController = app.injector.instanceOf[DetermineAffordabilityController]

  "GET /determine-affordability" - {
    "trigger call to ttp microservice affordability endpoint and update backend" in {
      stubCommonActions()
      EssttpBackend.Dates.findJourneyExtremeDates(testCrypto)()
      EssttpBackend.AffordabilityMinMaxApi.stubUpdateAffordability(TdAll.journeyId)
      Ttp.Affordability.stubRetrieveAffordability()
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.determineAffordability(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.howMuchCanYouPayEachMonthUrl)
      EssttpBackend.AffordabilityMinMaxApi.verifyUpdateAffordabilityRequest(TdAll.journeyId, TdAll.instalmentAmounts)
      Ttp.Affordability.verifyTtpAffordabilityRequest(CryptoFormat.NoOpCryptoFormat)
    }
  }

}
