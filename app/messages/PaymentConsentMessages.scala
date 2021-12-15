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

object PaymentConsentMessages {
  val paymentConsent_title: Message = Message(
    english = "Approve this payment",
    welsh = "Cymeradwyo’r taliad hwn")

  val paymentConsent_reference: Message = Message(
    english = "Payment reference",
    welsh = "Cyfeirnod talu")

  val vatRegNumber: Message = Message("VAT registration number", "Rhif cofrestru TAW")
  val vatReturnPeriod: Message = Message("VAT return period", "Cyfnod cyfrifyddu TAW")

  val accountsOfficeReference: Message = Message("Accounts Office reference", "Cyfeirnod y Swyddfa Gyfrifon")
  val penaltyReferenceNumber: Message = Message("Penalty reference number", "Cyfeirnod y gosb")

  val settlementAgreementReference: Message = Message("Settlement Agreement (PSA) reference number", "Cyfeirnod Cytundeb Setliad (PSA)")
  val epayePeriod: Message = Message("Accounting period", "Cyfnod cyfrifyddu")

  val tax_year: Message = Message("Tax year", "Blwyddyn dreth")

  val niEuVatOss_vatNumber: Message = Message("VAT number")
  val niEuVatOss_period: Message = Message("Return period")

  val paymentConsent_amount: Message = Message(
    english = "Amount",
    welsh = "Swm")

  val paymentConsent_email: Message = Message(
    english = "Email address",
    welsh = "Cyfeiriad e-bost")

  val paymentConsent_p1: Message = Message(
    english = "This is a service provided by Ecospend, an authorised payment institution regulated by the Financial Conduct Authority (FCA), which will initiate a payment directly from your bank to HMRC.",
    welsh = "Darperir y gwasanaeth hwn gan Ecospend, sefydliad taliadau awdurdodedig sy’n cael ei reoli gan yr Awdurdod Ymddygiad Ariannol (FCA). Bydd y sefydliad yn trefnu taliad yn uniongyrchol o’ch banc i CThEM.")

  val paymentConsent_p2_part1: Message = Message(
    english = "By clicking approve, you will be redirected",
    welsh = "Drwy glicio ar y botwm cymeradwyo, cewch eich ailgyfeirio")

  val paymentConsent_p2_part2: Message = Message(
    english = "to securely log in and approve the payment.",
    welsh = "i fewngofnodi’n ddiogel a chymeradwyo’r taliad")

  val paymentConsent_continue: Message = Message(
    english = "Approve this payment",
    welsh = "Cymeradwyo’r taliad hwn")

}
