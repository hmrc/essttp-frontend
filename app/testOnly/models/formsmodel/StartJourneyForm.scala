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

package testOnly.models.formsmodel

import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import essttp.rootmodel.{AmountInPence, EmpRef, Vrn}
import models.MoneyUtil.amountOfMoneyFormatter
import models.{EligibilityError, EligibilityErrors, Language}
import play.api.data.Forms.{mapping, optional, seq}
import play.api.data._
import testOnly.messages.Messages
import testOnly.models.testusermodel.RandomDataGenerator
import util.EnumFormatter

import scala.util.Random

final case class StartJourneyForm(
    signInAs:            SignInAs,
    enrolments:          Seq[Enrolment],
    origin:              Origin,
    eligibilityErrors:   Seq[EligibilityError],
    payeDebtTotalAmount: Option[BigDecimal],
    vatDebtTotalAmount:  Option[BigDecimal],
    interestAmount:      Option[BigDecimal],
    payeTaxReference:    Option[String],
    vatTaxReference:     Option[String]
) {
  val (taxOfficeNumber: TaxOfficeNumber, taxOfficeReference: TaxOfficeReference, empRef: EmpRef) = {
    payeTaxReference.fold(RandomDataGenerator.nextEpayeRefs()(Random)) { someTaxRef =>
      val ton = TaxOfficeNumber(someTaxRef.take(3))
      val tor = TaxOfficeReference(someTaxRef.drop(3))
      (ton, tor, EmpRef.makeEmpRef(ton, tor))
    }
  }
  val vrn: Vrn = vatTaxReference.fold(RandomDataGenerator.nextVrn()(Random))(Vrn(_))
}

object StartJourneyForm {

  def form(payeMaxAmountOfDebt: AmountInPence, vatMaxAmountOfDebt: AmountInPence)(implicit language: Language): Form[StartJourneyForm] = {

    val signInMapping: Mapping[SignInAs] = Forms.of(EnumFormatter.format(
      enum                    = SignInAs,
      errorMessageIfMissing   = Messages.`Select how to be signed in`.show,
      errorMessageIfEnumError = Messages.`Select how to be signed in`.show
    ))

    val enrolmentsMapping: Mapping[Seq[Enrolment]] = seq(
      Forms.of(EnumFormatter.format(enum = Enrolments))
    )

    val originMapping: FieldMapping[Origin] = Forms.of(EnumFormatter.format(
      enum                    = Origins,
      errorMessageIfMissing   = Messages.`Select which origin the journey should start from`.show,
      errorMessageIfEnumError = Messages.`Select which origin the journey should start from`.show
    ))

      def debtTotalAmountMapping(maxAmountOfDebtForRegime: AmountInPence): FieldMapping[BigDecimal] = Forms.of(amountOfMoneyFormatter(
        isTooSmall = AmountInPence(100) > AmountInPence(_),
        isTooLarge = AmountInPence(_) > maxAmountOfDebtForRegime
      ))

    val interestAmountMapping: Mapping[Option[BigDecimal]] =
      optional(Forms.of(amountOfMoneyFormatter(_ < 0, _ => false)))

    val taxRefMapping: Mapping[Option[String]] = optional(Forms.text)

    Form(
      mapping(
        "signInAs" -> signInMapping,
        "enrolments" -> enrolmentsMapping,
        "origin" -> originMapping,
        "eligibilityErrors" -> seq(enumeratum.Forms.enum(EligibilityErrors)),
        "payeDebtTotalAmount" -> optional(debtTotalAmountMapping(payeMaxAmountOfDebt)),
        "vatDebtTotalAmount" -> optional(debtTotalAmountMapping(vatMaxAmountOfDebt)),
        "interestAmount" -> interestAmountMapping,
        "payeTaxReference" -> taxRefMapping,
        "vatTaxReference" -> taxRefMapping
      )(StartJourneyForm.apply)(StartJourneyForm.unapply)
    )
  }
}
