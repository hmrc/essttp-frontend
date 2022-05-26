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
import _root_.essttp.journey.model.ttp._
import _root_.testOnly.views.html.IAmBtaPage
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.{Origins, SjRequest}
import essttp.rootmodel.{BackUrl, ReturnUrl}
import models.EligibilityErrors._
import models.{EligibilityError, EligibilityErrors}
import play.api.mvc._
import requests.RequestSupport
import testOnly.AuthLoginApiService
import testOnly.connectors.EssttpStubConnector
import testOnly.controllers.StartJourneyController._
import testOnly.formsmodel.StartJourneyForm
import testOnly.testusermodel.TestUser
import testOnly.views.html.TestOnlyStartPage
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartJourneyController @Inject() (
    as:                  Actions,
    appConfig:           AppConfig,
    essttpStubConnector: EssttpStubConnector,
    mcc:                 MessagesControllerComponents,
    testOnlyStartPage:   TestOnlyStartPage,
    journeyConnector:    JourneyConnector,
    loginService:        AuthLoginApiService,
    iAmBtaPage:          IAmBtaPage,
    requestSupport:      RequestSupport
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val startJourneyGet: Action[AnyContent] = as.default { implicit request =>
    Ok(testOnlyStartPage(StartJourneyForm.form))
  }

  val startJourneySubmit: Action[AnyContent] = as.default.async { implicit request =>
    StartJourneyForm.form.bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(testOnlyStartPage(formWithErrors))),
        startJourney
      )
  }

  private def startJourney(startJourneyForm: StartJourneyForm): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    for {
      _ <- essttpStubConnector.primeStubs(makeEligibilityCheckResult(startJourneyForm))
      maybeTestUser = TestUser.makeTestUser(startJourneyForm)
      session <- maybeTestUser.map(testUser => loginService.logIn(testUser)).getOrElse(Future.successful(Session.emptyCookie))
      redirect: Result = startJourneyForm.origin match {
        case Origins.Epaye.Bta         => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showBtaPage())
        case Origins.Epaye.GovUk       => Redirect("https://github.com/hmrc/essttp-frontend#emulate-start-journey-from-gov-uk")
        case Origins.Epaye.DetachedUrl => Redirect(_root_.controllers.routes.EpayeGovUkController.startJourney().url)
      }
    } yield redirect.withSession(session)
  }

  /**
   * Pretends being a Bta page
   */
  val showBtaPage: Action[AnyContent] = as.default { implicit request =>
    if (hc.sessionId.isEmpty) {
      Ok("Missing session id")
    } else {
      Ok(iAmBtaPage())
    }
  }

  /**
   * Pretends for testing purposes that journey started from Bta
   */
  val startJourneyEpayeBta: Action[AnyContent] = as.default.async { implicit request =>
    if (hc.sessionId.isEmpty) {
      Future.successful(Ok("Missing session id"))
    } else {
      journeyConnector.Epaye.startJourneyBta(SjRequest.Epaye.Simple(
        returnUrl = ReturnUrl(routes.StartJourneyController.showBtaPage().url + "?return-page"),
        backUrl   = BackUrl(routes.StartJourneyController.showBtaPage().url + "?starting-page")
      )).map(x => Redirect(x.nextUrl.value))
    }
  }
}

object StartJourneyController {

  def affinityGroup(auth: String): uk.gov.hmrc.auth.core.AffinityGroup = auth match {
    case "Organisation" => uk.gov.hmrc.auth.core.AffinityGroup.Organisation
    case "Individual"   => uk.gov.hmrc.auth.core.AffinityGroup.Individual
  }

  private def makeEligibilityCheckResult(form: StartJourneyForm): EligibilityCheckResult = {

    val disallowedChargeLocks = essttp.journey.model.ttp.DisallowedChargeLocks(
      ChargeId("A00000000001"),
      MainTrans("mainTrans"),
      MainTransDesc("mainTransDesc"),
      SubTrans("subTrans"),
      SubTransDesc("subTransDesc"),
      OutstandingDebtAmount(100000),
      InterestStartDate("2017-03-07"),
      AccruedInterestToDate(15.97),
      ChargeLocks(
        PaymentLock(status = false, reason = ""),
        PaymentLock(status = false, reason = ""),
        PaymentLock(status = false, reason = ""),
        PaymentLock(status = false, reason = "")
      )
    )

    val chargeTypeAssessments: List[ChargeTypeAssessment] = List(
      ChargeTypeAssessment(TaxPeriodFrom("2020-08-13"), TaxPeriodTo("2020-08-14"), DebtTotalAmount(300000), List(disallowedChargeLocks))
    )

    val containsError: EligibilityError => Boolean = (ee: EligibilityError) => form.eligibilityErrors.contains(ee)
    val eligibilityRules: EligibilityRules = {
      EligibilityRules(
        hasRlsOnAddress            = containsError(HasRlsOnAddress),
        markedAsInsolvent          = containsError(MarkedAsInsolvent),
        isLessThanMinDebtAllowance = containsError(IsLessThanMinDebtAllowance),
        isMoreThanMaxDebtAllowance = containsError(IsMoreThanMaxDebtAllowance),
        disallowedChargeLocks      = containsError(EligibilityErrors.DisallowedChargeLocks),
        existingTTP                = containsError(ExistingTtp),
        exceedsMaxDebtAge          = containsError(ExceedsMaxDebtAge),
        eligibleChargeType         = containsError(EligibleChargeType),
        missingFiledReturns        = containsError(MissingFiledReturns)
      )
    }
    EligibilityCheckResult(
      idType               = IdType("SSTTP"),
      idNumber             = IdNumber(form.empRef.value),
      regimeType           = RegimeType("PAYE"),
      processingDate       = ProcessingDate(""),
      customerDetails      = CustomerDetails(Country("Narnia"), PostCode("AA11AA")),
      minPlanLengthMonths  = MinPlanLengthMonths(1),
      maxPlanLengthMonths  = MaxPlanLengthMonths(3),
      eligibilityStatus    = EligibilityStatus(OverallEligibilityStatus(
        eligibilityRules.isEligible
      )),
      eligibilityRules     = eligibilityRules,
      chargeTypeAssessment = chargeTypeAssessments
    )
  }

}