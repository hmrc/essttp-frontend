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

import _root_.essttp.rootmodel.ttp.*
import _root_.testOnly.controllers.routes as testOnlyRoutes
import _root_.testOnly.views.html.*
import actions.Actions
import config.AppConfig
import essttp.journey.JourneyConnector
import essttp.journey.model.{Origins, SjRequest}
import essttp.rootmodel.*
import essttp.rootmodel.ttp.affordablequotes.DueDate
import essttp.rootmodel.ttp.eligibility.{DmSpecialOfficeProcessingRequiredCESA as _, *}
import models.EligibilityErrors.*
import models.{EligibilityError, EligibilityErrors}
import play.api.mvc.*
import requests.RequestSupport
import testOnly.AuthLoginApiService
import testOnly.connectors.EssttpStubConnector
import testOnly.controllers.StartJourneyController.*
import testOnly.models.formsmodel.{StartJourneyForm, TaxRegimeForm}
import testOnly.models.testusermodel.TestUser
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class StartJourneyController @Inject() (
  as:                       Actions,
  appConfig:                AppConfig,
  essttpStubConnector:      EssttpStubConnector,
  mcc:                      MessagesControllerComponents,
  testOnlyStartPage:        TestOnlyStartPage,
  journeyConnector:         JourneyConnector,
  loginService:             AuthLoginApiService,
  taxRegimePage:            TaxRegimePage,
  iAmBtaPage:               IAmBtaPage,
  iAmPtaPage:               IAmPtaPage,
  iAmEpayePage:             IAmEPAYEPage,
  iAmVatPage:               IAmVatPage,
  iAmVatPenaltiesPage:      IAmVatPenaltiesPage,
  iAmGovUkPage:             IAmGovUkPage,
  iAmMobilePage:            IAmMobilePage,
  iAmItsaViewAndChangePage: IAmItsaViewAndChangePage,
  requestSupport:           RequestSupport
)(using ExecutionContext, Random)
    extends FrontendController(mcc),
      Logging {

  import requestSupport.languageFromRequest

  val whichTaxRegime: Action[AnyContent] = as.default { implicit request =>
    Ok(taxRegimePage(TaxRegimeForm.form))
  }

  val whichTaxRegimeSubmit: Action[AnyContent] = as.default { implicit request =>
    TaxRegimeForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(taxRegimePage(formWithErrors)),
        {
          case TaxRegime.Epaye => Redirect(routes.StartJourneyController.startJourneyEpayeGet)
          case TaxRegime.Vat   => Redirect(routes.StartJourneyController.startJourneyVatGet)
          case TaxRegime.Sa    => Redirect(routes.StartJourneyController.startJourneySaGet)
          case TaxRegime.Simp  => Redirect(routes.StartJourneyController.startJourneySimpGet)
        }
      )
  }

  val startJourneyEpayeGet: Action[AnyContent] = as.default { implicit request =>
    Ok(startPage(TaxRegime.Epaye))
  }

  val startJourneyVatGet: Action[AnyContent] = as.default { implicit request =>
    Ok(startPage(TaxRegime.Vat))

  }

  val startJourneySaGet: Action[AnyContent] = as.default { implicit request =>
    Ok(startPage(TaxRegime.Sa))
  }

  val startJourneySimpGet: Action[AnyContent] = as.default { implicit request =>
    Ok(startPage(TaxRegime.Simp))
  }

  private def startPage(taxRegime: TaxRegime)(using Request[?]) =
    testOnlyStartPage(taxRegime, StartJourneyForm.form(taxRegime, appConfig))

  val startJourneyEpayeSubmit: Action[AnyContent] = as.default.async { implicit request =>
    startJourneySubmit(TaxRegime.Epaye)
  }

  val startJourneyVatSubmit: Action[AnyContent] = as.default.async { implicit request =>
    startJourneySubmit(TaxRegime.Vat)

  }

  val startJourneySaSubmit: Action[AnyContent] = as.default.async { implicit request =>
    startJourneySubmit(TaxRegime.Sa)
  }

  val startJourneySimpSubmit: Action[AnyContent] = as.default.async { implicit request =>
    startJourneySubmit(TaxRegime.Simp)
  }

  private def startJourneySubmit(taxRegime: TaxRegime)(using Request[?]): Future[Result] =
    StartJourneyForm
      .form(taxRegime, appConfig)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(testOnlyStartPage(taxRegime, formWithErrors))),
        startJourney(taxRegime, _)
      )

  private def startJourney(taxRegime: TaxRegime, startJourneyForm: StartJourneyForm): Future[Result] = {
    given HeaderCarrier = HeaderCarrier()
    for {
      _               <- essttpStubConnector.primeStubs(makeEligibilityCheckResult(taxRegime, startJourneyForm))
      maybeTestUser    = TestUser.makeTestUser(startJourneyForm)
      session         <-
        maybeTestUser.map(testUser => loginService.logIn(testUser)).getOrElse(Future.successful(Session.emptyCookie))
      redirectTo: Call = startJourneyForm.origin match {
                           case Origins.Epaye.Bta            => testOnlyRoutes.StartJourneyController.showBtaEpayePage
                           case Origins.Epaye.EpayeService   => testOnlyRoutes.StartJourneyController.showEpayePage
                           case Origins.Epaye.GovUk          => testOnlyRoutes.StartJourneyController.showGovukPage
                           case Origins.Epaye.DetachedUrl    =>
                             _root_.controllers.routes.StartJourneyController.startDetachedEpayeJourney
                           case Origins.Vat.Bta              => testOnlyRoutes.StartJourneyController.showBtaVatPage
                           case Origins.Vat.VatService       => testOnlyRoutes.StartJourneyController.showVatPage
                           case Origins.Vat.GovUk            => testOnlyRoutes.StartJourneyController.showGovukPage
                           case Origins.Vat.DetachedUrl      =>
                             _root_.controllers.routes.StartJourneyController.startDetachedVatJourney
                           case Origins.Vat.VatPenalties     => testOnlyRoutes.StartJourneyController.showVatPenaltiesPage
                           case Origins.Sa.Bta               => testOnlyRoutes.StartJourneyController.showBtaSaPage
                           case Origins.Sa.Pta               => testOnlyRoutes.StartJourneyController.showPtaSaPage
                           case Origins.Sa.Mobile            => testOnlyRoutes.StartJourneyController.showMobileSaPage
                           case Origins.Sa.GovUk             => testOnlyRoutes.StartJourneyController.showGovukPage
                           case Origins.Sa.DetachedUrl       =>
                             _root_.controllers.routes.StartJourneyController.startDetachedSaJourney
                           case Origins.Sa.ItsaViewAndChange =>
                             testOnlyRoutes.StartJourneyController.showItsaViewAndChangePage
                           case Origins.Simp.Pta             => testOnlyRoutes.StartJourneyController.showPtaSimpPage
                           case Origins.Simp.Mobile          => testOnlyRoutes.StartJourneyController.showMobileSimpPage
                           case Origins.Simp.GovUk           => testOnlyRoutes.StartJourneyController.showGovukPage
                           case Origins.Simp.DetachedUrl     =>
                             _root_.controllers.routes.StartJourneyController.startDetachedSimpJourney
                         }
    } yield Redirect(redirectTo).withSession(session)
  }

  private def withSessionId(result: => Future[Result])(using Request[?]): Future[Result] =
    if (hc.sessionId.isEmpty) Future.successful(Ok("Missing session id")) else result

  /** Pretends being a BtaEpaye page */
  val showBtaEpayePage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmBtaPage(testOnlyRoutes.StartJourneyController.startJourneyEpayeBta.url))))
  }

  /** Pretends being a BtaVat page */
  val showBtaVatPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmBtaPage(testOnlyRoutes.StartJourneyController.startJourneyVatBta.url))))
  }

  /** Pretends being a BtaSa page */
  val showBtaSaPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmBtaPage(testOnlyRoutes.StartJourneyController.startJourneySaBta.url))))
  }

  /** Pretends being a PtaSa page */
  val showPtaSaPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmPtaPage(testOnlyRoutes.StartJourneyController.startJourneySaPta.url))))
  }

  /** Pretends being a PtaSimp page */
  val showPtaSimpPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmPtaPage(testOnlyRoutes.StartJourneyController.startJourneySimpPta.url))))
  }

  /** Pretends being a MobileSa page */
  val showMobileSaPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmMobilePage(testOnlyRoutes.StartJourneyController.startJourneySaMobile.url))))
  }

  /** Pretends being a MobileSimp page */
  val showMobileSimpPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(
      Future.successful(Ok(iAmMobilePage(testOnlyRoutes.StartJourneyController.startJourneySimpMobile.url)))
    )
  }

  /** Pretends being a Govuk Epaye page */
  val showGovukPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmGovUkPage())))
  }

  /** Pretends being a EPAYE service page */
  val showEpayePage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmEpayePage())))
  }

  /** Pretends being a VAT service page */
  val showVatPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmVatPage())))
  }

  /** Pretends being a VAT penalties page */
  val showVatPenaltiesPage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(Future.successful(Ok(iAmVatPenaltiesPage())))
  }

  /** Pretends being an ITSA View & Change page */
  val showItsaViewAndChangePage: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId(
      Future.successful(
        Ok(iAmItsaViewAndChangePage(testOnlyRoutes.StartJourneyController.startJourneySaItsaViewAndChange.url))
      )
    )
  }

  /** Pretends for testing purposes that journey started from Bta
    */
  val startJourneyEpayeBta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Epaye
        .startJourneyBta(
          SjRequest.Epaye.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showBtaEpayePage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showBtaEpayePage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  /** Pretends for testing purposes that journey started from EPAYE service
    */
  val startJourneyEpayeEpaye: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Epaye
        .startJourneyEpayeService(
          SjRequest.Epaye.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showEpayePage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showEpayePage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneyVatBta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Vat
        .startJourneyBta(
          SjRequest.Vat.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showBtaVatPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showBtaVatPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneyVatVatService: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Vat
        .startJourneyVatService(
          SjRequest.Vat.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showVatPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showVatPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneyVatVatPenalties: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Vat
        .startJourneyVatPenalties(
          SjRequest.Vat.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showVatPenaltiesPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showVatPenaltiesPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneySaBta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Sa
        .startJourneyBta(
          SjRequest.Sa.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showBtaSaPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showBtaSaPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneySaPta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Sa
        .startJourneyPta(
          SjRequest.Sa.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showPtaSaPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showPtaSaPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneySaMobile: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Sa
        .startJourneyMobile(
          SjRequest.Sa.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showMobileSaPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showMobileSaPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneySaItsaViewAndChange: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Sa
        .startJourneyItsaViewAndChange(
          SjRequest.Sa.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showMobileSaPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showMobileSaPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneySimpPta: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Simp
        .startJourneyPta(
          SjRequest.Simp.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showPtaSimpPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showPtaSimpPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

  val startJourneySimpMobile: Action[AnyContent] = as.default.async { implicit request =>
    withSessionId {
      journeyConnector.Simp
        .startJourneyMobile(
          SjRequest.Simp.Simple(
            returnUrl = ReturnUrl(routes.StartJourneyController.showMobileSimpPage.url + "?return-page"),
            backUrl = BackUrl(routes.StartJourneyController.showMobileSimpPage.url + "?starting-page")
          )
        )
        .map(sjResponse => Redirect(sjResponse.nextUrl.value))
    }
  }

}

object StartJourneyController {

  private def makeEligibilityCheckResult(taxRegime: TaxRegime, form: StartJourneyForm): EligibilityCheckResult = {

    val debtAmountFromForm: AmountInPence = AmountInPence(form.debtTotalAmount)
    val interestAmount: AmountInPence     = AmountInPence(form.interestAmount.getOrElse(BigDecimal(0)))

    val customerDetail =
      if (form.emailAddressPresent)
        List(
          CustomerDetail(
            Some(Email(SensitiveString("bobross@joyofpainting.com"))),
            Some(EmailSource.ETMP)
          )
        )
      else List(CustomerDetail(None, None))

    val contactDetail = ContactDetail(
      Some(TelNumber("12345678910")),
      None,
      None,
      if (form.emailAddressPresent) Some(Email(SensitiveString("jamienorth@email.com"))) else None,
      Some(EmailSource.ETMP),
      Some(AltLetterFormat(1))
    )

    val addresses = List(
      Address(
        AddressType("Residential"),
        Some(AddressLine("His Castle")),
        Some(AddressLine("Left wing")),
        Some(AddressLine("Top floor")),
        Some(AddressLine("Attic")),
        Some(IsReturnedLetterService(value = false)),
        Some(contactDetail),
        Some(Postcode(SensitiveString("NO1HERE"))),
        Some(Country("UK")),
        List(
          PostcodeHistory(
            Postcode(SensitiveString("NO2HERE")),
            PostcodeDate(LocalDate.of(2500, 1, 1))
          )
        )
      )
    )

    val customerType     = form.customerType
    val transitionToCDCS =
      if form.customerType.exists(_.entryName == CustomerTypes.ClassicSANonTransitioned.entryName)
      then form.transitionToCDCS.map(TransitionToCDCS(_))
      else None

    val individualDetails =
      Some(
        IndividualDetails(
          Some(Title("Lord")),
          Some(FirstName("Jamie")),
          Some(LastName("North")),
          Some(DateOfBirth(LocalDate.of(2000, 1, 1))),
          Some(DistrictNumber("666")),
          customerType,
          transitionToCDCS
        )
      )

    val chargeTypeAssessments: List[ChargeTypeAssessment] = {
      def charges(outstandingAmount: AmountInPence, interest: AmountInPence): Charges = Charges(
        chargeType = ChargeType("InYearRTICharge-Tax"),
        mainType = MainType("InYearRTICharge(FPS)"),
        mainTrans = MainTrans(form.mainAndSubTrans.mainTrans),
        subTrans = SubTrans(form.mainAndSubTrans.subTrans),
        outstandingAmount = OutstandingAmount(outstandingAmount),
        interestStartDate = Some(InterestStartDate(LocalDate.parse("2017-03-07"))),
        dueDate = DueDate(LocalDate.parse("2017-03-07")),
        accruedInterest = AccruedInterest(interest),
        ineligibleChargeType = IneligibleChargeType(value = false),
        chargeOverMaxDebtAge =
          if (form.chargeBeforeMaxAccountingDate.isEmpty) Some(ChargeOverMaxDebtAge(value = false)) else None,
        locks = Some(
          List(
            Lock(
              lockType = LockType("Payment"),
              lockReason = LockReason("Risk/Fraud"),
              disallowedChargeLockType = DisallowedChargeLockType(value = false)
            )
          )
        ),
        dueDateNotReached = false,
        isInterestBearingCharge = form.isInterestBearingCharge.map(IsInterestBearingCharge(_)),
        useChargeReference = form.useChargeReference.map(UseChargeReference(_)),
        chargeBeforeMaxAccountingDate = form.chargeBeforeMaxAccountingDate.map(ChargeBeforeMaxAccountingDate(_)),
        ddInProgress = form.ddInProgress.map(DdInProgress(_)),
        chargeSource = form.chargeSource.map(ChargeSource(_)),
        parentChargeReference = Some(ParentChargeReference("XW006559808862")),
        parentMainTrans = Some(ParentMainTrans("4700")),
        originalCreationDate = Some(OriginalCreationDate(LocalDate.parse("2022-05-17"))),
        tieBreaker = Some(TieBreaker("xyz")),
        originalTieBreaker = Some(OriginalTieBreaker("xyz")),
        saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2022-05-17"))),
        creationDate = Some(CreationDate(LocalDate.parse("2022-05-17"))),
        originalChargeType = Some(OriginalChargeType("VAT Return Debit Charge"))
      )

      def formatDate(d: LocalDate): String = d.format(DateTimeFormatter.ISO_DATE)

      val latestDate = LocalDate.of(2020, 8, 14)

      @tailrec
      def loop(n: Int, acc: List[ChargeTypeAssessment]): List[ChargeTypeAssessment] =
        if (n <= 0) acc
        else {
          val debtSoFar     = AmountInPence(acc.flatMap(_.charges.map(_.outstandingAmount.value.value)).sum)
          val interestSoFar = AmountInPence(acc.flatMap(_.charges.map(_.accruedInterest.value.value)).sum)
          val debt          = AmountInPence((debtAmountFromForm.value - debtSoFar.value) / n)
          val interest      = AmountInPence((interestAmount.value - interestSoFar.value) / n)

          val charge = ChargeTypeAssessment(
            TaxPeriodFrom(formatDate(latestDate.minusDays(n))),
            TaxPeriodTo(formatDate(latestDate.minusDays(n - 1))),
            DebtTotalAmount(debt + interest),
            ChargeReference(s"A0000000000${n.toString}"),
            List(charges(debt, interest))
          )

          loop(
            n - 1,
            charge :: acc
          )
        }

      loop(form.numberOfChargeTypeAssessments, List.empty)
    }

    val containsError: EligibilityError => Boolean = (ee: EligibilityError) => form.eligibilityErrors.contains(ee)
    val eligibilityRules: EligibilityRules         =
      EligibilityRules(
        hasRlsOnAddress = containsError(HasRlsOnAddress),
        markedAsInsolvent = containsError(MarkedAsInsolvent),
        isLessThanMinDebtAllowance = containsError(IsLessThanMinDebtAllowance),
        isMoreThanMaxDebtAllowance = containsError(IsMoreThanMaxDebtAllowance),
        disallowedChargeLockTypes = containsError(EligibilityErrors.DisallowedChargeLockTypes),
        existingTTP = containsError(ExistingTtp),
        chargesOverMaxDebtAge = Some(containsError(ChargesOverMaxDebtAge)),
        ineligibleChargeTypes = containsError(IneligibleChargeTypes),
        missingFiledReturns = containsError(MissingFiledReturns),
        hasInvalidInterestSignals = Some(containsError(HasInvalidInterestSignals)),
        hasInvalidInterestSignalsCESA = Some(containsError(HasInvalidInterestSignalsCESA)),
        dmSpecialOfficeProcessingRequired = Some(containsError(DmSpecialOfficeProcessingRequired)),
        noDueDatesReached = containsError(NoDueDatesReached),
        cannotFindLockReason = Some(containsError(CannotFindLockReason)),
        creditsNotAllowed = Some(containsError(CreditsNotAllowed)),
        isMoreThanMaxPaymentReference = Some(containsError(IsMoreThanMaxPaymentReference)),
        chargesBeforeMaxAccountingDate = Some(containsError(ChargesBeforeMaxAccountingDate)),
        hasDisguisedRemuneration = Some(containsError(HasDisguisedRemuneration)),
        hasCapacitor = Some(containsError(HasCapacitor)),
        dmSpecialOfficeProcessingRequiredCDCS = Some(containsError(DmSpecialOfficeProcessingRequiredCDCS)),
        isAnMtdCustomer = Some(containsError(EligibilityErrors.IsAnMtdCustomer)),
        dmSpecialOfficeProcessingRequiredCESA = Some(containsError(DmSpecialOfficeProcessingRequiredCESA)),
        noMtditsaEnrollment = Some(containsError(EligibilityErrors.NoMtditsaEnrollment))
      )

    val customerPostcodes: List[CustomerPostcode] = (0 until form.numberOfCustomerPostcodes).toList.map(i =>
      CustomerPostcode(
        Postcode(SensitiveString(s"AA1${i.toString}AA")),
        PostcodeDate(LocalDate.of(2022, 1, 1).minusDays(i))
      )
    )

    EligibilityCheckResult(
      processingDateTime = ProcessingDateTime(LocalDate.now().toString),
      identification = makeIdentificationForTaxType(taxRegime, form),
      invalidSignals =
        Some(List(InvalidSignals(signalType = "xyz", signalValue = "123", signalDescription = Some("Description")))),
      customerPostcodes = customerPostcodes,
      regimePaymentFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanFrequency = PaymentPlanFrequencies.Monthly,
      paymentPlanMinLength = PaymentPlanMinLength(form.planLengthMinAndMax.min),
      paymentPlanMaxLength = PaymentPlanMaxLength(form.planLengthMinAndMax.max),
      eligibilityStatus = EligibilityStatus(EligibilityPass(eligibilityRules.isEligible)),
      eligibilityRules = eligibilityRules,
      chargeTypeAssessment = chargeTypeAssessments,
      customerDetails = customerDetail,
      individualDetails = individualDetails,
      addresses = addresses,
      regimeDigitalCorrespondence = RegimeDigitalCorrespondence(form.regimeDigitalCorrespondence),
      futureChargeLiabilitiesExcluded = false,
      chargeTypesExcluded = None
    )
  }

  def makeIdentificationForTaxType(taxRegime: TaxRegime, form: StartJourneyForm): List[Identification] =
    taxRegime match {
      case TaxRegime.Epaye =>
        List(
          Identification(IdType("EMPREF"), IdValue(form.taxReference.value)),
          Identification(IdType("BROCS"), IdValue(form.taxReference.value))
        )

      case TaxRegime.Vat =>
        List(Identification(IdType("VRN"), IdValue(form.taxReference.value)))

      case TaxRegime.Sa =>
        List(Identification(IdType("UTR"), IdValue(form.taxReference.value)))

      case TaxRegime.Simp =>
        List(Identification(IdType("NINO"), IdValue(form.taxReference.value)))
    }

}
