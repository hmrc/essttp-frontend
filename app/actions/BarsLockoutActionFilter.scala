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

import actionsmodel.AuthenticatedJourneyRequest
import controllers.{JourneyIncorrectStateRouter, routes}
import essttp.bars.BarsVerifyStatusConnector
import essttp.bars.model.BarsGetStatusParams
import essttp.journey.model.Journey
import play.api.Logging
import play.api.mvc.{ActionFilter, Request, Result, Results}

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsLockoutActionFilter @Inject() (barsVerifyStatusConnector: BarsVerifyStatusConnector)(
    implicit
    ec: ExecutionContext
) extends ActionFilter[AuthenticatedJourneyRequest] with Logging with Results {

  override protected def filter[A](journeyRequest: AuthenticatedJourneyRequest[A]): Future[Option[Result]] = {
    implicit val rh: Request[A] = journeyRequest.request

    journeyRequest.journey match {
      case j: Journey.BeforeComputedTaxId =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPageF(j).map(Some(_))
      case j: Journey.AfterComputedTaxId =>
        barsVerifyStatusConnector.status(BarsGetStatusParams(j.taxId)).map { status =>
          status.lockoutExpiryDateTime.map { expiresAt =>
            logger.info(s"BARs lockout for taxId: ${j.taxId}")
            Redirect(routes.BankDetailsController.barsLockout(formatted(expiresAt)))
          }
        }
    }
  }

  private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, h:mma")

  private def formatted(expiresAt: Instant): String = {
    fmt.format(expiresAt.atZone(ZoneId.of("Europe/London")))
      .replaceAll("AM$", "am")
      .replaceAll("PM$", "pm")
  }

  override protected def executionContext: ExecutionContext = ec
}
