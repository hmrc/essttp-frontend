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

import controllers.PaymentScheduleControllerSpec.SummaryRow
import essttp.journey.model.{Origin, Origins}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IteratorHasAsScala

class PaymentScheduleControllerSpec extends ItSpec {

  private val controller: PaymentScheduleController = app.injector.instanceOf[PaymentScheduleController]
  Seq[(String, Origin)](
    ("Epaye", Origins.Epaye.Bta),
    ("Vat", Origins.Vat.Bta),
    ("Sa", Origins.Sa.Bta)
  ).foreach {
      case (regime, origin) =>
        s"GET ${routes.PaymentScheduleController.checkPaymentSchedule.url}" - {

            def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map { e =>
              SummaryRow(
                e.select(".govuk-summary-list__key").html(),
                e.select(".govuk-summary-list__value").text(),
                e.select(".govuk-summary-list__actions > .govuk-link").attr("href")
              )
            }

            def testUpfrontPaymentSummaryRows(summary: Element)(canPayUpfrontValue: String, upfrontPaymentAmountValue: Option[String]) = {
              val upfrontPaymentSummaryRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

              val canPayUpfrontRow = SummaryRow(
                "Can you make an upfront payment?",
                canPayUpfrontValue,
                PageUrls.checkPaymentPlanChangeUrl("CanPayUpfront")
              )
              val upfrontPaymentAmountRow = upfrontPaymentAmountValue.map(amount =>
                SummaryRow(
                  "Upfront payment\n<br><span class=\"govuk-body-s\">Taken within 6 working days</span>",
                  amount,
                  PageUrls.checkPaymentPlanChangeUrl("UpfrontPaymentAmount")
                ))

              val expectedSummaryRows = List(Some(canPayUpfrontRow), upfrontPaymentAmountRow).collect { case Some(s) => s }

              extractSummaryRows(upfrontPaymentSummaryRows) shouldBe expectedSummaryRows
            }

            def testPaymentPlanRows(summary: Element)(
                affordableMonthlyPaymentAmount: String,
                paymentDayValue:                String,
                datesToAmountsValues:           List[(String, String)],
                totalToPayValue:                String
            ) = {
              val paymentPlanRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

              val monthlyPaymentAmountRow = SummaryRow(
                "How much can you afford to pay each month?",
                affordableMonthlyPaymentAmount,
                PageUrls.checkPaymentPlanChangeUrl("MonthlyPaymentAmount")
              )

              val paymentDayRow = SummaryRow(
                "Payments collected on",
                paymentDayValue,
                PageUrls.checkPaymentPlanChangeUrl("PaymentDay")
              )

              val paymentAmountRows = datesToAmountsValues.map {
                case (date, amount) =>
                  SummaryRow(date, amount, PageUrls.checkPaymentPlanChangeUrl("PaymentPlan"))
              }

              val totalToPayRow = SummaryRow("Total to pay", totalToPayValue, "")

              val expectedRows = monthlyPaymentAmountRow :: paymentDayRow :: paymentAmountRows ::: List(totalToPayRow)

              extractSummaryRows(paymentPlanRows) shouldBe expectedRows
            }

          s"[$regime journey] should return 200 and the can you make an upfront payment page when" - {

              def test(
                  journeyJsonBody: String
              )(
                  canPayUpfrontValue:        String,
                  upfrontPaymentAmountValue: Option[String],
                  paymentDayValue:           String,
                  datesToAmountsValues:      List[(String, String)],
                  totalToPayValue:           String
              ) = {
                stubCommonActions()
                EssttpBackend.EligibilityCheck.findJourney(testCrypto, origin)(journeyJsonBody)

                val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

                val result: Future[Result] = controller.checkPaymentSchedule(fakeRequest)
                val pageContent: String = contentAsString(result)
                val doc: Document = Jsoup.parse(pageContent)

                RequestAssertions.assertGetRequestOk(result)
                ContentAssertions.commonPageChecks(
                  doc,
                  expectedH1              = "Check your payment plan",
                  shouldBackLinkBePresent = true,
                  expectedSubmitUrl       = Some(routes.PaymentScheduleController.checkPaymentScheduleSubmit.url),
                  regimeBeingTested       = Some(origin.taxRegime)
                )

                val summaries = doc.select(".govuk-summary-list").iterator().asScala.toList
                summaries.size shouldBe 2

                testUpfrontPaymentSummaryRows(summaries(0))(canPayUpfrontValue, upfrontPaymentAmountValue)
                testPaymentPlanRows(summaries(1))("£300", paymentDayValue, datesToAmountsValues, totalToPayValue)
              }

            s"[$regime journey] there is an upfrontPayment amount" in {
              test(
                JourneyJsonTemplates.`Chosen Payment Plan`(origin = origin)
              )(
                  "Yes",
                  Some("£123.12"),
                  "28th or next working day",
                  List(
                    "August 2022" -> "£555.73",
                    "September 2022" -> "£555.73"
                  ),
                  "£1,111.47"
                )
            }

            s"[$regime journey] there is no upfrontPayment amount" in {
              test(
                JourneyJsonTemplates.`Chosen Payment Plan`("""{ "NoUpfrontPayment" : { } }""", origin = origin)
              )(
                  "No",
                  None,
                  "28th or next working day",
                  List(
                    "August 2022" -> "£555.73",
                    "September 2022" -> "£555.73"
                  ),
                  "£1,111.47"
                )
            }
          }

          s"[$regime journey] redirect to the missing info page if no payment plan has been selected yet" in {
            stubCommonActions()
            EssttpBackend.AffordableQuotes.findJourney(testCrypto, origin)()

            val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val result: Future[Result] = controller.checkPaymentSchedule(fakeRequest)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.MissingInfoController.missingInfo.url)

          }
        }

