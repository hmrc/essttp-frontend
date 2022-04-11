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

package models

import essttp.journey.JourneyConnector
import essttp.journey.model.SjResponse
import essttp.rootmodel.{ BackUrl, ReturnUrl }
import models.TaxRegimeFE.EpayeRegime
import play.api.mvc.{ Call, Request }
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait TaxOrigin[R <: TaxRegimeFE] {

  def regime: R

  def createJourney(jc: JourneyConnector)(implicit hc: HeaderCarrier, request: Request[_]): Future[SjResponse]

  def allowEnrolment(enrolment: Enrolment): Boolean = regime.allowEnrolment(enrolment)

  def journeyEntryPoint: Call

}

trait EpayeTaxOrigin extends TaxOrigin[EpayeRegime.type] {

  override def regime = EpayeRegime
}

object TaxOrigin {
  object EpayeBTA extends EpayeTaxOrigin {

    override def createJourney(jc: JourneyConnector)(implicit hc: HeaderCarrier, request: Request[_]): Future[SjResponse] = jc.Epaye.startJourneyBta(
      essttp.journey.model.SjRequest.Epaye.Simple(ReturnUrl("return here"), BackUrl("back to here")))

    override def journeyEntryPoint: Call = controllers.routes.EPayeStartController.ePayeStart()
  }

  object EpayeGovUk extends EpayeTaxOrigin {

    override def createJourney(jc: JourneyConnector)(implicit hc: HeaderCarrier, request: Request[_]): Future[SjResponse] = jc.Epaye.startJourneyGovUk(
      essttp.journey.model.SjRequest.Epaye.Empty())

    override def journeyEntryPoint: Call = controllers.routes.EPayeStartController.ePayeStart()

  }

  object EpayeNoOrigin extends EpayeTaxOrigin {

    override def createJourney(jc: JourneyConnector)(implicit hc: HeaderCarrier, request: Request[_]): Future[SjResponse] = jc.Epaye.startJourneyDetachedUrl(
      essttp.journey.model.SjRequest.Epaye.Empty())

    override def journeyEntryPoint: Call = controllers.routes.EPayeStartController.ePayeStart()

  }
}
