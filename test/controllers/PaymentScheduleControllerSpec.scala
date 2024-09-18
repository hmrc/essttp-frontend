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
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, Result, Session}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, PegaRecreateSessionAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import uk.gov.hmrc.http.{SessionKeys, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.IteratorHasAsScala

class PaymentScheduleControllerSpec extends ItSpec with PegaRecreateSessionAssertions {

  private val controller: PaymentScheduleController = app.injector.instanceOf[PaymentScheduleController]

  Seq[(String, Origin)](
    ("Epaye", Origins.Epaye.Bta),
    ("Vat", Origins.Vat.Bta),
    ("Sa", Origins.Sa.Bta),
    ("Sia", Origins.Sia.Pta)
  ).foreach {
      case (regime, origin) =>
        s"[$regime journey] GET ${routes.PaymentScheduleController.checkPaymentSchedule.url}" - {

            def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map { e =>
              SummaryRow(
                e.select(".govuk-summary-list__key").html(),
                e.select(".govuk-summary-list__value").text(),
                e.select(".govuk-summary-list__actions > .govuk-link").attr("href")
              )
            }

            def testUpfrontPaymentSummaryRows(summary: Element)(
                canPayUpfrontValue:        String,
                upfrontPaymentAmountValue: Option[String],
                reasonsToNotPayInFull:     List[String],
                canPayWithinSixMonths:     String
            ) = {
              val upfrontPaymentSummaryRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

              val whyCannotPayInFullRow = SummaryRow(
                "Why are you unable to pay in full?",
                reasonsToNotPayInFull.mkString("\n"),
                PageUrls.checkPaymentPlanChangeUrl("WhyUnableInFull", origin.taxRegime)
              )

              val canPayUpfrontRow = SummaryRow(
                "Can you make an upfront payment?",
                canPayUpfrontValue,
                PageUrls.checkPaymentPlanChangeUrl("CanPayUpfront", origin.taxRegime)
              )
              val upfrontPaymentAmountRow = upfrontPaymentAmountValue.map(amount =>
                SummaryRow(
                  "Upfront payment\n<br><span class=\"govuk-body-s\">Taken within 6 working days</span>",
                  amount,
                  PageUrls.checkPaymentPlanChangeUrl("UpfrontPaymentAmount", origin.taxRegime)
                ))

              val canPayWithinSixMonthsRow = SummaryRow(
                "Can you pay within 6 months?",
                canPayWithinSixMonths,
                PageUrls.checkPaymentPlanChangeUrl("PayWithin6Months", origin.taxRegime)
              )

              val expectedSummaryRows = List(
                Some(whyCannotPayInFullRow),
                Some(canPayUpfrontRow),
                upfrontPaymentAmountRow,
                Some(canPayWithinSixMonthsRow)
              ).collect { case Some(s) => s }

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
                PageUrls.checkPaymentPlanChangeUrl("MonthlyPaymentAmount", origin.taxRegime)
              )

              val paymentDayRow = SummaryRow(
                "Payments collected on",
                paymentDayValue,
                PageUrls.checkPaymentPlanChangeUrl("PaymentDay", origin.taxRegime)
              )

              val paymentAmountRows = datesToAmountsValues.map {
                case (date, amount) =>
                  SummaryRow(date, amount, PageUrls.checkPaymentPlanChangeUrl("PaymentPlan", origin.taxRegime))
              }

              val totalToPayRow = SummaryRow("Total to pay", totalToPayValue, "")

              val expectedRows = monthlyPaymentAmountRow :: paymentDayRow :: paymentAmountRows ::: List(totalToPayRow)

              extractSummaryRows(paymentPlanRows) shouldBe expectedRows
            }

          "should return 200 and the can you make an upfront payment page when" - {

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

                testUpfrontPaymentSummaryRows(summaries(0))(
                  canPayUpfrontValue,
                  upfrontPaymentAmountValue,
                  List("Bankrupt, Insolvent or Voluntary arrangement"),
                  canPayWithinSixMonths = "Yes"
                )
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

          "should redirect to ${routes.BankDetailsController.detailsAboutBankAccount.url} if the journey " +
            "has been updated successfully and send an audit event" in {
              stubCommonActions()
              EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)(
                JourneyJsonTemplates.`Chosen Payment Plan`(
                  upfrontPaymentAmountJsonString = """{"DeclaredUpfrontPayment": {"amount": 200}}""",
                  origin                         = origin,
                  regimeDigitalCorrespondence    = false
                )
              )
              EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(TdAll.journeyId, JourneyJsonTemplates.`Has Checked Payment Plan - No Affordability`(origin))

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
                 |        "origin": "${origin.toString().split('.').last}",
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

          behave like recreateSessionErrorBehaviour(controller.changeFromCheckPaymentSchedule("", _)(_))

          "should redirect to the correct page and update the cookie session with the pageId" - {

              def test(pageId: String, expectedRedirect: Call): Unit = {
                stubCommonActions()
                EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

                val expectedUpdatedSession = Session(
                  fakeRequest.session.data.updated(
                    Routing.clickedChangeFromSessionKey,
                    routes.PaymentScheduleController.checkPaymentSchedule.url
                  )
                )
                val result = controller.changeFromCheckPaymentSchedule(pageId, origin.taxRegime)(fakeRequest)

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
              test("PayWithin6Months", routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(origin.taxRegime))
            }

          }

          "should write the correct value for 'essttpClickedChangeFrom' in the session cookie in the journey state" - {

              def test(
                  stubGetJourney:                  () => StubMapping,
                  expectedEssttpClickedChangeFrom: Call
              ): Unit = {
                stubCommonActions()
                stubGetJourney()

                val expectedUpdatedSession = Session(
                  fakeRequest.session.data.updated(
                    Routing.clickedChangeFromSessionKey,
                    expectedEssttpClickedChangeFrom.url
                  )
                )
                val result = controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime)(fakeRequest)

                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
                session(result) shouldBe expectedUpdatedSession
                ()
              }

            "AfterStartedPegaCase" in {
              test(
                () => EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)(),
                testOnly.controllers.routes.PegaController.dummyPegaPage(origin.taxRegime)
              )
            }

            "AfterSelectedPaymentPlan" in {
              test(
                () => EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)(),
                routes.PaymentScheduleController.checkPaymentSchedule
              )
            }

            "AfterCheckedPaymentPlan on an affordability journey" in {
              test(
                () => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)(),
                testOnly.controllers.routes.PegaController.dummyPegaPage(origin.taxRegime)
              )
            }

            "AfterCheckedPaymentPlan on a non-affordability journey" in {
              test(
                () => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = false, testCrypto, origin)(),
                routes.PaymentScheduleController.checkPaymentSchedule
              )
            }

          }

          "be able to redirect correctly when no session is found but is successfully recreated" in {
            stubCommonActions()
            EssttpBackend.findByLatestSessionNotFound()
            EssttpBackend.Pega.stubRecreateSession(
              origin.taxRegime,
              Right(Json.parse(JourneyJsonTemplates.`Started PEGA case`(origin)(testCrypto)))
            )

            val request =
              FakeRequest("GET", s"/p?regime=${origin.taxRegime.entryName}")
                .withAuthToken()
                .withSession(SessionKeys.sessionId -> "IamATestSessionId")

            val expectedUpdatedSession = Session(
              request.session.data.updated(
                Routing.clickedChangeFromSessionKey,
                testOnly.controllers.routes.PegaController.dummyPegaPage(origin.taxRegime).url
              )
            )
            val result = controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime)(request)

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
                await(controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime)(fakeRequest))
              )
              error.statusCode shouldBe INTERNAL_SERVER_ERROR
              error.getMessage should startWith("Cannot change answer from check your payment plan page in journey state")
            }

            "the pageId is not recognised" in {
              stubCommonActions()
              EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)()

              a[NoSuchElementException] shouldBe thrownBy(
                await(controller.changeFromCheckPaymentSchedule("abc", origin.taxRegime)(fakeRequest))
              )
            }
          }

        }
    }

}

