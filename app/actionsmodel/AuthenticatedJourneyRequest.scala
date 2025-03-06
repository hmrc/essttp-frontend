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

package actionsmodel

import config.AppConfig
import essttp.bars.model.NumberOfBarsVerifyAttempts
import essttp.journey.model.{Journey, JourneyId}
import essttp.rootmodel.{GGCredId, Nino}
import essttp.rootmodel.ttp.eligibility.EligibilityCheckResult
import models.Language
import play.api.mvc._
import uk.gov.hmrc.auth.core.Enrolments

import java.time.Instant

/** Authenticated Journey requests have two stages: AuthenticatedJourneyRequest -> User has some enrolments (may not be
  * correct) and there is a journey found EligibleJourneyRequest -> User has correct enrolments, eligibility has been
  * checking in ttp api for journey and there is a journey in backend
  */

class AuthenticatedJourneyRequest[A](
  override val request:    Request[A],
  override val enrolments: Enrolments,
  val journey:             Journey,
  ggCredId:                GGCredId,
  nino:                    Option[Nino],
  override val lang:       Language
) extends AuthenticatedRequest[A](request, enrolments, ggCredId, nino, lang) {
  val journeyId: JourneyId = journey._id
}

class BarsLockedOutRequest[A](
  override val request:           Request[A],
  override val enrolments:        Enrolments,
  val journey:                    Journey,
  ggCredId:                       GGCredId,
  nino:                           Option[Nino],
  val numberOfBarsVerifyAttempts: NumberOfBarsVerifyAttempts,
  val barsLockoutExpiryTime:      Instant,
  override val lang:              Language
) extends AuthenticatedRequest[A](request, enrolments, ggCredId, nino, lang) {
  val journeyId: JourneyId = journey._id
}

class BarsNotLockedOutRequest[A](
  override val request:           Request[A],
  override val enrolments:        Enrolments,
  val journey:                    Journey,
  ggCredId:                       GGCredId,
  nino:                           Option[Nino],
  val numberOfBarsVerifyAttempts: NumberOfBarsVerifyAttempts,
  override val lang:              Language
) extends AuthenticatedRequest[A](request, enrolments, ggCredId, nino, lang) {
  val journeyId: JourneyId = journey._id
}

final class EligibleJourneyRequest[A](
  override val journey:           Journey,
  override val enrolments:        Enrolments,
  override val request:           Request[A],
  ggCredId:                       GGCredId,
  nino:                           Option[Nino],
  val numberOfBarsVerifyAttempts: NumberOfBarsVerifyAttempts,
  val eligibilityCheckResult:     EligibilityCheckResult,
  override val lang:              Language
) extends AuthenticatedJourneyRequest[A](request, enrolments, journey, ggCredId, nino, lang)

object EligibleJourneyRequest {

  extension [A](e: EligibleJourneyRequest[A]) {

    def isEmailAddressRequired(appConfig: AppConfig): Boolean =
      appConfig.emailJourneyEnabled && e.eligibilityCheckResult.regimeDigitalCorrespondence.value

  }

}