        s"POST ${routes.PaymentScheduleController.checkPaymentScheduleSubmit.url}" - {

          s"[$regime journey] should redirect to ${routes.BankDetailsController.detailsAboutBankAccount.url} if the journey " +
            "has been updated successfully and send an audit event" in {
              stubCommonActions()
              EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)(
                JourneyJsonTemplates.`Chosen Payment Plan`(
                  upfrontPaymentAmountJsonString = """{"DeclaredUpfrontPayment": {"amount": 200}}""",
                  origin                         = origin,
                  regimeDigitalCorrespondence    = false
                )
              )
              EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(TdAll.journeyId, JourneyJsonTemplates.`Has Checked Payment Plan`(origin))

              val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

              val result: Future[Result] = controller.checkPaymentScheduleSubmit(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(routes.BankDetailsController.detailsAboutBankAccount.url)
              EssttpBackend.HasCheckedPlan.verifyUpdateHasCheckedPlanRequest(TdAll.journeyId)

              AuditConnectorStub.verifyEventAudited(
                auditType  = "PlanDetails",
                auditEvent = Json.parse(
                  s"""
                 |{
                 |        "correlationId": "8d89a98b-0b26-4ab2-8114-f7c7c81c3059",
                 |        "origin": "Bta",
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
                ).as[JsObject]
              )
            }
        }

        "GET /check-payment-plan/change" - {

          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

          s"[$regime journey] should redirect to the correct page and update the cookie session with the pageId" - {

              def test(pageId: String, expectedRedirect: Call): Unit = {
                stubCommonActions()
                EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

                val expectedUpdatedSession = Session(
                  fakeRequest.session.data.updated(Routing.clickedChangeFromSessionKey, routes.PaymentScheduleController.checkPaymentSchedule.url)
                )
                val result = controller.changeFromCheckPaymentSchedule(pageId)(fakeRequest)

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

          }

          s"[$regime journey] should return an error when the pageId is not recognised" in {
            stubCommonActions()
            EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

            a[NoSuchElementException] shouldBe thrownBy(
              await(controller.changeFromCheckPaymentSchedule("abc")(fakeRequest))
            )
          }

        }
    }

}

object PaymentScheduleControllerSpec {

  final case class SummaryRow(question: String, answer: String, changeLink: String)

}
