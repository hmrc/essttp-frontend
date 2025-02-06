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

package messages

import cats.syntax.eq._
import essttp.rootmodel.ttp.eligibility.MainTrans
import essttp.rootmodel.{AmountInPence, CannotPayReason, Email, TaxRegime}
import models.Languages
import models.forms.BankDetailsForm._

import java.time.LocalDate
import scala.annotation.tailrec

object Messages {

  val continue: Message = Message(
    english = "Continue",
    welsh   = "Yn eich blaen"
  )

  val cancel: Message = Message(
    english = "Cancel",
    welsh   = "Canslo"
  )

  val back: Message = Message(
    english = "Back",
    welsh   = "Yn ôl"
  )

  val `Sign out`: Message = Message(
    english = "Sign out",
    welsh   = "Allgofnodi"
  )

  val `Sign in`: Message = Message(
    english = "Sign in",
    welsh   = "Mewngofnodi"
  )

  val `Yes`: Message = Message(
    english = "Yes",
    welsh   = "Iawn"
  )

  val `No`: Message = Message(
    english = "No",
    welsh   = "Na"
  )

  val error: Message = Message(
    english = "Error: ",
    welsh   = "Gwall: "
  )

  val change: Message = Message(
    english = "Change",
    welsh   = "Newid"
  )

  val `There is a problem`: Message = Message(
    english = "There is a problem",
    welsh   = "Mae problem wedi codi"
  )

  val `Start now`: Message = Message(
    english = "Start now",
    welsh   = "Dechrau nawr"
  )

  object WhichTaxRegime {

    val `Which tax do you want to set up a payment plan for?`: Message = Message(
      english = "Which tax do you want to set up a payment plan for?",
      welsh   = "Pa dreth rydych chi am sefydlu cynllun talu ar ei chyfer?"
    )

    val EPAYE: Message = Message(
      english = "Employers’ PAYE",
      welsh   = "TWE Cyflogwyr"
    )

    val VAT: Message = Message(
      english = "VAT",
      welsh   = "TAW"
    )

    val SA: Message = Message(
      english = "Self Assessment",
      welsh   = "Hunanasesiad"
    )

    val SIMP: Message = Message(
      english = "Simple Assessment",
      welsh   = "Asesiad Syml"
    )

    val `Select which tax you want to set up a payment plan for`: Message = Message(
      english = "Select which tax you want to set up a payment plan for",
      welsh   = "Dewiswch pa dreth rydych chi am sefydlu cynllun talu ar ei chyfer"
    )

  }

  object YourBill {

    val to: Message = Message(
      english = "to",
      welsh   = "i"
    )

    val month: Message = Message(
      english = "month",
      welsh   = "mis"
    )

    val `Bill due`: Message = Message(
      english = "Bill due",
      welsh   = "Bil yn ddyledus"
    )

    val `(includes interest added to date)`: Message = Message(
      english = "(includes interest added to date)",
      welsh   = "(yn cynnwys llog a ychwanegwyd hyd yn hyn)"
    )

    def yourBillIs(amount: AmountInPence, taxRegime: TaxRegime): Message =
      taxRegime match {
        case TaxRegime.Epaye =>
          Message(
            english = s"Your PAYE bill is ${amount.gdsFormatInPounds}",
            welsh   = s"Eich bil TWE yw ${amount.gdsFormatInPounds}"
          )

        case TaxRegime.Vat =>
          Message(
            english = s"Your VAT bill is ${amount.gdsFormatInPounds}",
            welsh   = s"Eich bil TAW yw ${amount.gdsFormatInPounds}"
          )

        case TaxRegime.Sa =>
          Message(
            english = s"Your Self Assessment tax bill is ${amount.gdsFormatInPounds}",
            welsh   = s"Mae’ch bil treth Hunanasesiad yn dod i gyfanswm o ${amount.gdsFormatInPounds}"
          )

        case TaxRegime.Simp =>
          Message(
            english = s"Your Simple Assessment tax bill is ${amount.gdsFormatInPounds}",
            welsh   = s"Mae’ch bil treth Asesiad Syml yn dod i gyfanswm o ${amount.gdsFormatInPounds}"
          )
      }

    val `Overdue payments`: Message = Message(
      english = "Overdue payments",
      welsh   = "Taliadau sy’n hwyr"
    )

    val `Self Assessment statement`: Message = Message(
      english = "Self Assessment statement",
      welsh   = "Datganiad Hunanasesiad"
    )

    def Due(date: LocalDate): Message = {
      val day = date.getDayOfMonth

      val enlgishDateString =
        s"${day.toString} ${DateMessages.monthName(date.getMonthValue).english} ${date.getYear.toString}"
      val welshDateString =
        s"${day.toString}${DateMessages.getSuffix(day)(Languages.Welsh)} " +
          s"${DateMessages.monthName(date.getMonthValue).show(Languages.Welsh)} " +
          s"${date.getYear.toString}"

      Message(
        english = s"Due $enlgishDateString",
        welsh   = s"Yn ddyledus erbyn $welshDateString"
      )
    }

    def `ChargeTypeRow`(mTrans: MainTrans, taxYearStartYear: Int, taxYearEndYear: Int): Message = {

      val chargeTypeDescriptionEnglish: String = ChargeTypeMessages.chargeFromMTrans.get(mTrans).map(_.english)
        .getOrElse(
          throw MainTrans.UnknownMainTransException(mTrans)
        )

      val chargeTypeDescriptionWelsh: String = ChargeTypeMessages.chargeFromMTrans.get(mTrans).map(_.show(Languages.Welsh))
        .getOrElse(
          throw MainTrans.UnknownMainTransException(mTrans)
        )

      val forTaxYearEnglish: String = s"for tax year ${taxYearStartYear.toString} to ${taxYearEndYear.toString}"
      val forTaxYearWelsh: String = s"ar gyfer blwyddyn dreth ${taxYearStartYear.toString} i ${taxYearEndYear.toString}"

      Message(
        english = s"$chargeTypeDescriptionEnglish $forTaxYearEnglish",
        welsh   = s"$chargeTypeDescriptionWelsh $forTaxYearWelsh"
      )
    }

  }

  object TimeOut {

    val `For your security, we signed you out`: Message = Message(
      english = "For your security, we signed you out",
      welsh   = "Er eich diogelwch, gwnaethom eich allgofnodi"
    )

    val `You’re about to be signed out`: Message = Message(
      english = "You’re about to be signed out",
      welsh   = "Rydych ar fin cael eich allgofnodi"
    )

  }

  object ServicePhase {

    def serviceName(taxRegime: TaxRegime): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = "Set up an Employers’ PAYE payment plan",
          welsh   = "Trefnu cynllun talu ar gyfer TWE Cyflogwyr"
        )

      case TaxRegime.Vat =>
        Message(
          english = "Set up a VAT payment plan",
          welsh   = "Trefnu cynllun talu TAW"
        )

      case TaxRegime.Sa =>
        Message(
          english = "Set up a Self Assessment payment plan",
          welsh   = "Sefydlu cynllun talu ar gyfer Hunanasesiad"
        )

