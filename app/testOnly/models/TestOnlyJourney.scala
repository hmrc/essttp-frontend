/*
 * Copyright 2025 HM Revenue & Customs
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

package testOnly.models

import essttp.journey.model.JourneyId
import essttp.rootmodel.TaxRegime
import essttp.rootmodel.ttp.affordablequotes.PaymentPlan
import essttp.utils.EnumFormat
import play.api.libs.json.{Format, Json, OFormat}
import testOnly.models.formsmodel.IncomeAndExpenditure
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class TestOnlyJourney(
  journeyId:            JourneyId,
  taxRegime:            TaxRegime,
  updatedAt:            Instant,
  incomeAndExpenditure: Option[IncomeAndExpenditure],
  paymentPlan:          Option[PaymentPlan]
)

object TestOnlyJourney {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  given format: OFormat[TestOnlyJourney] = {
    given Format[Instant]   = MongoJavatimeFormats.instantFormat
    given Format[TaxRegime] = EnumFormat(TaxRegime)

    Json.format
  }

}
