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

package testOnly.testusermodel

import testOnly.formsmodel.{Enrolments, SignInAs, StartJourneyForm}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}

import scala.util.Random

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

/**
 * Definition of a test user.
 * We use that data to
 * log user in with defined enrolments.
 */
final case class TestUser(
    nino:           Option[Nino],
    epayeEnrolment: Option[EpayeEnrolment],
    //TODO vat enrolment
    authorityId:     AuthorityId,
    affinityGroup:   AffinityGroup,
    confidenceLevel: ConfidenceLevel
)

object TestUser {

  private implicit val random: Random = Random

  def makeTestUser(form: StartJourneyForm): Option[TestUser] = {

    println(s"I am the sign in form used in makeTestUser: ${form.toString}")

    val maybeAffinityGroup = form.signInAs match {
      case SignInAs.NoSignIn     => None
      case SignInAs.Individual   => Some(AffinityGroup.Individual)
      case SignInAs.Organisation => Some(AffinityGroup.Organisation)
    }

    maybeAffinityGroup.map { affinityGroup =>
      TestUser(
        nino            = None, //TODO: read this from the form, populate if individual
        epayeEnrolment  = if (form.enrolments.contains(Enrolments.Epaye)) Some(EpayeEnrolment(
          taxOfficeNumber    = RandomDataGenerator.nextTaxOfficeNumber(),
          taxOfficeReference = RandomDataGenerator.nextTaxOfficeReference(),
          enrolmentStatus    = EnrolmentStatus.Activated //TODO: read this from the form
        ))
        else None,
        authorityId     = RandomDataGenerator.nextAuthorityId(),
        affinityGroup   = affinityGroup,
        confidenceLevel = ConfidenceLevel.L50 //TODO: read this from the form
      )
    }
  }
}
