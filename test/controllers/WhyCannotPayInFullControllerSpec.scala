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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.WhyCannotPayInFullControllerSpec.CheckBoxInfo
import essttp.journey.model.{Origins, WhyCannotPayInFullAnswers}
import essttp.rootmodel.CannotPayReason
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.mvc.Result
import play.api.test.Helpers._
import testsupport.ItSpec
import testsupport.reusableassertions.{ContentAssertions, RequestAssertions, UnchangedFromCYALinkAssertions}
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, TdAll}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

class WhyCannotPayInFullControllerSpec extends ItSpec, UnchangedFromCYALinkAssertions {

  val controller = app.injector.instanceOf[WhyCannotPayInFullController]

  "GET /why-are-you-unable-to-pay-in-full should" - {

    def testPageIsDisplayed(result: Future[Result], expectedPreselectedOptions: Set[CannotPayReason]): Unit = {
      RequestAssertions.assertGetRequestOk(result)

      val doc = Jsoup.parse(contentAsString(result))

      ContentAssertions.commonPageChecks(
        doc,
        "Why are you unable to pay in full?",
        shouldBackLinkBePresent = true,
        expectedSubmitUrl = Some(routes.WhyCannotPayInFullController.whyCannotPayInFullSubmit.url)
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
          "For example, unexpected repairs following theft or damage to premises.",
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
          "For example, COVID-19, extreme weather conditions.",
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
          "None of these",
          "",
          "exclusive"
        )
      )

      CannotPayReason.values.foreach { reason =>
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
        JourneyJsonTemplates.`Why Cannot Pay in Full - Required`(Origins.Epaye.Bta)(using testCrypto)
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

        val request = fakeRequest.withFormUrlEncodedBody(formData*).withMethod("POST")
        val result  = controller.whyCannotPayInFullSubmit(request)
        val doc     = Jsoup.parse(contentAsString(result))

        ContentAssertions.commonPageChecks(
          doc,
          "Why are you unable to pay in full?",
          shouldBackLinkBePresent = true,
          expectedSubmitUrl = Some(routes.WhyCannotPayInFullController.whyCannotPayInFullSubmit.url),
          hasFormError = true
        )

        val errorSummary = doc.select(".govuk-error-summary")
        val errorLink    = errorSummary.select("a")
        errorLink.text() shouldBe expectedError
        errorLink.attr("href") shouldBe "#option-UnexpectedReductionOfIncome"
        EssttpBackend.WhyCannotPayInFull.verifyNoneUpdateWhyCannotPayInFullRequest(TdAll.journeyId)
      }

      "nothing is submitted" in {
        testFormError()("Select all that apply or ‘None of these’")
      }

      "an unrecognised option is submitted" in {
        testFormError("WhyCannotPayInFull[]" -> "Unknown")("Select all that apply or ‘None of these’")
      }

      "more than one reason is selected and 'None of these' is selected" in {
        testFormError("WhyCannotPayInFull[]" -> "Other", "WhyCannotPayInFull[]" -> "Bankrupt")(
          "Select all that apply or ‘None of these’"
        )
      }

    }

    "redirect to the 'can you make an upfront' payment page' when valid form data has been submitted" in {
      stubCommonActions()
      EssttpBackend.EligibilityCheck.findJourney(testCrypto, Origins.Epaye.Bta)()
      EssttpBackend.WhyCannotPayInFull.stubUpdateWhyCannotPayInFull(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons),
        JourneyJsonTemplates.`Why Cannot Pay in Full - Required`(Origins.Epaye.Bta)(using testCrypto)
      )

      val formData = TdAll.whyCannotPayReasons.map(reason => "WhyCannotPayInFull[]" -> reason.entryName)

