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
import essttp.journey.JourneyConnector
import essttp.journey.model.{Origin, SjRequest}
import essttp.rootmodel.{BackUrl, ReturnUrl, TaxRegime}
import models.ttp
import models.ttp._
import play.api.data.Forms.{mapping, nonEmptyText, text}
import play.api.data.{Form, Forms}
import play.api.libs.json.{Format, Json}
import play.api.mvc._
import services.AuthLoginService
import testOnly.controllers.TestOnlyController._
import testOnly.models.Enrolment.{EPAYE, VAT}
import testOnly.models.{EligibilityError, Enrolment, TestOnlyJourney}
import testOnly.views.html.TestOnlyStart
import uk.gov.hmrc.auth.core.{AffinityGroup, EnrolmentIdentifier, Enrolment => CEnrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyController @Inject() (
    as:               Actions,
    appConfig:        AppConfig,
    authLoginService: AuthLoginService,
    stub:             EligibilityStubConnector,
    mcc:              MessagesControllerComponents,
    testOnlyPage:     TestOnlyStart,
    journeyConnector: JourneyConnector
)(implicit ec: ExecutionContext, requestHeader: RequestHeader)
  extends FrontendController(mcc)
  with Logging {

  val login: Action[AnyContent] = as.default { _ =>
    val loginUrl = appConfig.BaseUrl.gg
    Redirect(loginUrl)
  }

  val testOnlyStartPage: Action[AnyContent] = as.default { implicit request =>
    val form: Form[TestOnlyForm] = testOnlyForm()
    Ok(testOnlyPage(form))
  }

  val testOnlyStartPageSubmit: Action[AnyContent] = as.default.async { implicit request =>
    testOnlyForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(testOnlyPage(formWithErrors))),
        (p: TestOnlyForm) => {
          val origin = deriveOriginFromFormString(p.origin)
          val request = origin match {
            case Origin.Epaye.Bta         => journeyConnector.Epaye.startJourneyBta(epayeSimple)
            case Origin.Epaye.GovUk       => journeyConnector.Epaye.startJourneyGovUk(epayeEmpty)
            case Origin.Epaye.DetachedUrl => journeyConnector.Epaye.startJourneyGovUk(epayeEmpty)
            case Origin.Vat.Bta           => notDevelopedYetException //journeyConnector.Vat.startJourneyBta(vatSimple)
          }

          for {
            response <- request
            // make call to stubs too
            _ <- stub.insertEligibilityData(TaxRegime.Epaye, ttp.DefaultTaxId, DefaultTTP)
          } yield Redirect(response.nextUrl.nextUrl)
        }
      )
  }

  def routeCall(auth: String, enrolments: List[Enrolment], call: Call): Future[Result] = {
    if (auth == "none") {
      Future.successful(Redirect(appConfig.BaseUrl.essttpFrontend + call).withNewSession)
    } else {
      implicit val hc = HeaderCarrier()
      val result = for {
        session <- authLoginService.login(affinityGroup(auth), asEnrolments(enrolments))
      } yield Redirect(appConfig.BaseUrl.essttpFrontend + call.url).withSession(session)

      result.foldF(e => Future.failed(e.t), Future.successful)
    }

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

}

object TestOnlyController {

  /**
   * todo change the def to a val in essttp.journey.model.Origin
   * e.g. atm it's { def show = "Origin.Epaye.Bta" }
   * when trying to pattern match on that value, it fails due to non stable identifier
   */
  object OriginAsString {
    val epayeBta: String = Origin.Epaye.Bta.show
    val epayeGovUk: String = Origin.Epaye.GovUk.show
    val epayeDetached: String = Origin.Epaye.DetachedUrl.show
    val vatBta: String = Origin.Vat.Bta.show
  }

  private def deriveOriginFromFormString(originAsString: String): Origin = originAsString match {
    case OriginAsString.epayeBta      => Origin.Epaye.Bta
    case OriginAsString.epayeGovUk    => Origin.Epaye.GovUk
    case OriginAsString.epayeDetached => Origin.Epaye.DetachedUrl
    case OriginAsString.vatBta        => notDevelopedYetException
    // case Origin.Vat.GovUk.show         => notDevelopedYetException
    // case Origin.Vat.DetachedUrl.show   => notDevelopedYetException
  }

  private def returnUrl(url: String = "test-return-url") = ReturnUrl(url)

  private def backUrl(url: String = "test-back-url") = BackUrl(url)

  private val epayeSimple = SjRequest.Epaye.Simple(returnUrl = returnUrl(), backUrl = backUrl())
  private val epayeEmpty = SjRequest.Epaye.Empty()
  //  private val vatSimple = SjRequest.Vat.Simple(returnUrl = returnUrl(), backUrl = backUrl())

  import play.api.data.Form

  case class TestOnlyForm(
      auth:              String,
      enrolments:        Seq[String],
      origin:            String,
      eligibilityErrors: Seq[String]
  )

  case class TestOnlyRequest(
      auth:       String,
      enrolments: List[Enrolment],
      journey:    TestOnlyJourney
  )

  def testOnlyForm(): Form[TestOnlyForm] = Form(
    mapping(
      "auth" -> nonEmptyText,
      "enrolments" -> Forms.seq(text),
      "origin" -> nonEmptyText,
      "eligibilityErrors" -> Forms.seq(text)
    )(TestOnlyForm.apply)(TestOnlyForm.unapply)
  )

  case class AuthRequest(auth: String, enrolments: List[Enrolment])

  object AuthRequest {
    implicit val fmt: Format[AuthRequest] = Json.format[AuthRequest]
  }

  def affinityGroup(auth: String): AffinityGroup = auth match {
    case "Organisation" => AffinityGroup.Organisation
    case "Individual"   => AffinityGroup.Individual
  }

  def asEnrolments(l: List[Enrolment]): List[CEnrolment] = {
    l.map {
      case EPAYE => CEnrolment("IR-PAYE", Seq(EnrolmentIdentifier("TaxOfficeReference", "123AAAABBB")), "Activated")
      case VAT   => CEnrolment("IR-VAT", Seq(EnrolmentIdentifier("TaxOfficeReference", "123AAAABBB")), "Activated")
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

  private val notDevelopedYetException: Nothing = throw new NotImplementedError("this isn't built yet")

}
