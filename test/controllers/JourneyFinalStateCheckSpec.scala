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
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.stubs.EssttpBackend
import testsupport.testdata.PageUrls
import uk.gov.hmrc.http.SessionKeys

class JourneyFinalStateCheckSpec extends ItSpec {
  "Controllers using finalStateCheck(F) should route user to confirmation page when their journey is in completed state" - {
    forAll(Table(
      ("scenario", "action"),
      (PageUrls.yourBillIsUrl, app.injector.instanceOf[YourBillController].yourBill),
      (PageUrls.whyCannotPayInFull, app.injector.instanceOf[WhyCannotPayInFullController].whyCannotPayInFull),
      (PageUrls.canYouMakeAnUpfrontPaymentUrl, app.injector.instanceOf[UpfrontPaymentController].canYouMakeAnUpfrontPayment),
      (PageUrls.howMuchCanYouPayUpfrontUrl, app.injector.instanceOf[UpfrontPaymentController].upfrontPaymentAmount),
      (PageUrls.upfrontPaymentSummaryUrl, app.injector.instanceOf[UpfrontPaymentController].upfrontPaymentSummary),
      (PageUrls.retrievedExtremeDatesUrl, app.injector.instanceOf[DatesApiController].retrieveExtremeDates),
      (PageUrls.determineAffordabilityUrl, app.injector.instanceOf[DetermineAffordabilityController].determineAffordability),
      (PageUrls.canPayWithinSixMonthsUrl, app.injector.instanceOf[CanPayWithinSixMonthsController].canPayWithinSixMonths),
      (PageUrls.howMuchCanYouPayEachMonthUrl, app.injector.instanceOf[MonthlyPaymentAmountController].displayMonthlyPaymentAmount),
      (PageUrls.whichDayDoYouWantToPayUrl, app.injector.instanceOf[PaymentDayController].paymentDay),
      (PageUrls.retrieveStartDatesUrl, app.injector.instanceOf[DatesApiController].retrieveStartDates),
      (PageUrls.determineAffordableQuotesUrl, app.injector.instanceOf[DetermineAffordableQuotesController].retrieveAffordableQuotes),
      (PageUrls.instalmentsUrl, app.injector.instanceOf[InstalmentsController].instalmentOptions),
      (PageUrls.checkPaymentPlanUrl, app.injector.instanceOf[PaymentScheduleController].checkPaymentSchedule),
      (PageUrls.aboutYourBankAccountUrl, app.injector.instanceOf[BankDetailsController].detailsAboutBankAccount),
      (PageUrls.directDebitDetailsUrl, app.injector.instanceOf[BankDetailsController].enterBankDetails),
      (PageUrls.checkDirectDebitDetailsUrl, app.injector.instanceOf[BankDetailsController].checkBankDetails),
      (PageUrls.termsAndConditionsUrl, app.injector.instanceOf[TermsAndConditionsController].termsAndConditions),
      (PageUrls.submitArrangementUrl, app.injector.instanceOf[SubmitArrangementController].submitArrangement)
    )) {
      (scenario: String, action: Action[AnyContent]) =>
        {
          s"GET $scenario should redirect to ${PageUrls.epayeConfirmationUrl}" in {
            stubCommonActions()
            EssttpBackend.SubmitArrangement.findJourney(Origins.Epaye.Bta, testCrypto)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val result = action(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(PageUrls.epayeConfirmationUrl)
          }
        }
    }
  }
}
