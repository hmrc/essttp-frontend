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

import controllers.PaymentScheduleControllerSpec.SummaryRow
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.TdRequest.FakeRequestOps
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.{AuditConnectorStub, EssttpBackend}
import testsupport.testdata.{JourneyJsonTemplates, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.collection.JavaConverters._
import scala.concurrent.Future

class PaymentScheduleControllerSpec extends ItSpec {

  private val controller: PaymentScheduleController = app.injector.instanceOf[PaymentScheduleController]

  s"GET ${routes.PaymentScheduleController.checkPaymentSchedule.url}" - {

      def extractSummaryRows(elements: List[Element]): List[SummaryRow] = elements.map{ e =>
        SummaryRow(
          e.select(".govuk-summary-list__key").text(),
          e.select(".govuk-summary-list__value").text(),
          e.select(".govuk-summary-list__actions > .govuk-link").attr("href")
        )
      }

      def testUpfrontPaymentSummaryRows(summary: Element)(canPayUpfrontValue: String, upfrontPaymentAmountValue: Option[String]) = {
        val upfrontPaymentSummaryRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val canPayUpfrontRow = SummaryRow(
          "Can you make an upfront payment?",
          canPayUpfrontValue,
          routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url
        )
        val upfrontPaymentAmountRow = upfrontPaymentAmountValue.map(amount =>
          SummaryRow(
            "Taken within 10 working days",
            amount,
            routes.UpfrontPaymentController.upfrontPaymentAmount.url
          ))

        val expectedSummaryRows = List(Some(canPayUpfrontRow), upfrontPaymentAmountRow).collect{ case Some(s) => s }

        extractSummaryRows(upfrontPaymentSummaryRows) shouldBe expectedSummaryRows
      }

      def testPaymentPlanRows(summary: Element)(
          paymentDayValue:      String,
          datesToAmountsValues: List[(String, String)],
          totalToPayValue:      String
      ) = {
        val paymentPlanRows = summary.select(".govuk-summary-list__row").iterator().asScala.toList

        val paymentDayRow = SummaryRow(
          "Payments collected on",
          paymentDayValue,
          routes.PaymentDayController.paymentDay.url
        )

        val paymentAmountRows = datesToAmountsValues.map{
          case (date, amount) =>
            SummaryRow(
              date, amount, routes.InstalmentsController.instalmentOptions.url
            )
        }

        val totalToPayRow = SummaryRow("Total to pay", totalToPayValue, "")

        val expectedRows = paymentDayRow :: paymentAmountRows ::: List(totalToPayRow)

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
          EssttpBackend.EligibilityCheck.findJourney(testCrypto)(journeyJsonBody)

          val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

          val result: Future[Result] = controller.checkPaymentSchedule(fakeRequest)
          val pageContent: String = contentAsString(result)
          val doc: Document = Jsoup.parse(pageContent)

          RequestAssertions.assertGetRequestOk(result)
          ContentAssertions.commonPageChecks(
            doc,
            expectedH1        = "Check your payment plan",
            expectedBack      = Some(routes.InstalmentsController.instalmentOptions.url),
            expectedSubmitUrl = Some(routes.PaymentScheduleController.checkPaymentScheduleSubmit.url)
          )

          val summaries = doc.select(".govuk-summary-list").iterator().asScala.toList
          summaries.size shouldBe 2

          testUpfrontPaymentSummaryRows(summaries(0))(canPayUpfrontValue, upfrontPaymentAmountValue)
          testPaymentPlanRows(summaries(1))(paymentDayValue, datesToAmountsValues, totalToPayValue)
        }

      "there is an upfrontPayment amount" in {
        test(
          JourneyJsonTemplates.`Chosen Payment Plan`(encrypter = testCrypto)
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

      "there is no upfrontPayment amount" in {
        test(
          JourneyJsonTemplates.`Chosen Payment Plan`("""{ "NoUpfrontPayment" : { } }""", encrypter = testCrypto)
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
      EssttpBackend.AffordableQuotes.findJourney(testCrypto)()

      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> "IamATestSessionId")

      val result: Future[Result] = controller.checkPaymentSchedule(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.MissingInfoController.missingInfo.url)

    }

  }

  s"POST ${routes.PaymentScheduleController.checkPaymentScheduleSubmit.url}" - {

    s"should redirect to ${routes.BankDetailsController.detailsAboutBankAccount.url} if the journey " +
      "has been updated successfully and send an audit event" in {
        stubCommonActions()
        EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto)()
        EssttpBackend.HasCheckedPlan.stubUpdateHasCheckedPlan(TdAll.journeyId)

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
               |                    "amount": 55570,
               |                    "collectionNumber": 2,
               |                    "paymentDate": "2022-09-28"
               |                },
               |                {
               |                    "amount": 55570,
               |                    "collectionNumber": 1,
               |                    "paymentDate": "2022-08-28"
               |                }
               |            ],
               |            "initialPaymentAmount": 12312,
               |            "totalInterestCharged": 6,
               |            "totalNoPayments": 3,
               |            "totalPayable": 111147,
               |            "totalPaymentWithoutInterest": 111141
               |        },
               |        "taxDetail": {
               |            "accountsOfficeRef": "123PA44545546",
               |            "employerRef": "864FZ00049"
               |        },
               |        "taxType": "Epaye"
               |}
            """.stripMargin
          ).as[JsObject]
        )
      }
  }

}

object PaymentScheduleControllerSpec {

  final case class SummaryRow(question: String, answer: String, changeLink: String)

}
