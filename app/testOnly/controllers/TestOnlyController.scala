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
import connectors.EligibilityStubConnector
import essttp.rootmodel.TaxRegime
import models.ttp
import models.ttp._
import play.api.data.Forms.{ mapping, nonEmptyText, text }
import play.api.data.{ Form, Forms }
import play.api.libs.json.{ Format, Json }
import play.api.mvc._
import services.AuthLoginStubService
import testOnly.controllers.TestOnlyController._
import testOnly.models.Enrolment.{ EPAYE, VAT }
import testOnly.models.TestOnlyJourney.{ EpayeFromBTA, EpayeFromGovUk, EpayeNoOrigin, VATFromBTA, VATFromGovUk, VATNoOrigin }
import testOnly.models.{ EligibilityError, Enrolment, TestOnlyJourney }
import testOnly.views.html.TestOnlyStart
import uk.gov.hmrc.auth.core.{ AffinityGroup, EnrolmentIdentifier, Enrolment => CEnrolment }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class TestOnlyController @Inject() (
  as: Actions,
  appConfig: AppConfig,
  loginService: AuthLoginStubService,
  stub: EligibilityStubConnector,
  mcc: MessagesControllerComponents,
  testOnlyPage: TestOnlyStart)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val login = as.default { implicit request =>
    val loginUrl = appConfig.BaseUrl.authLoginStub
    Redirect(loginUrl)
  }

  val testOnlyStartPage: Action[AnyContent] = as.default { implicit request =>
    val form: Form[TestOnlyForm] = testOnlyForm()
    Ok(testOnlyPage(form))
  }

  val testOnlyStartPageSubmit: Action[AnyContent] = as.default.async { implicit request =>
    // TODO: build a service to call BE with payload
    /* BE endpoints:
      POST       /epaye/bta/journey/start
      POST       /epaye/gov-uk/journey/start
      POST       /epaye/detached-url/journey/start
     */
    testOnlyForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(testOnlyPage(formWithErrors))),
        (p: TestOnlyForm) => {
          val journey: TestOnlyJourney = p.origin match {
            case "paye_govuk" => EpayeFromGovUk
            case "paye_bta" => EpayeFromBTA
            case "paye_none" => EpayeNoOrigin
            case "vat_govuk" => VATFromGovUk
            case "vat_bta" => VATFromBTA
            case "vat_none" => VATNoOrigin
            case _ => sys.error("unable to start a journey without an origin")
          }
          val enrolmentMap: Map[String, Enrolment] = Map(
            "EPAYE" -> EPAYE,
            "VAT" -> VAT)

          startJourney(p.auth, p.enrolments.map(enrolmentMap).toList, p.eligibilityErrors.map(EligibilityError.withName).toList, journey)
        })
  }

  def routeCall(auth: String, enrolments: List[Enrolment], call: Call): Future[Result] = {
    if (auth == "none") {
      Future.successful(Redirect(call).withNewSession)
    } else {
      implicit val hc = HeaderCarrier()
      val result = for {
        session <- loginService.login(affinityGroup(auth), asEnrolments(enrolments))
      } yield Redirect(call).withSession(session)

      result.getOrElse(throw new IllegalArgumentException(s"failed to route call $call"))
    }

  }

  def btaEpayeLandingPage(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError])(implicit hc: HeaderCarrier): Future[Result] = {
    next(auth, enrolments, eligibilityErrors, controllers.routes.EpayeBTAController.landingPage())
  }

  def btaVatLandingPage(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError]): Future[Result] = {
    //Redirect(controllers.routes.BTAController.vatLandingPage)
    ???
  }

  def govUkEpayeLandingPage(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError])(implicit hc: HeaderCarrier): Future[Result] = {
    next(auth, enrolments, eligibilityErrors, controllers.routes.EpayeGovUkController.landingPage())
  }

  def noOriginEpayeLandingPage(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError])(implicit hc: HeaderCarrier): Future[Result] = {
    next(auth, enrolments, eligibilityErrors, controllers.routes.EpayeNoSourceController.landingPage())
  }

  def govUkVatLandingPage(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError]): Future[Result] = {
    //Redirect(controllers.routes.GovUkController.vatLandingPage)
    ???
  }

  def noOriginVatLandingPage(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError]): Future[Result] = {
    //routeCall(auth, enrolments, controllers.routes.NoSourceController.payeLandingPage())
    ???
  }

  def next(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError], call: Call)(implicit hc: HeaderCarrier): Future[Result] =
    if (eligibilityErrors.isEmpty) {
      for {
        _ <- stub.insertEligibilityData(TaxRegime.Epaye, ttp.DefaultTaxId, DefaultTTP)
        c <- routeCall(auth, enrolments, call)
      } yield c

    } else {
      for {
        _ <- stub.errors(TaxRegime.Epaye, ttp.DefaultTaxId, eligibilityErrors)
        c <- routeCall(auth, enrolments, call)
      } yield c
    }

  def startJourney(auth: String, enrolments: List[Enrolment], eligibilityErrors: List[EligibilityError], jt: TestOnlyJourney)(implicit hc: HeaderCarrier): Future[Result] = {
    jt match {
      case EpayeFromGovUk => govUkEpayeLandingPage(auth, enrolments, eligibilityErrors)
      case EpayeFromBTA => btaEpayeLandingPage(auth, enrolments, eligibilityErrors)
      case EpayeNoOrigin => noOriginEpayeLandingPage(auth, enrolments, eligibilityErrors)
      case VATFromGovUk => govUkVatLandingPage(auth, enrolments, eligibilityErrors)
      case VATFromBTA => btaVatLandingPage(auth, enrolments, eligibilityErrors)
      case VATNoOrigin => noOriginVatLandingPage(auth, enrolments, eligibilityErrors)
    }
  }

}

