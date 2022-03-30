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
import play.api.data.Forms.{ mapping, nonEmptyText, text }
import play.api.data.{ Form, Forms }
import play.api.libs.json.{ Format, Json }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents, Result }
import play.api.test.{ FakeRequest, Helpers }
import services.AuthLoginStubService
import testOnly.controllers.TestOnlyController._
import testOnly.models.Enrolment.{ EPAYE, VAT }
import testOnly.models.TestOnlyJourney.{ EpayeFromBTA, EpayeFromGovUk, EpayeNoOrigin, VATFromBTA, VATFromGovUk, VATNoOrigin }
import testOnly.models.{ Enrolment, TestOnlyJourney }
import testOnly.views.html.TestOnlyStart
import uk.gov.hmrc.auth.core.{ AffinityGroup, Enrolment => CEnrolment }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.ImplicitConversions.toFutureResult
import util.Logging

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class TestOnlyController @Inject() (
  as: Actions,
  appConfig: AppConfig,
  loginService: AuthLoginStubService,
  mcc: MessagesControllerComponents,
  testOnlyPage: TestOnlyStart)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val login = as.default { implicit request =>
    val loginUrl = appConfig.authLoginStubUrl
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

          startJourney(p.auth, p.enrolments.map(enrolmentMap).toList, journey)
        })
  }

  def btaEpayeLandingPage(auth: String, enrolments: List[Enrolment]): Future[Result] = {
    implicit val hc = HeaderCarrier()
    if (auth == "none") {
      Future.successful(Redirect(controllers.routes.BTAController.payeLandingPage).withNewSession)
    } else {
      val result = for {
        session <- loginService.login(affinityGroup(auth), asEnrolments(enrolments))
      } yield Redirect(controllers.routes.BTAController.payeLandingPage).withSession(session)

      result.getOrElse(throw new IllegalArgumentException("bta epaye failed"))
    }

  }

  def btaVatLandingPage(auth: String, enrolments: List[Enrolment]): Future[Result] = {
    Redirect(controllers.routes.BTAController.vatLandingPage)
  }

  def govUkEpayeLandingPage(auth: String, enrolments: List[Enrolment]): Future[Result] = {
    implicit val hc = HeaderCarrier()
    if (auth == "none") {
      Future.successful(Redirect(controllers.routes.GovUkController.startPaye).withNewSession)
    } else {
      val result = for {
        session <- loginService.login(affinityGroup(auth), asEnrolments(enrolments))
      } yield Redirect(controllers.routes.GovUkController.startPaye).withSession(session)

      result.getOrElse(throw new IllegalArgumentException("govuk epaye failed"))
    }
  }

  def noOriginEpayeLandingPage(auth: String, enrolments: List[Enrolment]): Future[Result] = {
    implicit val hc = HeaderCarrier()
    if (auth == "none") {
      Future.successful(Redirect(controllers.routes.NoSourceController.payeLandingPage()).withNewSession)
    } else {
      val result = for {
        session <- loginService.login(affinityGroup(auth), asEnrolments(enrolments))
      } yield Redirect(controllers.routes.NoSourceController.startPaye).withSession(session)

      result.getOrElse(throw new IllegalArgumentException("govuk epaye failed"))
    }
  }

  def govUkVatLandingPage(auth: String, enrolments: List[Enrolment]): Future[Result] = {
    Redirect(controllers.routes.GovUkController.vatLandingPage)
  }

  def startJourney(auth: String, enrolments: List[Enrolment], jt: TestOnlyJourney)(implicit hc: HeaderCarrier): Future[Result] = {
    jt match {
      case EpayeFromGovUk => govUkEpayeLandingPage(auth, enrolments)
      case EpayeFromBTA => btaEpayeLandingPage(auth, enrolments)
      case EpayeNoOrigin => noOriginEpayeLandingPage(auth, enrolments)
      case VATFromGovUk => govUkVatLandingPage(auth, enrolments)
      case VATFromBTA => btaVatLandingPage(auth, enrolments)
    }
  }

}

object TestOnlyController {
  import play.api.data.Form

  case class TestOnlyForm(
    auth: String,
    enrolments: Seq[String],
    origin: String)

  case class TestOnlyRequest(
    auth: String,
    enrolments: List[Enrolment],
    journey: TestOnlyJourney)

  def testOnlyForm(): Form[TestOnlyForm] = Form(
    mapping(
      "auth" -> nonEmptyText,
      "enrolments" -> Forms.seq(text),
      "origin" -> nonEmptyText)(TestOnlyForm.apply)(TestOnlyForm.unapply))

  case class AuthRequest(auth: String, enrolments: List[Enrolment])

  object AuthRequest {
    implicit val fmt: Format[AuthRequest] = Json.format[AuthRequest]
  }

  def affinityGroup(auth: String): AffinityGroup = auth match {
    case "Organisation" => AffinityGroup.Organisation
    case "Individual" => AffinityGroup.Individual
  }

  def asEnrolments(l: List[Enrolment]): List[CEnrolment] = {
    l.map(e => CEnrolment(e.name, Nil, "Active"))
  }

}
