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

package services

import cats.Eq
import cats.data.EitherT
import cats.syntax.either._
import com.google.inject.{ ImplementedBy, Inject, Singleton }
import connectors.AuthLoginStubConnector
import connectors.AuthLoginStubConnector.StubException
import play.api.libs.json.{ Format, Json, Writes }
import play.api.libs.ws.WSResponse
import play.api.mvc.{ Cookie, Session, SessionCookieBaker }
import services.AuthLoginStubService.{ AuthError, LSR, LoginData, liftError, loginDataOf }
import uk.gov.hmrc.auth.core.{ AffinityGroup, ConfidenceLevel, Enrolment }
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

import java.time.{ LocalDate, ZonedDateTime }
import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }

trait AuthLoginStubService {

  def login(group: AffinityGroup, enrolments: List[Enrolment])(implicit hc: HeaderCarrier): LSR[Session]

}

//val enrolment: CEnrolment = CEnrolment("key", Nil, "active")
//

class AuthLoginStubServiceImpl @Inject() (
  connector: AuthLoginStubConnector,
  sessionCookieCrypto: SessionCookieCrypto,
  sessionCookieBaker: SessionCookieBaker)(implicit ec: ExecutionContext) extends AuthLoginStubService {

  def createSession(response: WSResponse): Either[AuthError, Session] = {
    for {
      c <- Either.fromOption(response.cookie("mdtp"), AuthError(new IllegalStateException("missing cookie")))
    } yield {
      val decrypted = Cookie(name = "mdtp", value = sessionCookieCrypto.crypto.decrypt(Crypted(c.value)).value)
      sessionCookieBaker.decodeFromCookie(Some(decrypted))
    }
  }

  override def login(group: AffinityGroup, enrolments: List[Enrolment])(implicit hc: HeaderCarrier): LSR[Session] =
    connector.login(loginDataOf(group, enrolments)).leftMap(liftError).subflatMap(createSession)

}

object AuthLoginStubService {

  trait AffinityGrou

  type LSR[A] = EitherT[Future, AuthError, A]

  def liftError(error: StubException): AuthError = AuthError(error.e)

  final case class AuthError(t: Throwable)

  final case class LoginData(
    ggCredId: GGCredId,
    redirectUrl: String,
    confidenceLevel: ConfidenceLevel,
    affinityGroup: AffinityGroup,
    email: Option[EmailAddress],
    nino: Option[NINO],
    enrolment: Option[Enrolment])
  //  existingTaxChecks: List[SaveTaxCheckRequest])

  final case class GGCredId(value: String) extends AnyVal

  object GGCredId {

    implicit val format: Format[GGCredId] = Json.valueFormat[GGCredId]
    implicit val eq: Eq[GGCredId] = Eq.fromUniversalEquals
  }

  final case class EmailAddress(value: String) extends AnyVal

  object EmailAddress {

    implicit val format: Format[EmailAddress] = Json.valueFormat[EmailAddress]
    implicit val eq: Eq[EmailAddress] = Eq.fromUniversalEquals

  }

  final case class SaveTaxCheckRequest(
    taxCheckCode: HECTaxCheckCode,
    ggCredId: GGCredId,
    licenceType: LicenceType,
    verifier: Either[CRN, DateOfBirth],
    expiresAfter: LocalDate,
    createDate: ZonedDateTime,
    taxCheckStartDateTime: ZonedDateTime,
    isExtracted: Boolean,
    source: HECTaxCheckSource)

  object SaveTaxCheckRequest {

  }

  final case class NINO(value: String) extends AnyVal

  object NINO {

    implicit val format: Format[NINO] = Json.valueFormat[NINO]

  }

  final case class HECTaxCheckCode(value: String) extends AnyVal

  object HECTaxCheckCode {

    implicit val eq: Eq[HECTaxCheckCode] = Eq.fromUniversalEquals

    implicit val format: Format[HECTaxCheckCode] = Json.valueFormat[HECTaxCheckCode]

  }

  case class LicenceType private (ordinal: Int, name: String)

  object LicenceType {

    val DriverOfTaxisAndPrivateHires = LicenceType(0, "DriverOfTaxisAndPrivateHires")

    val OperatorOfPrivateHireVehicles = LicenceType(1, "OperatorOfPrivateHireVehicles")

    val ScrapMetalMobileCollector = LicenceType(2, "ScrapMetalMobileCollector")

    val ScrapMetalDealerSite = LicenceType(3, "ScrapMetalDealerSite")

    implicit val eq: Eq[LicenceType] = Eq.fromUniversalEquals

    @SuppressWarnings(Array("org.wartremover.warts.Throw", "org.wartremover.warts.Equals"))
    implicit val format: Format[LicenceType] = Json.format[LicenceType]

  }

  case class HECTaxCheckSource private (ordinal: Int, name: String)

  object HECTaxCheckSource {

    val Digital = HECTaxCheckSource(0, "Digital")

    @SuppressWarnings(Array("org.wartremover.warts.Throw", "org.wartremover.warts.Equals"))
    implicit val format: Format[HECTaxCheckSource] = Json.format[HECTaxCheckSource]

  }

  final case class CRN(value: String) extends AnyVal

  object CRN {

    implicit val format: Format[CRN] = Json.valueFormat
    implicit val eq: Eq[CRN] = Eq.fromUniversalEquals

  }

  final case class DateOfBirth(value: LocalDate) extends AnyVal

  object DateOfBirth {

    implicit val format: Format[DateOfBirth] = Json.valueFormat
  }

  def loginDataOf(group: AffinityGroup, enrolments: List[Enrolment]): _root_.services.AuthLoginStubService.LoginData = {
    LoginData(GGCredId(UUID.randomUUID().toString), "http://localhost:9999/nowhere",
      ConfidenceLevel.L50, AffinityGroup.Individual, None, None, enrolments.headOption)
  }

}

