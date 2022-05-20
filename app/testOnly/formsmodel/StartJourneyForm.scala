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
import langswitch.Language
import models.{EligibilityError, EligibilityErrors}
import play.api.data.{Form, Forms, Mapping}
import play.api.data.Forms.{mapping, seq}
import testOnly.messages.Messages
import testOnly.testusermodel.RandomDataGenerator
import util.EnumFormatter

import scala.util.Random

final case class StartJourneyForm(
    signInAs:          SignInAsFormValue,
    enrolments:        Seq[EnrolmentFormValue],
    origin:            Origin,
    eligibilityErrors: Seq[EligibilityError]
) {
  //TODO: move ton and tor to the form
  val (ton, tor, empRef) = RandomDataGenerator.nextEpayeRefs()(Random)
}

object StartJourneyForm {

  def form(implicit language: Language): Form[StartJourneyForm] = {

    val signInMapping: Mapping[SignInAsFormValue] = Forms.of(EnumFormatter.format(
      enum                    = SignInAsFormValue,
      errorMessageIfMissing   = Messages.`Select how to be signed in`.show,
      errorMessageIfEnumError = Messages.`Select how to be signed in`.show
    ))

    val enrolmentsMapping: Mapping[Seq[EnrolmentFormValue]] = seq(
      Forms.of(EnumFormatter.format(enum = Enrolments))
    )

    val originMapping = Forms.of(EnumFormatter.format(
      enum                    = Origins,
      errorMessageIfMissing   = Messages.`Select which origin the journey should start from`.show,
      errorMessageIfEnumError = Messages.`Select which origin the journey should start from`.show
    ))

    Form(
      mapping(
        "signInAs" -> signInMapping,
        "enrolments" -> enrolmentsMapping,
        "origin" -> originMapping,
        "eligibilityErrors" -> seq(enumeratum.Forms.enum(EligibilityErrors))
      )(StartJourneyForm.apply)(StartJourneyForm.unapply)
    )
  }
}
