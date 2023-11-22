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

import cats.syntax.either._
import essttp.journey.model.{Origin, Origins}
import essttp.rootmodel.TaxId
import essttp.rootmodel.{AmountInPence, EmpRef, TaxRegime, Vrn}
import models.MoneyUtil.{amountOfMoneyFormatter, formatAmountOfMoneyWithoutPoundSign}
import models.{EligibilityError, EligibilityErrors, Language}
import play.api.data.Forms.{boolean, mapping, number, optional, seq}
import play.api.data._
import play.api.data.format.Formatter
import testOnly.messages.Messages
import testOnly.models.testusermodel.RandomDataGenerator
import util.EnumFormatter

import scala.util.Random

final case class StartJourneyForm(
    signInAs:                      SignInAs,
    enrolments:                    Seq[Enrolment],
    origin:                        Origin,
    eligibilityErrors:             Seq[EligibilityError],
    debtTotalAmount:               BigDecimal,
    interestAmount:                Option[BigDecimal],
    taxReference:                  TaxId,
    taxRegime:                     TaxRegime,
    regimeDigitalCorrespondence:   Boolean,
    emailAddressPresent:           Boolean,
    isInterestBearingCharge:       Option[Boolean],
    useChargeReference:            Option[Boolean],
    chargeBeforeMaxAccountingDate: Option[Boolean],
    planMinLength:                 Int,
    planMaxLength:                 Int
)

object StartJourneyForm {

  def form(
      payeMaxAmountOfDebt: AmountInPence,
      vatMaxAmountOfDebt:  AmountInPence
  )(implicit language: Language): Form[StartJourneyForm] = {
    Form(
      mapping(
        "signInAs" -> signInMapping,
        "enrolments" -> enrolmentsMapping,
        "origin" -> originMapping,
        "eligibilityErrors" -> seq(enumeratum.Forms.enumMapping(EligibilityErrors)),
        "" -> Forms.of(debtTotalAmountFormat(payeMaxAmountOfDebt, vatMaxAmountOfDebt)),
        "interestAmount" -> interestAmountMapping,
        "" -> Forms.of(taxReferenceFormat),
        taxRegimeKey -> Forms.of(taxRegimeFormatter),
        "regimeDigitalCorrespondence" -> optionalBooleanMappingDefaultTrue,
        "emailAddressPresent" -> optionalBooleanMappingDefaultTrue,
        "isInterestBearingCharge" -> chargesOptionalFieldsMapping,
        "useChargeReference" -> chargesOptionalFieldsMapping,
        "chargeBeforeMaxAccountingDate" -> chargesOptionalFieldsMapping,
        "planMinLength" -> number,
        "planMaxLength" -> number
      )(StartJourneyForm.apply)(StartJourneyForm.unapply)
    )
  }

  private val taxRegimeKey: String = "taxRegime"
  private val payeDebtTotalAmountKey: String = "payeDebtTotalAmount"
  private val vatDebtTotalAmountKey: String = "vatDebtTotalAmount"
  private val payeTaxReferenceKey: String = "payeTaxReference"
  private val vatTaxReferenceKey: String = "vatTaxReference"

  private def signInMapping(implicit language: Language): Mapping[SignInAs] = {
    Forms.of(EnumFormatter.format(
      `enum`                  = SignInAs,
      errorMessageIfMissing   = Messages.`Select how to be signed in`.show,
      errorMessageIfEnumError = Messages.`Select how to be signed in`.show
    ))
  }

  private val enrolmentsMapping: Mapping[Seq[Enrolment]] = seq(
    Forms.of(EnumFormatter.format(`enum` = Enrolments))
  )

  private def originMapping(implicit language: Language): FieldMapping[Origin] = {
    Forms.of(EnumFormatter.format(
      `enum`                  = Origins,
      errorMessageIfMissing   = Messages.`Select which origin the journey should start from`.show,
      errorMessageIfEnumError = Messages.`Select which origin the journey should start from`.show
    ))
  }

  private val interestAmountMapping: Mapping[Option[BigDecimal]] =
    optional(Forms.of(amountOfMoneyFormatter(_ < 0, _ => false)))

  private val taxRegimeFormatter: Formatter[TaxRegime] = EnumFormatter.format(
    `enum`                  = TaxRegime,
    errorMessageIfMissing   = "Tax regime not found: missing",
    errorMessageIfEnumError = "Tax regime not found: enum error",
    insensitive             = true
  )

  private def debtTotalAmountFormat(
      payeMaxAmountOfDebt: AmountInPence,
      vatMaxAmountOfDebt:  AmountInPence
  ): Formatter[BigDecimal] = {
    new Formatter[BigDecimal] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        taxRegimeFormatter.bind(taxRegimeKey, data).flatMap { taxRegime =>
          val (maxAmount, amountKey) = taxRegime match {
            case TaxRegime.Epaye => payeMaxAmountOfDebt -> payeDebtTotalAmountKey
            case TaxRegime.Vat   => vatMaxAmountOfDebt -> vatDebtTotalAmountKey
          }
          val minAmount = AmountInPence(100)

          amountOfMoneyFormatter(
            isTooSmall = minAmount > AmountInPence(_),
            isTooLarge = AmountInPence(_) > maxAmount
          ).bind(amountKey, data).leftMap(errors =>
            errors.map { e =>
              val mappedMesage = e.message match {
                case "error.pattern"  => "Total debt amount must be a number"
                case "error.required" => "Total debt amount not found"
                case "error.tooLarge" => s"Total debt amount must be below ${maxAmount.gdsFormatInPounds}"
                case "error.tooSmall" => s"Total debt amount must be above ${minAmount.gdsFormatInPounds}"
                case other            => other
              }
              FormError(e.key, mappedMesage)
            })
        }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        Map(key -> formatAmountOfMoneyWithoutPoundSign(value))
    }
  }

  private val taxReferenceFormat: Formatter[TaxId] = {
    new Formatter[TaxId] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], TaxId] =
        taxRegimeFormatter.bind(taxRegimeKey, data).map { taxRegime =>
          val (taxReferenceKey, defaultTaxRef): (String, TaxId) = taxRegime match {
            case TaxRegime.Epaye => payeTaxReferenceKey -> RandomDataGenerator.nextEpayeRefs()(Random)._3
            case TaxRegime.Vat   => vatTaxReferenceKey -> RandomDataGenerator.nextVrn()(Random)
          }
          val taxId: Option[TaxId] =
            data.get(taxReferenceKey)
              .filter(_.nonEmpty).map { someTaxRef: String =>
                taxRegime match {
                  case TaxRegime.Epaye => EmpRef(someTaxRef)
                  case TaxRegime.Vat   => Vrn(someTaxRef)
                }
              }

          taxId.getOrElse(defaultTaxRef)
        }

      override def unbind(key: String, value: TaxId): Map[String, String] = {
        value match {
          case EmpRef(value) => Map(payeTaxReferenceKey -> value)
          case Vrn(value)    => Map(vatTaxReferenceKey -> value)
        }
      }
    }
  }

  private val optionalBooleanMappingDefaultTrue: Mapping[Boolean] = {
    optional(boolean).transform[Boolean](_.getOrElse(true), Some(_))
  }

  private val chargesOptionalFieldsMapping: Mapping[Option[Boolean]] = {
    optional(boolean)
  }

}
