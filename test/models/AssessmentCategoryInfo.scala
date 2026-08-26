/*
 * Copyright 2026 HM Revenue & Customs
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

package models

import essttp.rootmodel.ttp.eligibility.{AssessmentCategory, AssessmentEligibilityRules}
import play.api.libs.json.Json
import testsupport.testdata.TdAll

final case class AssessmentCategoryInfo(
  category:                   AssessmentCategory,
  assessmentEligibilityRules: AssessmentEligibilityRules = TdAll.assessmentEligibilityRules
)

object AssessmentCategoryInfo {

  def apply(
    category:          AssessmentCategory,
    eligibilityStatus: Boolean
  ): AssessmentCategoryInfo =
    if (eligibilityStatus)
      AssessmentCategoryInfo(category, TdAll.assessmentEligibilityRules)
    else
      AssessmentCategoryInfo(category, TdAll.assessmentEligibilityRules.copy(isLessThanMinDebtAllowance = true))

  extension (a: AssessmentCategoryInfo) {
    def prettyPrint: String =
      s"(category: ${a.category.entryName}, assessmentEligibilityRules: ${Json.toJson(a.assessmentEligibilityRules).toString})"
  }

}
