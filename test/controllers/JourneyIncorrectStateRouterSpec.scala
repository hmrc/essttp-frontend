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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import essttp.journey.model.Origins
import essttp.rootmodel.TaxRegime.Epaye
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}

class JourneyIncorrectStateRouterSpec extends ItSpec {

  "When JourneyIncorrectStateRouter is triggered by journey being in wrong state" - {
    forAll(
      Table(
        ("Stage", "Journey wiremock response", "Expected Redirect Location"),
        ("Stages.Started - EPAYE", () => EssttpBackend.StartJourney.findJourney(), PageUrls.epayeLandingPageUrl),
        (
          "Stages.Started - VAT",
          () => EssttpBackend.StartJourney.findJourney(Origins.Vat.GovUk),
          PageUrls.vatLandingPageUrl
        ),
        (
          "Stages.Started - SA",
          () => EssttpBackend.StartJourney.findJourney(Origins.Sa.GovUk),
          PageUrls.saLandingPageUrl
        ),
        (
          "Stages.Started - SIMP",
          () => EssttpBackend.StartJourney.findJourney(Origins.Simp.GovUk),
          PageUrls.simpLandingPageUrl
        ),
        (
          "Stages.ComputedTaxId",
          () => EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)(),
          PageUrls.determineEligibilityUrl
        ),
        (
          "Stages.EligibilityChecked",
          () => EssttpBackend.EligibilityCheck.findJourney(testCrypto)(),
          PageUrls.yourBillIsUrl
        ), // check this
        (
          "Stages.EligibilityChecked - PAYE",
          () =>
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(
              JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Epaye.Bta)
            ),
          PageUrls.epayeRLSUrl
        ),
        (
          "Stages.EligibilityChecked - VAT",
          () =>
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(
              JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Vat.Bta)
            ),
          PageUrls.vatRLSUrl
        ),
        (
          "Stages.EligibilityChecked - SA",
          () =>
            EssttpBackend.EligibilityCheck.findJourney(testCrypto)(
              JourneyJsonTemplates.`Eligibility Checked - Ineligible - HasRlsOnAddress`(Origins.Sa.Bta)
            ),
          PageUrls.saRLSUrl
        ),
        (
          "Stages.ObtainedWhyCannotPayInFull - PAYE",
          () =>
            EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto)(
              JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Epaye.Bta)
            ),
          PageUrls.canYouMakeAnUpfrontPaymentUrl
        ),
        (
          "Stages.ObtainedWhyCannotPayInFull - VAT",
          () =>
            EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto)(
              JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Vat.Bta)
            ),
          PageUrls.canYouMakeAnUpfrontPaymentUrl
        ),
        (
          "Stages.ObtainedWhyCannotPayInFull - SA",
          () =>
            EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto)(
              JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Sa.Bta)
            ),
          PageUrls.canYouMakeAnUpfrontPaymentUrl
        ),
        (
          "Stages.ObtainedWhyCannotPayInFull - SIMP",
          () =>
            EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto)(
              JourneyJsonTemplates.`Why Cannot Pay in Full - Not Required`(Origins.Simp.Pta)
            ),
          PageUrls.canYouMakeAnUpfrontPaymentUrl
        ),
        (
          "Stages.AnsweredCanPayUpfront - can pay upfront",
          () => EssttpBackend.CanPayUpfront.findJourney(testCrypto)(),
          PageUrls.howMuchCanYouPayUpfrontUrl
        ),
        (
          "Stages.AnsweredCanPayUpfront - can't pay upfront",
          () =>
            EssttpBackend.CanPayUpfront.findJourney(testCrypto)(
              JourneyJsonTemplates.`Answered Can Pay Upfront - No`(Origins.Epaye.Bta)(using testCrypto)
            ),
          PageUrls.retrievedExtremeDatesUrl
        ),
        (
          "Stages.EnteredUpfrontPaymentAmount",
          () => EssttpBackend.UpfrontPaymentAmount.findJourney(testCrypto)(),
          PageUrls.upfrontPaymentSummaryUrl
        ),
        (
          "Stages.RetrievedExtremeDates",
          () => EssttpBackend.Dates.findJourneyExtremeDates(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.determineAffordabilityUrl
        ),
        (
          "Stages.RetrievedAffordabilityResult",
          () => EssttpBackend.AffordabilityMinMaxApi.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.howMuchCanYouPayEachMonthUrl
        ),
        (
          "Stages.ObtainedCanPayWithinSixMonthsAnswers - not required",
          () => EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.canPayWithinSixMonthsUrl(Epaye, None)
        ),
        (
          "Stages.ObtainedCanPayWithinSixMonthsAnswers - no ",
          () =>
            EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
              JourneyJsonTemplates.`Obtained Can Pay Within 6 months - no`(Origins.Epaye.Bta)(using testCrypto)
            ),
          PageUrls.canPayWithinSixMonthsUrl(Epaye, None)
        ),
        (
          "Stages.ObtainedCanPayWithinSixMonthsAnswers - yes",
          () =>
            EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, Origins.Epaye.Bta)(
              JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(Origins.Epaye.Bta)(using testCrypto)
            ),
          PageUrls.canPayWithinSixMonthsUrl(Epaye, None)
        ),
        (
          "Stages.StartedPegaCase",
          () => EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.canPayWithinSixMonthsUrl(Epaye, None)
        ),
        (
          "Stages.EnteredMonthlyPaymentAmount",
          () => EssttpBackend.MonthlyPaymentAmount.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.whichDayDoYouWantToPayUrl
        ),
        (
          "Stages.EnteredDayOfMonth",
          () => EssttpBackend.DayOfMonth.findJourney(TdAll.dayOfMonth(), testCrypto, Origins.Epaye.Bta)(),
          PageUrls.retrieveStartDatesUrl
        ),
        (
          "Stages.RetrievedStartDates",
          () => EssttpBackend.Dates.findJourneyStartDates(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.determineAffordableQuotesUrl
        ),
        (
          "Stages.RetrievedAffordableQuotes",
          () => EssttpBackend.AffordableQuotes.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.instalmentsUrl
        ),
        (
          "Stages.ChosenPaymentPlan",
          () => EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.checkPaymentPlanUrl
        ),
        (
          "Stages.CheckedPaymentPlan",
          () => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto)(),
          PageUrls.aboutYourBankAccountUrl
        ),
        (
          "Stages.EnteredCanSetUpDirectDebit - is account holder",
          () => EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.directDebitDetailsUrl
        ),
        (
          "Stages.EnteredCanSetUpDirectDebit - is not account holder",
          () =>
            EssttpBackend.EnteredCanSetUpDirectDebit.findJourney(testCrypto, Origins.Epaye.Bta)(
              JourneyJsonTemplates.`Entered Can Set Up Direct Debit`(isAccountHolder = false)
            ),
          PageUrls.cannotSetupDirectDebitOnlineUrl
        ),
        (
          "Stages.EnteredDirectDebitDetails",
          () => EssttpBackend.DirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.checkDirectDebitDetailsUrl
        ),
        (
          "Stages.ConfirmedDirectDebitDetails",
          () => EssttpBackend.ConfirmedDirectDebitDetails.findJourney(testCrypto, Origins.Epaye.Bta)(),
          PageUrls.termsAndConditionsUrl
        ),
        (
          "Stages.ConfirmedDirectDebitDetails",
          () =>
            EssttpBackend.TermsAndConditions.findJourney(
              isEmailAddressRequired = true,
              testCrypto,
              Origins.Epaye.Bta,
              etmpEmail = Some(TdAll.etmpEmail)
            )(),
          PageUrls.whichEmailDoYouWantToUseUrl
        ),
        (
          "Stages.SubmittedArrangement",
          () => EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)(),
          PageUrls.epayeConfirmationUrl
        )
      )
    ) { (scenario: String, journeyState: () => StubMapping, expectedRedirectUrl: String) =>
      s"[ GET $scenario ] should redirect to the first page that supports that journey: [ $expectedRedirectUrl ]" in {
        stubCommonActions()
        journeyState()

        val result = app.injector.instanceOf[SubmitArrangementController].submitArrangement(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }
    }
  }
}
