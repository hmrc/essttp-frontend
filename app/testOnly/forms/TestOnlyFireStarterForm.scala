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

package testOnly.forms

import essttp.journey.model.{Origin, Origins}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import testOnly.models.{EligibilityError, EligibilityErrors, Enrolment, Enrolments, SignInAs}

final case class TestOnlyFireStarterForm(
    signIn:            SignInAs,
    enrolments:        Seq[Enrolment],
    origin:            Origin,
    eligibilityErrors: Seq[EligibilityError]
)

object TestOnlyFireStarterForm {
  val form: Form[TestOnlyFireStarterForm] = Form(
    mapping(
      "auth" -> enumeratum.Forms.enum(SignInAs),
      "enrolments" -> seq(enumeratum.Forms.enum(Enrolments)),
      "origin" -> enumeratum.Forms.enum(Origins),
      "eligibilityErrors" -> seq(enumeratum.Forms.enum(EligibilityErrors))
    )(TestOnlyFireStarterForm.apply)(TestOnlyFireStarterForm.unapply)
  )
}