object PaymentScheduleControllerSpec {

  final case class SummaryRow(question: String, answer: String, changeLink: String)

}

class PaymentSchedulePegaRedirectInConfigControllerSpec extends ItSpec {

  val pegaRedirectUrl = "/redirect-to-here"

  override lazy val configOverrides: Map[String, Any] = Map(
    "pega.change-link-return-url" -> pegaRedirectUrl
  )

  private val controller: PaymentScheduleController = app.injector.instanceOf[PaymentScheduleController]

  "GET /check-payment-plan/change" - {

    "should use the configured PEGA redirect URL for 'essttpClickedChangeFrom' in the session cookie in the journey state" - {

      val origin = Origins.Epaye.Bta

        def test(stubGetJourney: () => StubMapping): Unit = {
          stubCommonActions()
          stubGetJourney()

          val expectedUpdatedSession = Session(
            fakeRequest.session.data.updated(
              Routing.clickedChangeFromSessionKey,
              pegaRedirectUrl
            )
          )
          val result = controller.changeFromCheckPaymentSchedule("CanPayUpfront", origin.taxRegime)(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)
          session(result) shouldBe expectedUpdatedSession
          ()
        }

      "AfterStartedPegaCase" in {
        test(() => EssttpBackend.StartedPegaCase.findJourney(testCrypto, origin)())
      }

      "AfterCheckedPaymentPlan on an affordability journey" in {
        test(() => EssttpBackend.HasCheckedPlan.findJourney(withAffordability = true, testCrypto, origin)())
      }

    }

  }

}

