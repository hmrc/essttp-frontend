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

package connectors

import config.AppConfig
import connectors.EligibilityConnector._
import essttp.rootmodel.{ Aor, TaxId, TaxRegime, Vrn }
import models.ttp.TtpEligibilityData
import play.api.libs.json.{ Format, Json }
import testOnly.models.EligibilityError
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import util.RegimeUtils._
import uk.gov.hmrc.http.HttpReads.Implicits._
import util.TaxIdUtils.TaxIdOps

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EligibilityStubConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig) {

  def insertEligibilityData(regime: TaxRegime, taxId: TaxId, data: TtpEligibilityData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    httpClient
      .POST[TtpEligibilityData, Unit](
        url = insertUrl(regime, taxId),
        body = data,
        headers = Seq.empty)
  }

  def eligibilityData(idType: String, regime: TaxRegime, id: TaxId, showFinancials: Boolean)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[TtpEligibilityData] = {

    httpClient
      .POST[EligibilityRequest, TtpEligibilityData](
        url = ttpUrl,
        body = EligibilityRequest(idType, id.value, regime.name, showFinancials))

  }

  def errors(regime: TaxRegime, id: TaxId, errors: List[EligibilityError])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    httpClient
      .POST[ErrorData, Unit](
        url = errorUrl(regime, id),
        body = ErrorData(errors))
  }

  val ttpUrl = s"${appConfig.ttpBaseUrl}/time-to-pay/self-serve/eligibility"

  def errorUrl(regime: TaxRegime, taxId: TaxId): String = {
    s"${appConfig.ttpBaseUrl}/time-to-pay/self-serve/eligibility/${regime.entryName}/${taxId.value}/errors"
  }

  def insertUrl(regime: TaxRegime, taxId: TaxId): String = {
    s"${appConfig.ttpBaseUrl}/time-to-pay/self-serve/eligibility/${regime.entryName}/${taxId.value}"
  }

}

object EligibilityConnector {

  case class EligibilityRequest(idType: String, idNumber: String, regimeType: String, returnFinancials: Boolean)

  case class ErrorData(errors: List[EligibilityError])

  object ErrorData {
    implicit val fmt: Format[ErrorData] = Json.format[ErrorData]
  }

  object EligibilityRequest {
    implicit val fmt: Format[EligibilityRequest] = Json.format[EligibilityRequest]
  }

}