      val request = fakeRequest.withFormUrlEncodedBody(formData.toSeq*).withMethod("POST")
      val result  = controller.whyCannotPayInFullSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)

      EssttpBackend.WhyCannotPayInFull.verifyUpdateWhyCannotPayInFullRequest(
        TdAll.journeyId,
        WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
      )
    }

    "make a call to PEGA to start a case if the user has changed their answers after a PEGA journey has already been started" in {
      val newWhyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(Set(CannotPayReason.Other))

      stubCommonActions()
      EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)()

      EssttpBackend.WhyCannotPayInFull.stubUpdateWhyCannotPayInFull(
        TdAll.journeyId,
        newWhyCannotPayInFullAnswers,
        JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta, newWhyCannotPayInFullAnswers)(using testCrypto)
      )
      EssttpBackend.Pega.stubStartCase(TdAll.journeyId, Right(TdAll.pegaStartCaseResponse), recalculationNeeded = false)
      EssttpBackend.Pega.stubSaveJourneyForPega(TdAll.journeyId, Right(()))
      EssttpBackend.StartedPegaCase.stubUpdateStartPegaCaseResponse(
        TdAll.journeyId,
        JourneyJsonTemplates.`Started PEGA case`(Origins.Epaye.Bta)(using testCrypto)
      )

      val request = fakeRequest.withFormUrlEncodedBody("WhyCannotPayInFull[]" -> "Other").withMethod("POST")
      val result  = controller.whyCannotPayInFullSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)

      EssttpBackend.WhyCannotPayInFull
        .verifyUpdateWhyCannotPayInFullRequest(TdAll.journeyId, newWhyCannotPayInFullAnswers)
      EssttpBackend.Pega.verifyStartCaseCalled(TdAll.journeyId)
      EssttpBackend.Pega.verifySaveJourneyForPegaCalled(TdAll.journeyId)
      EssttpBackend.StartedPegaCase
        .verifyUpdateStartPegaCaseResponseRequest(TdAll.journeyId, TdAll.pegaStartCaseResponse)

    }

    "not make a call to PEGA to start a case if the user has not changed their answers after a PEGA journey has already been started" in {
      stubCommonActions()
      EssttpBackend.StartedPegaCase.findJourney(testCrypto, Origins.Epaye.Bta)(
        JourneyJsonTemplates.`Started PEGA case`(
          Origins.Epaye.Bta,
          whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
        )(using testCrypto)
      )

      val formData = TdAll.whyCannotPayReasons.map(reason => "WhyCannotPayInFull[]" -> reason.entryName)

      val request = fakeRequest.withFormUrlEncodedBody(formData.toSeq*).withMethod("POST")
      val result  = controller.whyCannotPayInFullSubmit(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UpfrontPaymentController.canYouMakeAnUpfrontPayment.url)

      EssttpBackend.Pega.verifyStartCaseNotCalled(TdAll.journeyId)
      EssttpBackend.Pega.verifySaveJourneyForPegaNotCalled(TdAll.journeyId)
    }

    behave like unchangedAnswerAfterClickingCYAChangeBehaviuor(
      Origins.Epaye.Bta,
      controller.whyCannotPayInFullSubmit,
      TdAll.whyCannotPayReasons.map(reason => "WhyCannotPayInFull[]" -> reason.entryName).toSeq,
      _ => new StubMapping()
    )

  }
}

object WhyCannotPayInFullControllerSpec {

  final case class CheckBoxInfo(value: String, label: String, hint: String, dataBehaviour: String) derives CanEqual

}

class WhyCannotPayInFullControllerPEGARedirectInConfigSpec extends ItSpec, UnchangedFromCYALinkAssertions {

  val pegaChangeLinkReturnUrl = "/abc"

  override protected lazy val configOverrides: Map[String, Any] = Map(
    "pega.change-link-return-url" -> pegaChangeLinkReturnUrl
  )

  lazy val controller = app.injector.instanceOf[WhyCannotPayInFullController]

  "When the PEGA change link return URL is defined in config" - {

    "POST why-are-you-unable-to-pay-in-full should" - {

      behave like unchangedAnswerAfterClickingCYAChangeBehaviuor(
        Origins.Epaye.Bta,
        controller.whyCannotPayInFullSubmit,
        TdAll.whyCannotPayReasons.map(reason => "WhyCannotPayInFull[]" -> reason.entryName).toSeq,
        _ => new StubMapping(),
        pegaChangeLinkUrl = pegaChangeLinkReturnUrl
      )

    }

  }

}
