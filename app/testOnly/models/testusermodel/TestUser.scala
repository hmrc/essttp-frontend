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

package testOnly.models.testusermodel

import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import essttp.rootmodel.{SaUtr, Vrn}
import testOnly.models.formsmodel.{Enrolments, SignInAs, StartJourneyForm}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}

import scala.util.Random

/** Definition of a test user. We use that data to log user in with defined enrolments.
  */
final case class TestUser(
  nino:            Option[Nino],
  epayeEnrolment:  Option[EpayeEnrolment],
  vatEnrolment:    Option[VatEnrolment],
  irSaEnrolment:   Option[IrSaEnrolment],
  mtdItEnrolment:  Option[MtdItEnrolment],
  authorityId:     AuthorityId,
  affinityGroup:   AffinityGroup,
  confidenceLevel: ConfidenceLevel
)

object TestUser {

  def makeTestUser(form: StartJourneyForm)(using Random): Option[TestUser] = {
    val maybeAffinityGroup: Option[AffinityGroup] = form.signInAs match {
      case SignInAs.NoSignIn     => None
      case SignInAs.Individual   => Some(AffinityGroup.Individual)
      case SignInAs.Organisation => Some(AffinityGroup.Organisation)
    }

    val maybeEpayeEnrolment: StartJourneyForm => Option[EpayeEnrolment] = { form =>
      if (form.enrolments.contains(Enrolments.Epaye)) {
        Some(
          EpayeEnrolment(
            taxOfficeNumber = TaxOfficeNumber(form.taxReference.value.take(3)),
            taxOfficeReference = TaxOfficeReference(form.taxReference.value.drop(3)),
            enrolmentStatus = EnrolmentStatus.Activated
          )
        )
      } else {
        None
      }
    }

    val maybeVatEnrolment: StartJourneyForm => Option[VatEnrolment] = { form =>
      if (form.enrolments.contains(Enrolments.Vat))
        Some(VatEnrolment(Vrn(form.taxReference.value), EnrolmentStatus.Activated))
      else None
    }

    val maybeIrSaEnrolment: StartJourneyForm => Option[IrSaEnrolment] = { form =>
      if (form.enrolments.contains(Enrolments.IrSa))
        Some(IrSaEnrolment(SaUtr(form.taxReference.value), EnrolmentStatus.Activated))
      else None
    }

    val maybeMdtItEnrolment: StartJourneyForm => Option[MtdItEnrolment] = { form =>
      if (form.enrolments.contains(Enrolments.MtdIt))
        Some(MtdItEnrolment(RandomDataGenerator.nextNumber(8), EnrolmentStatus.Activated))
      else None
    }

    maybeAffinityGroup.map { (affinityGroup: AffinityGroup) =>
      TestUser(
        nino = form.confidenceLevelAndNino.nino.map(Nino.apply),
        epayeEnrolment = maybeEpayeEnrolment(form),
        vatEnrolment = maybeVatEnrolment(form),
        irSaEnrolment = maybeIrSaEnrolment(form),
        mtdItEnrolment = maybeMdtItEnrolment(form),
        authorityId = form.credId.map(AuthorityId(_)).getOrElse(RandomDataGenerator.nextAuthorityId()),
        affinityGroup = affinityGroup,
        confidenceLevel = form.confidenceLevelAndNino.confidenceLevel
      )
    }
  }
}
