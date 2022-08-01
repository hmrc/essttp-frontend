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

import actions.EnrolmentDef
import essttp.journey.model.Journey
import essttp.rootmodel.EmpRef
import play.api.mvc.RequestHeader
import uk.gov.hmrc.auth.core.Enrolments

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject() (journeyService: JourneyService)(implicit ec: ExecutionContext) {

  def determineTaxId(journey: Journey.Stages.Started, enrolments: Enrolments)(implicit request: RequestHeader): Future[Unit] = {
    val computeEmpRef: Future[EmpRef] = Future {
      val (taxOfficeNumber, taxOfficeReference) = EnrolmentDef
        .Epaye
        .findEnrolmentValues(enrolments)
        .getOrElse(throw new RuntimeException("TaxOfficeNumber and TaxOfficeReference not found"))
      EmpRef.makeEmpRef(taxOfficeNumber, taxOfficeReference)
    }
    for {
      empRef <- computeEmpRef
      _ <- journeyService.UpdateTaxRef.updateEpayeTaxId(journey.id, empRef)
    } yield ()
  }
}
