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
import essttp.journey.model.ttp._
import essttp.journey.model.{Origins, SjRequest}
import essttp.rootmodel.{BackUrl, ReturnUrl}
import play.api.mvc._
import testOnly.AuthLoginApiService
import testOnly.connectors.EssttpStubConnector
import testOnly.controllers.StartJourneyController._
import testOnly.formsmodel.StartJourneyForm
import testOnly.models.EligibilityError
import testOnly.models.EligibilityErrors._
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
    loginService:        AuthLoginApiService
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

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

  private def startJourney(startJourneyForm: StartJourneyForm)(implicit request: RequestHeader): Future[Result] = {
    logger.debug(s"Start journey payload: ${epayeSimple}")
    val redirectUrl: Future[String] = startJourneyForm.origin match {
      case Origins.Epaye.Bta         => journeyConnector.Epaye.startJourneyBta(epayeSimple).map(_.nextUrl.value)
      case Origins.Epaye.GovUk       => Future.successful(_root_.controllers.routes.EpayeGovUkController.startJourney().url) //TODO send to github pages and aks to click the link
      case Origins.Epaye.DetachedUrl => Future.successful(_root_.controllers.routes.EpayeGovUkController.startJourney().url)
    }

    implicit val hc: HeaderCarrier = HeaderCarrier()

    for {
      _ <- essttpStubConnector.insertEligibilityData(defaultTTP(startJourneyForm))
      maybeTestUser = TestUser.makeTestUser(startJourneyForm)
      session <- maybeTestUser.map(testUser => loginService.logIn(testUser)).getOrElse(Future.successful(Session.emptyCookie))
      redirectUrl <- redirectUrl
    } yield Redirect(redirectUrl).withSession(session)
  }
}

object StartJourneyController {

  private def returnUrl(url: String = "test-return-url") = ReturnUrl(url)

  private def backUrl(url: String = "test-back-url") = BackUrl(url)

  private val epayeSimple = SjRequest.Epaye.Simple(returnUrl = returnUrl(), backUrl = backUrl())
  private val epayeEmpty = SjRequest.Epaye.Empty()

  def affinityGroup(auth: String): uk.gov.hmrc.auth.core.AffinityGroup = auth match {
    case "Organisation" => uk.gov.hmrc.auth.core.AffinityGroup.Organisation
    case "Individual"   => uk.gov.hmrc.auth.core.AffinityGroup.Individual
  }

  val defaultTTP: StartJourneyForm => EligibilityCheckResult = { form: StartJourneyForm =>
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
    val eligibilityRulesFromForm: EligibilityRules = {
      EligibilityRules(
        hasRlsOnAddress            = containsError(HasRlsOnAddress),
        markedAsInsolvent          = containsError(MarkedAsInsolvent),
        isLessThanMinDebtAllowance = containsError(IsLessThanMinDebtAllowance),
        isMoreThanMaxDebtAllowance = containsError(IsMoreThanMaxDebtAllowance),
        disallowedChargeLocks      = containsError(testOnly.models.EligibilityErrors.DisallowedChargeLocks),
        existingTTP                = containsError(ExistingTTP),
        exceedsMaxDebtAge          = containsError(ExceedsMaxDebtAge),
        eligibleChargeType         = containsError(EligibleChargeType),
        missingFiledReturns        = containsError(MissingFiledReturns)
      )
    }

    EligibilityCheckResult(
      idType               = IdType("SSTTP"),
      idNumber             = IdNumber("A00000000001"),
      regimeType           = RegimeType("PAYE"),
      processingDate       = ProcessingDate(""),
      customerDetails      = CustomerDetails(Country("Narnia"), PostCode("AA11AA")),
      minPlanLengthMonths  = MinPlanLengthMonths(1),
      maxPlanLengthMonths  = MaxPlanLengthMonths(3),
      eligibilityStatus    = EligibilityStatus(OverallEligibilityStatus(eligibilityRulesFromForm.moreThanOneReasonForIneligibility)),
      eligibilityRules     = eligibilityRulesFromForm,
      chargeTypeAssessment = chargeTypeAssessments
    )
  }

}
