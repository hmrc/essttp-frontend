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

import essttp.rootmodel.{AmountInPence, Email, TaxRegime}
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

  val `full stop`: Message = Message(
    english = ".",
    welsh   = "."
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

    def `Balancing payment for tax year`(taxYearStartYear: Int, taxYearEndYear: Int): Message = Message(
      english = s"Balancing payment for tax year ${taxYearStartYear.toString} to ${taxYearEndYear.toString}",
      welsh   = s"Taliad mantoli ar gyfer blwyddyn dreth ${taxYearStartYear.toString} i ${taxYearEndYear.toString}"
    )

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

    val `For further support, you can contact us on 0300 123 1813 to speak to an advisor.`: Message = Message(
      english = "For further support, you can contact us on <strong>0300 123 1813</strong> to speak to an adviser.",
      welsh   = "I gael cymorth pellach, gallwch gysylltu â Gwasanaeth Cwsmeriaid Cymraeg CThEF ar <strong>0300 200 1900</strong> i siarad ag ymgynghorydd."
    )

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
      english = "your Self Assessment Unique Taxpayer Reference (UTR) which can be 10 or 13 digits long",
      welsh   = "eich Cyfeirnod Unigryw y Trethdalwr (UTR) ar gyfer Hunanasesiad a allai fod yn 10 neu 13 digid o hyd "
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
    }

    def `Pay your ... bill in full`(taxRegime: TaxRegime): Message = {
      val (taxSpecificContentEnglish, taxSpecificContentWelsh) = taxRegime match {
        case TaxRegime.Epaye => "PAYE" -> "TWE"
        case TaxRegime.Vat   => "VAT" -> "TAW"
        case TaxRegime.Sa    => "Self Assessment tax" -> "treth Hunanasesiad"
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
    }

    def `Call us on 0300 123 1813 if you are having difficulty making a payment online.`: Message = Message(
      english = "Call us on <strong>0300 123 1813</strong> if you are having difficulty making a payment online.",
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
    }

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

    val `Go to your tax account`: Message = Message(
      english = "Go to your tax account",
      welsh   = "Ewch i’ch cyfrif treth"
    )

    val `to file your tax return.`: Message = Message(
      english = " to file your tax return.",
      welsh   = " er mwyn cyflwyno’ch Ffurflen Dreth."
    )

    val `Call us on 0300 123 1813 as you may be able to set up a plan over the phone`: Message = Message(
      english = "Call us on <strong>0300 123 1813</strong> as you may be able to set up a plan over the phone.",
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
      case TaxRegime.Sa => throw new NotImplementedError("ddInProgress flag not relevant to SA charges")
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
      english = s"""<a class="govuk-link" href="$link">I do not want to set up a payment plan</a>""",
      welsh   = s"""<a class="govuk-link" href="$link">Nid wyf am drefnu cynllun talu</a>"""
    )
  }

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

  }

  object UpfrontPayment {

    val `Upfront payment`: Message = Message(
      english = "Upfront payment",
      welsh   = "Taliad ymlaen llaw"
    )

    val `You'll pay less interest and have a shorter...`: Message = Message(
      english = "You’ll pay less interest and have a shorter payment plan if you can make an upfront payment. This payment will be taken from your bank account within 6 working days.",
      welsh   = "Byddwch yn talu llai o log a bydd gennych gynllun talu byrrach os gallwch wneud taliad ymlaen llaw. Caiff y taliad hwn ei gymryd o’ch cyfrif banc cyn pen 6 diwrnod gwaith."
    )

    val `An upfront payment is separate to...`: Message = Message(
      english = "An upfront payment is separate to any recent payments you have made.",
      welsh   = "Mae taliad ymlaen llaw ar wahân i unrhyw daliadau diweddar yr ydych wedi eu gwneud."
    )

    val `Can you make an upfront payment?`: Message = Message(
      english = "Can you make an upfront payment?",
      welsh   = "A allwch wneud taliad ymlaen llaw?"
    )

    val `Select yes if you can make an upfront payment`: Message = Message(
      english = "Select yes if you can make an upfront payment",
      welsh   = "Dewiswch ‘Iawn’ os gallwch wneud taliad ymlaen llaw"
    )

  }

  object Epaye {

    val `Set up an Employers' PAYE payment plan`: Message = Message(
      english = "Set up an Employers’ PAYE payment plan",
      welsh   = "Trefnu cynllun talu ar gyfer TWE Cyflogwyrc"
    )

    val `You can use this service to pay overdue payments...`: Message = Message(
      english = "You can use this service to pay overdue payments in instalments. The payments you make may incur interest.",
      welsh   = "Gallwch ddefnyddio’r gwasanaeth hwn i dalu taliadau hwyr fesul rhandaliad. Efallai y codir llog ar y taliadau a wnewch."
    )

    val `You can set up a payment plan online if you:`: Message = Message(
      english = "You can set up a payment plan online if you:",
      welsh   = "Gallwch fynd ar-lein i drefnu cynllun talu os yw’r canlynol yn wir:"
    )

    def `owe ... or less`(maxAmountOfDebt: AmountInPence): Message = Message(
      english = s"owe ${maxAmountOfDebt.gdsFormatInPounds} or less",
      welsh   = s"mae arnoch ${maxAmountOfDebt.gdsFormatInPounds} neu lai"
    )

    def `plan to pay your debt off within the next ... months`(maxPlanDurationInMonths: Int): Message = Message(
      english = s"plan to pay your debt off within the next ${maxPlanDurationInMonths.toString} months",
      welsh   = s"rydych yn bwriadu talu’ch dyled cyn pen y ${maxPlanDurationInMonths.toString} mis nesaf"
    )

    def `have debts that are ... years old or less`(maxDebtAgeInYears: Int): Message = Message(
      english = s"have debts that are ${maxDebtAgeInYears.toString} years old or less",
      welsh   = s"mae gennych ddyledion sy’n ${maxDebtAgeInYears.toString} blynedd oed neu lai"
    )

    val `do not have any other payment plans or debts with HMRC`: Message = Message(
      english = "do not have any other payment plans or debts with HMRC",
      welsh   = "nid oes gennych unrhyw gynlluniau talu na dyledion eraill gyda CThEF"
    )

    val `have sent any Employers’ PAYE submissions and Construction Industry Scheme (CIS) returns that are due`: Message = Message(
      english = "have sent any Employers’ PAYE submissions and Construction Industry Scheme (CIS) returns that are due",
      welsh   = "rydych wedi anfon unrhyw gyflwyniadau TWE y Cyflogwr a datganiadau Cynllun y Diwydiant Adeiladu (CIS) erbyn y dyddiadau cau"
    )

    val `have missed the deadline to pay a PAYE bill`: Message = Message(
      english = "have missed the deadline to pay a PAYE bill",
      welsh   = "rydych wedi methu’r dyddiad cau i dalu bil TWE"
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
      english = "You must keep up to date with your payments. HMRC may ask you to pay the total outstanding amount if you do not. HMRC intend this as a one-off payment plan to give you extra support.",
      welsh   = "Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Mae’n bosibl y bydd CThEF yn gofyn i chi dalu’r cyfanswm sydd heb ei dalu os na fyddwch yn gwneud eich taliadau mewn pryd. Bwriad CThEF yw bod hwn yn gynllun talu untro i roi cymorth ychwanegol i chi."
    )

  }

  object Vat {
    val `Set up a VAT payment plan`: Message = Message(
      english = "Set up a VAT payment plan",
      welsh   = "Trefnu cynllun talu TAW"
    )
    val `The payment plan covers all appropriate overdue amounts...`: Message = Message(
      english = "The payment plan covers all appropriate overdue amounts, surcharges, penalties and interest. The payments you make may incur interest.",
      welsh   = "Mae’r cynllun talu’n cwmpasu pob gordal, cosb, llog a swm gorddyledus priodol. Efallai y codir llog ar y taliadau a wnewch."
    )
    val `Who can use this service`: Message = Message(
      english = "Who can use this service",
      welsh   = "Pwy all ddefnyddio’r gwasanaeth hwn"
    )
    val `You can set up a payment plan online if you:`: Message = Message(
      english = "You can set up a payment plan online if you:",
      welsh   = "Gallwch fynd ar-lein i drefnu cynllun talu os yw’r canlynol yn wir:"
    )
    def `owe ... or less`(maxAmountOfDebt: AmountInPence): Message = Message(
      english = s"owe ${maxAmountOfDebt.gdsFormatInPounds} or less",
      welsh   = s"mae arnoch ${maxAmountOfDebt.gdsFormatInPounds} neu lai"
    )
    def `plan to pay your debt off within the next ... months`(maxPlanDuration: Int): Message = Message(
      english = s"plan to pay your debt off within the next ${maxPlanDuration.toString} months",
      welsh   = s"rydych yn bwriadu talu’ch dyled cyn pen y ${maxPlanDuration.toString} mis nesaf"
    )
    val `have a debt for an accounting period that started in 2023 or later`: Message = Message(
      english = "have a debt for an accounting period that started in 2023 or later",
      welsh   = "mae gennych ddyled am gyfnod cyfrifyddu a ddechreuodd yn 2023 neu’n hwyrach"
    )
    val `do not have any other payment plans or debts with HMRC`: Message = Message(
      english = "do not have any other payment plans or debts with HMRC",
      welsh   = "nid oes gennych unrhyw gynlluniau talu na dyledion eraill gyda CThEF"
    )
    val `have filed your tax returns`: Message = Message(
      english = "have filed your tax returns",
      welsh   = "rydych wedi cyflwyno’ch Ffurflenni TAW"
    )
    val `have missed the deadline to pay a VAT bill`: Message = Message(
      english = "have missed the deadline to pay a VAT bill",
      welsh   = "rydych wedi methu’r dyddiad cau i dalu bil TAW"
    )
    val `If you have a Customer Compliance Manager...`: Message = Message(
      english = "If you have a Customer Compliance Manager, consider discussing your needs with them before using this service.",
      welsh   = "Os oes gennych chi reolwr cydymffurfiad cwsmeriaid, ystyriwch drafod eich anghenion ag ef cyn defnyddio’r gwasanaeth hwn."
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
    val `Who cannot use this service:`: Message = Message(
      english = "Who cannot use this service:",
      welsh   = "Pwy sy’n methu defnyddio’r gwasanaeth hwn:"
    )
    val `cash accounting customers`: Message = Message(
      english = "cash accounting customers",
      welsh   = "cwsmeriaid cyfrifyddu arian parod"
    )
    val `annual accounting scheme members`: Message = Message(
      english = "annual accounting scheme members",
      welsh   = "aelodau o’r cynllun cyfrifyddu blynyddol"
    )
    val `payment on account customers`: Message = Message(
      english = "payment on account customers",
      welsh   = "cwsmeriaid taliad ar gyfrif"
    )
    val `You must keep up to date with your payments...`: Message = Message(
      english = "You must keep up to date with your payments. HMRC may ask you to pay the total outstanding amount if you do not. HMRC intend this as a one-off payment plan to give you extra support.",
      welsh   = "Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Mae’n bosibl y bydd CThEF yn gofyn i chi dalu’r cyfanswm sydd heb ei dalu os na fyddwch yn gwneud eich taliadau mewn pryd. Bwriad CThEF yw bod hwn yn gynllun talu untro i roi cymorth ychwanegol i chi."
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
      english = "upfront payment",
      welsh   = "Taliad ymlaen llaw"
    )

    val `Upfront payment`: Message = Message(
      english = "Upfront payment<br><span class=\"govuk-body-s\">Taken within 6 working days</span>",
      welsh   = "Taliad ymlaen llaw<br><span class=\"govuk-body-s\">I’w gymryd cyn pen 6 diwrnod gwaith</span>"
    )

    val `Upfront payment-visually-hidden-message`: Message = Message(
      english = "payment amount",
      welsh   = "swm y taliad"
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
      english = "payment day",
      welsh   = "y diwrnod talu"
    )

    val `Change months duration`: Message = Message(
      english = "how many months you want to pay over",
      welsh   = "dros sawl mis yr hoffech dalu"
    )

    val `Total to pay`: Message = Message(
      english = "Total to pay",
      welsh   = "Y cyfanswm i’w dalu"
    )

    val `How much you can afford to pay each month`: Message = Message(
      english = "how much you can afford to pay each month",
      welsh   = "faint y gallwch fforddio ei dalu bob mis"
    )

    def `If you already have a Direct Debit for TAX_REGIME`(taxRegime: TaxRegime): Message = {
      val (englishRegimeName, welshRegimeName) = taxRegime match {
        case TaxRegime.Epaye => "Employers’ PAYE" -> "TWE y Cyflogwyr"
        case TaxRegime.Vat   => "VAT" -> "TAW"
        case TaxRegime.Sa    => throw new NotImplementedError()
      }
      Message(
        english = s"If you already have a Direct Debit for $englishRegimeName, contact your bank to stop the next payment being collected. This will prevent you from being charged twice.",
        welsh   = s"Os oes gennych Debyd Uniongyrchol ar gyfer $welshRegimeName eisoes, cysylltwch â’ch banc i atal y taliad nesaf rhag cael ei gasglu. Bydd hyn yn eich atal rhag cael tâl wedi codi arnoch ddwywaith."
      )
    }

  }

  object AboutYourBankAccount {

    val `About your bank account`: Message = Message(
      english = "About your bank account",
      welsh   = "Ynglŷn â’ch cyfrif banc"
    )

    val `What type of account details are you providing?`: Message = Message(
      english = "What type of account details are you providing?",
      welsh   = "Pa fath o fanylion cyfrif yr ydych yn eu rhoi?"
    )

    val `Business bank account`: Message = Message(
      english = "Business bank account",
      welsh   = "Cyfrif banc busnes"
    )

    val `Personal bank account`: Message = Message(
      english = "Personal bank account",
      welsh   = "Cyfrif banc personol "
    )

    val `Select what type of account details you are providing`: Message = Message(
      english = "Select what type of account details you are providing",
      welsh   = "Dewiswch pa fath o gyfrif yr ydych yn ei ddarparu"
    )

    val `Are you the account holder`: Message = Message(
      english = "Are you the account holder?",
      welsh   = "Ai chi yw deiliad y cyfrif?"
    )

    val `You must be the sole account holder...`: Message = Message(
      english = "You must be the sole account holder, or for multi-signature accounts you must have authority to set up a Direct Debit without additional signatures.",
      welsh   = "Mae’n rhaid mai chi yw’r talwr a’r unig berson sydd ei angen i awdurdodi Debyd Uniongyrchol o’r cyfrif hwn."
    )

    val `Select yes if you are the account holder`: Message = Message(
      english = "Select yes if you are the account holder",
      welsh   = "Dewiswch ‘Iawn’ os mai chi yw deiliad y cyfrif"
    )

  }

  object BankDetails {

    val `Set up Direct Debit`: Message = Message(
      english = "Set up Direct Debit",
      welsh   = "Trefnu Debyd Uniongyrchol"
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
          english = "Enter the name on the account as it appears on bank statements. Do not copy and paste it.",
          welsh   = "Nodwch yr enw ar y cyfrif, fel y mae’n ymddangos ar gyfriflenni banc. Peidiwch â’i gopïo a’i ludo."
        ),
        s"sortCode.${accountDoesNotExist.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCode.${sortCodeOnDenyList.formError.message}" -> `Enter a valid combination of bank account number and sort code`,
        s"sortCode.${otherBarsError.formError.message}" -> `Enter a valid combination of bank account number and sort code`
      )
    }

    val errors: Map[String, (List[String] => Message)] = {
      nameErrors ++ (
        sortCoderErrors ++ accountNumberErrors ++ barsErrors
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

    val `Account name`: Message = Message(
      english = "Account name",
      welsh   = "Enw’r cyfrif"
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

    val `If we cancel this agreement...`: Message = Message(
      english = "If we cancel this agreement, you will need to pay the total amount you owe straight away.",
      welsh   = "Os byddwn yn canslo’r cytundeb hwn, bydd yn rhaid i chi dalu’r cyfanswm sydd arnoch ar unwaith."
    )

    val `We can use any refunds you might get to pay off your tax charges.`: Message = Message(
      english = "We can use any refunds you might get to pay off your tax charges.",
      welsh   = "Gallwn ddefnyddio unrhyw ad-daliadau y gallech eu cael i dalu’ch taliadau treth."
    )

    val `If your circumstances change...`: Message = Message(
      english = "Contact HMRC on <strong>0300 123 1813</strong> if anything changes that you think affects your payment plan.",
      welsh   = "Cysylltwch â CThEF ar <strong>0300 200 1900</strong> os oes unrhyw beth yn newid ac rydych o’r farn ei fod yn effeithio ar eich cynllun talu."
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

      }
    }

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

    val `Select which email address you want to use`: Message = Message(
      english = "Select which email address you want to use",
      welsh   = "Pa e-bost rydych am ei ddefnyddio?"
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

    val `HMRC will send you a letter within 5 working days with your payment dates.`: Message = Message(
      english = "HMRC will send you a letter within 5 working days with your payment dates.",
      welsh   = "Bydd CThEF yn anfon llythyr atoch cyn pen 5 diwrnod gwaith gyda’ch dyddiadau talu."
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

    val `Payment reference`: Message = Message(
      english = "Payment reference",
      welsh   = "Cyfeirnod y taliad"
    )

  }

  object GiveFeedback {

    val `Do you want to give feedback?`: Message = Message(
      english = "Do you want to give feedback on this service?",
      welsh   = "A ydych am roi adborth am y gwasanaeth hwn?"
    )

    val `If you select no`: Message = Message(
      english = "If you select no, you will be directed to GOV.UK.",
      welsh   = "Os ydych yn dewis ‘Na’, byddwch yn cael eich cyfeirio at GOV.UK."
    )

    val `Select yes if you want to give feedback`: Message = Message(
      english = "Select yes if you want to give feedback on this service",
      welsh   = "Dewiswch ‘Iawn’ os ydych am roi adborth am y gwasanaeth hwn"
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

