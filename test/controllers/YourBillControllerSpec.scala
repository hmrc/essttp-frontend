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

import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.PageUrls
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class YourBillControllerSpec extends ItSpec {

  private val controller: YourBillController = app.injector.instanceOf[YourBillController]

  "GET /your-bill should" - {
    "return you bill page for EPAYE" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your PAYE bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 Aug to 14 Aug (month 5) Bill due 7 March 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 Jul to 14 Jul (month 4) Bill due 7 February 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"
    }

    "return you bill page for VAT" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.yourBill(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = "Your VAT bill is £3,000",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.YourBillController.yourBillSubmit.url),
        regimeBeingTested       = Some(TaxRegime.Vat)
      )

      val tableRows = doc.select(".govuk-summary-list > .govuk-summary-list__row").asScala.toList
      tableRows.size shouldBe 2

      tableRows(0).select(".govuk-summary-list__key").text() shouldBe "13 August to 14 August 2020 Bill due 7 March 2017"
      tableRows(0).select(".govuk-summary-list__value").text() shouldBe "£1,000 (includes interest added to date)"

      tableRows(1).select(".govuk-summary-list__key").text() shouldBe "13 July to 14 July 2020 Bill due 7 February 2017"
      tableRows(1).select(".govuk-summary-list__value").text() shouldBe "£2,000 (includes interest added to date)"
    }
  }

  "POST /your-bill should" - {
    "redirect to can you make an upfront payment question page" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Vat.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result = controller.yourBillSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.canYouMakeAnUpfrontPaymentUrl)
    }
  }

  "YourBillController.monthNumberInTaxYear should return correct tax month" in {
    forAll(Table(
      ("Scenario", "Date", "Expected result"),
      ("January", "2022-01-10", 10),
      ("February", "2022-02-10", 11),
      ("March", "2022-03-10", 12),
      ("April", "2022-04-10", 1),
      ("May", "2022-05-10", 2),
      ("June", "2022-06-10", 3),
      ("July", "2022-07-10", 4),
      ("August", "2022-08-10", 5),
      ("September", "2022-09-10", 6),
      ("October", "2022-10-10", 7),
      ("November", "2022-11-10", 8),
      ("December", "2022-12-10", 9),
      ("April 5th", "2022-04-05", 12),
      ("April 6th", "2022-04-06", 1),
      ("March 5th", "2022-03-05", 11),
      ("March 6th", "2022-03-06", 12)
    )) {
      (scenario: String, date: String, expectedResult: Int) =>
        {
          YourBillController.monthNumberInTaxYear(LocalDate.parse(date)) shouldBe expectedResult withClue s"monthNumberInTaxYear failed for $scenario"
        }
    }
  }
}
