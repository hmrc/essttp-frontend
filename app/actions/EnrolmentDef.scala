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

package actions

import essttp.rootmodel.Vrn
import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import uk.gov.hmrc.auth.core.Enrolments

/**
 * Enrolment Definition - defines the enrolment a user has to have
 */
final case class EnrolmentDef(
    enrolmentKey:  String,
    identifierKey: String
)

object EnrolmentDef {

  object Epaye {
    def findEnrolmentValues(enrolments: Enrolments): Option[(TaxOfficeNumber, TaxOfficeReference)] = {
      for {
        taxOfficeNumber <- findMatchingEnrolmentsValues(enrolments, `IR-PAYE-TaxOfficeNumber`).headOption
        taxOfficeReference <- findMatchingEnrolmentsValues(enrolments, `IR-PAYE-TaxOfficeReference`).headOption
      } yield (TaxOfficeNumber(taxOfficeNumber), TaxOfficeReference(taxOfficeReference))
    }

    def hasRequiredEnrolments(enrolments: Enrolments): Boolean = {
      findEnrolmentValues(enrolments).isDefined
    }
    private val `IR-PAYE-TaxOfficeNumber`: EnrolmentDef = EnrolmentDef("IR-PAYE", "TaxOfficeNumber")
    private val `IR-PAYE-TaxOfficeReference`: EnrolmentDef = EnrolmentDef("IR-PAYE", "TaxOfficeReference")
  }

  object Vat {
    def findEnrolmentValues(enrolments: Enrolments): Option[Vrn] = findPrimaryVrn(enrolments)

    def hasRequiredEnrolments(enrolments: Enrolments): Boolean = {
      findEnrolmentValues(enrolments).isDefined
    }

    private val `HMRC-MTD-VAT`: EnrolmentDef = EnrolmentDef("HMRC-MTD-VAT", "VRN")
    private val `HMCE-VATDEC-ORG`: EnrolmentDef = EnrolmentDef("HMCE-VATDEC-ORG", "VATRegNo")
    private val `HMCE-VATVAR-ORG`: EnrolmentDef = EnrolmentDef("HMCE-VATVAR-ORG", "VATRegNo")

    /**
     * It extracts VRN as an option from enrolments.It might happen that enrolments have multiple vrn values.
     * If so it selects the MTD related one.
     * see https://jira.tools.tax.service.gov.uk/browse/OPS-5542
     */
    private def findPrimaryVrn(enrolments: Enrolments): Option[Vrn] = {
      val mtdVatVrn: Option[String] = findMtdVatVrns(enrolments).headOption
      val vatVarVrn: Option[String] = findVatVarVrns(enrolments).headOption
      val vatDecVrn: Option[String] = findVatDecVrns(enrolments).headOption
      mtdVatVrn.orElse(vatDecVrn).orElse(vatVarVrn).map(Vrn.apply)
    }

    private def findMtdVatVrns(enrolments: Enrolments): Set[String] =
      EnrolmentDef
        .findMatchingEnrolmentsValues(
          enrolments,
          `HMRC-MTD-VAT`
        )

    private def findVatVarVrns(enrolments: Enrolments): Set[String] =
      EnrolmentDef
        .findMatchingEnrolmentsValues(
          enrolments,
          `HMCE-VATVAR-ORG`
        )

    private def findVatDecVrns(enrolments: Enrolments): Set[String] =
      EnrolmentDef
        .findMatchingEnrolmentsValues(
          enrolments,
          `HMCE-VATDEC-ORG`
        )
  }

  /**
   * Checks if Enrolments contain entry matching EnrolmentDef
   */
  def existEnrolmentMatchingDef(enrolments: Enrolments, enrolmentDef: EnrolmentDef): Boolean =
    findMatchingEnrolmentsValues(enrolments, enrolmentDef)
      .nonEmpty

  private def findMatchingEnrolmentsValues(enrolments: Enrolments, enrolmentDef: EnrolmentDef): Set[String] = {
    for {
      enrolment <- enrolments.enrolments
      if enrolment.isActivated
      if enrolment.key.equalsIgnoreCase(enrolmentDef.enrolmentKey)
      identifier <- enrolment.identifiers
      if identifier.key.equalsIgnoreCase(enrolmentDef.identifierKey)
    } yield identifier.value
  }

}
