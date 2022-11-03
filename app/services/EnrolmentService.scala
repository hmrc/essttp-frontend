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

package services

import actions.{EnrolmentDef, EnrolmentDefResult}
import actions.EnrolmentDefResult.{EnrolmentNotFound, IdentifierNotFound, Inactive, Success}
import actionsmodel.AuthenticatedJourneyRequest
import essttp.journey.model.Journey
import essttp.rootmodel.{EmpRef, TaxId, TaxRegime, Vrn}
import models.audit.eligibility.EnrollmentReasons
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject() (journeyService: JourneyService, auditService: AuditService)(implicit ec: ExecutionContext) {

  def determineTaxIdAndUpdateJourney(
      journey:    Journey.Stages.Started,
      enrolments: Enrolments
  )(implicit r: AuthenticatedJourneyRequest[_], hc: HeaderCarrier): Future[Option[TaxId]] = {
    journey.taxRegime match {
      case TaxRegime.Epaye =>
        val enrolmentDefResult = EnrolmentDef.Epaye.findEnrolmentValues(enrolments).map {
          case (taxOfficeNumber, taxOfficeReference) =>
            EmpRef.makeEmpRef(taxOfficeNumber, taxOfficeReference)
        }

        handleTaxRegime[EmpRef](
          enrolmentDefResult,
          journey
        )(journeyService.UpdateTaxRef.updateEpayeTaxId(journey.id, _))

      case TaxRegime.Vat =>
        handleTaxRegime[Vrn](
          EnrolmentDef.Vat.findEnrolmentValues(enrolments),
          journey
        )(journeyService.UpdateTaxRef.updateVatTaxId(journey.id, _))
    }
  }

  private def handleTaxRegime[ID <: TaxId](
      idResult: EnrolmentDefResult[ID],
      journey:  Journey.Stages.Started
  )(update: ID => Future[Journey])(implicit r: AuthenticatedJourneyRequest[_], hc: HeaderCarrier): Future[Option[ID]] = {
    idResult match {
      case Success(id) =>
        update(id).map(_ => Some(id))

      case Inactive() =>
        auditService.auditEligibilityCheck(journey, Right(EnrollmentReasons.InactiveEnrollment()))
        Future.successful(None)

      case EnrolmentNotFound() =>
        auditService.auditEligibilityCheck(journey, Left(EnrollmentReasons.NotEnrolled()))
        Future.successful(None)

      case IdentifierNotFound(enrolmentDefs) =>
        throw new RuntimeException("Identifiers not found for [(id key, enrolment key)] = " +
          s"[${enrolmentDefs.map(e => s"(${e.identifierKey}, ${e.enrolmentKey})").mkString(",")}]")

    }

  }

}
