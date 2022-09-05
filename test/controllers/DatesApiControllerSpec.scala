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

import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuthStub, Dates, EssttpBackend}
import testsupport.testdata.{PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class DatesApiControllerSpec extends ItSpec {

  private val controller: DatesApiController = app.injector.instanceOf[DatesApiController]

  "GET /retrieve-extreme-dates" - {
    "trigger call to essttp-dates microservice extreme dates endpoint and update backend" in {
      AuthStub.authorise()
      EssttpBackend.UpfrontPaymentAmount.findJourney()
      Dates.stubExtremeDatesCall()
      EssttpBackend.Dates.stubUpdateExtremeDates(TdAll.journeyId)
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.retrieveExtremeDates(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.determineAffordabilityUrl)
      EssttpBackend.Dates.verifyUpdateExtremeDates(TdAll.journeyId)
      Dates.verifyExtremeDates()
    }
  }
  "GET /retrieve-start-dates" - {
    "trigger call to essttp-dates microservice start dates endpoint and update backend" in {
      AuthStub.authorise()
      EssttpBackend.DayOfMonth.findJourney()
      Dates.stubStartDatesCall()
      EssttpBackend.Dates.stubUpdateStartDates(TdAll.journeyId)
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.retrieveStartDates(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.determineAffordableQuotesUrl)
      EssttpBackend.Dates.verifyUpdateStartDates(TdAll.journeyId)
      Dates.verifyStartDates()
    }
  }

}
