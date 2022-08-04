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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.{AuthStub, EssttpBackend}
import testsupport.testdata.PageUrls
import uk.gov.hmrc.http.SessionKeys

class JourneyIncorrectStateRouter extends ItSpec {
  "When JourneyIncorrectStateRouter is triggered by journey being in wrong state" - {
    forAll(Table(
      ("Stage", "Journey wiremock response", "Expected Redirect Location"),
      ("Stages.Started", () => EssttpBackend.StartJourney.findJourney(), PageUrls.landingPageUrl),
      ("Stages.ComputedTaxId", () => EssttpBackend.DetermineTaxId.findJourney(), PageUrls.determineEligibilityUrl),
      ("Stages.EligibilityChecked", () => EssttpBackend.EligibilityCheck.findJourney(), PageUrls.yourBillIsUrl), //check this
      ("Stages.AnsweredCanPayUpfront", () => EssttpBackend.CanPayUpfront.findJourney(), PageUrls.canYouMakeAnUpfrontPaymentUrl),
      ("Stages.EnteredUpfrontPaymentAmount", () => EssttpBackend.UpfrontPaymentAmount.findJourney(), PageUrls.howMuchCanYouPayUpfrontUrl),
      ("Stages.RetrievedExtremeDates", () => EssttpBackend.Dates.findJourneyExtremeDates(), PageUrls.retrievedExtremeDatesUrl),
      ("Stages.RetrievedAffordabilityResult", () => EssttpBackend.AffordabilityMinMaxApi.findJourney(), PageUrls.determineAffordabilityUrl),
      ("Stages.EnteredMonthlyPaymentAmount", () => EssttpBackend.MonthlyPaymentAmount.findJourney(), PageUrls.howMuchCanYouPayEachMonthUrl),
      ("Stages.EnteredDayOfMonth", () => EssttpBackend.DayOfMonth.findJourney(), PageUrls.whichDayDoYouWantToPayUrl),
      ("Stages.RetrievedStartDates", () => EssttpBackend.Dates.findJourneyStartDates(), PageUrls.retrieveStartDatesUrl),
      ("Stages.RetrievedAffordableQuotes", () => EssttpBackend.AffordableQuotes.findJourney(), PageUrls.determineAffordableQuotesUrl),
      ("Stages.ChosenPaymentPlan", () => EssttpBackend.SelectedPaymentPlan.findJourney(), PageUrls.instalmentsUrl),
      ("Stages.CheckedPaymentPlan", () => EssttpBackend.HasCheckedPlan.findJourney(), PageUrls.instalmentScheduleUrl),
      ("Stages.ChosenTypeOfBankAccount", () => EssttpBackend.ChosenTypeOfBankAccount.findJourney(), PageUrls.typeOfAccountUrl),
      ("Stages.EnteredDirectDebitDetails", () => EssttpBackend.DirectDebitDetails.findJourney(), PageUrls.directDebitDetailsUrl),
      ("Stages.ConfirmedDirectDebitDetails", () => EssttpBackend.ConfirmedDirectDebitDetails.findJourney(), PageUrls.directDebitDetailsUrl),
      ("Stages.SubmittedArrangement", () => EssttpBackend.SubmitArrangement.findJourney(), PageUrls.confirmationUrl)
    )) {
      (scenario: String, journeyState: () => StubMapping, expectedRedirectUrl: String) =>
        {
          s"[ GET $scenario ] should redirect to the first page that supports that journey: [ $expectedRedirectUrl ]" in {
            AuthStub.authorise()
            journeyState()
            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")
            val result = app.injector.instanceOf[SubmitArrangementController].submitArrangement(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirectUrl)
          }
        }
    }
  }
}
