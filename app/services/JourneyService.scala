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

import com.google.inject.{Inject, Singleton}
import essttp.journey.JourneyConnector
import essttp.journey.model.JourneyId
import essttp.journey.model.ttp.EligibilityCheckResult
import essttp.rootmodel.{CanPayUpfront, EmpRef}
import play.api.mvc.RequestHeader
import util.Logging

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyService @Inject() (journeyConnector: JourneyConnector)(implicit ec: ExecutionContext) extends Logging {

  def updateEligibilityCheckResult(journeyId: JourneyId, eligibilityCheckResult: EligibilityCheckResult)(
      implicit
      requestHeader: RequestHeader
  ): Future[Unit] = {
    journeyConnector.updateEligibilityCheckResult(journeyId, eligibilityCheckResult)
  }

  object UpdateTaxRef {
    //paye
    def updatePayeTaxId(journeyId: JourneyId, empRef: EmpRef)(implicit requestHeader: RequestHeader): Future[Unit] = {
      journeyConnector.updateTaxId(journeyId, empRef)
    }
    //vat
    //others
  }

  def updateCanPayUpfront(journeyId: JourneyId, canPayUpfront: CanPayUpfront)(implicit requestHeader: RequestHeader): Future[Unit] = {
    journeyConnector.updateCanPayUpfront(journeyId, canPayUpfront)
  }

}
