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

package controllers

import _root_.actions.Actions
import controllers.BankDetailsController.bankDetailsForm
import models.{ AccountNumber, BankDetails, Journey, MockJourney, SortCode, UserAnswers }
import play.api.data.Forms.{ mapping, nonEmptyText }
import play.api.data.validation.{ Constraint, Invalid, Valid }
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import services.JourneyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.html.{BankDetailsSummary, SetUpBankDetails, TermsAndConditions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankDetailsController @Inject() (
    as:                   Actions,
    bankDetailsPage:      SetUpBankDetails,
    checkBankDetailsPage: BankDetailsSummary,
    termsPage:            TermsAndConditions,
    journeyService:       JourneyService,
    mcc:                  MessagesControllerComponents
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
  with Logging {

  val setUpBankDetails: Action[AnyContent] = as.default { implicit request =>
    Ok(bankDetailsPage(bankDetailsForm()))
  }

  val setUpBankDetailsSubmit: Action[AnyContent] = as.default.async { implicit request =>
    val j: MockJourney = MockJourney()
    bankDetailsForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(bankDetailsPage(formWithErrors))),
        (bankDetails: BankDetails) => {
          Future.successful(Redirect(routes.BankDetailsController.checkBankDetails()))
        }
      )
  }

  val checkBankDetails: Action[AnyContent] = as.default.async { implicit request =>
    val j: MockJourney = MockJourney(userAnswers = UserAnswers.empty.copy(bankDetails = Some(BankDetails(name = "John Doe", sortCode = SortCode("202020"), accountNumber = AccountNumber("12345678")))))
    Future.successful(Ok(checkBankDetailsPage(j.userAnswers)))
  }

  val termsAndConditions: Action[AnyContent] = as.default { implicit request =>
    Ok(termsPage())
  }
}

object BankDetailsController {
  import play.api.data.Form

  def bankDetailsForm(): Form[BankDetails] = Form(
    mapping(
      "name" -> nonEmptyText(maxLength = 100),
      "sortCode" -> sortCodeMapping,
      "accountNumber" -> accountNumberMapping
    )(BankDetails.apply)(BankDetails.unapply)
  )

  private val sortCodeRegex = "^[0-9]{6}$"

  private val sortCodeContstraint: Constraint[SortCode] =
    Constraint(sortCode =>
      if (!sortCode.value.forall(_.isDigit)) Invalid("error.nonNumeric")
      else if (sortCode.value.matches(sortCodeRegex)) Valid
      else Invalid("error.invalid"))

  private val sortCodeMapping = nonEmptyText
    .transform[SortCode](
      s => SortCode(s.replaceAllLiterally("-", "").replaceAll("\\s", "")),
      _.value
    )
    .verifying(sortCodeContstraint)

  private val accountNumberRegex = "^[0-9]{6,8}$"

  private val accountNumberConstraint: Constraint[AccountNumber] =
    Constraint(accountNumber =>
      if (!accountNumber.value.forall(_.isDigit)) Invalid("error.nonNumeric")
      else if (accountNumber.value.matches(accountNumberRegex)) Valid
      else Invalid("error.invalid"))

  private val accountNumberMapping = nonEmptyText
    .transform[AccountNumber](
      s => AccountNumber(s.replaceAll("\\s", "")),
      _.value
    )
    .verifying(accountNumberConstraint)

}
