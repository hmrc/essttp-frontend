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

package testOnly.formsmodel

import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import essttp.rootmodel.{AmountInPence, EmpRef}
import langswitch.Language
import models.MoneyUtil.amountOfMoneyFormatter
import models.{EligibilityError, EligibilityErrors}
import play.api.data.Forms.{mapping, optional, seq}
import play.api.data.{FieldMapping, Form, Forms, Mapping}
import testOnly.messages.Messages
import testOnly.testusermodel.RandomDataGenerator
import util.EnumFormatter

import scala.util.Random

final case class StartJourneyForm(
    signInAs:          SignInAs,
    enrolments:        Seq[Enrolment],
    origin:            Origin,
    eligibilityErrors: Seq[EligibilityError],
    debtTotalAmount:   BigDecimal,
    taxReference:      Option[String]
) {
  val (taxOfficeNumber: TaxOfficeNumber, taxOfficeReference: TaxOfficeReference, empRef: EmpRef) =
    taxReference.fold(RandomDataGenerator.nextEpayeRefs()(Random)) { someTaxRef =>
      val ton = TaxOfficeNumber(someTaxRef.take(3))
      val tor = TaxOfficeReference(someTaxRef.drop(3))
      (ton, tor, EmpRef.makeEmpRef(ton, tor))
    }
}

object StartJourneyForm {

  def form(implicit language: Language): Form[StartJourneyForm] = {

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

    val debtTotalAmountMapping: FieldMapping[BigDecimal] = Forms.of(amountOfMoneyFormatter(
      isTooSmall = AmountInPence(100) > AmountInPence(_),
      isTooLarge = AmountInPence(_) > AmountInPence(1500000)
    ))

    val taxRefMapping: Mapping[Option[String]] = optional(Forms.text)

    Form(
      mapping(
        "signInAs" -> signInMapping,
        "enrolments" -> enrolmentsMapping,
        "origin" -> originMapping,
        "eligibilityErrors" -> seq(enumeratum.Forms.enum(EligibilityErrors)),
        "debtTotalAmount" -> debtTotalAmountMapping,
        "taxReference" -> taxRefMapping
      )(StartJourneyForm.apply)(StartJourneyForm.unapply)
    )
  }
}