object TestOnlyController {
  import play.api.data.Form

  case class TestOnlyForm(
    auth: String,
    enrolments: Seq[String],
    origin: String,
    eligibilityErrors: Seq[String])

  case class TestOnlyRequest(
    auth: String,
    enrolments: List[Enrolment],
    journey: TestOnlyJourney)

  def testOnlyForm(): Form[TestOnlyForm] = Form(
    mapping(
      "auth" -> nonEmptyText,
      "enrolments" -> Forms.seq(text),
      "origin" -> nonEmptyText,
      "eligibilityErrors" -> Forms.seq(text))(TestOnlyForm.apply)(TestOnlyForm.unapply))

  case class AuthRequest(auth: String, enrolments: List[Enrolment])

  object AuthRequest {
    implicit val fmt: Format[AuthRequest] = Json.format[AuthRequest]
  }

  def affinityGroup(auth: String): AffinityGroup = auth match {
    case "Organisation" => AffinityGroup.Organisation
    case "Individual" => AffinityGroup.Individual
  }

  def asEnrolments(l: List[Enrolment]): List[CEnrolment] = {
    l.map {
      case EPAYE => CEnrolment("IR-PAYE", Seq(EnrolmentIdentifier("TaxOfficeReference", "123AAAABBB")), "Activated")
      case VAT => CEnrolment("IR-VAT", Seq(EnrolmentIdentifier("TaxOfficeReference", "123AAAABBB")), "Activated")
    }
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
        PaymentLock(false, "")))

    val chargeTypeAssessments: List[ChargeTypeAssessment] = List(
      ChargeTypeAssessment("2020-08-13", "2020-08-14", 300000, List(taxPeriodCharges)))

    TtpEligibilityData(
      "SSTTP",
      "A00000000001",
      "PAYE",
      "2022-03-10",
      CustomerDetails("NI", "B5 7LN"),
      EligibilityStatus(false, 1, 6),
      EligibilityRules(false, "Receiver is not known", false, false, false, false, false, 300, 600, false, false, false),
      FinancialLimitBreached(true, 16000),
      chargeTypeAssessments)
  }

}
