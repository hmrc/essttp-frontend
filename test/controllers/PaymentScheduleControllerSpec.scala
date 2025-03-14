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
import controllers.PaymentScheduleControllerSpec.SummaryRow
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxRegime
import models.{Language, Languages}
import models.Languages.{English, Welsh}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.Givens.canEqualPlaySession
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.reusableassertions.{ContentAssertions, PegaRecreateSessionAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll, TdJsonBodies}
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IteratorHasAsScala

class PaymentScheduleControllerSpec extends ItSpec, PegaRecreateSessionAssertions {

  private val controller: PaymentScheduleController = app.injector.instanceOf[PaymentScheduleController]

  Seq[(String, Origin)](
    ("Epaye", Origins.Epaye.Bta),
    ("Vat", Origins.Vat.Bta),
    ("Sa", Origins.Sa.Bta),
    ("Simp", Origins.Simp.Pta)
  ).foreach { case (regime, origin) =>
    s"[$regime journey] GET ${routes.PaymentScheduleController.checkPaymentSchedule.url}" - {

      def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map { e =>
        SummaryRow(
          e.select(".govuk-summary-list__key").html(),
          e.select(".govuk-summary-list__value").text(),
          e.select(".govuk-summary-list__actions > .govuk-link").attr("href"),
          e.select(".govuk-summary-list__actions > .govuk-link > span.govuk-visually-hidden").text()
        )
      }

      def testUpfrontPaymentSummaryRows(summary: Element)(
        canPayUpfrontValue:        String,
        upfrontPaymentAmountValue: Option[String],
        reasonsToNotPayInFull:     List[String],
        canPayWithinSixMonths:     String,
        lang:                      Language
      ) = {
        val upfrontPaymentSummaryRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val whyCannotPayInFullRow = SummaryRow(
          lang.fold("Why are you unable to pay in full?", "Pam nad oes modd i chi dalu’ch llawn?"),
          reasonsToNotPayInFull.mkString(" "),
          PageUrls.checkPaymentPlanChangeUrl("WhyUnableInFull", origin.taxRegime, None),
          lang.fold("why you are unable to pay in full", "pam nad oes modd i chi dalu’n llawn")
        )

        val canPayUpfrontRow        = SummaryRow(
          lang.fold("Can you make an upfront payment?", "A allwch wneud taliad ymlaen llaw?"),
          canPayUpfrontValue,
          PageUrls.checkPaymentPlanChangeUrl("CanPayUpfront", origin.taxRegime, None),
          lang.fold("whether you can make an upfront payment", "p’un a allwch wneud taliad ymlaen llaw")
        )
        val upfrontPaymentAmountRow = upfrontPaymentAmountValue.map(amount =>
          SummaryRow(
            lang.fold(
              "Upfront payment\n<br><span class=\"govuk-body-s\">Taken within 6 working days</span>",
              "Taliad ymlaen llaw\n<br><span class=\"govuk-body-s\">I’w gymryd cyn pen 6 diwrnod gwaith</span>"
            ),
            amount,
            PageUrls.checkPaymentPlanChangeUrl("UpfrontPaymentAmount", origin.taxRegime, None),
            lang.fold("your upfront payment amount", "swm eich taliad ymlaen llaw")
          )
        )

        val canPayWithinSixMonthsRow = SummaryRow(
          lang.fold("Can you pay within 6 months?", "A allwch dalu cyn pen 6 mis?"),
          canPayWithinSixMonths,
          PageUrls.checkPaymentPlanChangeUrl("PayWithin6Months", origin.taxRegime, None),
          lang.fold("whether you can pay within 6 months", "a ydych yn gallu talu cyn pen 6 mis")
        )

        val expectedSummaryRows = List(
          Some(whyCannotPayInFullRow),
          Some(canPayUpfrontRow),
          upfrontPaymentAmountRow,
          Some(canPayWithinSixMonthsRow)
        ).collect { case Some(s) => s }

        extractSummaryRows(upfrontPaymentSummaryRows) shouldBe expectedSummaryRows
      }

      def testMonthlyPaymentsRows(summary: Element)(
        affordableMonthlyPaymentAmount: String,
        paymentDayValue:                String,
        lang:                           Language
      ) = {
        val monthlyPaymentsRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val monthlyPaymentAmountRow = SummaryRow(
          lang.fold("How much can you afford to pay each month?", "Faint y gallwch fforddio ei dalu bob mis?"),
          affordableMonthlyPaymentAmount,
          PageUrls.checkPaymentPlanChangeUrl("MonthlyPaymentAmount", origin.taxRegime, None),
          lang.fold("how much you can afford to pay each month", "faint y gallwch fforddio ei dalu bob mis")
        )

        val paymentDayRow = SummaryRow(
          lang.fold("Payments collected on", "Mae taliadau’n cael eu casglu ar"),
          paymentDayValue,
          PageUrls.checkPaymentPlanChangeUrl("PaymentDay", origin.taxRegime, None),
          lang.fold(
            "which day your payments will be collected on",
            "ar ba ddiwrnod y bydd eich taliadau’n cael eu casglu"
          )
        )

        extractSummaryRows(monthlyPaymentsRows) shouldBe List(monthlyPaymentAmountRow, paymentDayRow)
      }

      def testPaymentPlanRowsOneMonth(summary: Element)(
        datesToAmountsValues:     List[(String, String)],
        totalToPayValue:          String,
        interestValue:            String,
        lang:                     Language,
        hasInterestBearingCharge: Boolean
      ) = {
        val paymentPlanRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val paymentPlanDurationRow =
          SummaryRow(
            lang.fold("Payment plan duration", "Hyd y cynllun talu"),
            lang.fold("1 month", "1 mis"),
            PageUrls.checkPaymentPlanChangeUrl("PaymentPlan", origin.taxRegime, None),
            lang.fold("payment plan duration", "hyd y cynllun talu")
          )

        val startMonthRow =
          SummaryRow(lang.fold("Start month", "Mis cyntaf"), datesToAmountsValues(0)._1, "", "")

        val paymentRow =
          SummaryRow(lang.fold("Payment", "Taliad"), datesToAmountsValues(0)._2, "", "")

        val totalToPayRow =
          expectedTotalToPayRow(totalToPayValue, interestValue, lang, hasInterestBearingCharge)

        extractSummaryRows(paymentPlanRows) shouldBe List(
          paymentPlanDurationRow,
          startMonthRow,
          paymentRow,
          totalToPayRow
        )
      }

      def testPaymentPlanRowsTwoMonths(summary: Element)(
        datesToAmountsValues:     List[(String, String)],
        totalToPayValue:          String,
        interestValue:            String,
        lang:                     Language,
        hasInterestBearingCharge: Boolean
      ) = {
        val paymentPlanRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val paymentPlanDurationRow =
          SummaryRow(
            lang.fold("Payment plan duration", "Hyd y cynllun talu"),
            lang.fold("2 months", "2 fis"),
            PageUrls.checkPaymentPlanChangeUrl("PaymentPlan", origin.taxRegime, None),
            lang.fold("payment plan duration", "hyd y cynllun talu")
          )

        val startMonthRow =
          SummaryRow(lang.fold("Start month", "Mis cyntaf"), datesToAmountsValues(0)._1, "", "")

        val firstPaymentRow =
          SummaryRow(lang.fold("First monthly payment", "Taliad misol cyntaf"), datesToAmountsValues(0)._2, "", "")

        val finalMonthRow =
          SummaryRow(lang.fold("Final month", "Mis olaf"), datesToAmountsValues(1)._1, "", "")

        val finalPaymentRow =
          SummaryRow(lang.fold("Final payment", "Taliad olaf"), datesToAmountsValues(1)._2, "", "")

        val totalToPayRow =
          SummaryRow(
            lang.fold("Total to pay", "Y cyfanswm i’w dalu"),
            lang.fold(
              if (hasInterestBearingCharge) s"$totalToPayValue including $interestValue interest"
              else totalToPayValue,
              if (hasInterestBearingCharge) s"$totalToPayValue gan gynnwys $interestValue o log"
              else totalToPayValue
            ),
            "",
            ""
          )

        extractSummaryRows(paymentPlanRows) shouldBe List(
          paymentPlanDurationRow,
          startMonthRow,
          firstPaymentRow,
          finalMonthRow,
          finalPaymentRow,
          totalToPayRow
        )
      }

      def testPaymentPlanRowsMoreThanTwoMonths(summary: Element)(
        datesToAmountsValues:     List[(String, String)],
        totalToPayValue:          String,
        interestValue:            String,
        lang:                     Language,
        hasInterestBearingCharge: Boolean
      ) = {
        val paymentPlanRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val paymentPlanDurationRow =
          SummaryRow(
            lang.fold("Payment plan duration", "Hyd y cynllun talu"),
            s"${datesToAmountsValues.size.toString} ${lang.fold("months", "mis")}",
            PageUrls.checkPaymentPlanChangeUrl("PaymentPlan", origin.taxRegime, None),
            lang.fold("payment plan duration", "hyd y cynllun talu")
          )

        val startMonthRow =
          SummaryRow(lang.fold("Start month", "Mis cyntaf"), datesToAmountsValues(0)._1, "", "")

        val firstPaymentRow =
          SummaryRow(
            lang.fold(
              s"First ${(datesToAmountsValues.size - 1).toString} monthly payments",
              s"Y ${(datesToAmountsValues.size - 1).toString} taliad misol cyntaf"
            ),
            datesToAmountsValues(0)._2,
            "",
            ""
          )

        val finalMonthRow =
          SummaryRow(
            lang.fold("Final month", "Mis olaf"),
            datesToAmountsValues(datesToAmountsValues.size - 1)._1,
            "",
            ""
          )

        val finalPaymentRow =
          SummaryRow(
            lang.fold("Final payment", "Taliad olaf"),
            datesToAmountsValues(datesToAmountsValues.size - 1)._2,
            "",
            ""
          )

        val totalToPayRow =
          expectedTotalToPayRow(totalToPayValue, interestValue, lang, hasInterestBearingCharge)

        extractSummaryRows(paymentPlanRows) shouldBe List(
          paymentPlanDurationRow,
          startMonthRow,
          firstPaymentRow,
          finalMonthRow,
          finalPaymentRow,
          totalToPayRow
        )
      }

      def expectedTotalToPayRow(
        totalToPayValue:          String,
        interestValue:            String,
        lang:                     Language,
        hasInterestBearingCharge: Boolean
      ): SummaryRow =
        SummaryRow(
          lang.fold("Total to pay", "Y cyfanswm i’w dalu"),
          lang.fold(
            if (hasInterestBearingCharge) s"$totalToPayValue including $interestValue interest"
            else totalToPayValue,
            if (hasInterestBearingCharge) s"$totalToPayValue gan gynnwys $interestValue o log"
            else totalToPayValue
          ),
          "",
          ""
        )

      def testPaymentPlanRows(summary: Element)(
        datesToAmountsValues:     List[(String, String)],
        totalToPayValue:          String,
        interestValue:            String,
        lang:                     Language,
        hasInterestBearingCharge: Boolean
      ) =
        if (datesToAmountsValues.size === 1)
          testPaymentPlanRowsOneMonth(summary)(
            datesToAmountsValues,
            totalToPayValue,
            interestValue,
            lang,
            hasInterestBearingCharge
          )
        else if (datesToAmountsValues.size === 2)
          testPaymentPlanRowsTwoMonths(summary)(
            datesToAmountsValues,
            totalToPayValue,
            interestValue,
            lang,
            hasInterestBearingCharge
          )
        else
          testPaymentPlanRowsMoreThanTwoMonths(summary)(
            datesToAmountsValues,
            totalToPayValue,
            interestValue,
            lang,
            hasInterestBearingCharge
          )

      "should return 200 and the can you make an upfront payment page when" - {

        def test(
          journeyJsonBody:           String
        )(
          canPayUpfrontValue:        String,
          upfrontPaymentAmountValue: Option[String],
          paymentDayValue:           String,
          datesToAmountsValues:      List[(String, String)],
          totalToPayValue:           String,
          interestValue:             String,
          lang:                      Language,
          hasInterestBearingCharge:  Boolean
        ) = {
          stubCommonActions()
          EssttpBackend.EligibilityCheck.findJourney(
            testCrypto,
            origin,
            maybeChargeIsInterestBearingCharge = Some(hasInterestBearingCharge)
          )(journeyJsonBody)

          val request                = lang.fold(fakeRequest.withLangEnglish(), fakeRequest.withLangWelsh())
          val result: Future[Result] = controller.checkPaymentSchedule(request)
          val pageContent: String    = contentAsString(result)
          val doc: Document          = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1 = lang.fold("Check your payment plan", "Gwirio’ch cynllun talu"),
            shouldBackLinkBePresent = true,
            expectedSubmitUrl = Some(routes.PaymentScheduleController.checkPaymentScheduleSubmit.url),
            regimeBeingTested = Some(origin.taxRegime),
            language = lang
          )

          val summaries = doc.select(".govuk-summary-list").iterator().asScala.toList
          summaries.size shouldBe 3

          testUpfrontPaymentSummaryRows(summaries(0))(
            canPayUpfrontValue,
            upfrontPaymentAmountValue,
            List(
              lang.fold("Change to personal circumstances", "Newid yn eich amgylchiadau personol"),
              lang.fold("No money set aside to pay", "Dim arian wedi’i neilltuo i dalu")
            ),
            canPayWithinSixMonths = lang.fold("Yes", "Iawn"),
            lang
          )

          testMonthlyPaymentsRows(summaries(1))("£300", paymentDayValue, lang)

          testPaymentPlanRows(summaries(2))(
            datesToAmountsValues,
            totalToPayValue,
            interestValue,
            lang,
            hasInterestBearingCharge
          )
        }

        s"[$regime journey] there is an upfrontPayment amount with a two month plan" - {
          "(English)(hasInterestBearingCharge = true)" in {
            test(
              JourneyJsonTemplates
                .`Chosen Payment Plan`(origin = origin, maybeChargeIsInterestBearingCharge = Some(true))
            )(
              "Yes",
              Some("£123.12"),
              "28th or next working day",
              List(
                "August 2022"    -> "£555.73",
                "September 2022" -> "£555.73"
              ),
              "£1,111.47",
              "£0.06",
              Languages.English,
              hasInterestBearingCharge = true
            )
          }

          "(Welsh)(hasInterestBearingCharge = true)" in {
            test(
              JourneyJsonTemplates
                .`Chosen Payment Plan`(origin = origin, maybeChargeIsInterestBearingCharge = Some(true))
            )(
              "Iawn",
              Some("£123.12"),
              "28ain neu’r diwrnod gwaith nesaf",
              List(
                "Awst 2022" -> "£555.73",
                "Medi 2022" -> "£555.73"
              ),
              "£1,111.47",
              "£0.06",
              Languages.Welsh,
              hasInterestBearingCharge = true
            )
          }

          "(English)(hasInterestBearingCharge = false)" in {
            test(
              JourneyJsonTemplates
                .`Chosen Payment Plan`(origin = origin, maybeChargeIsInterestBearingCharge = Some(false))
            )(
              "Yes",
              Some("£123.12"),
              "28th or next working day",
              List(
                "August 2022"    -> "£555.73",
                "September 2022" -> "£555.73"
              ),
              "£1,111.47",
              "£0.06",
              Languages.English,
              hasInterestBearingCharge = false
            )
          }

          "(Welsh)(hasInterestBearingCharge = false)" in {
            test(
              JourneyJsonTemplates
                .`Chosen Payment Plan`(origin = origin, maybeChargeIsInterestBearingCharge = Some(false))
            )(
              "Iawn",
              Some("£123.12"),
              "28ain neu’r diwrnod gwaith nesaf",
              List(
                "Awst 2022" -> "£555.73",
                "Medi 2022" -> "£555.73"
              ),
              "£1,111.47",
              "£0.06",
              Languages.Welsh,
              hasInterestBearingCharge = false
            )
          }
        }

        s"[$regime journey] there is no upfrontPayment amount with a two month plan (English)" in {
          test(
            JourneyJsonTemplates.`Chosen Payment Plan`("""{ "NoUpfrontPayment" : { } }""", origin = origin)
          )(
            "No",
            None,
            "28th or next working day",
            List(
              "August 2022"    -> "£555.73",
              "September 2022" -> "£555.73"
            ),
            "£1,111.47",
            "£0.06",
            Languages.English,
            hasInterestBearingCharge = true
          )
        }

        s"[$regime journey] there is no upfrontPayment amount with a two month plan (Welsh)" in {
          test(
            JourneyJsonTemplates.`Chosen Payment Plan`("""{ "NoUpfrontPayment" : { } }""", origin = origin)
          )(
            "Na",
            None,
            "28ain neu’r diwrnod gwaith nesaf",
            List(
              "Awst 2022" -> "£555.73",
              "Medi 2022" -> "£555.73"
            ),
            "£1,111.47",
            "£0.06",
            Languages.Welsh,
            hasInterestBearingCharge = true
          )
        }

        s"[$regime journey] there is an upfrontPayment amount with a one month plan (English)" in {
          test(
            JourneyJsonTemplates.`Chosen Payment Plan`(
              origin = origin,
              selectedPlanJourneyInfo = TdJsonBodies.selectedPlanOneMonthJourneyInfo
            )
          )(
            "Yes",
            Some("£123.12"),
            "28th or next working day",
            List("August 2022" -> "£555.73"),
            "£1,111.47",
            "£0.06",
            Languages.English,
            hasInterestBearingCharge = true
          )
        }

        s"[$regime journey] there is an upfrontPayment amount with a one month plan (Welsh)" in {
          test(
            JourneyJsonTemplates.`Chosen Payment Plan`(
              origin = origin,
              selectedPlanJourneyInfo = TdJsonBodies.selectedPlanOneMonthJourneyInfo
            )
          )(
            "Iawn",
            Some("£123.12"),
            "28ain neu’r diwrnod gwaith nesaf",
            List("Awst 2022" -> "£555.73"),
            "£1,111.47",
            "£0.06",
            Languages.Welsh,
            hasInterestBearingCharge = true
          )
        }

        s"[$regime journey] there is an upfrontPayment amount with more than two months in the plan (English)" in {
          test(
            JourneyJsonTemplates.`Chosen Payment Plan`(
              origin = origin,
              selectedPlanJourneyInfo = TdJsonBodies.selectedPlanThreeMonthsJourneyInfo
            )
          )(
            "Yes",
            Some("£123.12"),
            "28th or next working day",
            List(
              "August 2022"    -> "£370.50",
              "September 2022" -> "£370.50",
              "October 2022"   -> "£370.51"
            ),
            "£1,111.51",
            "£0.10",
            Languages.English,
            hasInterestBearingCharge = true
          )
        }

        s"[$regime journey] there is an upfrontPayment amount with more than two months in the plan (Welsh)" in {
          test(
            JourneyJsonTemplates.`Chosen Payment Plan`(
              origin = origin,
              selectedPlanJourneyInfo = TdJsonBodies.selectedPlanThreeMonthsJourneyInfo
            )
          )(
            "Iawn",
            Some("£123.12"),
            "28ain neu’r diwrnod gwaith nesaf",
            List(
              "Awst 2022"   -> "£370.50",
              "Medi 2022"   -> "£370.50",
              "Hydref 2022" -> "£370.51"
            ),
            "£1,111.51",
            "£0.10",
            Languages.Welsh,
            hasInterestBearingCharge = true
          )
        }

      }

      "redirect to the missing info page if no payment plan has been selected yet" in {
        stubCommonActions()
        EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)()

        val result: Future[Result] = controller.checkPaymentSchedule(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.MissingInfoController.missingInfo.url)

      }

