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

package testOnly.controllers

import _root_.actions.Actions
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.{Origin, Origins, SjRequest, SjResponse}
import essttp.rootmodel.{BackUrl, ReturnUrl, TaxRegime}
import models.ttp
import models.ttp._
import play.api.libs.json.{Format, Json}
import play.api.mvc._
import testOnly.connectors.EssttpStubConnector
import testOnly.controllers.TestOnlyController._
import testOnly.forms.TestOnlyFireStarterForm
import testOnly.views.html.TestOnlyStartPage
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyController @Inject() (
    as:                  Actions,
    appConfig:           AppConfig,
    essttpStubConnector: EssttpStubConnector,
    mcc:                 MessagesControllerComponents,
    testOnlyStartPage:   TestOnlyStartPage,
    journeyConnector:    JourneyConnector
)(implicit ec: ExecutionContext, requestHeader: RequestHeader)
  extends FrontendController(mcc)
  with Logging {

  val startJourneyGet: Action[AnyContent] = as.default { implicit request =>
    Ok(testOnlyStartPage(TestOnlyFireStarterForm.form))
  }

  val startJourneySubmit: Action[AnyContent] = as.default.async { implicit request =>
    TestOnlyFireStarterForm.form.bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(testOnlyStartPage(formWithErrors))),
        startJourney
      )
  }

  private def startJourney(testOnlyFireStarterForm: TestOnlyFireStarterForm)
    (implicit hc: HeaderCarrier): Future[Result] = {

    val origin: Origin = testOnlyFireStarterForm.origin
    val sjResponseF: Future[SjResponse] = origin match {
      case Origins.Epaye.Bta         => journeyConnector.Epaye.startJourneyBta(epayeSimple)
      case Origins.Epaye.GovUk       => journeyConnector.Epaye.startJourneyGovUk(epayeEmpty)
      case Origins.Epaye.DetachedUrl => journeyConnector.Epaye.startJourneyGovUk(epayeEmpty)
    }
    implicit val hc: HeaderCarrier = HeaderCarrier()
    for {
      response <- sjResponseF
      //TODO login and inject auth token(bearer token) into play session
      // make call to stubs too
      _ <- essttpStubConnector.insertEligibilityData(TaxRegime.Epaye, ttp.DefaultTaxId, DefaultTTP)
    } yield Redirect(response.nextUrl.value)

  }

}

object TestOnlyController {

  private def returnUrl(url: String = "test-return-url") = ReturnUrl(url)

  private def backUrl(url: String = "test-back-url") = BackUrl(url)

  private val epayeSimple = SjRequest.Epaye.Simple(returnUrl = returnUrl(), backUrl = backUrl())
  private val epayeEmpty = SjRequest.Epaye.Empty()

  def affinityGroup(auth: String): uk.gov.hmrc.auth.core.AffinityGroup = auth match {
    case "Organisation" => uk.gov.hmrc.auth.core.AffinityGroup.Organisation
    case "Individual"   => uk.gov.hmrc.auth.core.AffinityGroup.Individual
  }

  val DefaultTTP = {
    val taxPeriodCharges = TaxPeriodCharges(
      "T5545454554",
      "22000",
      "",
      "1000",
      "",
      100000,
      "2017-03-07",
      15.97,
      true,
      ChargeLocks(
        PaymentLock(false, ""),
        PaymentLock(false, ""),
        PaymentLock(false, ""),
        PaymentLock(false, ""),
        PaymentLock(false, "")
      )
    )

    val chargeTypeAssessments: List[ChargeTypeAssessment] = List(
      ChargeTypeAssessment("2020-08-13", "2020-08-14", 300000, List(taxPeriodCharges))
    )

    TtpEligibilityData(
      "SSTTP",
      "A00000000001",
      "PAYE",
      "2022-03-10",
      CustomerDetails("NI", "B5 7LN"),
      EligibilityStatus(false, 1, 6),
      EligibilityRules(false, "Receiver is not known", false, false, false, false, false, 300, 600, false, false, false),
      FinancialLimitBreached(true, 16000),
      chargeTypeAssessments
    )
  }

}
