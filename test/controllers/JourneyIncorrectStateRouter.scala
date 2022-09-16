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
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls}
import uk.gov.hmrc.http.SessionKeys

class JourneyIncorrectStateRouter extends ItSpec {

  "When JourneyIncorrectStateRouter is triggered by journey being in wrong state" - {
    forAll(Table(
      ("Stage", "Journey wiremock response", "Expected Redirect Location"),
      ("Stages.Started", () => EssttpBackend.StartJourney.findJourney(), PageUrls.landingPageUrl),
      ("Stages.ComputedTaxId", () => EssttpBackend.DetermineTaxId.findJourney(), PageUrls.determineEligibilityUrl),
      ("Stages.EligibilityChecked", () => EssttpBackend.EligibilityCheck.findJourney(testCrypto)(), PageUrls.yourBillIsUrl), //check this
      ("Stages.EligibilityChecked", () => EssttpBackend.EligibilityCheck.findJourney(testCrypto)(
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(testCrypto)
      ), PageUrls.notEligibleUrl),
      ("Stages.AnsweredCanPayUpfront", () => EssttpBackend.CanPayUpfront.findJourney(testCrypto)(), PageUrls.canYouMakeAnUpfrontPaymentUrl),
      ("Stages.AnsweredCanPayUpfront", () => EssttpBackend.CanPayUpfront.findJourney(testCrypto)(
        JourneyJsonTemplates.`Answered Can Pay Upfront - No`(testCrypto)
      ), PageUrls.retrievedExtremeDatesUrl),
      ("Stages.EnteredUpfrontPaymentAmount", () => EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto)(), PageUrls.upfrontPaymentSummaryUrl),
      ("Stages.RetrievedExtremeDates", () => EssttpBackend.Dates.findJourneyExtremeDates(testCrypto)(), PageUrls.determineAffordabilityUrl),
      ("Stages.RetrievedAffordabilityResult", () => EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto)(), PageUrls.howMuchCanYouPayEachMonthUrl),
      ("Stages.EnteredMonthlyPaymentAmount", () => EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto)(), PageUrls.whichDayDoYouWantToPayUrl),
      ("Stages.EnteredDayOfMonth", () => EssttpBackend.DayOfMonth.findJourney(testCrypto)(), PageUrls.retrieveStartDatesUrl),
      ("Stages.RetrievedStartDates", () => EssttpBackend.Dates.findJourneyStartDates(testCrypto)(), PageUrls.determineAffordableQuotesUrl),
      ("Stages.RetrievedAffordableQuotes", () => EssttpBackend.AffordableQuotes.findJourney(testCrypto)(), PageUrls.instalmentsUrl),
      ("Stages.ChosenPaymentPlan", () => EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto)(), PageUrls.instalmentScheduleUrl),
      ("Stages.CheckedPaymentPlan", () => EssttpBackend.HasCheckedPlan.findJourney(testCrypto)(), PageUrls.typeOfAccountUrl),
      ("Stages.ChosenTypeOfBankAccount", () => EssttpBackend.ChosenTypeOfBankAccount.findJourney(testCrypto)(), PageUrls.directDebitDetailsUrl),
      ("Stages.EnteredDirectDebitDetails", () => EssttpBackend.DirectDebitDetails.findJourney(testCrypto)(), PageUrls.checkDirectDebitDetailsUrl),
      ("Stages.ConfirmedDirectDebitDetails", () => EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto)(), PageUrls.termsAndConditionsUrl),
      ("Stages.SubmittedArrangement", () => EssttpBackend.SubmitArrangement.findJourney(testCrypto)(), PageUrls.confirmationUrl)
    )) {
      (scenario: String, journeyState: () => StubMapping, expectedRedirectUrl: String) =>
        {
          s"[ GET $scenario ] should redirect to the first page that supports that journey: [ $expectedRedirectUrl ]" in {
            stubCommonActions()
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
