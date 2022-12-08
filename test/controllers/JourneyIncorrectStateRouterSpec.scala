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
import essttp.journey.model.Origins
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

class JourneyIncorrectStateRouterSpec extends ItSpec {

  "When JourneyIncorrectStateRouter is triggered by journey being in wrong state" - {
    forAll(Table(
      ("Stage", "Journey wiremock response", "Expected Redirect Location"),
      ("Stages.Started - EPAYE", () => EssttpBackend.StartJourney.findJourney(), PageUrls.epayeLandingPageUrl),
      ("Stages.Started - VAT", () => EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk), PageUrls.vatLandingPageUrl),
      ("Stages.ComputedTaxId", () => EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)(), PageUrls.determineEligibilityUrl),
      ("Stages.EligibilityChecked", () => EssttpBackend.EligibilityCheck.findJourney(testCrypto)(), PageUrls.yourBillIsUrl), //check this
      ("Stages.EligibilityChecked - PAYE", () => EssttpBackend.EligibilityCheck.findJourney(testCrypto)(
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Epaye.Bta)
      ), PageUrls.payeNotEligibleUrl),
      ("Stages.EligibilityChecked - VAT", () => EssttpBackend.EligibilityCheck.findJourney(testCrypto)(
        JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Vat.Bta)
      ), PageUrls.vatNotEligibleUrl),
      ("Stages.AnsweredCanPayUpfront - can pay upfront", () => EssttpBackend.CanPayUpfront.findJourney(testCrypto)(), PageUrls.howMuchCanYouPayUpfrontUrl),
      ("Stages.AnsweredCanPayUpfront - can't pay upfront",
        () => EssttpBackend.CanPayUpfront.findJourney(testCrypto)(JourneyJsonTemplates.`Answered Can Pay Upfront - No`(Origins.Epaye.Bta)(testCrypto)),
        PageUrls.retrievedExtremeDatesUrl
      ),
      ("Stages.EnteredUpfrontPaymentAmount", () => EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto)(), PageUrls.upfrontPaymentSummaryUrl),
      ("Stages.RetrievedExtremeDates", () => EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)(), PageUrls.determineAffordabilityUrl),
      ("Stages.RetrievedAffordabilityResult", () => EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.howMuchCanYouPayEachMonthUrl),
      ("Stages.EnteredMonthlyPaymentAmount", () => EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.whichDayDoYouWantToPayUrl),
      ("Stages.EnteredDayOfMonth", () => EssttpBackend.DayOfMonth.findJourney(TdAll.dayOfMonth(), testCrypto, Origins.Epaye.Bta)(), PageUrls.retrieveStartDatesUrl),
      ("Stages.RetrievedStartDates", () => EssttpBackend.Dates.findJourneyStartDates(testCrypto, Origins.Epaye.Bta)(), PageUrls.determineAffordableQuotesUrl),
      ("Stages.RetrievedAffordableQuotes", () => EssttpBackend.AffordableQuotes.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.instalmentsUrl),
      ("Stages.ChosenPaymentPlan", () => EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.instalmentScheduleUrl),
      ("Stages.CheckedPaymentPlan", () => EssttpBackend.HasCheckedPlan.findJourney(testCrypto)(), PageUrls.aboutYourBankAccountUrl),
      ("Stages.EnteredDetailsAboutBankAccount - is account holder", () => EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.directDebitDetailsUrl),
      ("Stages.EnteredDetailsAboutBankAccount - is not account holder",
        () => EssttpBackend.EnteredDetailsAboutBankAccount.findJourney(testCrypto, Origins.Epaye.Bta)(JourneyJsonTemplates.`Entered Details About Bank Account - Business`(isAccountHolder = false)),
        PageUrls.cannotSetupDirectDebitOnlineUrl
      ),
      ("Stages.EnteredDirectDebitDetails", () => EssttpBackend.DirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.checkDirectDebitDetailsUrl),
      ("Stages.ConfirmedDirectDebitDetails", () => EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)(), PageUrls.termsAndConditionsUrl),
      ("Stages.ConfirmedDirectDebitDetails", () => EssttpBackend.TermsAndConditions.findJourney(isEmailAddressRequired = true, testCrypto, Origins.Epaye.Bta, etmpEmail = Some(TdAll.etmpEmail))(), PageUrls.whichEmailDoYouWantToUseUrl),
      ("Stages.SubmittedArrangement", () => EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)(), PageUrls.epayeConfirmationUrl)
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
