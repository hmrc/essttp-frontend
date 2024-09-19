/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.WhyCannotPayInFullControllerSpec.CheckBoxInfo
import essttp.journey.model.{Origins, WhyCannotPayInFullAnswers}
import essttp.rootmodel.CannotPayReason
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.mvc.Result
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, TdAll}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class WhyCannotPayInFullControllerSpec extends ItSpec {

  val controller = app.injector.instanceOf[WhyCannotPayInFullController]

  "GET /why-are-you-unable-to-pay-in-full should" - {

      def testPageIsDisplayed(result: Future[Result], expectedPreselectedOptions: Set[CannotPayReason]): Unit = {
        RequestAssertions.assertGetRequestOk(result)

        val doc = Jsoup.parse(contentAsString(result))

        ContentAssertions.commonPageChecks(
          doc,
          "Why are you unable to pay in full?",
          shouldBackLinkBePresent = true,
          expectedSubmitUrl       = Some(routes.WhyCannotPayInFullController.whyCannotPayInFullSubmit.url)
        )

        val hint = doc.select(".govuk-form-group > .govuk-fieldset > .govuk-hint").text()
        hint shouldBe "Your answers help us plan services in the future. Select all that apply."

        val checkboxes: List[Element] = doc.select(".govuk-checkboxes__item").asScala.toList
        checkboxes.size shouldBe 8

        val valuesWithLabelsHintsAndDataBehaviour = checkboxes.map { checkbox =>
          CheckBoxInfo(
            checkbox.select(".govuk-checkboxes__input").`val`(),
            checkbox.select(".govuk-checkboxes__label").text(),
            checkbox.select(".govuk-checkboxes__hint").text(),
            checkbox.select(".govuk-checkboxes__input").attr("data-behaviour")
          )
        }

        valuesWithLabelsHintsAndDataBehaviour shouldBe List(
          CheckBoxInfo(
            "UnexpectedReductionOfIncome",
            "Unexpected reduction of income",
            "For example, lost or reduced business or unemployment.",
            ""
          ),
          CheckBoxInfo(
            "UnexpectedIncreaseInSpending",
            "Unexpected increase in spending",
            "For example unexpected repairs following theft or damage to premises.",
            ""
          ),
          CheckBoxInfo(
            "LostOrReducedAbilityToEarnOrTrade",
            "Lost or reduced ability to earn or trade",
            "",
            ""
          ),
          CheckBoxInfo(
            "NationalOrLocalDisaster",
            "National or local disaster",
            "For example COVID-19, extreme weather conditions.",
            ""
          ),
          CheckBoxInfo(
            "ChangeToPersonalCircumstances",
            "Change to personal circumstances",
            "For example, ill health or bereavement.",
            ""
          ),
          CheckBoxInfo(
            "NoMoneySetAside",
            "No money set aside to pay",
            "",
            ""
          ),
          CheckBoxInfo(
            "WaitingForRefund",
            "Waiting for a refund from HMRC",
            "",
            ""
          ),
          CheckBoxInfo(
            "Other",
            "None of the above",
            "",
            "exclusive"
          )
        )

        CannotPayReason.values.foreach{ reason =>
          val checkboxInput = doc.select(s"#option-${reason.entryName}")
          checkboxInput.hasClass("govuk-checkboxes__input") shouldBe true
          checkboxInput.hasAttr("checked") shouldBe expectedPreselectedOptions.contains(reason)
        }
      }

    "not show the page if eligibility has not been checked yet" in {
      stubCommonActions()
      EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()

      val result = controller.whyCannotPayInFull(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.DetermineEligibilityController.determineEligibility.url)
    }

    "display the page when options have not been previously selected" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.Bta)()

      val result = controller.whyCannotPayInFull(fakeRequest)
      testPageIsDisplayed(result, Set.empty)
    }

    "display the page when options have been previously selected" in {
      stubCommonActions()
      EssttpBackend.WhyCannotPayInFull.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Why Cannot Pay in Full - Required`(Origins.Epaye.Bta)(testCrypto)
      )

      val result = controller.whyCannotPayInFull(fakeRequest)
      testPageIsDisplayed(result, TdAll.whyCannotPayReasons)
    }

  }

  "POST why-are-you-unable-to-pay-in-full should" - {

    "return a form error when" - {

        def testFormError(formData: (String, String)*)(expectedError: String): Unit = {
          stubCommonActions()
          EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.Bta)()

          val request = fakeRequest.withFormUrlEncodedBody(formData: _*).withMethod("POST")
          val result = controller.whyCannotPayInFullSubmit(request)
          val doc = Jsoup.parse(contentAsString(result))

          ContentAssertions.commonPageChecks(
            doc,
            "Why are you unable to pay in full?",
            shouldBackLinkBePresent = true,
            expectedSubmitUrl       = Some(routes.WhyCannotPayInFullController.whyCannotPayInFullSubmit.url),
            hasFormError            = true
          )

          val errorSummary = doc.select(".govuk-error-summary")
          val errorLink = errorSummary.select("a")
          errorLink.text() shouldBe expectedError
          errorLink.attr("href") shouldBe "#WhyCannotPayInFull"
          EssttpBackend.WhyCannotPayInFull.verifyNoneUpdateWhyCannotPayInFullRequest(TdAll.journeyId)
        }

      "nothing is submitted" in {
        testFormError()("Select all that apply or ‘none of the above’")
      }

      "an unrecognised option is submitted" in {
        testFormError("WhyCannotPayInFull[]" -> "Unknown")("Select all that apply or ‘none of the above’")
      }

      "more than one reason is selected and 'None of the above' is selected" in {
        testFormError("WhyCannotPayInFull[]" -> "Other", "WhyCannotPayInFull[]" -> "Bankrupt")("Select all that apply or ‘none of the above’")
      }

    }

    "redirect to the 'can you make an upfront' payment page' when valid form data has been submitted" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.WhyCannotPayInFull.stubUpdateWhyCannotPayInFull(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons),
        JourneyJsonTemplates.`Why Cannot Pay in Full - Required`(Origins.Epaye.Bta)(testCrypto)
      )

      val formData = TdAll.whyCannotPayReasons.map(reason => "WhyCannotPayInFull[]" -> reason.entryName)

      val request = fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*).withMethod("POST")
      val result = controller.whyCannotPayInFullSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)

      EssttpBackend.WhyCannotPayInFull.verifyUpdateWhyCannotPayInFullRequest(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
      )
    }

  }
}

object WhyCannotPayInFullControllerSpec {

  final case class CheckBoxInfo(value: String, label: String, hint: String, dataBehaviour: String)

}