      case TaxRegime.Simp =>
        Message(
          english = "Set up a Simple Assessment payment plan",
          welsh   = "Sefydlu cynllun talu ar gyfer Asesiad Syml"
        )
    }

    val `Set up a payment plan`: Message = Message(
      english = "Set up a payment plan",
      welsh   = "Trefnu cynllun talu"
    )

    val beta: Message = Message(
      english = "Beta",
      welsh   = "Beta"
    )

    def bannerText(link: String): Message = Message(
      english = s"""This is a new service – your <a class="govuk-link" href="$link">feedback</a> will help us to improve it.""",
      welsh   = s"""Mae hwn yn wasanaeth newydd – bydd eich <a class="govuk-link" href="$link">adborth</a> yn ein helpu i’w wella."""
    )
  }

  object ExtraSupport {

    val `If you need extra support`: Message = Message(
      english = "If you need extra support",
      welsh   = "Os oes angen cymorth ychwanegol arnoch chi"
    )

    def `Find out the different ways to deal with HMRC...`(link: String): Message = Message(
      english = s"""Find out the different ways to <a href="$link" class="govuk-link">deal with HMRC if you need some help</a>.""",
      welsh   = s"""Dysgwch am y ffyrdd gwahanol o <a href="$link" class="govuk-link">ddelio â CThEF os oes angen help arnoch chi</a>."""
    )

    def `You can also use Relay UK...`(link: String): Message = Message(
      english = s"""You can also use <a href="$link" class="govuk-link">Relay UK</a> if you cannot hear or speak on the phone: dial <strong>18001</strong> then <strong>0345 300 3900</strong>.""",
      welsh   = s"""Gallwch hefyd ddefnyddio <a href="$link" class="govuk-link">Relay UK</a> os na allwch glywed na siarad dros y ffôn: deialwch <strong>18001</strong> ac yna <strong>0345 300 3900</strong>. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw."""
    )

    val `If you are outside the UK...`: Message = Message(
      english = "If you are outside the UK: <strong>+44 2890 538 192</strong>",
      welsh   = "Os ydych y tu allan i’r DU: <strong>+44 300 200 1900</strong>"
    )

    // The "If you are calling from outside the UK" section is only shown in English
    val `If you're calling from outside the UK`: Message = Message(
      english = "If you’re calling from outside the UK"
    )

    val `Call us on...`: Message = Message(
      english = "Call us on <strong>+44 2890 538 192</strong>."
    )

    val `Our opening times are...`: Message = Message(
      english = "Our opening times are Monday to Friday, 8am to 6pm (UK time). We are closed on weekends and bank holidays."
    )

  }

  object NotEligible {

    val `Before you call, make sure you have:`: Message = Message(
      english = "Before you call, make sure you have:",
      welsh   = "Cyn i chi ffonio, sicrhewch fod gennych y canlynol:"
    )

    val `your Accounts Office reference...`: Message = Message(
      english = "your Accounts Office reference which is 13 characters long, like 123PX00123456",
      welsh   = "eich cyfeirnod Swyddfa Gyfrifon, sy’n 13 o gymeriadau o hyd, er enghraifft 123PX00123456"
    )

    val `your VAT registration number which is 9 digits long, like 123456789`: Message = Message(
      english = "your VAT registration number which is 9 digits long, like 123456789",
      welsh   = "eich rhif cofrestru TAW, sy’n 9 digid o hyd, er enghraifft 123456789"
    )

    val `your Self Assessment Unique Taxpayer Reference (UTR)...`: Message = Message(
      english = "your Self Assessment Unique Taxpayer Reference (UTR) which is 10 digits long, like 1234567890",
      welsh   = "eich Cyfeirnod Unigryw y Trethdalwr (UTR) ar gyfer Hunanasesiad sy’n 10 digid o hyd, fel 1234567890"
    )

    val `your National Insurance number`: Message = Message(
      english = "your National Insurance number",
      welsh   = "eich rhif Yswiriant Gwladol"
    )

    val `information on any savings or investments you have`: Message = Message(
      english = "information on any savings or investments you have",
      welsh   = "gwybodaeth am unrhyw gynilion neu fuddsoddiadau sydd gennych"
    )

    val `details of your income and spending`: Message = Message(
      english = "details of your income and spending",
      welsh   = "manylion eich incwm a’ch gwariant"
    )

    val `your bank details`: Message = Message(
      english = "your bank details",
      welsh   = "eich manylion banc"
    )

    val `We're likely to ask:`: Message = Message(
      english = "We’re likely to ask:",
      welsh   = "Rydym yn debygol o ofyn:"
    )

    val `what you've done to try to pay the bill`: Message = Message(
      english = "what you’ve done to try to pay the bill",
      welsh   = "beth rydych wedi’i wneud i geisio talu’r bil"
    )

    val `if you can pay some of the bill now`: Message = Message(
      english = "if you can pay some of the bill now",
      welsh   = "a allwch dalu rhywfaint o’r bil nawr"
    )

    val `Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.`: Message = Message(
      english = "Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.",
      welsh   = "Ein horiau agor yw Dydd Llun i Ddydd Gwener, 08:30 i 17:00. Rydym ar gau ar benwythnosau a gwyliau banc."
    )

    val `Call us about a payment plan`: Message = Message(
      english = "Call us about a payment plan",
      welsh   = "Ffoniwch ni ynghylch cynllun talu"
    )

    def `You cannot set up ... debt too large`(taxRegime: TaxRegime, maxAmountOfDebt: AmountInPence): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = s"You cannot set up an Employers’ PAYE payment plan online because you owe more than ${maxAmountOfDebt.gdsFormatInPounds}.",
          welsh   = s"Ni allwch drefnu cynllun talu TAW ar-lein oherwydd mae arnoch dros ${maxAmountOfDebt.gdsFormatInPounds}."
        )
      case TaxRegime.Vat =>
        Message(
          english = s"You cannot set up a VAT payment plan online because you owe more than ${maxAmountOfDebt.gdsFormatInPounds}.",
          welsh   = s"Ni allwch drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein oherwydd mae arnoch dros ${maxAmountOfDebt.gdsFormatInPounds}."
        )
      case TaxRegime.Sa =>
        Message(
          english = s"You cannot set up a Self Assessment payment plan online because you owe more than ${maxAmountOfDebt.gdsFormatInPounds}.",
          welsh   = s"Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd mae arnoch dros ${maxAmountOfDebt.gdsFormatInPounds}."
        )
      case TaxRegime.Simp =>
        Message(
          english = s"You cannot set up a Simple Assessment payment plan online because you owe more than ${maxAmountOfDebt.gdsFormatInPounds}.",
          welsh   = s"Ni allwch drefnu cynllun talu Asesiad Syml ar-lein oherwydd mae arnoch dros ${maxAmountOfDebt.gdsFormatInPounds}."
        )
    }

    def `Pay your ... bill in full`(taxRegime: TaxRegime): Message = {
      val (taxSpecificContentEnglish, taxSpecificContentWelsh) = taxRegime match {
        case TaxRegime.Epaye => "PAYE" -> "TWE"
        case TaxRegime.Vat   => "VAT" -> "TAW"
        case TaxRegime.Sa    => "Self Assessment tax" -> "treth Hunanasesiad"
        case TaxRegime.Simp  => "Simple Assessment tax" -> "treth Asesiad Syml"
      }
      Message(
        english = s"Pay your $taxSpecificContentEnglish bill in full",
        welsh   = s"Talu’ch bil $taxSpecificContentWelsh yn llawn"
      )
    }

    val `You cannot use this service`: Message = Message(
      english = "You cannot use this service",
      welsh   = "Ni allwch ddefnyddio’r gwasanaeth hwn"
    )

    def `You cannot set up ... debt too small`(taxRegime: TaxRegime): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = "You cannot set up an Employers’ PAYE payment plan online because your bill is too small.",
          welsh   = "Ni allwch drefnu cynllun talu ar gyfer TWE y Cyflogwr ar-lein oherwydd bod eich bil yn rhy fach."
        )
      case TaxRegime.Vat =>
        Message(
          english = "You cannot set up a VAT payment plan online because your bill is too small.",
          welsh   = "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd bod eich bil yn rhy fach."
        )
      case TaxRegime.Sa =>
        Message(
          english = "You cannot set up a Self Assessment payment plan online because your bill is too small.",
          welsh   = "Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd bod eich bil yn rhy fach."
        )
      case TaxRegime.Simp =>
        Message(
          english = "You cannot set up a Simple Assessment payment plan online because your bill is too small.",
          welsh   = "Ni allwch drefnu cynllun talu Asesiad Syml oherwydd bod eich bil yn rhy fach."
        )
    }

    def `Make a payment online to cover your ... bill in full.`(taxRegime: TaxRegime, link: String): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = s"""<a class="govuk-link" href="$link">Make a payment online</a> to cover your PAYE bill in full.""",
          welsh   = s"""<a class="govuk-link" href="$link">Gwnewch daliad ar-lein</a> i dalu’ch bil TWE yn llawn."""
        )
      case TaxRegime.Vat =>
        Message(
          english = s"""<a class="govuk-link" href="$link">Make a payment online</a> to cover your VAT bill in full.""",
          welsh   = s"""<a class="govuk-link" href="$link">Gwnewch daliad ar-lein</a> i dalu’ch bil TAW yn llawn."""
        )
      case TaxRegime.Sa =>
        Message(
          english = s"""<a class="govuk-link" href="$link">Make a payment online</a> to cover your Self Assessment tax bill in full.""",
          welsh   = s"""<a class="govuk-link" href="$link">Gwnewch daliad ar-lein</a> i dalu’ch bil Hunanasesiad yn llawn."""
        )
      case TaxRegime.Simp =>
        Message(
          english = s"""<a class="govuk-link" href="$link">Make a payment online</a> to cover your Simple Assessment tax bill in full.""",
          welsh   = s"""<a class="govuk-link" href="$link">Gwnewch daliad ar-lein</a> i dalu’ch bil treth Asesiad Syml yn llawn."""
        )
    }

    def `Call us on 0300 123 1813 if you are having difficulty making a payment online.`: Message = Message(
      english = "Call us on <strong>0300 123 1813</strong> if you are having difficulty making a payment online.",
      welsh   = "Os ydych yn cael anawsterau wrth dalu ar-lein, ffoniwch ni ar <strong>0300 200 1900</strong>."
    )

    def `Call us on 0300 322 7835 if you are having difficulty making a payment online.`: Message = Message(
      english = "Call us on <strong>0300 322 7835</strong> if you are having difficulty making a payment online.",
      welsh   = "Os ydych yn cael anawsterau wrth dalu ar-lein, ffoniwch ni ar <strong>0300 200 1900</strong>."
    )

    def `You cannot set up ... debt too old`(taxRegime: TaxRegime, ageOfDebtInYearsOrDays: Int): Message = taxRegime match {
      //years
      case TaxRegime.Epaye => Message(
        english = s"You cannot set up an Employers’ PAYE payment plan online because your payment deadline was over ${ageOfDebtInYearsOrDays.toString} years ago.",
        welsh   = s"Ni allwch drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros ${ageOfDebtInYearsOrDays.toString} mlynedd yn ôl."
      )
      //days (for now)
      case TaxRegime.Vat => Message(
        english = s"You cannot set up a VAT payment plan online because your payment deadline was over ${ageOfDebtInYearsOrDays.toString} days ago.",
        welsh   = s"Ni allwch drefnu cynllun talu TAW ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros ${ageOfDebtInYearsOrDays.toString} wythnos yn ôl."
      )
      case TaxRegime.Sa => Message(
        english = s"You cannot set up a Self Assessment payment plan online because your payment deadline was over ${ageOfDebtInYearsOrDays.toString} days ago.",
        welsh   = s"Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros ${ageOfDebtInYearsOrDays.toString} diwrnod yn ôl."
      )
      case TaxRegime.Simp => Message(
        english = s"You cannot set up a Simple Assessment payment plan online because your payment deadline was over ${ageOfDebtInYearsOrDays.toString} days ago.",
        welsh   = s"Ni allwch drefnu cynllun talu Asesiad Syml ar-lein oherwydd roedd y dyddiad cau ar gyfer talu dros ${ageOfDebtInYearsOrDays.toString} diwrnod yn ôl."
      )
    }

    def `You cannot set up ... accounting period that started before`(accountingPeriodStart: String): Message = Message(
      english = s"You cannot set up a VAT payment plan online because your debt is for an accounting period that started before $accountingPeriodStart.",
      welsh   = s"Ni allwch drefnu cynllun talu TAW ar-lein oherwydd bod eich dyled am gyfnod cyfrifyddu a ddechreuodd cyn $accountingPeriodStart."
    )

    val `You already have a payment plan with HMRC`: Message = Message(
      english = "You already have a payment plan with HMRC",
      welsh   = "Mae gennych chi gynllun talu gyda CThEF yn barod"
    )

    val `You cannot set up an Employers' PAYE payment plan online`: Message = Message(
      english = "You cannot set up an Employers’ PAYE payment plan online because you already have a payment plan with HMRC.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer TWE y Cyflogwr oherwydd bod gennych gynllun talu ar-lein gyda CThEF yn barod."
    )

    val `You cannot set up a VAT payment plan online.`: Message = Message(
      english = "You cannot set up a VAT payment plan online because you already have a payment plan with HMRC.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer TAW oherwydd bod gennych gynllun talu gyda CThEF yn barod."
    )

    val `You cannot set up a Self Assessment payment plan online.`: Message = Message(
      english = "You cannot set up a Self Assessment payment plan online because you already have a payment plan with HMRC.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer Hunanasesiad oherwydd bod gennych gynllun talu gyda CThEF yn barod."
    )

    val `You cannot set up a Simple Assessment payment plan online.`: Message = Message(
      english = "You cannot set up a Simple Assessment payment plan online because you already have a payment plan with HMRC.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer Asesiad Syml oherwydd bod gennych gynllun talu gyda CThEF yn barod."
    )

    def `Generic ineligible message`(taxRegime: TaxRegime): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = "You are not eligible to set up an Employers’ PAYE payment plan online.",
          welsh   = "Nid ydych yn gymwys i drefnu cynllun talu ar gyfer TWE Cyflogwyr ar-lein."
        )
      case TaxRegime.Vat =>
        Message(
          english = "You are not eligible to set up a VAT payment plan online.",
          welsh   = "Nid ydych yn gymwys i drefnu cynllun talu TAW ar-lein."
        )
      case TaxRegime.Sa =>
        Message(
          english = "You are not eligible to set up a Self Assessment payment plan online.",
          welsh   = "Nid ydych yn gymwys i drefnu cynllun talu Hunanasesiad ar-lein."
        )
      case TaxRegime.Simp =>
        Message(
          english = "You are not eligible to set up a Simple Assessment payment plan online.",
          welsh   = "Nid ydych yn gymwys i drefnu cynllun talu Asesiad Syml ar-lein."
        )
    }

    val `Update your personal details to use this service`: Message = Message(
      english = "Update your personal details to use this service",
      welsh   = "Diweddaru’ch manylion personol i ddefnyddio’r gwasanaeth hwn"
    )

    def `Generic RLS message`(taxRegime: TaxRegime): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = "You cannot set up an Employers’ PAYE payment plan online because some of your personal details are not up to date.",
          welsh   = "Ni allwch drefnu cynllun talu ar gyfer TWE y Cyflogwr ar-lein oherwydd nad yw rhai o’ch manylion personol yn gyfredol."
        )
      case TaxRegime.Vat =>
        Message(
          english = "You cannot set up a VAT payment plan online because some of your personal details are not up to date.",
          welsh   = "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd nad yw rhai o’ch manylion personol yn gyfredol."
        )
      case TaxRegime.Sa =>
        Message(
          english = "You cannot set up a Self Assessment payment plan online because some of your personal details are not up to date.",
          welsh   = "Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd nad yw rhai o’ch manylion personol yn gyfredol."
        )
      case TaxRegime.Simp =>
        Message(
          english = "You cannot set up a Simple Assessment payment plan online because some of your personal details are not up to date.",
          welsh   = "Ni allwch drefnu cynllun talu Asesiad Syml ar-lein oherwydd nad yw rhai o’ch manylion personol yn gyfredol."
        )
    }

    def `You must update your details with HMRC`(link: String): Message = Message(
      english = s"""You must <a href="$link" class="govuk-link">update your details with HMRC</a>. After you’ve updated your details, wait 3 working days before trying again online.""",
      welsh   = s"""Mae’n rhaid i chi <a href="$link" class="govuk-link">roi’ch manylion newydd i CThEF</a>. Ar ôl i chi diweddaru’ch manylion, arhoswch 3 diwrnod gwaith cyn rhoi tro arall arni ar-lein."""
    )

    def `File your return to use this service`(taxRegime: TaxRegime): Message = taxRegime match {
      case TaxRegime.Epaye =>
        Message(
          english = "File your return to use this service",
          welsh   = "Cyflwynwch eich Ffurflen Dreth i ddefnyddio’r gwasanaeth hwn"
        )
      case TaxRegime.Vat =>
        Message(
          english = "File your return to use this service",
          welsh   = "Cyflwynwch eich Ffurflen Dreth i ddefnyddio’r gwasanaeth hwn"
        )
      case TaxRegime.Sa =>
        Message(
          english = "File your Self Assessment tax return to use this service",
          welsh   = "Cyflwynwch eich Ffurflen Dreth Hunanasesiad er mwyn defnyddio’r gwasanaeth hwn"
        )
      case TaxRegime.Simp =>
        Message(
          english = "File your Simple Assessment tax return to use this service",
          welsh   = "Cyflwynwch eich Ffurflen Dreth Asesiad Syml er mwyn defnyddio’r gwasanaeth hwn"
        )
    }

    val `If you have recently filed your return, your account can take up to 3 days to update. Try again after 3 days.`: Message = Message(
      english = "If you have recently filed your return, your account can take up to 3 days to update. Try again after 3 days.",
      welsh   = "Os ydych wedi cyflwyno’ch Ffurflen Dreth yn ddiweddar, gall gymryd hyd at 3 diwrnod i ddiweddaru’ch cyfrif. Rhowch gynnig arall arni ar ôl 3 diwrnod."
    )

    def `You must file your tax return before you can set up an Employers’ PAYE payment plan online`(fileReturnUrl: String): Message = Message(
      english = s"""You must <a class="govuk-link" href="$fileReturnUrl">file your tax return</a> before you can set up an Employers’ PAYE payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a class="govuk-link" href="$fileReturnUrl">gyflwyno’ch Ffurflen Dreth</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer TWE y Cyflogwr."""
    )

    def `You must file your tax return before you can set up a VAT payment plan online`(fileReturnUrl: String): Message = Message(
      english = s"""You must <a class="govuk-link" href="$fileReturnUrl">file your tax return</a> before you can set up a VAT payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a class="govuk-link" href="$fileReturnUrl">gyflwyno’ch Ffurflen Dreth</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer TAW."""
    )

    def `You must file your tax return before you can set up a Self Assessment payment plan online`(fileReturnUrl: String): Message = Message(
      english = s"""You must <a class="govuk-link" href="$fileReturnUrl">file your tax return</a> before you can set up a Self Assessment payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a class="govuk-link" href="$fileReturnUrl">gyflwyno’ch Ffurflen Dreth</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer Hunanasesiad ar-lein."""
    )

    def `You must file your tax return before you can set up a Simple Assessment payment plan online`(fileReturnUrl: String): Message = Message(
      english = s"""You must <a class="govuk-link" href="$fileReturnUrl">file your tax return</a> before you can set up a Simple Assessment payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a class="govuk-link" href="$fileReturnUrl">gyflwyno’ch Ffurflen Dreth</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer Asesiad Syml ar-lein."""
    )

    val `Call us on 0300 123 1813 as you may be able to set up a plan over the phone`: Message = Message(
      english = "Call us on <strong>0300 123 1813</strong> as you may be able to set up a plan over the phone.",
      welsh   = "Ffoniwch ni ar <strong>0300 200 1900</strong> oherwydd mae’n bosibl y gallwch drefnu cynllun dros y ffôn."
    )

    val `Call us on 0300 322 7835 as you may be able to set up a plan over the phone`: Message = Message(
      english = "Call us on <strong>0300 322 7835</strong> as you may be able to set up a plan over the phone.",
      welsh   = "Ffoniwch ni ar <strong>0300 200 1900</strong> oherwydd mae’n bosibl y gallwch drefnu cynllun dros y ffôn."
    )

    val `Call us on 0300 123 1813 if you need to speak to an adviser.`: Message = Message(
      english = "Call us on <strong>0300 123 1813</strong> if you need to speak to an adviser.",
      welsh   = "Ffoniwch ni ar <strong>0300 200 1900</strong> os oes angen i chi siarad ag ymgynghorydd."
    )

    val `You cannot set up an Employers' PAYE payment plan online...not overdue`: Message = Message(
      english = "You cannot set up an Employers’ PAYE payment plan online because your bill is not overdue.",
      welsh   = "Ni allwch drefnu cynllun talu ar gyfer TWE y Cyflogwr ar-lein oherwydd nad yw’ch bil yn hwyr."
    )

    val `You cannot set up a VAT payment plan online...not overdue`: Message = Message(
      english = "You cannot set up a VAT payment plan online because your bill is not overdue.",
      welsh   = "Ni allwch drefnu cynllun talu TAW ar-lein oherwydd nad yw’ch bil yn hwyr."
    )

    val `You may be able to set up a payment plan once the deadline...`: Message = Message(
      english = "You may be able to set up a payment plan once the deadline has passed to pay your bill.",
      welsh   = "Efallai y byddwch yn gallu trefnu cynllun talu unwaith y bydd y dyddiad cau wedi mynd heibio i dalu’ch bil."
    )

    val `You have chosen not to set up an Employers' PAYE payment plan online.`: Message = Message(
      english = "You have chosen not to set up an Employers’ PAYE payment plan online.",
      welsh   = "Rydych wedi dewis peidio â threfnu cynllun talu TWE y Cyflogwr ar-lein."
    )

    val `You have chosen not to set up a VAT payment plan online.`: Message = Message(
      english = "You have chosen not to set up a VAT payment plan online.",
      welsh   = "Rydych wedi dewis peidio â threfnu cynllun talu TAW ar-lein."
    )

  }

  object DualChargeWarning {

    val `You already have a Direct Debit`: Message = Message(
      english = "You already have a Direct Debit",
      welsh   = "Mae eisoes gennych drefniant Debyd Uniongyrchol"
    )

    def `You already have a Direct Debit set up for...`(taxRegime: TaxRegime): Message = taxRegime match {
      case TaxRegime.Epaye => Message(
        english = "You already have a Direct Debit set up for Employers’ PAYE.",
        welsh   = "Mae eisoes gennych drefniant Debyd Uniongyrchol er mwyn talu TWE y Cyflogwr."
      )
      case TaxRegime.Vat => Message(
        english = "You already have a Direct Debit set up for VAT.",
        welsh   = "Mae eisoes gennych drefniant Debyd Uniongyrchol er mwyn talu TAW."
      )
      case TaxRegime.Sa   => throw new NotImplementedError("ddInProgress flag not relevant to SA charges")
      case TaxRegime.Simp => throw new NotImplementedError("ddInProgress flag Not relevant to SIMP charges")
    }

    def `If you set up a payment plan, the following charge.. could be collected twice.`(chargesInPlural: Boolean): Message =
      if (chargesInPlural) {
        Message(
          english = "If you set up a payment plan, the following charges could be collected twice.",
          welsh   = "Os ydych yn trefnu cynllun talu, mae’n bosibl y gall y taliadau hyn gael eu casglu ddwywaith."
        )
      } else {
        Message(
          english = "If you set up a payment plan, the following charge could be collected twice.",
          welsh   = "Os ydych yn trefnu cynllun talu, mae’n bosibl y gall y taliad hwn gael ei gasglu ddwywaith."
        )
      }

    val `Contact your bank to discuss your payment options before setting up a payment plan.`: Message = Message(
      english = "Contact your bank to discuss your payment options before setting up a payment plan.",
      welsh   = "Dylech gysylltu â’ch banc i drafod eich opsiynau talu cyn i chi drefnu cynllun talu."
    )

    val `If you select continue you understand that you may be charged twice if you do not contact your bank.`: Message = Message(
      english = "If you select ‘continue’ you understand that you may be charged twice if you do not contact your bank.",
      welsh   = "Os dewiswch yr opsiwn i fynd yn eich blaen cyn cysylltu â’ch banc, rydych yn deall ei bod yn bosibl y gall taliadau gael eu casglu ddwywaith."
    )

    def `I do not want to set up a payment plan`(link: String): Message = Message(
      english = s"""<a class="govuk-link" id="kickout" href="$link">I do not want to set up a payment plan</a>""",
      welsh   = s"""<a class="govuk-link" id="kickout" href="$link">Nid wyf am drefnu cynllun talu</a>"""
    )
  }

  //OPS-12345 awaiting real data
  def `No NINO has been found`: Message = Message(
    english = "No nino has been found"
  )

  object EnrolmentMissing {

    val `Enrol for PAYE Online to use this service`: Message = Message(
      english = "Enrol for PAYE Online to use this service",
      welsh   = "Ymrestru ar gyfer TWE Ar-lein er mwyn defnyddio’r gwasanaeth hwn"
    )

    def `You must enrol for PAYE Online before you can set up an Employers’ PAYE payment plan.`(payeLink: String): Message = Message(
      english = s"""You must <a href="$payeLink" class="govuk-link">enrol for PAYE Online</a> before you can set up an Employers’ PAYE payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a href="$payeLink" class="govuk-link">ymrestru ar gyfer TWE Ar-lein</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer TWE y Cyflogwr."""
    )

    val `Register for VAT online to use this service`: Message = Message(
      english = "Register for VAT online to use this service",
      welsh   = "Cofrestru ar gyfer TAW ar-lein er mwyn defnyddio’r gwasanaeth hwn"
    )

    def `You must register for VAT online before you can set up a VAT payment plan.`(vatLink: String): Message = Message(
      english = s"""You must <a href="$vatLink" class="govuk-link">register for VAT online</a> before you can set up a VAT payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a href="$vatLink" class="govuk-link">gofrestru ar gyfer TAW ar-lein</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer TAW."""
    )

    val `Request access to Self Assessment to use this service`: Message = Message(
      english = "Request access to Self Assessment to use this service",
      welsh   = "Gwneud cais i gael mynediad at eich cyfrif Hunanasesiad er mwyn defnyddio’r gwasanaeth hwn"
    )

    def `You must request access to Self Assessment before you can set up a Self Assessment payment plan.`(saLink: String): Message = Message(
      english = s"""You must <a href="$saLink" class="govuk-link">request access to Self Assessment</a> before you can set up a Self Assessment payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a href="$saLink" class="govuk-link">wneud cais i gael mynediad at eich cyfrif Hunanasesiad</a> cyn i chi allu trefnu cynllun talu ar-lein ar gyfer Hunanasesiad."""
    )

    val `If you already have access, sign in with the Government Gateway user ID that has your enrolment.`: Message = Message(
      english = "If you already have access, sign in with the Government Gateway user ID that has your enrolment.",
      welsh   = "Os oes gennych fynediad yn barod, mae’n rhaid i chi fewngofnodi gan ddefnyddio’r Dynodydd Defnyddiwr (ID) Porth y Llywodraeth sydd â’ch cofrestriad."
    )

    val `Sign up for Making Tax Digital for Income Tax to use this service`: Message = Message(
      english = "Sign up for Making Tax Digital for Income Tax to use this service",
      welsh   = "Cofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm er mwyn defnyddio’r gwasanaeth hwn"
    )

    val `If you’ve already signed up, sign in with the Government Gateway user ID that has your enrolment.`: Message = Message(
      english = "If you’ve already signed up, sign in with the Government Gateway user ID that has your enrolment.",
      welsh   = "Os ydych chi eisoes wedi cofrestru, mae’n rhaid i chi fewngofnodi gan ddefnyddio’r Dynodydd Defnyddiwr (ID) Porth y Llywodraeth sydd â’ch cofrestriad."
    )

    def `You must sign up for Making Tax Digital for Income Tax before you can set up a Self Assessment payment plan online.`(mtdLink: String): Message = Message(
      english = s"""You must <a href="$mtdLink" class="govuk-link">sign up for Making Tax Digital for Income Tax</a> before you can set up a Self Assessment payment plan online.""",
      welsh   = s"""Mae’n rhaid i chi <a href="$mtdLink" class="govuk-link">gofrestru ar gyfer y cynllun Troi Treth yn Ddigidol ar gyfer Treth Incwm</a> cyn i chi allu trefnu cynllun talu ar gyfer Hunanasesiad ar-lein."""
    )
  }

  object WhyCannotPayInFull {

    val `Why are you unable to pay in full?`: Message = Message(
      english = "Why are you unable to pay in full?",
      welsh   = "Pam nad oes modd i chi dalu’ch llawn?"
    )

    val `Your answers help us plan services in the future...`: Message = Message(
      english = "Your answers help us plan services in the future. Select all that apply.",
      welsh   = "Bydd eich atebion yn ein helpu i gynllunio gwasanaethau yn y dyfodol. Dewiswch bob un sy’n berthnasol."
    )

    val or: Message = Message(
      english = "or",
      welsh   = "neu"
    )

    val `Select all that apply or 'none of these'`: Message = Message(
      english = "Select all that apply or ‘None of these’",
      welsh   = "Dewiswch bob un sy’n berthnasol neu ‘Dim un o’r rhain’"
    )

    def checkboxMessageWithHint(cannotPayReason: CannotPayReason): (Message, Option[Message]) = cannotPayReason match {
      case CannotPayReason.UnexpectedReductionOfIncome =>
        Message(
          english = "Unexpected reduction of income",
          welsh   = "Gostyngiad annisgwyl mewn incwm"
        ) -> Some(
          Message(
            english = "For example, lost or reduced business or unemployment.",
            welsh   = "Er enghraifft, colli neu leihau busnes neu ddiweithdra."
          )
        )
      case CannotPayReason.UnexpectedIncreaseInSpending =>
        Message(
          english = "Unexpected increase in spending",
          welsh   = "Cynnydd annisgwyl mewn gwariant"
        ) -> Some(
          Message(
            english = "For example, unexpected repairs following theft or damage to premises.",
            welsh   = "Er enghraifft, atgyweiriadau annisgwyl yn dilyn lladrad neu niwed i eiddo."
          )
        )
      case CannotPayReason.LostOrReducedAbilityToEarnOrTrade =>
        Message(
          english = "Lost or reduced ability to earn or trade",
          welsh   = "Colli neu leihau gallu i ennill neu fasnachu"
        ) -> None
      case CannotPayReason.NationalOrLocalDisaster =>
        Message(
          english = "National or local disaster",
          welsh   = "Trychineb lleol neu genedlaethol"
        ) -> Some(
          Message(
            english = "For example, COVID-19, extreme weather conditions.",
            welsh   = "Er enghraifft COVID 19, amgylchiadau tywydd garw."
          )
        )
      case CannotPayReason.ChangeToPersonalCircumstances =>
        Message(
          english = "Change to personal circumstances",
          welsh   = "Newid yn eich amgylchiadau personol"
        ) -> Some(
          Message(
            english = "For example, ill health or bereavement.",
            welsh   = "Er enghraifft, salwch neu brofedigaeth."
          )
        )
      case CannotPayReason.NoMoneySetAside =>
        Message(
          english = "No money set aside to pay",
          welsh   = "Dim arian wedi’i neilltuo i dalu"
        ) -> None
      case CannotPayReason.WaitingForRefund =>
        Message(
          english = "Waiting for a refund from HMRC",
          welsh   = "Aros am ad-daliad gan CThEF"
        ) -> None
      case CannotPayReason.Other =>
        Message(
          english = "None of these",
          welsh   = "Dim un o’r rhain"
        ) -> None
    }

  }

  object UpfrontPayment {

    val `Upfront payment`: Message = Message(
      english = "Upfront payment",
      welsh   = "Taliad ymlaen llaw"
    )

    val `If you pay some of your bill upfront, you'll`: Message = Message(
      english = "If you pay some of your bill upfront, you’ll:",
      welsh   = "Os byddwch yn talu rhywfaint o’ch bil ymlaen llaw, bydd y canlynol yn wir:"
    )

    val `have a shorter payment plan`: Message = Message(
      english = "have a shorter payment plan",
      welsh   = "bydd gennych gynllun talu byrrach"
    )

    val `pay less interest`: Message = Message(
      english = "pay less interest",
      welsh   = "byddwch yn talu llai o log"
    )

    val `An upfront payment is separate to any recent payments you've made...`: Message = Message(
      english = "An upfront payment is separate to any recent payments you’ve made. We’ll take it from your bank account within 6 working days.",
      welsh   = "Mae taliad ymlaen llaw ar wahân i unrhyw daliadau diweddar yr ydych wedi eu gwneud. Byddwn yn ei gymryd o’ch cyfrif banc cyn pen 6 diwrnod gwaith."
    )

    val `Can you make an upfront payment?`: Message = Message(
      english = "Can you make an upfront payment?",
      welsh   = "A allwch wneud taliad ymlaen llaw?"
    )

    val `...whether you can make an upfront payment`: Message = Message(
      english = "whether you can make an upfront payment",
      welsh   = "p’un a allwch wneud taliad ymlaen llaw"
    )

    val `Select yes if you can make an upfront payment`: Message = Message(
      english = "Select yes if you can make an upfront payment",
      welsh   = "Dewiswch ‘Iawn’ os gallwch wneud taliad ymlaen llaw"
    )

  }

  object CallHmrc {
    val `If you do not think you can set up a plan online, call HMRC and find out if you can set up a plan over the phone.`: Message = Message(
      english = "If you do not think you can set up a plan online, call HMRC and find out if you can set up a plan over the phone.",
      welsh   = "Os nad ydych yn credu y gallwch sefydlu cynllun ar-lein, ffoniwch CThEF a dysgwch a allwch sefydlu cynllun dros y ffôn."
    )

    val `Telephone: ...`: Message = Message(
      english = "Telephone: <strong>0300 123 1813</strong>",
      welsh   = "Ffôn: <strong>0300 123 1813</strong>"
    )

    val `Outside UK: ...`: Message = Message(
      english = "Outside UK: <strong>+44 2890 538 192</strong>",
      welsh   = "O’r tu allan i’r DU: <strong>+44 2890 538 192</strong>"
    )

    val `Our phone line opening hours are:`: Message = Message(
      english = "Our phone line opening hours are:",
      welsh   = "Oriau agor ein llinell ffôn yw:"
    )

    val `Monday to Friday: ...`: Message = Message(
      english = "Monday to Friday: 8am to 6pm",
      welsh   = "Dydd Llun i ddydd Gwener: 8:30 i 17:00"
    )

    val `Closed weekends and bank holidays.`: Message = Message(
      english = "Closed weekends and bank holidays.",
      welsh   = "Ar gau ar benwythnosau a gwyliau banc."
    )

    val `Text service`: Message = Message(
      english = "Text service",
      welsh   = "Gwasanaeth Text Relay"
    )

    def `Use Relay UK if you cannot hear or speak on the telephone...`(linkUrl: String): Message = Message(
      english = s"""Use Relay UK if you cannot hear or speak on the telephone, dial <strong>18001</strong> then <strong>0345 300 3900</strong>. Find out more on the <a class="govuk-link" href="$linkUrl" rel="noreferrer noopener" target="_blank">Relay UK website (opens in new tab)</a>.""",
      welsh   = s"""Defnyddiwch wasanaeth Text Relay UK os na allwch glywed na siarad dros y ffôn. Deialwch <strong>18001</strong> ac yna <strong>0345 300 3900</strong>. Dysgwch ragor am hyn ar <a href="$linkUrl" rel="noreferrer noopener" target="_blank">wefan Text Relay UK (yn agor tab newydd)</a>."""
    )

    def `If a health condition or personal circumstances make it difficult to contact us...`: Message = Message(
      english = s"""If a health condition or personal circumstances make it difficult to contact us""",
      welsh   = s"""Os yw cyflwr iechyd neu amgylchiadau personol yn ei gwneud hi’n anodd i chi gysylltu â ni"""
    )

    def `Our guidance Get help from HMRC...`(link: String): Message = Message(
      english = s"""Our guidance <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">Get help from HMRC if you need extra support (opens in new tab)</a> explains how we can support you.""",
      welsh   = s"""Bydd ein harweiniad ynghylch <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">‘Cael help gan CThEF os oes angen cymorth ychwanegol arnoch’ (yn agor tab newydd)</a> yn esbonio sut y gallwn eich helpu."""
    )
  }

  object Epaye {

    val `Set up an Employers' PAYE payment plan`: Message = Message(
      english = "Set up an Employers’ PAYE payment plan",
      welsh   = "Trefnu cynllun talu ar gyfer TWE cyflogwyr"
    )

    val `Use this service to set up a payment plan..`: Message = Message(
      english = "Use this service to set up a payment plan for your outstanding employers’ PAYE bill. Payments are taken by Direct Debit and include interest charged at the Bank of England base rate plus 2.5% per year.",
      welsh   = "Defnyddiwch y gwasanaeth hwn i sefydlu cynllun talu ar gyfer eich bil TWE y cyflogwyr sy’n weddill. Mae taliadau’n cael eu cymryd drwy Ddebyd Uniongyrchol ac maent yn cynnwys llog a godir ar gyfradd sylfaenol Banc Lloegr ynghyd â 2.5% y flwyddyn."
    )

    val `You must be able to authorise a Direct Debit...`: Message = Message(
      english = "You must be able to authorise a Direct Debit without a signature from any other account holders and be named on the UK bank account you’ll use to pay.",
      welsh   = "Mae’n rhaid i chi allu awdurdodi Debyd Uniongyrchol heb lofnod gan unrhyw ddeiliaid cyfrif eraill a chael eich enwi ar gyfrif banc y DU y byddwch yn ei ddefnyddio i’w dalu."
    )

    val `You’ll need to stay up to date with your payments or we could ask you to pay in full.`: Message = Message(
      english = "You’ll need to stay up to date with your payments or we could ask you to pay in full.",
      welsh   = "Bydd angen i chi gael yr wybodaeth ddiweddaraf am eich taliadau neu gallem ofyn i chi dalu’n llawn."
    )

    val `To set up a plan, your company or partnership must:`: Message = Message(
      english = "To set up a plan, your company or partnership must:",
      welsh   = "I sefydlu cynllun, mae’n rhaid i’r canlynol fod yn wir:"
    )

    val `have missed the deadline to pay a PAYE bill`: Message = Message(
      english = "have missed the deadline to pay a PAYE bill",
      welsh   = "mae’ch cwmni neu’ch partneriaeth wedi methu’r dyddiad cau i dalu bil TWE"
    )

    def `owe ... or less`(maxAmountOfDebt: AmountInPence): Message = Message(
      english = s"owe ${maxAmountOfDebt.gdsFormatInPounds} or less",
      welsh   = s"mae arnoch ${maxAmountOfDebt.gdsFormatInPounds} neu lai"
    )

    val `have debts that are 5 years old or less`: Message = Message(
      english = "have debts that are 5 years old or less",
      welsh   = "mae gan eich cwmni neu’ch partneriaeth ddyledion sy’n 5 blynedd oed neu lai"
    )

    val `have no other payment plans or debts with HMRC`: Message = Message(
      english = "have no other payment plans or debts with HMRC",
      welsh   = "nid oes gan eich cwmni neu’ch partneriaeth unrhyw gynlluniau talu na dyledion eraill gyda CThEF"
    )

    val `have no outstanding employers’ PAYE submissions or Construction Industry Scheme returns`: Message = Message(
      english = "have no outstanding employers’ PAYE submissions or Construction Industry Scheme returns",
      welsh   = "nid oes gan eich cwmni neu’ch partneriaeth unrhyw gyflwyniadau TWE y cyflogwr sy’n weddill neu Ffurflenni Treth Cynllun y Diwydiant Adeiladu"
    )

  }

  object Vat {
    val `Set up a VAT payment plan`: Message = Message(
      english = "Set up a VAT payment plan",
      welsh   = "Trefnu cynllun talu ar gyfer TAW"
    )

    val `Use this service to set up a payment plan..`: Message = Message(
      english = "Use this service to set up a payment plan for your outstanding VAT bill. Payments are taken by Direct Debit and include interest charged at the Bank of England base rate plus 2.5% per year.",
      welsh   = "Defnyddiwch y gwasanaeth hwn i drefnu cynllun talu ar gyfer eich bil TAW sy’n weddill. Mae taliadau’n cael eu cymryd drwy Ddebyd Uniongyrchol ac maent yn cynnwys llog a godir ar gyfradd sylfaenol Banc Lloegr ynghyd â 2.5% y flwyddyn."
    )

    val `You must be able to authorise a Direct Debit...`: Message = Message(
      english = "You must be able to authorise a Direct Debit without a signature from any other account holders and be named on the UK bank account you’ll use to pay.",
      welsh   = "Mae’n rhaid i chi allu awdurdodi Debyd Uniongyrchol heb lofnod gan unrhyw ddeiliaid cyfrif eraill a chael eich enwi ar gyfrif banc y DU y byddwch yn ei ddefnyddio i’w dalu."
    )

    val `You’ll need to stay up to date with your payments or we could ask you to pay in full.`: Message = Message(
      english = "You’ll need to stay up to date with your payments or we could ask you to pay in full.",
      welsh   = "Bydd angen i chi gael yr wybodaeth ddiweddaraf am eich taliadau neu gallem ofyn i chi dalu’n llawn."
    )

    val `To set up a plan, your company or partnership must:`: Message = Message(
      english = "To set up a plan, your company or partnership must:",
      welsh   = "I sefydlu cynllun, mae’n rhaid i’r canlynol fod yn wir:"
    )

    val `have missed the deadline to pay a VAT bill`: Message = Message(
      english = "have missed the deadline to pay a VAT bill",
      welsh   = "rydych wedi methu’r dyddiad cau i dalu bil TAW"
    )

    def `owe ... or less`(maxAmountOfDebt: AmountInPence): Message = Message(
      english = s"owe ${maxAmountOfDebt.gdsFormatInPounds} or less",
      welsh   = s"mae arnoch ${maxAmountOfDebt.gdsFormatInPounds} neu lai"
    )

    val `have a debt for an accounting period that started in 2023 or later`: Message = Message(
      english = "have a debt for an accounting period that started in 2023 or later",
      welsh   = "mae gennych ddyled am gyfnod cyfrifyddu a ddechreuodd yn 2023 neu’n hwyrach"
    )

    val `have no other payment plans or debts with HMRC`: Message = Message(
      english = "have no other payment plans or debts with HMRC",
      welsh   = "nid oes gan eich cwmni neu’ch partneriaeth unrhyw gynlluniau talu na dyledion eraill gyda CThEF"
    )

    val `have filed your tax returns`: Message = Message(
      english = "have filed your tax returns",
      welsh   = "rydych wedi cyflwyno’ch Ffurflenni TAW"
    )

    val `If you have a Customer Compliance Manager...`: Message = Message(
      english = "If you have a Customer Compliance Manager, discuss your needs with them before using this service.",
      welsh   = "Os oes gennych chi reolwr cydymffurfiad cwsmeriaid, trafodwch eich anghenion ag ef cyn defnyddio’r gwasanaeth hwn."
    )

    val `You cannot use this service if you are:`: Message = Message(
      english = "You cannot use this service if you are:",
      welsh   = "Ni allwch ddefnyddio’r gwasanaeth hwn os ydych yn un o’r canlynol:"
    )

    val `a cash accounting customer`: Message = Message(
      english = "a cash accounting customer",
      welsh   = "cwsmer cyfrifyddu arian parod"
    )

    val `an annual accounting scheme member`: Message = Message(
      english = "an annual accounting scheme member",
      welsh   = "aelod o’r cynllun cyfrifyddu blynyddol"
    )

    val `a payment on account customer`: Message = Message(
      english = "a payment on account customer",
      welsh   = "cwsmer taliad ar gyfrif"
    )

  }

  object Sa {

    val `Set up a Self Assessment payment plan`: Message = Message(
      english = "Set up a Self Assessment payment plan",
      welsh   = "Sefydlu cynllun talu ar gyfer Hunanasesiad"
    )

    val `A payment plan allows you to pay your tax charges in instalments over a period of time.`: Message = Message(
      english = "A payment plan allows you to pay your tax charges in instalments over a period of time.",
      welsh   = "Mae cynllun talu yn eich galluogi i dalu’ch taliadau treth fesul rhandaliad dros gyfnod o amser."
    )

    val `Your plan covers the tax you owe...`: Message = Message(
      english = "Your plan covers the tax you owe and, if applicable, the 2 advance payments towards your tax bill. It also covers any penalties or charges against your account. You’ll have to pay interest on the amount you pay late.",
      welsh   = "Mae eich cynllun yn cwmpasu’r dreth sydd arnoch ac, os yw’n berthnasol, y ddau daliad ymlaen llaw tuag at eich bil treth. Mae’r cynllun hefyd yn cwmpasu unrhyw gosbau neu daliadau yn erbyn eich cyfrif. Bydd yn rhaid i chi dalu llog ar y swm a dalwch yn hwyr."
    )

    val `To be eligible to set up an online payment plan you need to:`: Message = Message(
      english = "To be eligible to set up an online payment plan you need to:",
      welsh   = "I fod yn gymwys i sefydlu cynllun talu ar-lein, mae’n rhaid i’r canlynol fod yn wir amdanoch:"
    )

    val `ensure your tax returns are up to date`: Message = Message(
      english = "ensure your tax returns are up to date",
      welsh   = "mae’n rhaid i chi sicrhau bod eich Ffurflenni Treth yn gyfredol"
    )

    def `owe ... or less`(maxAmountOfDebt: AmountInPence): Message = Message(
      english = s"owe ${maxAmountOfDebt.gdsFormatInPounds} or less",
      welsh   = s"mae arnoch ${maxAmountOfDebt.gdsFormatInPounds} neu lai"
    )

    val `have no other tax debts`: Message = Message(
      english = "have no other tax debts",
      welsh   = "nid oes gennych unrhyw ddyledion treth eraill"
    )

    val `have no other HMRC payment plans set up`: Message = Message(
      english = "have no other HMRC payment plans set up",
      welsh   = "nid ydych wedi sefydlu cynlluniau talu eraill gyda CThEF"
    )

    def `You can use this service within ... days of the payment deadline.`(maxAgeOfDebtInDays: Int): Message = Message(
      english = s"You can use this service within ${maxAgeOfDebtInDays.toString} days of the payment deadline.",
      welsh   = s"Gallwch ddefnyddio’r gwasanaeth hwn cyn pen ${maxAgeOfDebtInDays.toString} diwrnod i’r dyddiad cau ar gyfer talu."
    )

    val `Before you start`: Message = Message(
      english = "Before you start",
      welsh   = "Cyn i chi ddechrau"
    )

    val `HMRC intend this as a one-off payment plan...`: Message = Message(
      english = "HMRC intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the entire outstanding amount.",
      welsh   = "Bwriad CThEF yw y bydd hwn yn gynllun talu un-tro er mwyn rhoi cymorth ychwanegol i chi. Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Os na fyddwch, mae’n bosibl y bydd CThEF yn gofyn i chi dalu’r swm cyfan sy’n weddill."
    )

    val `To set up the payment plan, you’ll need to know your monthly income and spending, and any savings or investments.`: Message = Message(
      english = "To set up the payment plan, you’ll need to know your monthly income and spending, and any savings or investments.",
      welsh   = "Er mwyn sefydlu’r cynllun talu, bydd angen i chi wybod beth yw’ch incwm a’ch gwariant misol, ac unrhyw gynilion neu fuddsoddiadau."
    )

  }

  object Simp {

    val `Set up a Simple Assessment payment plan`: Message = Message(
      english = "Set up a Simple Assessment payment plan",
      welsh   = "Trefnu cynllun talu Asesiad Syml"
    )

    val `You can use this service to pay overdue payments in instalments.`: Message = Message(
      english = "You can use this service to pay overdue payments in instalments.",
      welsh   = "Gallwch ddefnyddio’r gwasanaeth hwn i dalu taliadau hwyr fesul rhandaliad."
    )

    val `You are eligible to set up an online payment plan if:`: Message = Message(
      english = "You are eligible to set up an online payment plan if:",
      welsh   = "Rydych chi’n gymwys i drefnu cynllun talu ar-lein os yw’r canlynol yn wir:"
    )

    def `you owe ... or less`(maxAmountOfDebt: AmountInPence): Message = Message(
      english = s"you owe ${maxAmountOfDebt.gdsFormatInPounds} or less",
      welsh   = s"mae arnoch ${maxAmountOfDebt.gdsFormatInPounds} neu lai",
    )

    val `you do not have any other debts with HMRC`: Message = Message(
      english = "you do not have any other debts with HMRC",
      welsh   = "does gennych chi ddim dyledion eraill gyda CThEF"
    )

    val `you do not have any payment plans with HMRC`: Message = Message(
      english = "you do not have any payment plans with HMRC",
      welsh   = "does gennych chi ddim cynlluniau talu gyda CThEF"
    )

    val `You can choose to pay:`: Message = Message(
      english = "You can choose to pay:",
      welsh   = "Gallwch ddewis talu:"
    )

    val `part of the payment upfront and part in monthly instalments`: Message = Message(
      english = "part of the payment upfront and part in monthly instalments",
      welsh   = "rhan o’r taliad ymlaen llaw a rhan ohono fesul rhandaliad"
    )

    val `monthly instalments only`: Message = Message(
      english = "monthly instalments only",
      welsh   = "fesul rhandaliad misol yn unig"
    )

    val `Before you start`: Message = Message(
      english = "Before you start",
      welsh   = "Cyn i chi ddechrau"
    )

    val `You must be:`: Message = Message(
      english = "You must be:",
      welsh   = "Mae’n rhaid i chi fod:"
    )

    val `a named account holder for the UK bank account you intend to use`: Message = Message(
      english = "a named account holder for the UK bank account you intend to use",
      welsh   = "wedi’ch enwi’n ddeiliad y cyfrif ar gyfer y cyfrif banc yn y DU rydych yn bwriadu ei ddefnyddio"
    )

    val `able to authorise a Direct Debit`: Message = Message(
      english = "able to authorise a Direct Debit",
      welsh   = "yn gallu awdurdodi Debyd Uniongyrchol"
    )

    val `You must keep up to date with your payments...`: Message = Message(
      english = "You must keep up to date with your payments. HMRC may ask you to pay the total outstanding amount if you do not. " +
        "HMRC intend this as a one off payment plan to give you extra support.",
      welsh   = "Mae’n rhaid i chi sicrhau eich bod chi’n gwneud eich taliadau mewn pryd. Mae’n bosibl y bydd CThEF yn gofyn i chi dalu’r cyfanswm sy’n ddyledus os na fyddwch chi’n gwneud eich taliadau mewn pryd. Bwriad CThEF yw y bydd hwn yn gynllun talu untro er mwyn rhoi cymorth ychwanegol i chi."
    )

  }

  object UpfrontPaymentAmount {

    val `How much can you pay upfront?`: Message = Message(
      english = "How much can you pay upfront?",
      welsh   = "Faint y gallwch ei dalu ymlaen llaw?"
    )

    def getError(key: String, max: AmountInPence, min: AmountInPence): Message = {
      lazy val outOfBoundsMessage = Message(
        english = s"Your upfront payment must be between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}",
        welsh   = s"Mae’n rhaid i’ch taliad ymlaen llaw fod rhwng ${min.gdsFormatInPounds} a ${max.gdsFormatInPounds}"
      )

      key match {
        case "error.required" =>
          Message(
            english = "Enter your upfront payment",
            welsh   = "Nodwch eich taliad ymlaen llaw"
          )

        case "error.pattern" =>
          Message(
            english = "How much you can pay upfront must be an amount of money",
            welsh   = "Mae’n rhaid i’r hyn y gallwch ei dalu ymlaen llaw fod yn swm o arian"
          )

        case "error.tooSmall" => outOfBoundsMessage

        case "error.tooLarge" => outOfBoundsMessage
      }
    }

    def `Enter an amount between`(min: AmountInPence, max: AmountInPence): Message = Message(
      english = s"Enter an amount between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}",
      welsh   = s"Nodwch swm sydd rhwng ${min.gdsFormatInPounds} a ${max.gdsFormatInPounds}"
    )

  }

  object UpfrontPaymentSummary {

    val `Payment summary`: Message = Message(
      english = "Payment summary",
      welsh   = "Crynodeb o’r taliadau"
    )

    val `Change Payment`: Message = Message(
      english = "whether you can make an upfront payment",
      welsh   = "p’un a allwch wneud taliad ymlaen llaw"
    )

    val `Upfront payment`: Message = Message(
      english = "Upfront payment<br><span class=\"govuk-body-s\">Taken within 6 working days</span>",
      welsh   = "Taliad ymlaen llaw<br><span class=\"govuk-body-s\">I’w gymryd cyn pen 6 diwrnod gwaith</span>"
    )

    val `Upfront payment-visually-hidden-message`: Message = Message(
      english = "your upfront payment amount",
      welsh   = "swm eich taliad ymlaen llaw"
    )

    val `Remaining amount to pay`: Message = Message(
      english = "Remaining amount to pay",
      welsh   = "Swm sy’n weddill i’w dalu"
    )

    val `(interest may be added to this amount)`: Message = Message(
      english = """<span class="govuk-body-s">(interest may be added to this amount)</span>""",
      welsh   = """<span class="govuk-body-s">(bydd llog yn cael ei ychwanegu at y swm hwn)</span>"""
    )

  }

  object CanPayWithinSixMonths {

    val `Paying within 6 months`: Message = Message(
      english = "Paying within 6 months",
      welsh   = "Talu cyn pen 6 mis"
    )

    val `If you can afford to pay within 6 months...`: Message = Message(
      english = "If you can afford to pay within 6 months, you’ll pay less interest than on a longer plan.",
      welsh   = "Os gallwch fforddio talu cyn pen 6 mis, byddwch yn talu llai o log nag ar gynllun hirach."
    )

    val `Remaining amount to pay`: Message = Message(
      english = "Remaining amount to pay",
      welsh   = "Swm sy’n weddill i’w dalu"
    )

    val `Can you pay within 6 months?`: Message = Message(
      english = "Can you pay within 6 months?",
      welsh   = "A allwch dalu cyn pen 6 mis?"
    )

    val `Yes, I can pay within 6 months`: Message = Message(
      english = "Yes, I can pay within 6 months",
      welsh   = "Iawn, gallaf dalu cyn pen 6 mis"
    )

    val `No, I need a longer plan`: Message = Message(
      english = "No, I need a longer plan",
      welsh   = "Na, bydd angen cynllun hirach arnaf ar gyfer talu"
    )

    val `Select yes if you can pay within 6 months`: Message = Message(
      english = "Select yes if you can pay within 6 months",
      welsh   = "Dewiswch ‘Iawn’ os gallwch dalu cyn pen 6 mis"
    )

  }

  object MonthlyPaymentAmount {

    val `Monthly payments`: Message = Message(
      english = "Monthly payments",
      welsh   = "Taliadau misol"
    )

    val `How much can you afford to pay each month?`: Message = Message(
      english = "How much can you afford to pay each month?",
      welsh   = "Faint y gallwch fforddio ei dalu bob mis?"
    )

    def `The miminum payment you can make is ...`(min: AmountInPence): Message = Message(
      english = s"The minimum payment you can make is ${min.gdsFormatInPounds}.",
      welsh   = s"Yr isafswm y gallwch ei dalu yw ${min.gdsFormatInPounds}."
    )

    def getHint(max: AmountInPence, min: AmountInPence): Message = Message(
      english = s"Enter an amount between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}",
      welsh   = s"Nodwch swm sydd rhwng ${min.gdsFormatInPounds} a ${max.gdsFormatInPounds}"
    )

    def getError(key: String, max: AmountInPence, min: AmountInPence): Message = {
      lazy val outOfBoundsMessage = Message(
        english = s"How much you can afford to pay each month must be between ${min.gdsFormatInPounds} and ${max.gdsFormatInPounds}",
        welsh   = s"Mae’n rhaid i faint y gallwch fforddio ei dalu bob mis fod rhwng ${min.gdsFormatInPounds} a ${max.gdsFormatInPounds}"
      )

      key match {
        case "error.required" =>
          Message(
            english = "Enter how much you can afford to pay each month",
            welsh   = "Nodwch faint y gallwch fforddio ei dalu bob mis"
          )
        case "error.pattern" =>
          Message(
            english = "How much you can afford to pay each month must be an amount of money",
            welsh   = "Mae’n rhaid i’r hyn y gallwch fforddio ei dalu bob mis fod yn swm o arian"
          )

        case "error.tooSmall" => outOfBoundsMessage
        case "error.tooLarge" => outOfBoundsMessage
      }
    }

    val `I cannot afford the minimum payment`: Message = Message(
      english = "I cannot afford the minimum payment",
      welsh   = "Nid wyf yn gallu fforddio’r taliad isaf"
    )

    val `You may still be able to set up a payment plan...`: Message = Message(
      english = "You may still be able to set up a payment plan over the phone. Call us on <strong>0300 123 1813</strong> to speak to an adviser.",
      welsh   = "Mae’n bosibl y byddwch chi’n dal i allu trefnu cynllun talu dros y ffôn. Ffoniwch ni ar <strong>0300 200 1900</strong> i siarad ag ymgynghorydd."
    )

  }

  object PaymentDay {

    val `Which day do you want to pay each month?`: Message = Message(
      english = "Which day do you want to pay each month?",
      welsh   = "Ar ba ddiwrnod a ydych eisiau talu bob mis?"
    )

    val `28th or next working day`: Message = Message(
      english = "28th or next working day",
      welsh   = "yr 28ain neu’r diwrnod gwaith nesaf"
    )

    val `A different day`: Message = Message(
      english = "A different day",
      welsh   = "Diwrnod gwahanol"
    )

    def getError(key: String): Message = key match {
      case "PaymentDay.error.required" =>
        Message(
          english = "Select which day you want to pay each month",
          welsh   = "Dewiswch ar ba ddiwrnod a ydych eisiau talu bob mis"
        )

      case "DifferentDay.error.required" =>
        Message(
          english = "Enter the day you want to pay each month",
          welsh   = "Nodwch y diwrnod rydych eisiau talu arno bob mis"
        )

      case "DifferentDay.error.outOfRange" =>
        Message(
          english = "The day you want to pay must be between 1 and 28",
          welsh   = "Rhaid i’r diwrnod rydych eisiau talu arno bob mis fod rhwng 1 a 28"
        )

      case "DifferentDay.error.invalid" =>
        Message(
          english = "The day you want to pay must be a number",
          welsh   = "Mae’n rhaid i’r diwrnod rydych eisiau talu arno fod yn rhif"
        )
    }

    val `Enter a day between 1 and 28`: Message = Message(
      english = "Enter a day between 1 and 28",
      welsh   = "Nodwch ddiwrnod rhwng 1 a 28"
    )

  }

  object Instalments {

    val `Select a payment plan`: Message = Message(
      english = "Select a payment plan",
      welsh   = "Dewiswch gynllun talu"
    )

    val `Based on what you can pay each month, you can now select a payment plan.`: Message = Message(
      english = "Based on what you can pay each month, you can now select a payment plan.",
      welsh   = "Yn seiliedig at yr hyn y gallwch ei dalu bob mis, gallwch nawr ddewis gynllun talu."
    )

    val `How we calculate interest`: Message = Message(
      english = "How we calculate interest",
      welsh   = "Sut rydym yn cyfrifo llog"
    )

    val `We charge interest on all overdue amounts`: Message = Message(
      english = "We charge interest on all overdue amounts.",
      welsh   = "Rydym yn codi llog ar bob swm sy’n hwyr."
    )

    val `We charge the Bank of England base rate plus 2.5% per year`: Message = Message(
      english = "We charge the <strong>Bank of England base rate plus 2.5%</strong> per year.",
      welsh   = "Rydym yn codi <strong>cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%</strong> y flwyddyn."
    )

    val `If interest rates change...`: Message = Message(
      english = "If the interest rate changes during your payment plan, you may need to settle any difference at the end. " +
        "We will contact you if this is the case.",
      welsh   = "Os bydd y gyfradd llog yn newid yn ystod eich cynllun talu, efallai bydd yn rhaid i chi setlo unrhyw wahaniaeth ar y diwedd. " +
        "Byddwn yn cysylltu â chi os yw hyn yn wir."
    )

    val `How many months do you want to pay over?`: Message = Message(
      english = "How many months do you want to pay over?",
      welsh   = "Dros sawl mis yr hoffech dalu?"
    )

    private def getInstalmentOptionOneMonth(amount: AmountInPence): Message = Message(
      english = s"1 month at ${amount.gdsFormatInPounds}",
      welsh   = s"1 mis ar ${amount.gdsFormatInPounds}"
    )

    private def getInstalmentOptionMoreThanOneMonth(numberOfMonths: Int, amount: AmountInPence): Message = Message(
      english = s"${numberOfMonths.toString} months at ${amount.gdsFormatInPounds}",
      welsh   = s"${numberOfMonths.toString} mis ar ${amount.gdsFormatInPounds}"
    )

    def getInstalmentOption(numberOfMonths: Int, amount: AmountInPence): Message =
      if (numberOfMonths > 1) getInstalmentOptionMoreThanOneMonth(numberOfMonths, amount)
      else getInstalmentOptionOneMonth(amount)

    def `Estimated total interest of x`(interest: AmountInPence): Message = Message(
      english = s"Estimated total interest of ${interest.gdsFormatInPounds}",
      welsh   = s"Cyfanswm llog amcangyfrifedig o ${interest.gdsFormatInPounds}"
    )

    def getError(key: String): Message = key match {
      case "error.required" =>
        Message(
          english = "Select how many months you want to pay over",
          welsh   = "Dewiswch ateb ar gyfer dros sawl mis yr hoffech dalu"
        )
      case _ =>
        Message(
          english = "Select how many months you want to pay over",
          welsh   = "Dewiswch ateb ar gyfer dros sawl mis yr hoffech dalu"
        )
    }

  }

  object PaymentSchedule {

    val `Check your payment plan`: Message = Message(
      english = "Check your payment plan",
      welsh   = "Gwirio’ch cynllun talu"
    )

    val `why you are unable to pay in full`: Message = Message(
      english = "why you are unable to pay in full",
      welsh   = "pam nad oes modd i chi dalu’n llawn"
    )

    val `whether you can pay within 6 months`: Message = Message(
      english = "whether you can pay within 6 months",
      welsh   = "a ydych yn gallu talu cyn pen 6 mis"
    )

    val `how much you can afford to pay each month`: Message = Message(
      english = "how much you can afford to pay each month",
      welsh   = "faint y gallwch fforddio ei dalu bob mis"
    )

    val `Monthly payments`: Message = Message(
      english = "Monthly payments",
      welsh   = "Taliadau misol"
    )

    val `Payments collected on`: Message = Message(
      english = "Payments collected on",
      welsh   = "Mae taliadau’n cael eu casglu ar"
    )

    val `or next working day`: Message = Message(
      english = "or next working day",
      welsh   = "neu’r diwrnod gwaith nesaf"
    )

    val `payment day`: Message = Message(
      english = "which day your payments will be collected on",
      welsh   = "ar ba ddiwrnod y bydd eich taliadau’n cael eu casglu"
    )

    val `Payment plan instalments`: Message = Message(
      english = "Payment plan instalments",
      welsh   = "Cynllun talu ar ffurf rhandaliadau"
    )

    val `Payment plan duration`: Message = Message(
      english = "Payment plan duration",
      welsh   = "Hyd y cynllun talu"
    )

    def `... months`(i: Int): Message = Message(
      english = if (i === 1) "1 month" else s"${i.toString} months",
      welsh   = s"${i.toString} ${if (i === 2) "fis" else "mis"}"
    )

    val `Change months duration`: Message = Message(
      english = "payment plan duration",
      welsh   = "hyd y cynllun talu"
    )

    val `Start month`: Message = Message(
      english = "Start month",
      welsh   = "Mis cyntaf"
    )

    def `First ... montly payments`(i: Int): Message = Message(
      english = s"First ${i.toString} monthly payments",
      welsh   = s"Y ${i.toString} taliad misol cyntaf"
    )

    val `First monthly payment`: Message = Message(
      english = "First monthly payment",
      welsh   = "Taliad misol cyntaf"
    )

    val `Final month`: Message = Message(
      english = "Final month",
      welsh   = "Mis olaf"
    )

    val `Final payment`: Message = Message(
      english = "Final payment",
      welsh   = "Taliad olaf"
    )

    def `including ... interest`(amountInPence: AmountInPence): Message = Message(
      english = s"including ${amountInPence.gdsFormatInPounds} interest",
      welsh   = s"gan gynnwys ${amountInPence.gdsFormatInPounds} o log"
    )

    val Payment: Message = Message(
      english = "Payment",
      welsh   = "Taliad"
    )

    val `Estimated total interest`: Message = Message(
      english = "Estimated total interest<br><span class=\"govuk-body-s\">Included in your plan</span>",
      welsh   = "Amcangyfrif o gyfanswm y llog<br><span class=\"govuk-body-s\">TYn gynwysedig yn eich cynllun</span>"
    )

    val `Total to pay`: Message = Message(
      english = "Total to pay",
      welsh   = "Y cyfanswm i’w dalu"
    )

    val `How much you can afford to pay each month`: Message = Message(
      english = "how much you can afford to pay each month",
      welsh   = "faint y gallwch fforddio ei dalu bob mis"
    )
  }

  object AboutYourBankAccount {

    val `Check you can set up a Direct Debit`: Message = Message(
      english = "Check you can set up a Direct Debit",
      welsh   = "Gwiriwch os allwch sefydlu Debyd Uniongyrchol"
    )

    val `To set up a Direct Debit online, you must be`: Message = Message(
      english = "To set up a Direct Debit online, you must be:",
      welsh   = "I sefydlu Debyd Uniongyrchol, mae’n rhaid i’r canlynol fod yn wir:"
    )

    val `named on the UK bank account...`: Message = Message(
      english = "named on the UK bank account you'll use to pay",
      welsh   = "mae’n rhaid eich bod wedi’ch enwi ar gyfrif banc y DU y byddwch yn ei ddefnyddio i dalu"
    )

    val `authorised to set up a Direct Debit...`: Message = Message(
      english = "authorised to set up a Direct Debit without a signature from any other account holders",
      welsh   = "mae’n rhaid eich bod wedi’ch awdurdodi i sefydlu Debyd Uniongyrchol heb lofnod gan unrhyw ddeiliaid cyfrif eraill"
    )

    val `Can you set up a Direct Debit for this payment plan?`: Message = Message(
      english = "Can you set up a Direct Debit for this payment plan?",
      welsh   = "A allwch sefydlu Debyd Uniongyrchol ar gyfer y cynllun talu hwn?"
    )

    val `Account type`: Message = Message(
      english = "Account type",
      welsh   = "Math o gyfrif"
    )

    val `Business`: Message = Message(
      english = "Business",
      welsh   = "Busnes"
    )

    val `Personal`: Message = Message(
      english = "Personal",
      welsh   = "Personol "
    )

    val `Select yes if you can set up a Direct Debit for this payment plan`: Message = Message(
      english = "Select yes if you can set up a Direct Debit for this payment plan",
      welsh   = "Dewiswch ‘Iawn’ os gallwch sefydlu Debyd Uniongyrchol ar gyfer y cynllun talu hwn"
    )

  }

  object BankDetails {

    val `Bank account details`: Message = Message(
      english = "Bank account details",
      welsh   = "Manylion cyfrif banc"
    )

    val `Name on the account`: Message = Message(
      english = "Name on the account",
      welsh   = "Yr enw sydd ar y cyfrif"
    )

    val `Sort code`: Message = Message(
      english = "Sort code",
      welsh   = "Cod didoli"
    )

    val `Must be 6 digits long`: Message = Message(
      english = "Must be 6 digits long",
      welsh   = "Mae’n rhaid i hyn fod yn 6 digid o hyd"
    )

    val `Account number`: Message = Message(
      english = "Account number",
      welsh   = "Rhif y cyfrif"
    )

    val `Must be between 6 and 8 digits long`: Message = Message(
      english = "Must be between 6 and 8 digits long",
      welsh   = "Mae’n rhaid iddo fod rhwng 6 ac 8 digid o hyd"
    )

    /**
     * Separate given list with commas until the last two items which are separated by the given `lastSeparator`, e.g.
     * {{{
     *   commaSeparateList(List("a"), "or")           = "a"
     *   commaSeparateList(List("a", "b"), "or")      = "a or b"
     *   commaSeparateList(List("a", "b", "c"), "or") = "a, b or c"
     * }}}
     */
    private def commaSeparateList(list: List[String], lastSeparator: String): String = {
        @tailrec
        def loop(l: List[String], acc: String): String = l match {
          case Nil      => ""

          case c :: Nil => c

          case c1 :: c2 :: Nil =>
            if (acc.isEmpty) s"$c1 $lastSeparator $c2"
            else s"$acc, $c1 $lastSeparator $c2"

          case head :: tail =>
            if (acc.isEmpty) loop(tail, head)
            else loop(tail, s"$acc, $head")
        }
      loop(list, "")
    }

    private val nameErrors: Map[String, (List[String] => Message)] =
      Map(
        "name.error.required" -> { _ =>
          Message(
            english = "Enter the name on the account",
            welsh   = "Nodwch yr enw sydd ar y cyfrif"
          )
        },
        "name.error.disallowedCharacters" -> { disallowedCharacters =>
          Message(
            english = s"Name on the account must not contain ${commaSeparateList(disallowedCharacters, "or")}",
            welsh   = s"Mae’n rhaid i’r enw sydd ar y cyfrif peidio â chynnwys ${commaSeparateList(disallowedCharacters, "neu")}",
          )
        },
        "name.error.maxLength" -> { _ =>
          Message(
            english = "Name on the account must be between 2 and 39 characters",
            welsh   = "Mae’n rhaid i’r enw ar y cyfrif fod rhwng 2 a 39 o gymeriadau"
          )
        },
        "name.error.minLength" -> { _ =>
          Message(
            english = "Name on the account must be between 2 and 39 characters",
            welsh   = "Mae’n rhaid i’r enw ar y cyfrif fod rhwng 2 a 39 o gymeriadau"
          )
        }
      )

    private val sortCoderErrors: Map[String, Message] = Map(
      "sortCode.error.required" -> Message(
        english = "Enter sort code",
        welsh   = "Nodwch god didoli"
      ),
      "sortCode.error.nonNumeric" -> Message(
        english = "Sort code must be a number",
        welsh   = "Mae’n rhaid i’r cod didoli fod yn rhif"
      ),
      "sortCode.error.invalid" -> Message(
        english = "Sort code must be 6 digits",
        welsh   = "Mae’n rhaid i’r cod didoli fod yn 6 digid"
      )
    )

    private val accountNumberErrors: Map[String, Message] =
      Map(
        "accountNumber.error.required" -> Message(
          english = "Enter account number",
          welsh   = "Nodwch rif y cyfrif"
        ),
        "accountNumber.error.nonNumeric" -> Message(
          english = "Account number must be a number",
          welsh   = "Mae’n rhaid i rif y cyfrif fod yn rhif"
        ),
        "accountNumber.error.invalid" -> Message(
          english = "Account number must be between 6 and 8 digits",
          welsh   = "Mae’n rhaid i rif y cyfrif fod rhwng 6 ac 8 digid"
        )
      )

    private val accountTypeErrors: Map[String, Message] =
      Map(
        "accountType.error.required" -> Message(
          english = "Select what type of account details you are providing",
          welsh   = "Dewiswch pa fath o gyfrif yr ydych yn ei ddarparu"
        )
      )

    private val barsErrors: Map[String, Message] = {
      val `Enter a valid combination of bank account number and sort code`: Message = Message(
        english = "Enter a valid combination of bank account number and sort code",
        welsh   = "Nodwch gyfuniad dilys o rif cyfrif banc a chod didoli"
      )

      Map(
        s"sortCode.${accountNumberNotWellFormatted.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCodeXXX.${accountNumberNotWellFormatted.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCode.${sortCodeNotPresentOnEiscd.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCode.${sortCodeDoesNotSupportsDirectDebit.formError.message}" -> Message(
          english = "You have entered a sort code which does not accept this type of payment. Check you have entered a valid sort code or enter details for a different account",
          welsh   = "Rydych wedi nodi cod didoli nad yw’n derbyn y math hwn o daliad. Gwiriwch eich bod wedi nodi cod didoli dilys, neu nodwch fanylion ar gyfer cyfrif gwahanol"
        ),
        s"name.${nameDoesNotMatch.formError.message}" -> Message(
          english = "Enter the name on the account as it appears on bank statements.",
          welsh   = "Nodwch yr enw ar y cyfrif, fel y mae’n ymddangos ar gyfriflenni banc."
        ),
        s"sortCode.${accountDoesNotExist.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCode.${sortCodeOnDenyList.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCode.${otherBarsError.formError.message}" -> `Enter a valid combination of bank account number and sort code`
      )
    }

    val errors: Map[String, (List[String] => Message)] = {
      nameErrors ++ (
        sortCoderErrors ++ accountNumberErrors ++ barsErrors ++ accountTypeErrors
      ).map { case (k, v) => k -> { _: List[String] => v } }
    }

    val `Bank details`: Message = Message(
      english = "Bank details",
      welsh   = "Manylion banc"
    )
  }

  object CheckBankDetails {

    val `Check your Direct Debit details`: Message = Message(
      english = "Check your Direct Debit details",
      welsh   = "Gwiriwch fanylion eich Debyd Uniongyrchol"
    )

    val `Account type`: Message = Message(
      english = "Account type",
      welsh   = "Math o gyfrif"
    )

    val `Name on the account`: Message = Message(
      english = "Name on the account",
      welsh   = "Yr enw sydd ar y cyfrif"
    )

    val `Change your Direct Debit details`: Message = Message(
      english = "Change your Direct Debit details",
      welsh   = "Newid manylion Debyd Uniongyrchol"
    )

    val `The Direct Debit Guarantee`: Message = Message(
      english = "The Direct Debit Guarantee",
      welsh   = "Y Warant Debyd Uniongyrchol"
    )

    val `This Guarantee is offered...`: Message = Message(
      english = "This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.",
      welsh   = "Cynigir y Warant hon gan bob banc a chymdeithas adeiladu sy’n derbyn cyfarwyddiadau i dalu Debydau Uniongyrchol."
    )

    val `If there are any changes to the amount...`: Message = Message(
      english = "If there are any changes to the amount, date or frequency of your Direct Debit HMRC NDDS will notify you 10 working days in advance of your account being debited or as otherwise agreed. If you request HMRC NDDS to collect a payment, confirmation of the amount and date will be given to you at the time of the request.",
      welsh   = "Os oes unrhyw newidiadau i swm, dyddiad neu amlder eich Debyd Uniongyrchol, bydd NDDS CThEF yn rhoi gwybod i chi 10 diwrnod gwaith cyn i’ch cyfrif gael ei ddebydu, neu fel y cytunwyd fel arall. Os byddwch yn gwneud cais i NDDS CThEF gasglu taliad, rhoddir cadarnhad o’r swm a’r dyddiad i chi ar adeg y cais."
    )

    val `If an error is made in the payment...`: Message = Message(
      english = "If an error is made in the payment of your Direct Debit by HMRC NDDS or your bank or building society you are entitled to a full and immediate refund of the amount paid from your bank or building society. If you receive a refund you are not entitled to, you must pay it back when HMRC NDDS asks you to.",
      welsh   = "Os gwneir camgymeriad gan NDDS CThEF neu eich banc neu’ch cymdeithas adeiladu wrth dalu Debyd Uniongyrchol, mae gennych hawl i ad-daliad llawn a di-oed o’r swm a dalwyd o’ch banc neu’ch cymdeithas adeiladu. Os byddwch yn cael ad-daliad nad oes hawl gennych iddo, bydd yn rhaid i chi ei dalu’n ôl pan fydd NDDS CThEF yn gofyn i chi wneud hynny."
    )

    val `You can cancel a Direct Debit...`: Message = Message(
      english = "You can cancel a Direct Debit at any time by simply contacting your bank or building society. Written confirmation may be required. Please also notify us.",
      welsh   = "Gallwch ganslo Debyd Uniongyrchol ar unrhyw adeg drwy gysylltu â’ch banc neu’ch cymdeithas adeiladu. Efallai y bydd angen cadarnhad ysgrifenedig. Rhowch wybod i ni hefyd."
    )

  }

  object TermsAndConditions {

    val `Terms and conditions`: Message = Message(
      english = "Terms and conditions",
      welsh   = "Telerau ac amodau"
    )

    val `We can cancel this agreement if you:`: Message = Message(
      english = "We can cancel this agreement if you:",
      welsh   = "Gallwn ganslo’r cytundeb hwn os:"
    )

    val `pay late or miss a payment`: Message = Message(
      english = "pay late or miss a payment",
      welsh   = "ydych yn talu’n hwyr neu’n methu taliad"
    )

    val `pay another tax bill late`: Message = Message(
      english = "pay another tax bill late",
      welsh   = "ydych yn talu bil treth arall yn hwyr"
    )

    val `do not submit your future tax returns on time`: Message = Message(
      english = "do not submit your future tax returns on time",
      welsh   = "nad ydych yn cyflwyno’ch Ffurflenni Treth yn y dyfodol mewn pryd"
    )

    val `Contact HMRC...If we cancel this agreement...`: Message = Message(
      english = "Contact HMRC straight away if you have a problem. If we cancel this agreement, you’ll need to pay the total amount you owe. We can also use any refunds you are due to pay off your bill.",
      welsh   = "Cysylltwch â CThEF ar unwaith os oes gennych broblem. Os byddwn yn canslo’r cytundeb hwn, bydd angen i chi dalu’r cyfanswm sydd arnoch. Gallwn hefyd ddefnyddio unrhyw ad-daliadau rydych sydd arnoch i dalu’ch bil."
    )

    val `You should also tell us if your circumstances change...`: Message = Message(
      english = "You should also tell us if your circumstances change, and you can pay more or pay in full.",
      welsh   = "Dylech hefyd roi gwybod i ni os bydd eich amgylchiadau’n newid, a gallwch dalu mwy neu dalu’n llawn."
    )

    val `You can write to us...`: Message = Message(
      english = "You can write to us about your Direct Debit:",
      welsh   = "Gallwch ysgrifennu atom ynglŷn â’ch Debyd Uniongyrchol:"
    )

    def `HMRC address`(taxRegime: TaxRegime): Message = {
      taxRegime match {
        case TaxRegime.Epaye =>
          Message(
            english = "DM PAYE<br>HM Revenue and Customs<br>BX9 1EW<br>United Kingdom",
            welsh   = "Gwasanaeth Cwsmeriaid Cymraeg CThEF<br>HMRC<br>BX9 1ST"
          )
        case TaxRegime.Vat =>
          Message(
            english = "HMRC Direct Debit Support Team VAT 2<br>DMB 612<br>BX5 5AB<br>United Kingdom",
            welsh   = "Gwasanaeth Cwsmeriaid Cymraeg CThEF <br>HMRC<br>BX9 1ST"
          )
        case TaxRegime.Sa =>
          Message(
            english = "Debt Management<br>Self Assessment<br>HM Revenue and Customs<br>BX9 1AS<br>United Kingdom",
            welsh   = "Rheolaeth Dyledion<br>Hunanasesiad<br>Gwasanaeth Cwsmeriaid Cymraeg CThEF<br>HMRC<br>BX9 1ST"
          )
        case TaxRegime.Simp =>
          Message(
            english = "Debt Management<br>Simple Assessment<br>HM Revenue and Customs<br>BX9 1AS<br>United Kingdom",
            welsh   = "Rheolaeth Dyledion<br>Asesiad Syml<br>Gwasanaeth Cwsmeriaid Cymraeg CThEF<br>HMRC<br>BX9 1ST"
          )

      }
    }

    val `Call the debt management helpline`: Message = Message(
      english = "Call the debt management helpline",
      welsh   = "Ffoniwch y llinell gymorth rheoli dyledion"
    )

    val `Telephone...`: Message = Message(
      english = "Telephone: <strong>0300 123 1813</strong>",
      welsh   = "Ffôn: <strong>0300 200 1900</strong>"
    )

    val `Outside UK...`: Message = Message(
      english = "Outside UK: <strong>+44 2890 538 192</strong>"
    )

    val `Our phone line opening hours...`: Message = Message(
      english = "Our phone line opening hours are:",
      welsh   = "Oriau agor ein llinell ffôn yw:"
    )

    val `Monday to Friday...`: Message = Message(
      english = "Monday to Friday: 8am to 6pm",
      welsh   = "Dydd Llun i ddydd Gwener: 8:30 i 17:00"
    )

    val `Closed weekends...`: Message = Message(
      english = "Closed weekends and bank holidays.",
      welsh   = "Ar gau ar benwythnosau a gwyliau banc."
    )

    val `Text service`: Message = Message(
      english = "Text service",
      welsh   = "Gwasanaeth Text Relay"
    )

    def `Use Relay UK...`(link: String): Message = Message(
      english = s"""Use Relay UK if you cannot hear or speak on the telephone, dial <strong>18001</strong> then <strong>0345 300 3900</strong>. Find out more on the <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">Relay UK website (opens in new tab)</a>.""",
      welsh   = s"""Defnyddiwch wasanaeth Text Relay UK os na allwch glywed na siarad dros y ffôn. Deialwch <strong>18001</strong> ac yna <strong>0345 300 3900</strong>. Dysgwch ragor am hyn ar <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">wefan Text Relay UK (yn agor tab newydd)</a>."""
    )

    val `If a health condition...`: Message = Message(
      english = "If a health condition or personal circumstances make it difficult to contact us",
      welsh   = "Os yw cyflwr iechyd neu amgylchiadau personol yn ei gwneud hi’n anodd i chi gysylltu â ni"
    )

    def `Our guidance Get help from HMRC...`(link: String): Message = Message(
      english = s"""Our guidance <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">Get help from HMRC if you need extra support (opens in new tab)</a> explains how we can support you.""",
      welsh   = s"""Bydd ein harweiniad ynghylch <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">‘Cael help gan CThEF os oes angen cymorth ychwanegol arnoch’ (yn agor tab newydd)</a> yn esbonio sut y gallwn eich helpu."""
    )

    val `Declaration`: Message = Message(
      english = "Declaration",
      welsh   = "Datganiad"
    )

    val `I agree to the terms and conditions...`: Message = Message(
      english = "I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt.",
      welsh   = "Cytunaf â thelerau ac amodau’r cynllun talu hwn. Cadarnhaf mai dyma’r cynharaf y gallaf setlo’r ddyled hon."
    )

    val `Agree and continue`: Message = Message(
      english = "Agree and continue",
      welsh   = "Cytuno ac yn eich blaen"
    )
  }

  object EmailEntry {
    val `Which email do you want to use?`: Message = Message(
      english = "Which email do you want to use?",
      welsh   = "Pa e-bost rydych am ei ddefnyddio?"
    )

    val `Enter your email address`: Message = Message(
      english = "Enter your email address",
      welsh   = "Nodwch eich cyfeiriad e-bost"
    )

    val `We will use this email address to...`: Message = Message(
      english = "We will use this email address to send you information about your payment plan. It may take <strong>up to 24 hours</strong> to receive notifications after you set up your plan.",
      welsh   = "Byddwn yn defnyddio’r cyfeiriad e-bost hwn er mwyn anfon gwybodaeth atoch am eich cynllun talu. Gallai gymryd <strong>hyd at 24 awr</strong> i gael hysbysiadau ar ôl i chi drefnu’ch cynllun."
    )

    val `A new email address`: Message = Message(
      english = "A new email address",
      welsh   = "Cyfeiriad e-bost newydd"
    )

    val `For example, myname@sample.com`: Message = Message(
      english = "For example, myname@sample.com",
      welsh   = "Er enghraifft, fyenw@enghraifft.cymru"
    )

    val `Email address`: Message = Message(
      english = "Email address",
      welsh   = "Cyfeiriad e-bost"
    )

    def getError(key: String): Message = key match {
      case "selectAnEmailToUseRadio.error.required" =>
        Message(
          english = "Select which email address you want to use",
          welsh   = "Dewiswch pa gyfeiriad e-bost rydych chi am ei ddefnyddio"
        )

      case "newEmailInput.error.required" =>
        Message(
          english = "Enter your email address in the correct format, like name@example.com",
          welsh   = "Nodwch eich cyfeiriad e-bost yn y fformat cywir, megis enw@enghraifft.com"
        )

      case "newEmailInput.error.tooManyChar" =>
        Message(
          english = "Enter an email address with 256 characters or less",
          welsh   = "Nodwch gyfeiriad e-bost gan ddefnyddio 256 o gymeriadau neu lai"
        )

      case "newEmailInput.error.invalidFormat" =>
        Message(
          english = "Enter your email address in the correct format, like name@example.com",
          welsh   = "Nodwch eich cyfeiriad e-bost yn y fformat cywir, megis enw@enghraifft.com"
        )
    }
  }

  object EmailConfirmed {

    val `Email address confirmed`: Message = Message(
      english = "Email address confirmed",
      welsh   = "Cyfeiriad e-bost wedi’i gadarnhau"
    )

    def `The email address ... has been confirmed`(email: Email): Message = Message(
      english = s"The email address <strong>${email.value.decryptedValue}</strong> has been confirmed.",
      welsh   = s"Mae’r cyfeiriad e-bost <strong>${email.value.decryptedValue}</strong> wedi’i gadarnhau."
    )

    val `We'll only use this address to contact you about your payment plan`: Message = Message(
      english = "We’ll only use this address to contact you about your payment plan.",
      welsh   = "Byddwn yn defnyddio’r cyfeiriad hwn i gysylltu â chi ynghylch eich cyfrif cynllun talu."
    )

    val `Your email has not been updated in other government services`: Message = Message(
      english = "Your email has not been updated in other government services.",
      welsh   = "Nid yw’ch e-bost wedi cael ei ddiweddaru ar gyfer gwasanaethau eraill y llywodraeth."
    )

  }

  object TooManyPasscodeJourneys {

    val `You have tried to verify an email address too many times`: Message = Message(
      english = "You have tried to verify an email address too many times",
      welsh   = "Rydych wedi ceisio dilysu cyfeiriad e-bost gormod o weithiau"
    )

    def `You have tried to verify <EMAIL> too many times.`(email: String): Message = Message(
      english = s"You have tried to verify <strong>$email</strong> too many times.",
      welsh   = s"Rydych wedi ceisio dilysu <strong>$email</strong> gormod o weithiau."
    )

    def `You will need to verify a new email address.`(link: String): Message = Message(
      english = s"""You will need to <a href="$link" class="govuk-link">verify a different email address</a>.""",
      welsh   = s"""Bydd angen i chi <a href="$link" class="govuk-link">ddilysu cyfeiriad e-bost gwahanol</a>."""
    )

  }

  object TooManyPasscodes {

    val `Email verification code entered too many times`: Message = Message(
      english = "Email verification code entered too many times",
      welsh   = "Cod dilysu e-bost wedi’i nodi gormod o weithiau"
    )

    val `You have entered an email verification code too many times`: Message = Message(
      english = "You have entered an email verification code too many times.",
      welsh   = "Rydych chi wedi nodi cod dilysu e-bost gormod o weithiau."
    )

    def `You can go back to enter a new email address`(link: String): Message = Message(
      english = s"""You can <a class="govuk-link" href="$link">go back to enter a new email address</a>.""",
      welsh   = s"""Gallwch <a class="govuk-link" href="$link">fynd yn ôl i nodi cyfeiriad e-bost newydd</a>."""
    )

  }

  object TooManyEmails {

    val `You have tried to verify too many email addresses`: Message = Message(
      english = "You have tried to verify too many email addresses",
      welsh   = "Rydych wedi ceisio dilysu gormod o gyfeiriadau e-bost"
    )

    def `You have been locked out because you have tried to verify too many email addresses`(dateAndTime: String): Message = Message(
      english = s"""You have been locked out because you have tried to verify too many email addresses. Please try again on <strong>$dateAndTime</strong>.""",
      welsh   = s"""Rydych chi wedi cael eich cloi allan oherwydd eich bod wedi ceisio dilysu gormod o gyfeiriadau e-bost. Rhowch gynnig arall arni ar <strong>$dateAndTime</strong>."""
    )

    val `at`: Message = Message(
      english = "at",
      welsh   = "am"
    )

  }

  object NotSoleSignatory {

    val `Call us about a payment plan`: Message = Message(
      english = "Call us about a payment plan",
      welsh   = "Ffoniwch ni ynghylch cynllun talu"
    )

    val `You cannot set up a Self Assessment payment plan online if you are not the only account holder.`: Message = Message(
      english = "You cannot set up a Self Assessment payment plan online if you are not the only account holder.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer Asesiad Syml os nad chi yw’r unig ddeiliad y cyfrif."
    )

    val `You cannot set up a Simple Assessment payment plan online if you are not the only account holder.`: Message = Message(
      english = "You cannot set up a Simple Assessment payment plan online if you are not the only account holder.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer Hunanasesiad os nad chi yw’r unig ddeiliad y cyfrif."
    )

    val `You cannot set up an Employers’ PAYE payment plan online if you are not the only account holder.`: Message = Message(
      english = "You cannot set up an Employers’ PAYE payment plan online if you are not the only account holder.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer TWE y Cyflogwr os nad chi yw’r unig ddeiliad y cyfrif."
    )

    val `You cannot set up a VAT payment plan online if you are not the only account holder.`: Message = Message(
      english = "You cannot set up a VAT payment plan online if you are not the only account holder.",
      welsh   = "Ni allwch drefnu cynllun talu ar-lein ar gyfer TAW os nad chi yw’r unig ddeiliad y cyfrif."
    )

    val `Call us on 0300 123 1813 if you need to set up a Direct Debit from a joint account. All account holders must be present when calling.`: Message = Message(
      english = "Call us on <strong>0300 123 1813</strong> if you need to set up a Direct Debit from a joint account. All account holders must be present when calling.",
      welsh   = "Ffoniwch ni ar <strong>0300 200 1900</strong> os oes angen trefnu Debyd Uniongyrchol o gyfrif ar y cyd. Mae’n rhaid i holl ddeiliaid y cyfrif fod yn bresennol pan fyddwch yn ffonio."
    )

  }

  object BankDetailsLockout {

    val `You've tried to confirm your bank details too many times`: Message = Message(
      english = "You’ve tried to confirm your bank details too many times",
      welsh   = "Rydych wedi ceisio cadarnhau eich manylion banc gormod o weithiau"
    )

    def waitUntil(lockExpires: String): Message = Message(
      english = s"You’ll need to wait until ${lockExpires} before trying to confirm your bank details again.",
      welsh   = s"Bydd angen i chi aros tan $lockExpires cyn ceisio cadarnhau eich manylion banc eto."
    )

    val `You may still be able to set up a payment plan over the phone.`: Message = Message(
      english = "You may still be able to set up a payment plan over the phone.",
      welsh   = "Mae’n bosibl y byddwch yn dal i allu trefnu cynllun talu dros y ffôn."
    )

    val `For further support you can contact the Payment Support Service...`: Message = Message(
      english = "For further support you can contact us on <strong>0300 123 1813</strong> to speak to an adviser.",
      welsh   = "I gael cymorth pellach, gallwch gysylltu â Gwasanaeth Cwsmeriaid Cymraeg CThEF ar <strong>0300 200 1900</strong> i siarad ag ymgynghorydd."
    )

    val am: Message = Message(
      english = "am",
      welsh   = "am"
    )

    val pm: Message = Message(
      english = "pm",
      welsh   = "pm"
    )

  }

  object Confirmation {
    val `Your payment plan is set up`: Message = Message(
      english = "Your payment plan is set up",
      welsh   = "Mae’ch cynllun talu wedi’i drefnu"
    )

    val `Your payment reference is`: Message = Message(
      english = "Your payment reference is",
      welsh   = "Eich cyfeirnod talu yw"
    )

    val `What happens next`: Message = Message(
      english = "What happens next",
      welsh   = "Yr hyn sy’n digwydd nesaf"
    )

    val `In the next 24 hours we'll:`: Message = Message(
      english = "In the next 24 hours we’ll:",
      welsh   = "Yn ystod y 24 awr nesaf, byddwn yn gwneud y canlynol:"
    )

    val `update your tax account with your payment plan`: Message = Message(
      english = "update your tax account with your payment plan",
      welsh   = "diweddaru’ch cyfrif treth gyda’ch cynllun talu"
    )

    val `send payment due dates to your business tax account inbox`: Message = Message(
      english = "send payment due dates to your business tax account inbox",
      welsh   = "anfon dyddiadau dyledus talu i’ch mewnflwch cyfrif treth busnes"
    )

    val `You'll also receive a letter with your payment dates. We'll send this out within 5 days.`: Message = Message(
      english = "You’ll also receive a letter with your payment dates. We’ll send this out within 5 days.",
      welsh   = "Byddwch hefyd yn cael llythyr gyda’ch dyddiadau talu. Byddwn yn anfon hwn cyn pen 5 diwrnod."
    )

    val `If you've made an upfront payment, we'll take it from your bank account within 6 working days.`: Message = Message(
      english = "If you’ve made an upfront payment, we’ll take it from your bank account within 6 working days.",
      welsh   = "Os ydych wedi gwneud taliad ymlaen llaw, byddwn yn ei gymryd o’ch cyfrif banc cyn pen 6 diwrnod gwaith."
    )

    val `You can call HMRC to update your payment plan. Make sure you have your payment reference number ready.`: Message = Message(
      english = "You can call HMRC to update your payment plan. Make sure you have your payment reference number ready.",
      welsh   = "Gallwch ffonio CThEF i ddiweddaru’ch cynllun talu. Gwnewch yn siŵr bod eich cyfeirnod talu yn barod."
    )

    val `Call the debt management helpline`: Message = Message(
      english = "Call the debt management helpline",
      welsh   = "Ffoniwch y llinell gymorth rheoli dyledion"
    )

    val `Telephone: 0300 123 1813`: Message = Message(
      english = "Telephone: <strong>0300 123 1813</strong>",
      welsh   = "Ffôn: <strong>0300 123 1813</strong>"
    )

    val `Outside UK: +44 2890 538 192`: Message = Message(
      english = "Outside UK: <strong>+44 2890 538 192</strong>",
      welsh   = "O’r tu allan i’r DU: <strong>+44 2890 538 192</strong>"
    )

    val `Our phone line opening hours are:`: Message = Message(
      english = "Our phone line opening hours are:",
      welsh   = "Oriau agor ein llinell ffôn yw:"
    )

    val `Monday to Friday: 8am to 6pm`: Message = Message(
      english = "Monday to Friday: 8am to 6pm",
      welsh   = "Dydd Llun i ddydd Gwener: 8:30 i 17:00"
    )

    val `Closed weekends and bank holidays.`: Message = Message(
      english = "Closed weekends and bank holidays.",
      welsh   = "Ar gau ar benwythnosau a gwyliau banc."
    )

    val `Text service`: Message = Message(
      english = "Text service",
      welsh   = "Gwasanaeth Text Relay"
    )

    def `Use Relay UK if you cannot hear or speak on the telephone...`(link: String): Message = Message(
      english = s"""Use Relay UK if you cannot hear or speak on the telephone, dial <strong>18001</strong> then <strong>0345 300 3900</strong>. Find out more on the <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">Relay UK website (opens in new tab)</a>.""",
      welsh   = s"""Defnyddiwch wasanaeth Text Relay UK os na allwch glywed na siarad dros y ffôn. Deialwch <strong>18001</strong> ac yna <strong>0345 300 3900</strong>. Dysgwch ragor am hyn ar <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">wefan Text Relay UK (yn agor tab newydd)</a>."""
    )

    val `If a health condition or personal circumstances make it difficult to contact us`: Message = Message(
      english = "If a health condition or personal circumstances make it difficult to contact us",
      welsh   = "Os yw cyflwr iechyd neu amgylchiadau personol yn ei gwneud hi’n anodd i chi gysylltu â ni"
    )

    def `Our guidance Get help from HMRC if you need extra support...`(link: String): Message = Message(
      english = s"""Our guidance <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">Get help from HMRC if you need extra support (opens in new tab)</a> explains how we can support you.""",
      welsh   = s"""Bydd ein harweiniad ynghylch <a href="$link" class="govuk-link" rel="noreferrer noopener" target="_blank">‘Cael help gan CThEF os oes angen cymorth ychwanegol arnoch’ (yn agor tab newydd)</a> yn esbonio sut y gallwn eich helpu."""
    )

    val `What you need to do next`: Message = Message(
      english = "What you need to do next",
      welsh   = "Yr hyn y mae angen i chi ei wneud nesaf"
    )

    def `View your payment plan where`(link: String): Message = Message(
      english = s"""<a href="$link" class="govuk-link">View your payment plan</a> where you will be able to print or save a copy.""",
      welsh   = s"""<a href="$link" class="govuk-link">Bwrw golwg dros eich cynllun talu</a> lle byddwch yn gallu argraffu neu gadw copi ohono."""
    )

    val `We will not send you a copy`: Message = Message(
      english = "<strong>We will not send a copy of your payment plan in the post. This is your only chance to access this information.</strong>",
      welsh   = "<strong>Ni fyddwn yn anfon copi o’ch cynllun talu drwy’r post. Dyma’ch unig gyfle i gael mynediad at yr wybodaeth hon.</strong>"
    )

    val `About your payment plan`: Message = Message(
      english = "About your payment plan",
      welsh   = "Ynglŷn â’ch cynllun talu"
    )

    val `We will send a secure message with payment due dates to your business tax account inbox within 24 hours.`: Message = Message(
      english = "We will send a secure message with payment due dates to your business tax account inbox within 24 hours.",
      welsh   = "Byddwn yn anfon neges ddiogel gyda dyddiadau cau ar gyfer talu i fewnflwch eich cyfrif treth busnes cyn pen 24 awr."
    )

    def paymentInfo(hasUpfrontPayment: Boolean, paymentDate: String): Message = Message(
      english = s"${if (hasUpfrontPayment) "Your upfront payment will be taken within 6 working days. " else ""}Your next payment will be taken on $paymentDate or the next working day.",
      welsh   = s"${if (hasUpfrontPayment) "Caiff eich taliad ymlaen llaw ei gymryd cyn pen 6 diwrnod gwaith. " else ""}Caiff eich taliad nesaf ei gymryd ar $paymentDate neu’r diwrnod gwaith nesaf."
    )

    val `Your tax account will be updated with your payment plan within 24 hours.`: Message = Message(
      english = "Your tax account will be updated with your payment plan within 24 hours.",
      welsh   = "Bydd eich cyfrif treth yn cael ei ddiweddaru gyda’ch cynllun talu cyn pen 24 awr."
    )

    val `Print a copy of your payment plan`: Message = Message(
      english = "Print a copy of your payment plan",
      welsh   = "Argraffu copi o’ch cynllun talu"
    )

    val `Print or save a copy of your payment plan`: Message = Message(
      english = "Print or save a copy of your payment plan",
      welsh   = "Argraffwch neu cadwch gopi o’r cynllun talu"
    )

    val `View your payment plan`: Message = Message(
      english = "View your payment plan",
      welsh   = "Bwrw golwg dros eich cynllun talu"
    )

    val `If you need to change your payment plan`: Message = Message(
      english = "If you need to change your payment plan",
      welsh   = "Os oes angen i chi newid eich cynllun talu"
    )

    val `Call the HMRC Helpline on 0300 123 1813.`: Message = Message(
      english = "Call the HMRC Helpline on 0300 123 1813.",
      welsh   = "Ffoniwch Wasanaeth Cwsmeriaid Cymraeg CThEF ar 0300 200 1900."
    )

    val `Go to tax account`: Message = Message(
      english = "Go to tax account",
      welsh   = "Ewch i’r cyfrif treth"
    )

    val `What did you think of this service`: Message = Message(
      english = "What did you think of this service?",
      welsh   = "Beth oedd eich barn am y gwasanaeth hwn?"
    )

    val `(takes 30 seconds)`: Message = Message(
      english = "(takes 30 seconds)",
      welsh   = "(mae’n cymryd 30 eiliad)"
    )

  }

  object PrintSummary {

    val `Your payment plan`: Message = Message(
      english = "Your payment plan",
      welsh   = "Eich cynllun talu"
    )
    def `Confirmation of your payment plan`(amount: String): Message = Message(
      english = s"Confirmation of plan to pay $amount",
      welsh   = s"Cadarnhad o’ch cynllun talu i dalu $amount"
    )

    def `You have set up a payment plan`(amount: String): Message = Message(
      english = s"You have set up a payment plan to pay $amount",
      welsh   = s"Rydych wedi trefnu cynllun talu i dalu $amount"
    )

    def `HMRC has agreed to this plan`(amount: String): Message = Message(
      english = s"HMRC has agreed to this plan on the understanding that you’ve told us about all your HMRC debts. This plan is only for $amount.",
      welsh   = s"Mae CThEF wedi cytuno i’r cynllun hwn ar y ddealltwriaeth eich bod wedi rhoi gwybod i ni am eich dyledion gyda CThEF i gyd. Mae’r cynllun ar gyfer $amount yn unig."
    )
    val `You need to make any other payments on time`: Message = Message(
      english = "You need to make any other payments on time. You also need to send in all future tax returns on time.",
      welsh   = "Mae angen i chi gwneud unrhyw daliadau eraill mewn pryd. Mae angen i chi hefyd anfon bob Ffurflen Dreth atom mewn pryd yn y dyfodol."
    )
    val `Make sure that your payments reach us`: Message = Message(
      english = "Make sure that your payments reach us by the date agreed.",
      welsh   = "Gwnewch yn siŵr bod eich taliadau yn ein cyrraedd erbyn y dyddiad y cytunwyd arno."
    )

    val `Payment reference`: Message = Message(
      english = "Payment reference",
      welsh   = "Cyfeirnod y taliad"
    )

  }

  object NotPaidOnTime {

    val `If you do not pay on time`: Message = Message(
      english = "If you do not pay on time",
      welsh   = "Os nad ydych yn talu mewn pryd"
    )

    val `If you do not send future tax returns`: Message = Message(
      english = "If you do not send future tax returns or pay any tax due on time, we may cancel this plan and ask you to pay in full.",
      welsh   = "Os na fyddwch yr anfon Ffurflenni Treth neu’n talu unrhyw dreth mewn pryd, mae’n bosibl y byddwn yn canslo’r cynllun hwn a gofyn i chi dalu’n llawn."
    )

    val `We’ve calculated interest at the current rate`: Message = Message(
      english = "We’ve calculated interest at the current rate using the amounts and dates agreed. If you pay on any other date, or the interest rate changes, the amount of interest we charge will change.",
      welsh   = "Rydym wedi cyfrifo llog ar y gyfradd bresennol gan ddefnyddio’r symiau a dyddiadau y cytunwyd arnynt. Os ydych yn talu ar unrhyw ddyddiad arall, neu os yw’r gyfradd llog yn newid, bydd swm y llog byddwn yn ei godi yn newid."
    )

    val `If you’re due a refund from us`: Message = Message(
      english = "If you’re due a refund from us while you’re in this plan, we’ll take it off the amount you owe. This will reduce the amount of interest you’ll have to pay.",
      welsh   = "Os yw ad-daliad yn ddyledus i chi oddi wrthym tra byddwch yn y cynllun hwn, byddwn yn ei dynnu oddi ar y swm sy’n ddyledus gennych. Bydd hyn yn gostwng swm y llog y bydd yn rhaid i chi ei dalu."
    )

    val `If you’ve set up a Direct Debit`: Message = Message(
      english = "If you’ve set up a Direct Debit, we’ll continue to collect the original amount unless you give us permission to change it. You can do this by calling us on <strong>0300 123 1813</strong>.",
      welsh   = "Os ydych wedi trefnu Debyd Uniongyrchol, byddwn yn parhau i gasglu’r swm gwreiddiol oni bai eich bod yn rhoi caniatâd i ni ei newid. Gallwch wneud hyn drwy ffonio ni ar <strong>0300 200 1900</strong>."
    )

    val `If we do not hear from you`: Message = Message(
      english = "If we do not hear from you, we’ll put any overpayment at the end of the plan towards any future tax you owe, or send you a refund.",
      welsh   = "Os na fyddwn yn clywed gennych, byddwn yn rhoi unrhyw ordaliad ar ddiwedd y cynllun tuag at unrhyw dreth bydd arnoch yn y dyfodol neu byddwn yn anfon ad-daliad atoch."
    )

    val `When you make any future Self Assessment payments`: Message = Message(
      english = "When you make any future Self Assessment payments, we’ll put these towards your payment plan instead of your latest Self Assessment bill. If you want us to put your payment towards your Self Assessment bill first, call us 5 days after you’ve paid.",
      welsh   = "Pan fyddwch yn gwneud unrhyw daliadau Hunanasesiad yn y dyfodol, byddwn yn rhoi’r rhain tuag at eich cynllun talu yn hytrach na’ch bil Hunanasesiad diweddaraf. Os ydych am i ni roi eich taliad tuag at eich bil Hunanasesiad yn gyntaf, ffoniwch ni 5 diwrnod ar ôl i chi dalu."
    )

  }

  object DifficultyPaying {

    val `If you’re having difficulty paying`: Message = Message(
      english = "If you’re having difficulty paying",
      welsh   = "Os ydych yn cael trafferth i dalu"
    )

    val `We’re here to help`: Message = Message(
      english = "We’re here to help. Call us on <strong>0300 123 1813</strong>. Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.",
      welsh   = "Rydym yma i helpu. Ffoniwch ni ar <strong>0300 200 1900</strong>. Ein horiau agor yw Dydd Llun i Ddydd Gwener, 8am i 5:30pm. Rydym ar gau ar benwythnosau a gwyliau banc."
    )

    def `You’ll need your payment reference`(ref: String): Message = Message(
      english = s"You’ll need your payment reference which is <strong>$ref</strong>",
      welsh   = s"Bydd angen eich cyfeirnod talu arnoch, sef <strong>$ref</strong>"
    )

    val `HM Revenue and Customs`: Message = Message(
      english = "<strong>HM Revenue & Customs</strong>",
      welsh   = "<strong>Cyllid a Thollau EF</strong>"
    )

  }

  object MissingInformation {

    val `Some information is missing`: Message = Message(
      english = "Some information is missing",
      welsh   = "Mae rhywfaint o wybodaeth ar goll"
    )

    val `You must provide more information to set up a payment plan.`: Message = Message(
      english = "You must provide more information to set up a payment plan.",
      welsh   = "Mae’n rhaid i chi roi rhagor o wybodaeth er mwyn sefydlu cynllun talu."
    )

  }

  object Shuttered {

    val `Sorry the service is unavailable`: Message = Message(
      english = "Sorry, the service is unavailable",
      welsh   = "Mae’n ddrwg gennym – nid yw’r gwasanaeth ar gael"
    )

    val `You will be able to use the service later`: Message = Message(
      english = "You will be able to use the service later.",
      welsh   = "Byddwch yn gallu defnyddio’r gwasanaeth yn nes ymlaen."
    )

    def `You can contact the Payment Support Service...`(link: String): Message = Message(
      english = s"""You can <a class="govuk-link" href="$link">contact the Payment Support Service</a> to set up a payment plan by phone.""",
      welsh   = s"""Gallwch <a class="govuk-link" href="$link">gysylltu â Gwasanaeth Cwsmeriaid Cymraeg CThEF</a> er mwyn sefydlu cynllun talu dros y ffôn."""
    )

  }

}

