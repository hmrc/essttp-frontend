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

package actionrefiners

import controllers.YourBillController
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.EssttpBackend.BarsVerifyStatusStub
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.PageUrls
import uk.gov.hmrc.http.SessionKeys
import util.QueryParameterUtils._
import java.net.URLEncoder
import java.time.Instant

class BarsLockoutActionFilterSpec extends ItSpec {

  // this is the first page that in journey that th BarsLockoutActionFilter can lockout
  val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "BarsLockoutActionFilter" - {
    "should return redirect to your bill page when bars verify status does not have a lockout expiry set" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney()
      BarsVerifyStatusStub.statusUnlocked()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.yourBill(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "should return redirect to the lockout page when bars verify status has a lockout expiry set" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney()

      val expiry = Instant.now
      BarsVerifyStatusStub.statusLocked(expiry)

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result = controller.yourBill(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      val encodedExpiry = URLEncoder.encode(expiry.encodedLongFormat, "utf-8")
      redirectLocation(result) shouldBe Some(s"${PageUrls.lockoutUrl}?p=$encodedExpiry")
    }

  }
}
