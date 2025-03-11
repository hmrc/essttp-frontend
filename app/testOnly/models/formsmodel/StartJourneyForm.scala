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

package testOnly.models.formsmodel

import cats.implicits.catsSyntaxOptionId
import cats.syntax.either._
import config.AppConfig
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel._
import models.MoneyUtil.{amountOfMoneyFormatter, formatAmountOfMoneyWithoutPoundSign}
import models.{EligibilityError, EligibilityErrors, Language}
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formatter
import testOnly.messages.Messages
import testOnly.models.testusermodel.{ConfidenceLevelAndNino, RandomDataGenerator}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import util.EnumFormatter

import scala.util.{Random, Try}

//TODO OPS-12584 - Clean this up when TTP has implemented the changes to the Eligibility API. The newTtpApi option in start page will not be needed
final case class StartJourneyForm(
  signInAs:                      SignInAs,
  enrolments:                    Seq[Enrolment],
  origin:                        Origin,
  eligibilityErrors:             Seq[EligibilityError],
  debtTotalAmount:               BigDecimal,
  interestAmount:                Option[BigDecimal],
  taxReference:                  TaxId,
  regimeDigitalCorrespondence:   Boolean,
  emailAddressPresent:           Boolean,
  isInterestBearingCharge:       Option[Boolean],
  useChargeReference:            Option[Boolean],
  chargeBeforeMaxAccountingDate: Option[Boolean],
  ddInProgress:                  Option[Boolean],
  transitionToCDCS:              Option[Boolean],
  chargeSource:                  Option[String],
  planMinLength:                 Int,
  planMaxLength:                 Int,
  mainTrans:                     String,
  subTrans:                      String,
  confidenceLevelAndNino:        ConfidenceLevelAndNino,
  credId:                        Option[String]
)

object StartJourneyForm {

  def form(
    taxRegime: TaxRegime,
    appConfig: AppConfig
  )(using Language): Form[StartJourneyForm] =
    Form(
      mapping(
        "signInAs"                      -> signInMapping,
        "enrolments"                    -> enrolmentsMapping,
        "origin"                        -> originMapping,
        "eligibilityErrors"             -> seq(enumeratum.Forms.enumMapping(EligibilityErrors)),
        ""                              -> Forms.of(debtTotalAmountFormat(taxRegime, appConfig)),
        "interestAmount"                -> interestAmountMapping,
        ""                              -> Forms.of(taxReferenceFormat(taxRegime)),
        "regimeDigitalCorrespondence"   -> optionalBooleanMappingDefaultTrue,
        "emailAddressPresent"           -> optionalBooleanMappingDefaultTrue,
        "isInterestBearingCharge"       -> chargesOptionalFieldsMapping,
        "useChargeReference"            -> chargesOptionalFieldsMapping,
        "chargeBeforeMaxAccountingDate" -> chargesOptionalFieldsMapping,
        "ddInProgress"                  -> chargesOptionalFieldsMapping,
        "transitionToCDCS"              -> chargesOptionalFieldsMapping,
        "chargeSource"                  -> chargesOptionalStringMapping,
        "planMinLength"                 -> number,
        "planMaxLength"                 -> number,
        "mainTrans"                     -> textWithFourDigitsMapping("mainTrans"),
        "subTrans"                      -> textWithFourDigitsMapping("subTrans"),
        ""                              -> Forms.of(confidenceLevelAndNinoFormatter),
        "credId"                        -> optional(text).transform[Option[String]](_.filter(_.nonEmpty), identity)
      )(StartJourneyForm.apply)(Tuple.fromProductTyped[StartJourneyForm](_).some)
    )

  private def textWithFourDigitsMapping(key: String) =
    text
      .transform[String](_.replaceAll("\\s", ""), identity)
      .verifying(s"$key must be four digits", s => s.length == 4 && s.forall(_.isDigit))

  private val confidenceLevelKey: String = "confidenceLevel"

  private val ninoKey: String = "nino"

