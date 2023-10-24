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

package testOnly.controllers

import _root_.actions.Actions
import _root_.essttp.rootmodel.ttp._
import _root_.testOnly.views.html.{IAmBtaPage, IAmEPAYEPage, IAmGovUkPage, IAmVatPage, TestOnlyStartPage}
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.{Origins, SjRequest}
import essttp.rootmodel.ttp.affordablequotes.DueDate
import essttp.rootmodel.ttp.eligibility._
import essttp.rootmodel.{AmountInPence, BackUrl, Email, ReturnUrl, TaxRegime}
import models.EligibilityErrors._
import models.{EligibilityError, EligibilityErrors}
import play.api.mvc._
import requests.RequestSupport
import testOnly.AuthLoginApiService
import testOnly.connectors.EssttpStubConnector
import testOnly.controllers.StartJourneyController._
import testOnly.models.formsmodel.StartJourneyForm
import testOnly.models.testusermodel.TestUser
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import java.time.LocalDate
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
    iAmEpayePage:        IAmEPAYEPage,
    iAmVatPage:          IAmVatPage,
    iAmGovUkPage:        IAmGovUkPage,
    requestSupport:      RequestSupport
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val startJourneyGet: Action[AnyContent] = as.default { implicit request =>
    Ok(testOnlyStartPage(StartJourneyForm.form(appConfig.PolicyParameters.EPAYE.maxAmountOfDebt, appConfig.PolicyParameters.VAT.maxAmountOfDebt)))
  }

  val startJourneySubmit: Action[AnyContent] = as.default.async { implicit request =>
    StartJourneyForm.form(appConfig.PolicyParameters.EPAYE.maxAmountOfDebt, appConfig.PolicyParameters.VAT.maxAmountOfDebt)
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(Ok(testOnlyStartPage(formWithErrors))),
        startJourney
      )
  }

  private def startJourney(startJourneyForm: StartJourneyForm): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    for {
      _ <- essttpStubConnector.primeStubs(makeEligibilityCheckResult(startJourneyForm)(appConfig))
      maybeTestUser = TestUser.makeTestUser(startJourneyForm)
      session <- maybeTestUser.map(testUser => loginService.logIn(testUser)).getOrElse(Future.successful(Session.emptyCookie))
      redirect: Result = startJourneyForm.origin match {
        case Origins.Epaye.Bta          => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showBtaEpayePage)
        case Origins.Epaye.EpayeService => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showEpayePage)
        case Origins.Epaye.GovUk        => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showGovukEpayePage)
        case Origins.Epaye.DetachedUrl  => Redirect(_root_.controllers.routes.StartJourneyController.startDetachedEpayeJourney.url)
        case Origins.Vat.Bta            => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showBtaVatPage)
        case Origins.Vat.VatService     => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showVatPage)
        case Origins.Vat.GovUk          => Redirect(_root_.testOnly.controllers.routes.StartJourneyController.showGovukVatPage)
        case Origins.Vat.DetachedUrl    => Redirect(_root_.controllers.routes.StartJourneyController.startDetachedVatJourney.url)
      }
    } yield redirect.withSession(session)
  }

  private def withSessionId(result: => Future[Result])(implicit request: Request[_]): Future[Result] =
    if (hc.sessionId.isEmpty) Future.successful(Ok("Missing session id")) else result

  /** Pretends being a BtaEpaye page */
  val showBtaEpayePage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmBtaPage(_root_.testOnly.controllers.routes.StartJourneyController.startJourneyEpayeBta.url))))
  }

  /** Pretends being a BtaVat page */
  val showBtaVatPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmBtaPage(_root_.testOnly.controllers.routes.StartJourneyController.startJourneyVatBta.url))))
  }

  /** Pretends being a Govuk Epaye page */
  val showGovukEpayePage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmGovUkPage(TaxRegime.Epaye))))
  }

  /** Pretends being a Govuk Vat page */
  val showGovukVatPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmGovUkPage(TaxRegime.Vat))))
  }
  /** Pretends being a EPAYE service page */
  val showEpayePage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmEpayePage())))
  }
  /** Pretends being a VAT service page */
  val showVatPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmVatPage())))
  }

  /**
   * Pretends for testing purposes that journey started from Bta
   */
  val startJourneyEpayeBta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Epaye.startJourneyBta(SjRequest.Epaye.Simple(
        returnUrl = ReturnUrl(routes.StartJourneyController.showBtaEpayePage.url + "?return-page"),
        backUrl   = BackUrl(routes.StartJourneyController.showBtaEpayePage.url + "?starting-page")
      )).map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }
  /**
   * Pretends for testing purposes that journey started from EPAYE service
   */
  val startJourneyEpayeEpaye: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Epaye.startJourneyEpayeService(SjRequest.Epaye.Simple(
        returnUrl = ReturnUrl(routes.StartJourneyController.showEpayePage.url + "?return-page"),
        backUrl   = BackUrl(routes.StartJourneyController.showEpayePage.url + "?starting-page")
      )).map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneyVatBta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Vat.startJourneyBta(SjRequest.Vat.Simple(
        returnUrl = ReturnUrl(routes.StartJourneyController.showBtaVatPage.url + "?return-page"),
        backUrl   = BackUrl(routes.StartJourneyController.showBtaVatPage.url + "?starting-page")
      )).map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneyVatVatService: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Vat.startJourneyVatService(SjRequest.Vat.Simple(
        returnUrl = ReturnUrl(routes.StartJourneyController.showVatPage.url + "?return-page"),
        backUrl   = BackUrl(routes.StartJourneyController.showVatPage.url + "?starting-page")
      )).map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }
}

object StartJourneyController {

