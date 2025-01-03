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

package testOnly.controllers

import actions.Actions
import actionsmodel.AuthenticatedJourneyRequest
import essttp.journey.model.Journey
import essttp.rootmodel.TaxRegime
import essttp.utils.Errors
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import testOnly.PegaPlanService
import testOnly.models.TestOnlyJourney
import testOnly.models.formsmodel.{IncomeAndExpenditure, IncomeAndExpenditureForm}
import testOnly.repos.TestOnlyJourneyRepo
import testOnly.views.html.{PegaCheckYourAnswers, PegaIncomeAndExpenditure}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PegaController @Inject() (
    as:                       Actions,
    mcc:                      MessagesControllerComponents,
    testOnlyJourneyRepo:      TestOnlyJourneyRepo,
    incomeAndExpenditurePage: PegaIncomeAndExpenditure,
    cyaPage:                  PegaCheckYourAnswers,
    pegaPlanService:          PegaPlanService,
    config:                   Configuration
)(implicit ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  implicit def toFuture(result: Result): Future[Result] = Future.successful(result)

  private val logOutOnReturn = config.get[Boolean]("pega.test-only.log-out-on-return")

  private def withTestOnlyJourney(request: AuthenticatedJourneyRequest[_])(f: TestOnlyJourney => Future[Result]): Future[Result] =
    testOnlyJourneyRepo.get(request.journeyId).flatMap{
      case None    => sys.error(s"Could not find test-only journey for journey id ${request.journeyId.value}")
      case Some(j) => f(j)
    }

  // regime is here to allow us to see if query param is populated properly
  def start(@unused regime: TaxRegime): Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    val testOnlyJourney = TestOnlyJourney(request.journeyId, request.journey.taxRegime, Instant.now(), None, None)
    testOnlyJourneyRepo.insert(testOnlyJourney).map{ _ =>
      Redirect(routes.PegaController.incomeAndExpenditure)
    }
  }

  val incomeAndExpenditure: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    withTestOnlyJourney(request){ testOnlyJourney =>
      val form =
        IncomeAndExpenditureForm.form.fill(testOnlyJourney.incomeAndExpenditure.getOrElse(IncomeAndExpenditure.default))

      Ok(incomeAndExpenditurePage(form))
    }
  }

  val incomeAndExpenditureSubmit: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    withTestOnlyJourney(request){ testOnlyJourney =>
      IncomeAndExpenditureForm.form.bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(incomeAndExpenditurePage(formWithErrors))),
          { incomeAndExpenditure =>
            pegaPlanService.getPlans(request.journey, incomeAndExpenditure)
              .map(_.sortWith(_.planDuration.value > _.planDuration.value))
              .flatMap{
                case Nil =>
                  val formWithErrors =
                    IncomeAndExpenditureForm.form.fill(incomeAndExpenditure).withError("", "Could not find any plans. Try adjusting the net income")
                  BadRequest(incomeAndExpenditurePage(formWithErrors))

                case head :: tail =>
                  val plan = tail.maxByOption(_.planDuration.value).getOrElse(head)
                  val newJourney = testOnlyJourney.copy(
                    incomeAndExpenditure = Some(incomeAndExpenditure),
                    paymentPlan          = Some(plan),
                    updatedAt            = Instant.now()
                  )
                  testOnlyJourneyRepo.insert(newJourney)
                    .map(_ => Redirect(routes.PegaController.checkYourAnswers))

              }
          }
        )
    }
  }

  val checkYourAnswers: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    withTestOnlyJourney(request) { testOnlyJourney =>
      val whyCannotPayInFullAnswers = request.journey match {
        case j: Journey.AfterWhyCannotPayInFullAnswers => j.whyCannotPayInFullAnswers
        case other                                     => Errors.throwServerErrorException(s"Could not find WhyCannotPayInFullAnswers from journey in state ${other.name}")
      }

      val upfrontPaymentAnswers = request.journey match {
        case j: Journey.AfterUpfrontPaymentAnswers => j.upfrontPaymentAnswers
        case other                                 => Errors.throwServerErrorException(s"Could not find UpfrontPaymentAnswers from journey in state ${other.name}")
      }

      val canPayWithinSixMonthsAnswers = request.journey match {
        case j: Journey.AfterCanPayWithinSixMonthsAnswers => j.canPayWithinSixMonthsAnswers
        case other                                        => Errors.throwServerErrorException(s"Could not find CanPayWithinSixMonthsAnswers from journey in state ${other.name}")
      }

      val incomeAndExpenditure =
        testOnlyJourney.incomeAndExpenditure.getOrElse(
          Errors.throwServerErrorException("Could not find income and expenditure answers")
        )

      val paymentPlan =
        testOnlyJourney.paymentPlan.getOrElse(
          Errors.throwServerErrorException("Could not find payment plan")
        )

      Ok(cyaPage(
        logOutOnReturn,
        request.enrolments,
        whyCannotPayInFullAnswers,
        upfrontPaymentAnswers,
        canPayWithinSixMonthsAnswers,
        incomeAndExpenditure,
        paymentPlan
      ))
    }
  }

  val checkYourAnswersContinue: Action[AnyContent] = as.authenticatedJourneyAction.async { implicit request =>
    withTestOnlyJourney(request) { testOnlyJourney =>
      pegaPlanService.storePegaGetCaseResponse(request.journey, testOnlyJourney).map{ _ =>
        redirectToActualService(
          controllers.routes.PegaController.callback(request.journey.taxRegime, Some(request.lang))
        )
      }
    }
  }

  def change(pageId: String): Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    redirectToActualService(
      controllers.routes.PaymentScheduleController.changeFromCheckPaymentSchedule(pageId, request.journey.taxRegime, Some(request.lang))
    )
  }

  val backFromPegaLanding: Action[AnyContent] = as.authenticatedJourneyAction { implicit request =>
    redirectToActualService(
      controllers.routes.CanPayWithinSixMonthsController.canPayWithinSixMonths(request.journey.taxRegime, Some(request.lang))
    )
  }

  private def redirectToActualService(redirectTo: Call): Result = {
    val redirect = Redirect(redirectTo)
    if (logOutOnReturn) redirect.withNewSession else redirect
  }

}
