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

package views.partials

import views.html.EPaye.ineligible.Ineligible
import views.html.partials._

import javax.inject.Inject

class Partials @Inject() (
    val ineligibleTemplatePage:     Ineligible,
    val genericIneligiblePartial:   GenericIneligiblePartial,
    val debtTooLargePartial:        DebtTooLargePartial,
    val debtTooOldPartial:          DebtTooOldPartial,
    val existingPaymentPlanPartial: ExistingPaymentPlanPartial,
    val returnsNotUpToDatePartial:  ReturnsNotUpToDatePartial
)