      "return an error when" - {

        "the journey is in state" - {

          "AfterStartedPegaCase" in {
            stubCommonActions()
            EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)()

            val exception = intercept[UpstreamErrorResponse](await(controller.checkPaymentSchedule(fakeRequest)))

            exception.statusCode shouldBe INTERNAL_SERVER_ERROR
            exception.message shouldBe "Not expecting to check payment plan here when started PEGA case"
          }

          "AfterCheckedPaymentPlan on an affordability journey" in {
            stubCommonActions()
            EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)()

            val exception = intercept[UpstreamErrorResponse](await(controller.checkPaymentSchedule(fakeRequest)))

            exception.statusCode shouldBe INTERNAL_SERVER_ERROR
            exception.message shouldBe "Not expecting to check payment plan here on affordability journey"
          }

        }

      }
    }

    s"[$regime journey] POST ${routes.PaymentScheduleController.checkPaymentScheduleSubmit.url}" - {

      "should redirect to ${routes.BankDetailsController.canSetUpDirectDebit.url} if the journey " +
        "has been updated successfully and send an audit event" in {
          stubCommonActions()
          EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)(
            JourneyJsonTemplates.`Chosen Payment Plan`(
              upfrontPaymentAmountJsonString = """{"DeclaredUpfrontPayment": {"amount": 200}}""",
              origin = origin,
              regimeDigitalCorrespondence = false
            )
          )
          EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(
            TdAll.journeyId,
            JourneyJsonTemplates.`Has Checked Payment Plan - No Affordability`(origin)
          )

          val result: Future[Result] = controller.checkPaymentScheduleSubmit(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.BankDetailsController.detailsAboutBankAccount.url)
          EssttpBackend.HasCheckedPlan.verifyUpdateHasCheckedPlanRequest(TdAll.journeyId)

          AuditConnectorStub.verifyEventAudited(
            auditType = "PlanDetails",
            auditEvent = Json
              .parse(
                s"""
                 |{
                 |        "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                 |        "origin": "${origin.toString().split('.').last}",
                 |        "canPayInSixMonths": true,
                 |        "unableToPayReason": [],
                 |        "schedule": {
                 |            "collectionDate": 28,
                 |            "collectionLengthCalendarMonths": 2,
                 |            "collections": [
                 |                {
                 |                    "amount": 555.70,
                 |                    "collectionNumber": 2,
                 |                    "paymentDate": "2022-09-28"
                 |                },
                 |                {
                 |                    "amount": 555.70,
                 |                    "collectionNumber": 1,
                 |                    "paymentDate": "2022-08-28"
                 |                }
                 |            ],
                 |            "initialPaymentAmount": 123.12,
                 |            "totalInterestCharged": 0.06,
                 |            "totalNoPayments": 3,
                 |            "totalPayable": 1111.47,
                 |            "totalPaymentWithoutInterest": 1111.41
                 |        },
                 |        "taxDetail": ${TdAll.taxDetailJsonString(origin.taxRegime)},
                 |        "taxType": "$regime"
                 |}
            """.stripMargin
              )
              .as[JsObject]
          )
        }

      "return an error when" - {

        "the journey is in state" - {

          "AfterStartedPegaCase" in {
            stubCommonActions()
            EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)()

            val exception = intercept[UpstreamErrorResponse](await(controller.checkPaymentScheduleSubmit(fakeRequest)))

            exception.statusCode shouldBe INTERNAL_SERVER_ERROR
            exception.message shouldBe "Not expecting to check payment plan here when started PEGA case"
          }

          "AfterCheckedPaymentPlan on an affordability journey" in {
            stubCommonActions()
            EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)()

            val exception = intercept[UpstreamErrorResponse](await(controller.checkPaymentScheduleSubmit(fakeRequest)))

            exception.statusCode shouldBe INTERNAL_SERVER_ERROR
            exception.message shouldBe "Not expecting to check payment plan here on affordability journey"
          }

        }

      }
    }

    s"[$regime journey] GET /check-payment-plan/change" - {

      behave like recreateSessionErrorBehaviour(controller.changeFromCheckPaymentSchedule("", _, None)(_))

      "should redirect to the correct page and update the cookie session with the pageId" - {

        def test(pageId: String, expectedRedirect: Call): Unit = {
          stubCommonActions()
          EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

          val expectedUpdatedSession = Session(
            fakeRequest.session.data.updated(
              Routing.clickedChangeFromSessionKey,
              "true"
            )
          )
          val result                 = controller.changeFromCheckPaymentSchedule(pageId, origin.taxRegime, None)(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirect.url)
          session(result) shouldBe expectedUpdatedSession
          ()
        }

        "CanPayUpfront" in {
          test("CanPayUpfront", routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment)
        }

        "UpfrontPaymentAmount" in {
          test("UpfrontPaymentAmount", routes.UpfrontPaymentController.upfrontPaymentAmount)
        }

        "MonthlyPaymentAmount" in {
          test("MonthlyPaymentAmount", routes.MonthlyPaymentAmountController.displayMonthlyPaymentAmount)
        }

        "PaymentDay" in {
          test("PaymentDay", routes.PaymentDayController.paymentDay)
        }

        "PaymentPlan" in {
          test("PaymentPlan", routes.InstalmentsController.instalmentOptions)
        }

        "WhyUnableInFull" in {
          test("WhyUnableInFull", routes.WhyCannotPayInFullController.whyCannotPayInFull)
        }

        "PayWithin6Months" in {
          test("PayWithin6Months", routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(origin.taxRegime, None))
        }

      }

      "should change the language cookie to english if lang=en is supplied as a query parameter" in {
        stubCommonActions()

        EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

        val request = fakeRequest.withLangWelsh().withHeaders(HeaderNames.REFERER -> "bleh")
        val result  =
          controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, Some(English))(request)

        cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("en")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            routes.PaymentScheduleController.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, None).url
          )
      }

      "should change the language cookie to welsh if lang=cy is supplied as a query parameter" in {
        stubCommonActions()

        EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

        val request = fakeRequest.withLangEnglish().withHeaders(HeaderNames.REFERER -> "bloh")
        val result  = controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, Some(Welsh))(request)
        cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("cy")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            routes.PaymentScheduleController.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, None).url
          )
      }

      "have the query parameters in the url" in {
        routes.PaymentScheduleController
          .changeFromCheckPaymentSchedule(
            "CanPayUpfront",
            TaxRegime.Epaye,
            Some(Languages.English)
          )
          .url shouldBe "/set-up-a-payment-plan/check-your-payment-plan/change/CanPayUpfront?regime=epaye&lang=en"

        routes.PaymentScheduleController
          .changeFromCheckPaymentSchedule(
            "CanPayUpfront",
            TaxRegime.Vat,
            Some(Languages.Welsh)
          )
          .url shouldBe "/set-up-a-payment-plan/check-your-payment-plan/change/CanPayUpfront?regime=vat&lang=cy"
      }

      "should write the correct value for 'essttpClickedChangeFrom' in the session cookie in the journey state" - {

        def test(
          stubGetJourney: () => StubMapping
        ): Unit = {
          stubCommonActions()
          stubGetJourney()

          val expectedUpdatedSession = Session(
            fakeRequest.session.data.updated(
              Routing.clickedChangeFromSessionKey,
              "true"
            )
          )
          val result                 = controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, None)(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
          session(result) shouldBe expectedUpdatedSession
          ()
        }

        "AfterStartedPegaCase" in {
          test(() => EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)())
        }

        "AfterSelectedPaymentPlan" in {
          test(() => EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)())
        }

        "AfterCheckedPaymentPlan on an affordability journey" in {
          test(() => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)())
        }

        "AfterCheckedPaymentPlan on a non-affordability journey" in {
          test(() => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)())
        }

      }

      "be able to redirect correctly when no session is found but is successfully recreated" in {
        stubCommonActions()
        EssttpBackend.findByLatestSessionNotFound()
        EssttpBackend.Pega.stubRecreateSession(
          origin.taxRegime,
          Right(Json.parse(JourneyJsonTemplates.`Started PEGA case`(origin)(using testCrypto)))
        )

        val request =
          FakeRequest("GET", s"/p?regime=${origin.taxRegime.entryName}")
            .withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId")

        val expectedUpdatedSession = Session(
          request.session.data.updated(
            Routing.clickedChangeFromSessionKey,
            "true"
          )
        )
        val result                 = controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, None)(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
        session(result) shouldBe expectedUpdatedSession

        EssttpBackend.verifyFindByLatestSessionId()
        EssttpBackend.Pega.verifyRecreateSessionCalled(origin.taxRegime)
      }

      "should return an error when" - {

        "the journey is not in an appropriate state" in {
          stubCommonActions()
          EssttpBackend.CanPayWithinSixMonths.findJourney(testCrypto, origin)()

          val error = intercept[UpstreamErrorResponse](
            await(controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime, None)(fakeRequest))
          )
          error.statusCode shouldBe INTERNAL_SERVER_ERROR
          error.getMessage should startWith("Cannot change answer from check your payment plan page in journey state")
        }

        "the pageId is not recognised" in {
          stubCommonActions()
          EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

          a[NoSuchElementException] shouldBe thrownBy(
            await(controller.changeFromCheckPaymentSchedule("abc", origin.taxRegime, None)(fakeRequest))
          )
        }
      }

    }
  }

}

object PaymentScheduleControllerSpec {

  final case class SummaryRow(question: String, answer: String, changeLink: String, changeLinkHiddenText: String)
      derives CanEqual

}