  def affinityGroup(auth: String): uk.gov.hmrc.auth.core.AffinityGroup = auth match {
    case "Organisation" => uk.gov.hmrc.auth.core.AffinityGroup.Organisation
    case "Individual"   => uk.gov.hmrc.auth.core.AffinityGroup.Individual
  }

  private def makeEligibilityCheckResult(form: StartJourneyForm)(appConfig: AppConfig): EligibilityCheckResult = {

    val debtAmountFromForm: AmountInPence = AmountInPence(form.debtTotalAmount)
    val interestAmount: AmountInPence = AmountInPence(form.interestAmount.getOrElse(BigDecimal(0)))

    val maybeCustomerDetail =
      if (form.emailAddressPresent) Some(List(CustomerDetail(Some(Email(SensitiveString("bobross@joyofpainting.com"))), Some(EmailSource.ETMP))))
      else Some(List.empty)

    val maybeRegimeDigitalCorrespondence = Some(RegimeDigitalCorrespondence(form.regimeDigitalCorrespondence))

    val charges: Charges = Charges(
      chargeType              = ChargeType("InYearRTICharge-Tax"),
      mainType                = MainType("InYearRTICharge(FPS)"),
      chargeReference         = ChargeReference(form.taxReference.value),
      mainTrans               = MainTrans("mainTrans"),
      subTrans                = SubTrans("subTrans"),
      outstandingAmount       = OutstandingAmount(debtAmountFromForm),
      interestStartDate       = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
      dueDate                 = DueDate(LocalDate.parse("2017-03-07")),
      accruedInterest         = AccruedInterest(interestAmount),
      ineligibleChargeType    = IneligibleChargeType(false),
      chargeOverMaxDebtAge    = ChargeOverMaxDebtAge(false),
      locks                   = Some(
        List(
          Lock(
            lockType                 = LockType("Payment"),
            lockReason               = LockReason("Risk/Fraud"),
            disallowedChargeLockType = DisallowedChargeLockType(false)
          )
        )
      ),
      dueDateNotReached       = false,
      isInterestBearingCharge = None,
      useChargeReference      = None
    )

    val chargeTypeAssessments: List[ChargeTypeAssessment] = List(
      ChargeTypeAssessment(
        TaxPeriodFrom("2020-08-13"),
        TaxPeriodTo("2020-08-14"),
        DebtTotalAmount(debtAmountFromForm + interestAmount),
        List(charges)
      )
    )

    val planMaxLengthFromConfig: Int = form.taxRegime match {
      case TaxRegime.Epaye => appConfig.PolicyParameters.EPAYE.maxPlanDurationInMonths
      case TaxRegime.Vat   => appConfig.PolicyParameters.VAT.maxPlanDurationInMonths
    }

    val containsError: EligibilityError => Boolean = (ee: EligibilityError) => form.eligibilityErrors.contains(ee)
    val eligibilityRules: EligibilityRules = {
      EligibilityRules(
        hasRlsOnAddress                   = containsError(HasRlsOnAddress),
        markedAsInsolvent                 = containsError(MarkedAsInsolvent),
        isLessThanMinDebtAllowance        = containsError(IsLessThanMinDebtAllowance),
        isMoreThanMaxDebtAllowance        = containsError(IsMoreThanMaxDebtAllowance),
        disallowedChargeLockTypes         = containsError(EligibilityErrors.DisallowedChargeLockTypes),
        existingTTP                       = containsError(ExistingTtp),
        chargesOverMaxDebtAge             = containsError(ChargesOverMaxDebtAge),
        ineligibleChargeTypes             = containsError(IneligibleChargeTypes),
        missingFiledReturns               = containsError(MissingFiledReturns),
        hasInvalidInterestSignals         = Some(containsError(HasInvalidInterestSignals)),
        dmSpecialOfficeProcessingRequired = Some(containsError(DmSpecialOfficeProcessingRequired)),
        noDueDatesReached                 = containsError(NoDueDatesReached),
        cannotFindLockReason              = Some(containsError(CannotFindLockReason)),
        creditsNotAllowed                 = Some(containsError(CreditsNotAllowed)),
        isMoreThanMaxPaymentReference     = Some(containsError(IsMoreThanMaxPaymentReference))
      )
    }
    EligibilityCheckResult(
      processingDateTime              = ProcessingDateTime(LocalDate.now().toString),
      identification                  = makeIdentificationForTaxType(form),
      customerPostcodes               = List(CustomerPostcode(Postcode(SensitiveString("AA11AA")), PostcodeDate("2022-01-01"))),
      regimePaymentFrequency          = PaymentPlanFrequencies.Monthly,
      paymentPlanFrequency            = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength            = PaymentPlanMinLength(1),
      paymentPlanMaxLength            = PaymentPlanMaxLength(planMaxLengthFromConfig),
      eligibilityStatus               = EligibilityStatus(EligibilityPass(eligibilityRules.isEligible)),
      eligibilityRules                = eligibilityRules,
      chargeTypeAssessment            = chargeTypeAssessments,
      customerDetails                 = maybeCustomerDetail,
      regimeDigitalCorrespondence     = maybeRegimeDigitalCorrespondence,
      futureChargeLiabilitiesExcluded = false
    )
  }

  def makeIdentificationForTaxType(form: StartJourneyForm): List[Identification] = {
    form.taxRegime match {
      case TaxRegime.Epaye => List(
        Identification(IdType("EMPREF"), IdValue(form.taxReference.value)),
        Identification(IdType("BROCS"), IdValue(form.taxReference.value))
      )
      case TaxRegime.Vat => List(Identification(IdType("VRN"), IdValue(form.taxReference.value)))
    }
  }

}
