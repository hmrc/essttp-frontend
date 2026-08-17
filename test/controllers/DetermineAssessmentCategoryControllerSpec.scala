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
import essttp.rootmodel.ttp.eligibility.AssessmentCategory
import models.AssessmentCategoryInfo
import play.api.http.Status
import play.api.test.Helpers.*
import testsupport.stubs.EssttpBackend
import testsupport.testdata.{JourneyJsonTemplates, PageUrls, TdAll}
import testsupport.ItSpec

class DetermineAssessmentCategoryControllerSpec extends ItSpec {

  private val controller: DetermineAssessmentCategoryController =
    app.injector.instanceOf[DetermineAssessmentCategoryController]

  "DetermineASsessmentCategoryController when" - {

    "determineAssessmentCategory is called should" - {

      "redirect to the correct page when" - {

        "eligibility has not been checked yet" in {
          stubCommonActions()
          EssttpBackend.DetermineTaxId.findJourney(Origins.Epaye.Bta)()

          val result = controller.determineAssessmentCategory(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.determineEligibilityUrl)
        }

        AssessmentCategory.values
          .map { a =>
            val expectedRedirect = a match {
              case AssessmentCategory.Standard            => PageUrls.yourBillIsUrl
              case AssessmentCategory.Liabilities         => PageUrls.yourUpcomingBillIsUrl
              case AssessmentCategory.Debts               => PageUrls.yourBillIsUrl
              case AssessmentCategory.DebtsAndLiabilities => PageUrls.yourBillCombinedUrl
            }
            a -> expectedRedirect
          }
          .foreach { (assessmentCategory, expectedRedirect) =>
            s"the assessment category ${assessmentCategory.toString} has already been determined" in {
              stubCommonActions()
              EssttpBackend.DetermineAssessmentCategory
                .findJourney(testCrypto, Origins.Epaye.Bta, assessmentCategory)()

              val result = controller.determineAssessmentCategory(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirect)
            }
          }

      }

      "update the assessment category and redirect to the correct page when " +
        "the eligibility check result contains assessment category:" - {

          def test(
            eligibilityAssessmentCategories: Seq[AssessmentCategoryInfo],
            expectedAssessmentCategory:      AssessmentCategory,
            expectedRedirect:                String
          ): Unit = {
            stubCommonActions()
            EssttpBackend.EligibilityCheck
              .findJourney(testCrypto, Origins.Epaye.Bta, assessmentCategories = eligibilityAssessmentCategories)()
            EssttpBackend.DetermineAssessmentCategory.stubUpdateAssessmentCategory(
              TdAll.journeyId,
              JourneyJsonTemplates
                .`Assessment Category Determined`(Origins.Epaye.Bta, assessmentCategory = expectedAssessmentCategory)(
                  using testCrypto
                )
            )

            val result = controller.determineAssessmentCategory(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirect)

            EssttpBackend.DetermineAssessmentCategory.verifyAssessmentCategoryRequest(
              TdAll.journeyId,
              expectedAssessmentCategory
            )
            ()
          }

          "standard" in {
            test(
              Seq(AssessmentCategoryInfo(AssessmentCategory.Standard)),
              AssessmentCategory.Standard,
              PageUrls.yourBillIsUrl
            )
          }

          "debts" in {
            test(
              Seq(AssessmentCategoryInfo(AssessmentCategory.Debts)),
              AssessmentCategory.Debts,
              PageUrls.yourBillIsUrl
            )
          }

          "liabilities" in {
            test(
              Seq(AssessmentCategoryInfo(AssessmentCategory.Liabilities)),
              AssessmentCategory.Liabilities,
              PageUrls.yourUpcomingBillIsUrl
            )
          }

          "debtsAndLiabilities if debts are ineligible but liabilities and debtsAndLiabilities are eligible" in {
            test(
              Seq(
                AssessmentCategoryInfo(AssessmentCategory.Debts, eligibilityStatus = false),
                AssessmentCategoryInfo(AssessmentCategory.Liabilities),
                AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities)
              ),
              AssessmentCategory.DebtsAndLiabilities,
              PageUrls.yourBillCombinedUrl
            )
          }

          "debts if debts are eligible but liabilities and debtsAndLiabilities are ineligible" in {
            test(
              Seq(
                AssessmentCategoryInfo(AssessmentCategory.Debts),
                AssessmentCategoryInfo(AssessmentCategory.Liabilities, eligibilityStatus = false),
                AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, eligibilityStatus = false)
              ),
              AssessmentCategory.Debts,
              PageUrls.yourBillIsUrl
            )
          }
        }

      "not update the assessment category and redirect to the correct page when " +
        "the eligibility check result contains assessment category debts and liabilities" in {
          stubCommonActions()
          EssttpBackend.EligibilityCheck
            .findJourney(
              testCrypto,
              Origins.Epaye.Bta,
              assessmentCategories = Seq(
                AssessmentCategoryInfo(AssessmentCategory.Debts),
                AssessmentCategoryInfo(AssessmentCategory.Liabilities),
                AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities)
              )
            )()

          val result = controller.determineAssessmentCategory(fakeRequest)
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(PageUrls.yourBillIsUrl)

          EssttpBackend.DetermineAssessmentCategory.verifyAssessmentCategoryUpdateNotCalled(
            TdAll.journeyId
          )
        }

      Seq(
        (true, false, true),
        (true, true, false),
        (false, true, false),
        (false, false, true),
        (false, false, false)
      ).foreach { (debtsEligible, liabilitiesEligible, debtsAndLiabilitiesEligible) =>
        s"return an error if the eligibility check result contains debts and liabilities assessment categories " +
          s"with an unknown combination of eligibility statuses: debts: $debtsEligible, liabilities: $liabilitiesEligible, debtsAndLiabilities: $debtsAndLiabilitiesEligible" in {
            stubCommonActions()
            EssttpBackend.EligibilityCheck
              .findJourney(
                testCrypto,
                Origins.Epaye.Bta,
                assessmentCategories = Seq(
                  AssessmentCategoryInfo(AssessmentCategory.Debts, debtsEligible),
                  AssessmentCategoryInfo(AssessmentCategory.Liabilities, liabilitiesEligible),
                  AssessmentCategoryInfo(AssessmentCategory.DebtsAndLiabilities, debtsAndLiabilitiesEligible)
                )
              )()

            val error = intercept[Exception](await(controller.determineAssessmentCategory(fakeRequest)))
            error.getMessage should include(
              s"Got unexpected eligibility status for debts and liabilities: debts: $debtsEligible, liabilities: $liabilitiesEligible, debtsAndLiabilities: $debtsAndLiabilitiesEligible"
            )

          }

      }

    }
  }
}
