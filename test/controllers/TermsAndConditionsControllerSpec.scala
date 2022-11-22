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

import essttp.journey.model.Origins
import essttp.rootmodel.IsEmailAddressRequired
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class TermsAndConditionsControllerSpec extends ItSpec {

  private val controller: TermsAndConditionsController = app.injector.instanceOf[TermsAndConditionsController]

  object TermsAndConditionsPage {
    val expectedH1: String = "Terms and conditions"
  }

  "GET /terms-and-conditions should" - {
    "return 200 and the terms and conditions page" in {
      stubCommonActions()
      EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
      val result: Future[Result] = controller.termsAndConditions(fakeRequest)
      val pageContent: String = contentAsString(result)
      val doc: Document = Jsoup.parse(pageContent)

      RequestAssertions.assertGetRequestOk(result)
      ContentAssertions.commonPageChecks(
        doc,
        expectedH1              = TermsAndConditionsPage.expectedH1,
        shouldBackLinkBePresent = true,
        expectedSubmitUrl       = Some(routes.TermsAndConditionsController.termsAndConditionsSubmit.url)
      )

      ContentAssertions.assertListOfContent(
        elements = doc.select(".govuk-body")
      )(
          expectedContent = List(
            "We can cancel this agreement if you:",
            "If we cancel this agreement, you will need to pay the total amount you owe straight away.",
            "We can use any refunds you might get to pay off your tax charges.",
            "If your circumstances change and you can pay more or you can pay in full, you need to let us know.",
            "You can write to us about your Direct Debit:",
            "DM PAYE HM Revenue and Customs BX9 1EW United Kingdom",
            "I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt."
          )
        )

      ContentAssertions.assertListOfContent(
        elements = doc.select(".govuk-list--bullet").select("li")
      )(
          expectedContent = List(
            "pay late or miss a payment",
            "pay another tax bill late",
            "do not submit your future tax returns on time"
          )
        )

      doc.select(".govuk-heading-m").text() shouldBe "Declaration"
      doc.select(".govuk-button").text() shouldBe "Agree and continue"

      ContentAssertions.formSubmitShouldDisableSubmitButton(doc)
    }
  }

  "POST /terms-and-conditions should" - {

    "redirect the user to the email journey if it is enabled and update backend" in {
      stubCommonActions()
      EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
        TdAll.journeyId,
        JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = true)
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.whichEmailDoYouWantToUseUrl)
      EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(true))
    }
  }

}

class TermsAndConditionsControllerEmailDisabledSpec extends ItSpec {

  override lazy val configOverrides: Map[String, Any] = Map("features.email-journey" -> false)

  val controller: TermsAndConditionsController = app.injector.instanceOf[TermsAndConditionsController]

  "POST /terms-and-conditions should" - {

    "redirect the user to the submit arrangement endpoint if the email journey is enabled and update backend" in {
      stubCommonActions()
      EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.TermsAndConditions.stubUpdateAgreedTermsAndConditions(
        TdAll.journeyId,
        JourneyJsonTemplates.`Agreed Terms and Conditions`(isEmailAddresRequired = false)
      )

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.termsAndConditionsSubmit(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(PageUrls.submitArrangementUrl)
      EssttpBackend.TermsAndConditions.verifyUpdateAgreedTermsAndConditionsRequest(TdAll.journeyId, IsEmailAddressRequired(false))
    }
  }

}
