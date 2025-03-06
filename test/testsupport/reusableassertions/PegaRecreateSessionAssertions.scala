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

package testsupport.reusableassertions

import controllers.routes
import essttp.rootmodel.TaxRegime
import org.scalatest.freespec.AnyFreeSpecLike
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.*
import testsupport.ItSpec
import testsupport.stubs.EssttpBackend
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

trait PegaRecreateSessionAssertions extends AnyFreeSpecLike { this: ItSpec =>

  def recreateSessionErrorBehaviour(performAction: (TaxRegime, Request[AnyContent]) => Future[Result]): Unit = {

    "return an error when" - {

      "there is no session found in the BE and the call to reconstruct a session returns an error" in {
        stubCommonActions()
        EssttpBackend.findByLatestSessionNotFound()
        EssttpBackend.Pega.stubRecreateSession(TaxRegime.Sa, Left(SERVICE_UNAVAILABLE))

        val exception = intercept[UpstreamErrorResponse](
          await(performAction(TaxRegime.Sa, fakeRequestWithPath("/a?regime=sa")))
        )
        exception.statusCode shouldBe SERVICE_UNAVAILABLE

        EssttpBackend.verifyFindByLatestSessionId()
        EssttpBackend.Pega.verifyRecreateSessionCalled(TaxRegime.Sa)
      }

    }

    "redirect to the which tax regime page when" - {

      "no journey is found and no journey was reconstructed when a regime can be found in the query parameters" in {
        stubCommonActions()
        EssttpBackend.findByLatestSessionNotFound()
        EssttpBackend.Pega.stubRecreateSession(TaxRegime.Epaye, Left(NOT_FOUND))

        val result = performAction(TaxRegime.Epaye, fakeRequestWithPath("/a?regime=vat"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.WhichTaxRegimeController.whichTaxRegime.url)

        EssttpBackend.verifyFindByLatestSessionId()
        EssttpBackend.Pega.verifyRecreateSessionCalled(TaxRegime.Vat)
      }

      "no journey is found if there is no regime in the query parameters" in {
        stubCommonActions()
        EssttpBackend.findByLatestSessionNotFound()
        EssttpBackend.Pega.stubRecreateSession(TaxRegime.Epaye, Left(NOT_FOUND))

        val result = performAction(TaxRegime.Epaye, fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.WhichTaxRegimeController.whichTaxRegime.url)

        EssttpBackend.verifyFindByLatestSessionId()
        EssttpBackend.Pega.verifyRecreateSessionNotCalled(TaxRegime.Epaye)
        EssttpBackend.Pega.verifyRecreateSessionNotCalled(TaxRegime.Vat)
        EssttpBackend.Pega.verifyRecreateSessionNotCalled(TaxRegime.Sa)
      }

    }

  }

}
