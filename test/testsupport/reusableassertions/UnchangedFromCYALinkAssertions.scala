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

package testsupport.reusableassertions

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.{Routing, routes}
import essttp.journey.model.{Origin, WhyCannotPayInFullAnswers}
import essttp.rootmodel.TaxRegime
import models.{Language, Languages}
import org.scalatest.freespec.AnyFreeSpecLike
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testsupport.ItSpec
import testsupport.TdRequest._
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, TdAll}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

trait UnchangedFromCYALinkAssertions extends AnyFreeSpecLike { this: ItSpec =>

  def unchangedAnswerAfterClickingCYAChangeBehaviuor(
    origin:            Origin,
    action:            Action[AnyContent],
    formData:          Seq[(String, String)],
    stubUpdateJourney: String => StubMapping,
    pegaChangeLinkUrl: String = ""
  ): Unit = {
    s"[${origin.taxRegime.toString} journey] should redirect to correct place if the user came from a change link and did not " +
      "change their answer and the journey state is" - {

        def expectedPegaRedirectUrl(lang: Language) =
          if (pegaChangeLinkUrl.isEmpty)
            testOnly.controllers.routes.PegaController.start(origin.taxRegime).url
          else {
            val expectedRegiem = origin.taxRegime match {
              case TaxRegime.Epaye => "PAYE"
              case TaxRegime.Vat   => "VAT"
              case TaxRegime.Sa    => "SA"
              case TaxRegime.Simp  => throw new NotImplementedError()
            }
            val expecterLang   = lang match {
              case Languages.English => "en"
              case Languages.Welsh   => "cy"
            }

            s"$pegaChangeLinkUrl?regime=$expectedRegiem&lang=$expecterLang"
          }

        def test(
          stubGetJourney:        () => StubMapping,
          updateJourneyResponse: String,
          lang:                  Language,
          expectedRedirectUrl:   String
        ) = {
          stubCommonActions()
          stubGetJourney()
          stubUpdateJourney(updateJourneyResponse)

          val fakeRequest = FakeRequest(
            method = "POST",
            path = "/blah"
          ).withAuthToken()
            .withSession(SessionKeys.sessionId -> "IamATestSessionId", Routing.clickedChangeFromSessionKey -> "true")
            .withFormUrlEncodedBody(formData*)
            .withLang(lang)

          val result: Future[Result] = action(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedRedirectUrl)
          session(result).get(Routing.clickedChangeFromSessionKey) shouldBe None
        }

        "AfterSelectedPaymentPlan" in {
          test(
            () => EssttpBackend.SelectedPaymentPlan.findJourney(testCrypto, origin)(),
            JourneyJsonTemplates.`Chosen Payment Plan`(origin = origin)(using testCrypto),
            Languages.English,
            routes.PaymentScheduleController.checkPaymentSchedule.url
          )
        }

        "AfterCheckedPaymentPlan without affordability" in {
          test(
            () =>
              EssttpBackend.HasCheckedPlan.findJourney(
                withAffordability = false,
                testCrypto,
                origin,
                whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
              )(),
            JourneyJsonTemplates.`Has Checked Payment Plan - No Affordability`(origin)(using testCrypto),
            Languages.English,
            routes.PaymentScheduleController.checkPaymentSchedule.url
          )
        }

        "after upfront payment amount" in {
          test(
            () =>
              EssttpBackend.UpfrontPaymentAmount.findJourney(
                testCrypto,
                origin,
                whyCannotPayReasons = TdAll.whyCannotPayReasons
              )(),
            JourneyJsonTemplates.`Entered Upfront payment amount`(
              origin,
              whyCannotPayReasons = TdAll.whyCannotPayReasons
            )(using testCrypto),
            Languages.English,
            routes.UpfrontPaymentController.upfrontPaymentSummary.url
          )
        }

        if (origin.taxRegime != TaxRegime.Simp) {
          "AfterStartedPegaCase when the user is navigating in" - {

            "English" in {
              test(
                () =>
                  EssttpBackend.StartedPegaCase.findJourney(
                    testCrypto,
                    origin,
                    whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
                  )(),
                JourneyJsonTemplates.`Started PEGA case`(origin)(using testCrypto),
                Languages.English,
                expectedPegaRedirectUrl(Languages.English)
              )
            }

            "Welsh" in {
              test(
                () =>
                  EssttpBackend.StartedPegaCase.findJourney(
                    testCrypto,
                    origin,
                    whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
                  )(),
                JourneyJsonTemplates.`Started PEGA case`(origin)(using testCrypto),
                Languages.Welsh,
                expectedPegaRedirectUrl(Languages.Welsh)
              )
            }

          }

          "AfterCheckedPaymentPlan with affordability when the user is navigating in" - {

            "Engish" in {
              test(
                () =>
                  EssttpBackend.HasCheckedPlan.findJourney(
                    withAffordability = true,
                    testCrypto,
                    origin,
                    whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
                  )(),
                JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(origin)(using testCrypto),
                Languages.English,
                expectedPegaRedirectUrl(Languages.English)
              )
            }

            "Welsh" in {
              test(
                () =>
                  EssttpBackend.HasCheckedPlan.findJourney(
                    withAffordability = true,
                    testCrypto,
                    origin,
                    whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
                  )(),
                JourneyJsonTemplates.`Has Checked Payment Plan - With Affordability`(origin)(using testCrypto),
                Languages.Welsh,
                expectedPegaRedirectUrl(Languages.Welsh)
              )
            }
          }

          "after upfront payment answers if the user declared an upfront payment" in {
            test(
              () =>
                EssttpBackend.CanPayWithinSixMonths.findJourney(
                  testCrypto,
                  origin
                )(
                  JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(
                    origin,
                    whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
                  )
                ),
              JourneyJsonTemplates.`Obtained Can Pay Within 6 months - yes`(
                origin,
                whyCannotPayInFullAnswers = WhyCannotPayInFullAnswers.WhyCannotPayInFull(TdAll.whyCannotPayReasons)
              )(using testCrypto),
              Languages.English,
              routes.UpfrontPaymentController.upfrontPaymentSummary.url
            )
          }

        }

      }

  }

}