  private val confidenceLevelAndNinoFormatter: Formatter[ConfidenceLevelAndNino] =
    new Formatter[ConfidenceLevelAndNino] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], ConfidenceLevelAndNino] = {
        val result = for {
          s    <- Either.fromOption(data.get(confidenceLevelKey), FormError(key, "Confidence level is required"))
          i    <- Try(s.toInt).toEither.leftMap(_ =>
                    FormError(confidenceLevelKey, "Could not parse confidence level as number")
                  )
          cl   <-
            ConfidenceLevel.fromInt(i).toEither.leftMap(_ => FormError(confidenceLevelKey, "Invalid confidence level"))
          nino <- {
            val value = data.get(ninoKey).filter(_.nonEmpty)
            if (cl > ConfidenceLevel.L50)
              Either.fromOption(value, FormError(ninoKey, "NINO required if CL > 50")).map(Some(_))
            else Right(value)
          }
        } yield ConfidenceLevelAndNino(cl, nino)

        result.leftMap(Seq(_))
      }

      override def unbind(key: String, value: ConfidenceLevelAndNino): Map[String, String] =
        Map(
          confidenceLevelKey -> Some(value.confidenceLevel.level.toString),
          ninoKey            -> value.nino
        ).collect { case (k, Some(v)) => k -> v }
    }

  private val payeDebtTotalAmountKey: String = "payeDebtTotalAmount"
  private val vatDebtTotalAmountKey: String  = "vatDebtTotalAmount"
  private val saDebtTotalAmountKey: String   = "saDebtTotalAmount"
  private val simpDebtTotalAmountKey: String = "simpDebtTotalAmount"

  private val payeTaxReferenceKey: String = "payeTaxReference"
  private val vatTaxReferenceKey: String  = "vatTaxReference"
  private val saTaxReferenceKey: String   = "saTaxReference"
  private val simpTaxReferenceKey: String = "nino"

  private def signInMapping(using Language): Mapping[SignInAs] =
    Forms.of(
      EnumFormatter.format(
        `enum` = SignInAs,
        errorMessageIfMissing = Messages.`Select how to be signed in`.show,
        errorMessageIfEnumError = Messages.`Select how to be signed in`.show
      )
    )

  private val enrolmentsMapping: Mapping[Seq[Enrolment]] = seq(
    Forms.of(EnumFormatter.format(`enum` = Enrolments))
  )

  private def originMapping(using Language): FieldMapping[Origin] =
    Forms.of(
      EnumFormatter.format(
        `enum` = Origins,
        errorMessageIfMissing = Messages.`Select which origin the journey should start from`.show,
        errorMessageIfEnumError = Messages.`Select which origin the journey should start from`.show
      )
    )

  private val interestAmountMapping: Mapping[Option[BigDecimal]] =
    optional(Forms.of(amountOfMoneyFormatter(_ < 0, _ => false)))

  private def debtTotalAmountFormat(
    taxRegime: TaxRegime,
    appConfig: AppConfig
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] = {

        val (maxAmount, amountKey) = taxRegime match {
          case TaxRegime.Epaye => appConfig.PolicyParameters.EPAYE.maxAmountOfDebt -> payeDebtTotalAmountKey
          case TaxRegime.Vat   => appConfig.PolicyParameters.VAT.maxAmountOfDebt   -> vatDebtTotalAmountKey
          case TaxRegime.Sa    => appConfig.PolicyParameters.SA.maxAmountOfDebt    -> saDebtTotalAmountKey
          case TaxRegime.Simp  => appConfig.PolicyParameters.SIMP.maxAmountOfDebt  -> simpDebtTotalAmountKey
        }
        val minAmount              = AmountInPence(100)

        amountOfMoneyFormatter(
          isTooSmall = minAmount > AmountInPence(_),
          isTooLarge = AmountInPence(_) > maxAmount
        ).bind(amountKey, data)
          .leftMap(errors =>
            errors.map { e =>
              val mappedMesage = e.message match {
                case "error.pattern"  => "Total debt amount must be a number"
                case "error.required" => "Total debt amount not found"
                case "error.tooLarge" => s"Total debt amount must be below ${maxAmount.gdsFormatInPounds}"
                case "error.tooSmall" => s"Total debt amount must be above ${minAmount.gdsFormatInPounds}"
                case other            => other
              }
              FormError(e.key, mappedMesage)
            }
          )
      }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        Map(key -> formatAmountOfMoneyWithoutPoundSign(value))
    }

  private def taxReferenceFormat(taxRegime: TaxRegime): Formatter[TaxId] =
    new Formatter[TaxId] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], TaxId] = {
        val (taxReferenceKey, defaultTaxRef): (String, TaxId) = taxRegime match {
          case TaxRegime.Epaye => payeTaxReferenceKey -> RandomDataGenerator.nextEpayeRefs()(using Random)._3
          case TaxRegime.Vat   => vatTaxReferenceKey  -> RandomDataGenerator.nextVrn()(using Random)
          case TaxRegime.Sa    => saTaxReferenceKey   -> RandomDataGenerator.nextSaUtr()(using Random)
          case TaxRegime.Simp  => simpTaxReferenceKey -> RandomDataGenerator.nextNino()(using Random)
        }

        val taxId: TaxId = data
          .get(taxReferenceKey)
          .filter(_.nonEmpty)
          .map[TaxId] { (someTaxRef: String) =>
            taxRegime match {
              case TaxRegime.Epaye => EmpRef(someTaxRef)
              case TaxRegime.Vat   => Vrn(someTaxRef)
              case TaxRegime.Sa    => SaUtr(someTaxRef)
              case TaxRegime.Simp  => Nino(someTaxRef)
            }
          }
          .getOrElse(defaultTaxRef)

        Right(taxId)
      }

      override def unbind(key: String, value: TaxId): Map[String, String] =
        value match {
          case EmpRef(value) => Map(payeTaxReferenceKey -> value)
          case Vrn(value)    => Map(vatTaxReferenceKey -> value)
          case SaUtr(value)  => Map(saTaxReferenceKey -> value)
          case Nino(value)   => Map(simpTaxReferenceKey -> value)
        }
    }

  private val optionalBooleanMappingDefaultTrue: Mapping[Boolean] =
    optional(boolean).transform[Boolean](_.getOrElse(true), Some(_))

  private val chargesOptionalFieldsMapping: Mapping[Option[Boolean]] =
    optional(boolean)

  private val chargesOptionalStringMapping: Mapping[Option[String]] =
    optional(text)

}
