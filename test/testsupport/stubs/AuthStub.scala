/*
 * Copyright 2023 HM Revenue & Customs
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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, Json, OFormat}
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.auth.core.retrieve.Credentials

object AuthStub {
  def authorise(
    allEnrolments: Option[Set[Enrolment]] = Some(Set(TdAll.payeEnrolment)),
    credentials:   Option[Credentials] = Some(Credentials("authId-999", "GovernmentGateway")),
    authNino:      Option[String] = None
  ): StubMapping = {

    given OFormat[Enrolment] = {
      given OFormat[EnrolmentIdentifier] = Json.format[EnrolmentIdentifier]
      Json.format[Enrolment]
    }

    val optionalCredentialsPart         = credentials.fold(
      Json.obj()
    )(credential =>
      Json.obj(
        "optionalCredentials" -> Json.obj(
          "providerId"   -> credential.providerId,
          "providerType" -> credential.providerType
        )
      )
    )
    val enrolments: Set[Enrolment]      = allEnrolments.getOrElse(Set())
    val allEnrolmentsJsonPart: JsObject = Json.obj("allEnrolments" -> enrolments)

    val ninoPart = authNino.fold(Json.obj())(nino => Json.obj("nino" -> nino))

    val authoriseJsonBody: JsObject = allEnrolmentsJsonPart ++ optionalCredentialsPart ++ ninoPart

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.prettyPrint(authoriseJsonBody))
        )
    )
  }
}
