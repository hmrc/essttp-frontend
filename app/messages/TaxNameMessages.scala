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

package messages

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
