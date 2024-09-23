/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import actions.Actions
import actionsmodel.AuthenticatedJourneyRequest
import cats.syntax.either._
import cats.syntax.eq._
import config.AppConfig
import controllers.JourneyFinalStateCheck.finalStateCheck
import essttp.journey.JourneyConnector
import essttp.journey.model.{Journey, WhyCannotPayInFullAnswers}
import essttp.rootmodel.CannotPayReason
import messages.Messages
import models.Language
import play.api.data.Forms.{of, set}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import requests.RequestSupport
import services.PegaService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhyCannotPayInFullController @Inject() (
    as:               Actions,
    mcc:              MessagesControllerComponents,
    requestSupport:   RequestSupport,
    views:            Views,
    journeyConnector: JourneyConnector,
    pegaService:      PegaService
)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc)
  with Logging {

  import requestSupport._

  val whyCannotPayInFull: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    request.journey match {
      case j: Journey.BeforeEligibilityChecked =>
        JourneyIncorrectStateRouter.logErrorAndRouteToDefaultPage(j)

      case j: Journey.AfterEligibilityChecked =>
        finalStateCheck(
          j,
          {
            val previousAnswers = existingAnswersInJourney(request.journey)
            val form = previousAnswers.fold(WhyCannotPayInFullController.form)(WhyCannotPayInFullController.form.fill)

            Ok(views.whyCannotPayInFull(form))
          }
        )
    }
  }

  private def existingAnswersInJourney(journey: Journey): Option[Set[CannotPayReason]] = journey match {
    case _: Journey.BeforeWhyCannotPayInFullAnswers => None
    case j: Journey.AfterWhyCannotPayInFullAnswers => j.whyCannotPayInFullAnswers match {
      case WhyCannotPayInFullAnswers.AnswerNotRequired           => None
      case WhyCannotPayInFullAnswers.WhyCannotPayInFull(reasons) => Some(reasons)
    }
  }

  val whyCannotPayInFullSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    WhyCannotPayInFullController.form.bindFromRequest().fold(
      formWithErrors => Ok(views.whyCannotPayInFull(formWithErrors)),
      { reasons =>

        updateJourney(request.journey, existingAnswersInJourney(request.journey), reasons)
          .map{ updatedJourney =>
            Routing.redirectToNext(
              routes.WhyCannotPayInFullController.whyCannotPayInFull,
              updatedJourney,
              //even if the value is changed we still want to go to cya page
              submittedValueUnchanged = true
            )
          }
      }
    )
  }

  private def updateJourney(
      journey:    Journey,
      oldAnswers: Option[Set[CannotPayReason]],
      newAnswers: Set[CannotPayReason]
  )(implicit r: AuthenticatedJourneyRequest[_]): Future[Journey] =
    if (oldAnswers.contains(newAnswers)) {
      // don't need to call BE to update journey if nothing has changed
      Future.successful(journey)
    } else {
      // need to start a PEGA case if one has already been started and the answers have changed
      val needToStartPegaCase = journey match {
        case _: Journey.AfterStartedPegaCase => true
        case _                               => false
      }

      if (needToStartPegaCase) {
        pegaService.startCase(journey).map(_._1)
      } else {
        journeyConnector.updateWhyCannotPayInFullAnswers(
          r.journeyId,
          WhyCannotPayInFullAnswers.WhyCannotPayInFull(newAnswers)
        )
      }
    }

}

object WhyCannotPayInFullController {

  def form(implicit lang: Language): Form[Set[CannotPayReason]] = {
    val cannotPayReasonFormatter = new Formatter[CannotPayReason] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], CannotPayReason] = {
        Either.fromOption(
          data.get(key).flatMap(CannotPayReason.withNameInsensitiveOption), {
            Seq(FormError("WhyCannotPayInFull", Messages.WhyCannotPayInFull.`Select all that apply or 'none of the above'`.show))
          }
        )
      }

      override def unbind(key: String, value: CannotPayReason): Map[String, String] =
        Map(key -> value.entryName)
    }

    Form(
      "WhyCannotPayInFull" -> set(of(cannotPayReasonFormatter))
        .verifying(
          Messages.WhyCannotPayInFull.`Select all that apply or 'none of the above'`.show,
          set => set.nonEmpty && (set.size === 1 || !set.contains(CannotPayReason.Other))
        )
    )
  }

}
