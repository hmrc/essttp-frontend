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

package actions

import actions.EnrolmentDefResult.{EnrolmentNotFound, IdentifierNotFound, Inactive, Success}
import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import essttp.rootmodel.{SaUtr, Vrn}
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

    val `IR-PAYE-TaxOfficeNumber`: EnrolmentDef = EnrolmentDef("IR-PAYE", "TaxOfficeNumber")
    val `IR-PAYE-TaxOfficeReference`: EnrolmentDef = EnrolmentDef("IR-PAYE", "TaxOfficeReference")

    def findEnrolmentValues(enrolments: Enrolments): EnrolmentDefResult[(TaxOfficeNumber, TaxOfficeReference)] =
      (
        findMatchingEnrolmentsValues(enrolments, `IR-PAYE-TaxOfficeNumber`),
        findMatchingEnrolmentsValues(enrolments, `IR-PAYE-TaxOfficeReference`)
      ) match {
          case (Success(taxOfficeNumber), Success(taxOfficeReference)) =>
            Success(TaxOfficeNumber(taxOfficeNumber) -> TaxOfficeReference(taxOfficeReference))

          case (IdentifierNotFound(enrolmentDef1), IdentifierNotFound(enrolmentDef2)) =>
            IdentifierNotFound(enrolmentDef1 ++ enrolmentDef2)

          case (IdentifierNotFound(enrolmentDef), _) =>
            IdentifierNotFound(enrolmentDef)

          case (_, IdentifierNotFound(enrolmentDef)) =>
            IdentifierNotFound(enrolmentDef)

          case (_: EnrolmentNotFound[_], _) => EnrolmentNotFound()
          case (_, _: EnrolmentNotFound[_]) => EnrolmentNotFound()
          case (_: Inactive[_], _)          => Inactive()
          case (_, _: Inactive[_])          => Inactive()
        }

  }

  object Vat {

    val `HMRC-MTD-VAT`: EnrolmentDef = EnrolmentDef("HMRC-MTD-VAT", "VRN")
    val `HMCE-VATDEC-ORG`: EnrolmentDef = EnrolmentDef("HMCE-VATDEC-ORG", "VATRegNo")
    val `HMCE-VATVAR-ORG`: EnrolmentDef = EnrolmentDef("HMCE-VATVAR-ORG", "VATRegNo")

    /**
     * It extracts VRN as an option from enrolments.It might happen that enrolments have multiple vrn values.
     * If so it selects the MTD related one.
     * see https://jira.tools.tax.service.gov.uk/browse/OPS-5542
     */
    def findEnrolmentValues(enrolments: Enrolments): EnrolmentDefResult[Vrn] = {
      val mtdVatVrn: EnrolmentDefResult[String] = findMatchingEnrolmentsValues(
        enrolments,
        `HMRC-MTD-VAT`
      )
      val vatVarVrn: EnrolmentDefResult[String] = findMatchingEnrolmentsValues(
        enrolments,
        `HMCE-VATVAR-ORG`
      )
      val vatDecVrn: EnrolmentDefResult[String] = findMatchingEnrolmentsValues(
        enrolments,
        `HMCE-VATDEC-ORG`
      )

      val vrns = List(mtdVatVrn, vatVarVrn, vatDecVrn)
      val vrn = vrns.collectFirst{ case Success(vrn) => Vrn(vrn) }

      vrn.map(Success(_)).getOrElse{
        val identifierNotFounds = vrns.collect{ case n: IdentifierNotFound[_] => n }
        val enrolmentsNotFound = vrns.collect { case e: EnrolmentNotFound[_] => e }

        if (identifierNotFounds.nonEmpty)
          IdentifierNotFound(identifierNotFounds.flatMap(_.enrolmentDefs).toSet)
        else if (enrolmentsNotFound.nonEmpty)
          EnrolmentNotFound()
        else
          Inactive()
      }

    }

  }

  object Sa {

    val `IR-SA`: EnrolmentDef = EnrolmentDef("IR-SA", "UTR")

    def findEnrolmentValues(enrolments: Enrolments): EnrolmentDefResult[SaUtr] =
      findMatchingEnrolmentsValues(enrolments, `IR-SA`).map(SaUtr(_))
  }

  private def findMatchingEnrolmentsValues(enrolments: Enrolments, enrolmentDef: EnrolmentDef): EnrolmentDefResult[String] = {
    enrolments.enrolments.find(_.key.equalsIgnoreCase(enrolmentDef.enrolmentKey)) match {
      case Some(enrolment) =>
        enrolment.identifiers.find(_.key.equalsIgnoreCase(enrolmentDef.identifierKey))
          .fold[EnrolmentDefResult[String]](
            EnrolmentDefResult.IdentifierNotFound(Set(enrolmentDef))
          )(id =>
              if (enrolment.isActivated) EnrolmentDefResult.Success(id.value)
              else EnrolmentDefResult.Inactive())
      case None => EnrolmentDefResult.EnrolmentNotFound()

    }
  }

}

