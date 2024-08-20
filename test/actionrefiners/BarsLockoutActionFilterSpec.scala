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

package actionrefiners

import controllers.YourBillController
import play.api.http.Status
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.EssttpBackend.BarsVerifyStatusStub
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.PageUrls
import java.time.Instant

class BarsLockoutActionFilterSpec extends ItSpec {

  // this is the first page in the journey that the BarsLockoutActionFilter can lockout
  private val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "BarsLockoutActionFilter" - {
    "should return redirect to your bill page when bars verify status does not have a lockout expiry set" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()
      BarsVerifyStatusStub.statusUnlocked()

      val result = controller.yourBill(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "should return redirect to the lockout page when bars verify status has a lockout expiry set" in {
      AuthStub.authorise()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto)()

      val expiry = Instant.now
      BarsVerifyStatusStub.statusLocked(expiry)

      val result = controller.yourBill(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.lockoutUrl)
    }

  }
}
