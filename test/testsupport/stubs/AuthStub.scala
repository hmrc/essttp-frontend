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
                 credentials: Option[Credentials] = Some(Credentials("authId-999", "GovernmentGateway"))
               ): StubMapping = {

    implicit val enrolmentFormat: OFormat[Enrolment] = {
      implicit val f = Json.format[EnrolmentIdentifier]
      Json.format[Enrolment]
    }

    val optionalCredentialsPart = credentials.fold(
      Json.obj()
    )(credential =>
      Json.obj(
        "optionalCredentials" -> Json.obj(
          "providerId" -> credential.providerId,
          "providerType" -> credential.providerType
        )
      )
    )
    val enrolments: Set[Enrolment] = allEnrolments.getOrElse(Set())
    val allEnrolmentsJsonPart: JsObject = Json.obj("allEnrolments" -> enrolments)

    val authoriseJsonBody: JsObject = allEnrolmentsJsonPart ++ optionalCredentialsPart

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.prettyPrint(authoriseJsonBody))
        )
    )
  }
}
