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

package controllers.pagerouters

import controllers.routes
import essttp.rootmodel.ttp.EligibilityCheckResult
import models.EligibilityErrors
import models.EligibilityErrors.{DisallowedChargeLockTypes, IneligibleChargeTypes, ChargesOverMaxDebtAge, ExistingTtp, HasRlsOnAddress, IsLessThanMinDebtAllowance, IsMoreThanMaxDebtAllowance, MarkedAsInsolvent, MissingFiledReturns, MultipleReasons}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

object EligibilityRouter {

  def nextPage(eligibilityResult: EligibilityCheckResult): Result = {
    val nextUrl: Call = if (eligibilityResult.isEligible) {
      routes.YourBillController.yourBill
    } else {
      EligibilityErrors.toEligibilityError(eligibilityResult.eligibilityRules) match {
        case Some(MultipleReasons)            => routes.IneligibleController.genericIneligiblePage
        case None                             => routes.IneligibleController.genericIneligiblePage
        case Some(HasRlsOnAddress)            => routes.IneligibleController.genericIneligiblePage
        case Some(MarkedAsInsolvent)          => routes.IneligibleController.genericIneligiblePage
        case Some(IsLessThanMinDebtAllowance) => routes.IneligibleController.genericIneligiblePage
        case Some(IsMoreThanMaxDebtAllowance) => routes.IneligibleController.debtTooLargePage
        case Some(DisallowedChargeLockTypes)  => routes.IneligibleController.genericIneligiblePage
        case Some(ExistingTtp)                => routes.IneligibleController.alreadyHaveAPaymentPlanPage
        case Some(ChargesOverMaxDebtAge)      => routes.IneligibleController.debtTooOldPage
        case Some(IneligibleChargeTypes)      => routes.IneligibleController.genericIneligiblePage
        case Some(MissingFiledReturns)        => routes.IneligibleController.fileYourReturnPage
      }
    }
    Redirect(nextUrl)
  }

}
