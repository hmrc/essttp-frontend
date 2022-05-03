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

import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import testsupport.ItSpec
import play.api.test.Helpers._
import play.api.http.Status

import scala.concurrent.Future

class UpfrontPaymentControllerSpec extends ItSpec {

  private val controller: UpfrontPaymentController = app.injector.instanceOf[UpfrontPaymentController]

  "GET /upfrontpayment or whatever - Pawel, shall we add tests like these or just stick with int?" - {
    "return 200 and the upfront payment page" in {
      val fakeRequest = FakeRequest("GET", "/upfront-payment")
      val result: Future[Result] = controller.upfrontPayment(fakeRequest)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) should include("Can you make an upfront payment?")
    }
  }
}
