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
import connectors.SsttpConnector
import connectors.SsttpConnector.Eligibility
import play.api.data.{Form, Forms}
import play.api.data.Forms.{mapping, nonEmptyText, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import testOnly.controllers.TestOnlyController.{TestOnlyForm, TestOnlyRequest, testOnlyForm}
import testOnly.models.Enrolment.{EPAYE, VAT}
import testOnly.models.{Enrolment, TestOnlyJourney}
import testOnly.models.TestOnlyJourney.{EpayeFromBTA, EpayeFromGovUk, EpayeNoOrigin, VATFromBTA, VATFromGovUk, VATNoOrigin}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import testOnly.views.html.TestOnlyStart
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestOnlyController @Inject() (
  as: Actions,
  appConfig: AppConfig,
  stub: SsttpConnector,
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
        formWithErrors => Ok(testOnlyPage(formWithErrors)),
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

          setupEligibility(p.auth, p.enrolments.map(enrolmentMap).toList, journey)
        })
  }

  def createEligibility() = {
    implicit val hc = HeaderCarrier()
    val result = for {
      _ <- stub.makeEligibility(Eligibility.Empty)
    } yield Redirect(controllers.routes.LandingController.landingPage)

    result.getOrElse(throw new IllegalStateException("bad"))
  }

  def epayeFromGovUK(auth: String, enrolments: List[Enrolment]) = createEligibility()

  def epayeFromBTA(auth: String, enrolments: List[Enrolment]) = createEligibility()

  def epayeNoOrigin(auth: String, enrolments: List[Enrolment]) = createEligibility()

  def vatFromGovUk(auth: String, enrolments: List[Enrolment]) = createEligibility()

  def vatFromBTA(auth: String, enrolments: List[Enrolment]) = createEligibility()

  def vatNoOrigin(auth: String, enrolments: List[Enrolment]) = createEligibility()

  def setupEligibility(auth: String, enrolments: List[Enrolment], jt: TestOnlyJourney) = {
    jt match {
      case EpayeFromGovUk => epayeFromGovUK(auth, enrolments)
      case EpayeFromBTA => epayeFromBTA(auth, enrolments)
      case EpayeNoOrigin => epayeNoOrigin(auth, enrolments)
      case VATFromGovUk => vatFromGovUk(auth, enrolments)
      case VATFromBTA => vatFromBTA(auth, enrolments)
      case VATNoOrigin => vatNoOrigin(auth, enrolments)
    }
    //Redirect(controllers.routes.LandingController.landingPage())
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

}
