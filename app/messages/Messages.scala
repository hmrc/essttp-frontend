/*
 * Copyright 2021 HM Revenue & Customs
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

object Messages {
  val gov_uk: Message = Message(
    english = "GOV.UK",
    welsh = "GOV.UK")

  val dash: Message = Message(
    english = " - ",
    welsh = " - ")

  val generic_service_name: Message = Message(
    english = "Pay your tax",
    welsh = "Talwch eich treth")

  val sign_out: Message = Message(
    english = "Sign out",
    welsh = "Allgofnodi")

  val error: Message = Message(
    english = "Error: ",
    welsh = "Gwall: ")

  val generic_continue: Message = Message(
    english = "Continue",
    welsh = "Yn eich blaen")

  val generic_cancel: Message = Message(
    english = "Cancel",
    welsh = "Canslo")

  val generic_back: Message = Message(
    english = "Back",
    welsh = "Yn ôl")

  val generic_sign_out: Message = Message(
    english = "Sign Out",
    welsh = "Allgofnodi")

  val generic_amount: Message = Message(
    english = "Amount",
    welsh = "Swm")

  val generic_date: Message = Message(
    english = "Date",
    welsh = "Dyddiad")

  val generic_tax: Message = Message(
    english = "Tax",
    welsh = "Treth")

  val error_header: Message = Message(
    english = "There is a problem",
    welsh = "Mae problem wedi codi")

  val to: Message = Message(
    english = "to",
    welsh = "i")

  val paymentSuccess_title: Message = Message(
    english = "Payment complete",
    welsh = "Taliad wedi’i wneud")

  val paymentSuccess_panelContent: Message = Message(
    english = "Your payment reference is",
    welsh = "Cyfeirnod eich taliad yw")

  val paymentSuccess_printLink: Message = Message(
    english = "Print or download a copy of your payment confirmation",
    welsh = "Argraffu neu lawrlwytho copi o gadarnhad o’ch taliad")

  val paymentSuccess_h2: Message = Message(
    english = "What happens next",
    welsh = "Yr hyn sy’n digwydd nesaf")

  val paymentSuccess_para: Message = Message(
    english = "Your payment will take 3 to 5 days to show in your online tax account.",
    welsh = "Bydd eich taliad yn cymryd 3 i 5 diwrnod i ymddangos yn eich cyfrif treth ar-lein.")

  val paymentVerified_title: Message = Message(
    english = "Payment processing ",
    welsh = "Wrthi'n prosesu taliad")

  val paymentVerified_para: Message = Message(
    english = "Your payment is scheduled to complete in the next 2 hours. It will take 3 to 5 days to show in your online tax account.",
    welsh = "Disgwylir i’ch taliad gael ei wneud yn ystod y 2 awr nesaf. Bydd yn cymryd 3 i 5 diwrnod i ymddangos yn eich cyfrif treth ar-lein.")

  val paymentUnsuccessful_title: Message = Message(
    english = "Payment unsuccessful",
    welsh = "Taliad wedi methu")

  val paymentUnsuccessful_p1: Message = Message(
    english = "Your payment has not been made.",
    welsh = "Nid yw’ch taliad wedi’i wneud.")

  val paymentUnsuccessful_p2: Message = Message(
    english = "No payment has been taken from your account.",
    welsh = "Nid oes taliad wedi cael ei gymryd o’ch cyfrif.")

  val paymentUnsuccessful_button: Message = Message(
    english = "Start again",
    welsh = "Dechrau eto")

  val paymentUnsuccessful_link: Message = Message(
    english = "Pay another way",
    welsh = "Talu gan ddefnyddio dull arall")

  val paymentCancelled_title: Message = Message(
    english = "Payment cancelled",
    welsh = "Taliad wedi’i ganslo")

  val paymentCancelled_p1: Message = Message(
    english = "You have cancelled your payment.",
    welsh = "Rydych wedi canslo’ch taliad.")

  val paymentCompleteEmail: Message = Message(
    english = "We have sent a confirmation email to ",
    welsh = "Rydym wedi anfon e-bost cadarnhau atoch at ")

  val feedback_header: Message = Message(
    english = "Help us improve our services",
    welsh = "Helpu ni i wella ein gwasanaethau")

  val feedback_para: Message = Message(
    english = "We use your feedback to make our services better.",
    welsh = "Rydym yn defnyddio’ch adborth i wella ein gwasanaethau.")

  val feedback_link: Message = Message(
    english = "Tell us what you think of this service",
    welsh = "Rhowch wybod i ni beth yw eich barn am y gwasanaeth hwn")

  val feedback_link_suffix: Message = Message(
    english = "(takes 30 seconds)",
    welsh = "(mae’n cymryd 30 eiliad)")

  val thereIsAProblem_heading: Message = Message(
    english = "Sorry, there is a problem with this service",
    welsh = "Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth hwn")

  val thereIsAProblem_p1: Message = Message(
    english = "Try again in a few minutes.",
    welsh = "Rhowch gynnig arall arni mewn ychydig o funudau.")

  val thereIsAProblem_button: Message = Message(
    english = "Try again",
    welsh = "Rhowch gynnig arall arni  ")

  val pageNotFound_title: Message = Message(
    english = "Page not found - Pay your tax - GOV.UK",
    welsh = "Heb ddod o hyd i’r dudalen - Talwch eich treth - GOV.UK")

  val pageNotFound_header: Message = Message(
    english = "Page not found",
    welsh = "Heb ddod o hyd i’r dudalen")

  val pageNotFound_p1_text: Message = Message(
    english = "If you typed the web address, check it is correct.",
    welsh = "Os gwnaethoch deipio’r cyfeiriad gwe, dylech wirio ei fod yn gywir.")

  val pageNotFound_p2_text: Message = Message(
    english = "If you pasted the web address, check you copied the entire address.",
    welsh = "Os gwnaethoch bastio’r cyfeiriad gwe, dylech wirio’ch bod wedi copïo’r cyfeiriad yn llawn.")

  val pageNotFound_p3_text_part1: Message = Message(
    english = "If the web address is correct or you selected a link or button, ",
    welsh = "Os yw’r cyfeiriad gwe yn gywir, neu os dewisoch gysylltiad neu fotwm, ")

  val pageNotFound_p3_link: Message = Message(
    english = "contact the Payments Support Service",
    welsh = "cysylltwch â’r llinell gymorth ar gyfer taliadau")

  val pageNotFound_p3_text_part2: Message = Message(
    english = " if you need to speak to someone about making a payment.",
    welsh = " os oes angen i chi siarad â rhywun am wneud taliad.")

  val pageNotFound_p4_text: Message = Message(
    english = "Find out about ",
    welsh = "Rhagor o wybodaeth am ")

  val pageNotFound_p4_link: Message = Message(
    english = "other ways to pay",
    welsh = "ddulliau eraill o dalu")

  val pageNotFound_p4_text_period: Message = Message(
    english = ".",
    welsh = ".")

  val footer_cookies: Message = Message(
    english = "Cookies",
    welsh = "Cwcis")

  val footer_privacyNotice: Message = Message(
    english = "Privacy",
    welsh = "Preifatrwydd")

  val footer_termsAndConditions: Message = Message(
    english = "Terms and conditions",
    welsh = "Telerau ac Amodau")

  val footer_helpUsingGovUk: Message = Message(
    english = "Help using GOV.UK",
    welsh = "Help wrth ddefnyddio GOV.UK")

  val footer_accessibilityStatement: Message = Message(
    english = "Accessibility statement",
    welsh = "Datganiad hygyrchedd")

}

object TaxNameMessages {
  val self_assessment_tax_name: Message = Message(
    english = "Self Assessment",
    welsh = "Hunanasesiad")

  val epaye_tax_name: Message = Message(
    english = "Employers’ PAYE and National Insurance",
    welsh = "TWE ac Yswiriant Gwladol y Cyflogwr")

  val paye_penalty_tax_name: Message = Message(
    english = "PAYE late payment or filing penalty",
    welsh = "Am dalu neu gyflwyno TWE yn hwyr")

  val paye_interest_tax_name: Message = Message(
    english = "Employers’ PAYE interest payment",
    welsh = "Taliad llog TWE cyflogwr")

  val paye_settlement_tax_name: Message = Message(
    english = "Employers’ PAYE Settlement Agreement",
    welsh = "Cytundeb Setliad TWE y Cyflogwr")

  val paye_late_cis_tax_name: Message = Message(
    english = "Construction Industry Scheme late filing penalty",
    welsh = "Cynllun y Diwydiant Adeiladu (CIS) - cosb am dalu'n hwyr")

  val class_1a_ni_tax_name: Message = Message(
    english = "Employers’ Class 1A National Insurance",
    welsh = "Yswiriant Gwladol Dosbarth 1A y Cyflogwr")

  val vat_tax_name: Message = Message(
    english = "VAT",
    welsh = "TAW")

  val corporation_tax_tax_name: Message = Message(
    english = "Corporation Tax",
    welsh = "Treth Gorfforaeth")

  val cgt_tax_name: Message = Message(
    english = "Capital Gains Tax on UK property",
    welsh = "Treth Enillion Cyfalaf ar eiddo yn y DU")

  val simpleAssessment_tax_name: Message = Message(
    english = "Simple Assessment",
    welsh = "Asesiad Syml")

  val ni_eu_vat_oss_tax_name: Message = Message(english = "VAT One Stop Shop Union Scheme")

  val bioFuels_tax_name: Message = Message(
    english = "Duty on biofuels or gas for road use",
    welsh = "Toll ar fiodanwyddau neu ar nwy ar gyfer defnydd y ffordd" //This welsh translation might be wrong, I've requested Rachel get a fresh translation
  )

  val sdlt_tax_name: Message = Message(
    english = "Stamp Duty Land Tax",
    welsh = "Treth Dir y Tollau Stamp")

  val gbPbRgDuty_name: Message = Message(
    english = "General Betting, Pool Betting or Remote Gaming Duty",
    welsh = "Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell")

  val machineGamesDuty_name: Message = Message(
    english = "Machine Games Duty",
    welsh = "Toll Peiriannau Hapchwarae")

}

object TaxTitleMessages {
  val self_assessment_title: Message = Message(
    english = "Pay your Self Assessment",
    welsh = "Talu eich Hunanasesiad")

  val epaye_title: Message = Message(
    english = "Pay your employers’ PAYE and National Insurance",
    welsh = "Talwch eich TWE a'ch Yswiriant Gwladol y cyflogwr")

  val paye_penalty_title: Message = Message(
    english = "Pay your PAYE late payment or filing penalty",
    welsh = "Talu’ch cosb am dalu neu gyflwyno TWE yn hwyr")

  val paye_interest_title: Message = Message(
    english = "Pay employers’ PAYE interest",
    welsh = "Talu llog TWE cyflogwr")

  val paye_settlement_title: Message = Message(
    english = "Pay your PAYE Settlement Agreement",
    welsh = "Talwch eich Cytundeb Setliad TWE y cyflogwr")

  val paye_late_cis_title: Message = Message(
    english = "Pay your Construction Industry Scheme penalty",
    welsh = "Talwch eich cosb - Cynllun y Diwydiant Adeiladu")

  val class_1a_ni_title: Message = Message(
    english = "Pay your employers’ Class 1A National Insurance (P11D bill)",
    welsh = "Talu’ch Yswiriant Gwladol Dosbarth 1A y cyflogwr (bil P11D)")

  val vat_title: Message = Message(
    english = "Pay your VAT",
    welsh = "Talu eich TAW")

  val corporation_tax_title: Message = Message(
    english = "Pay your Corporation Tax",
    welsh = "Talu eich Treth Gorfforaeth")

  val bta_title: Message = Message(
    english = "Business tax account",
    welsh = "Cyfrif treth busnes")

  val cgt_title: Message = Message(
    english = "Report and pay Capital Gains Tax on UK property",
    welsh = "Rhoi gwybod am a thalu Treth Enillion Cyfalaf ar eiddo yn y DU")

  val simpleAssessment_title: Message = Message(
    english = "Pay your Simple Assessment",
    welsh = "Talu eich Asesiad Syml")

  val ni_eu_vat_oss_title: Message = Message("Submit a One Stop Shop return and pay VAT")

  val bioFuels_title: Message = Message(
    english = "Pay duty on biofuels or gas for road use",
    welsh = "Talu toll ar fiodanwyddau neu ar nwy ar gyfer defnydd y ffordd")

  val sdlt_title: Message = Message(
    english = "Pay your Stamp Duty Land Tax",
    welsh = "Talwch eich Treth Dir y Tollau Stamp")

  val gbPbRgDuty_title: Message = Message(
    english = "Pay General Betting, Pool Betting or Remote Gaming Duty",
    welsh = "Talu Toll Betio Cyffredinol, Toll Cronfa Fetio neu Doll Hapchwarae o Bell")

  val machineGamesDuty_title: Message = Message(
    english = "Pay Machine Games Duty",
    welsh = "Talu’r Doll Peiriannau Hapchwarae")

  val gamingBingoDuty_title: Message = Message(
    english = "Pay Gaming or Bingo Duty",
    welsh = "Talu Toll Hapchwarae neu Doll Bingo")
}
object DateMessages {

  //the same as dd MMMM yy
  def january(day: Int, year: String): Message = Message(
    english = s"$day January $year",
    welsh = s"$day Ionawr $year")

  def february(day: Int, year: String): Message = Message(
    english = s"$day February $year",
    welsh = s"$day Chwefror $year")

  def march(day: Int, year: String): Message = Message(
    english = s"$day March $year",
    welsh = s"$day Mawrth $year")

  def april(day: Int, year: String): Message = Message(
    english = s"$day April $year",
    welsh = s"$day Ebrill $year")

  def may(day: Int, year: String): Message = Message(
    english = s"$day May $year",
    welsh = s"$day Mai $year")

  def june(day: Int, year: String): Message = Message(
    english = s"$day June $year",
    welsh = s"$day Mehefin $year")

  def july(day: Int, year: String): Message = Message(
    english = s"$day July $year",
    welsh = s"$day Gorffenn $year")

  def august(day: Int, year: String): Message = Message(
    english = s"$day August $year",
    welsh = s"$day Awst $year")

  def september(day: Int, year: String): Message = Message(
    english = s"$day September $year",
    welsh = s"$day Medi $year")

  def october(day: Int, year: String): Message = Message(
    english = s"$day October $year",
    welsh = s"$day Hydref $year")

  def november(day: Int, year: String): Message = Message(
    english = s"$day November $year",
    welsh = s"$day Tachwedd $year")

  def december(day: Int, year: String): Message = Message(
    english = s"$day December $year",
    welsh = s"$day Rhagfyr $year")

}
