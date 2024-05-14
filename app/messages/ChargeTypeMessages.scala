/*
 * Copyright 2024 HM Revenue & Customs
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

import essttp.rootmodel.ttp.eligibility.MainTrans

object ChargeTypeMessages {

  val chargeFromMTrans: Map[MainTrans, Message] = Map(
    MainTrans("5060") -> Message(english = "Revenue assessment", welsh = "Asesiad refeniw"),
    MainTrans("4910") -> Message(english = "Balancing payment", welsh = "Taliad mantoli"),
    MainTrans("5050") -> Message(english = "Revenue Determination", welsh = "Penderfyniad refeniw"),
    MainTrans("4950") -> Message(english = "Daily penalty", welsh = "Cosb ddyddiol"),
    MainTrans("4990") -> Message(english = "Partnership daily penalty", welsh = "Cosb ddyddiol i bartneriaeth"),
    MainTrans("5210") -> Message(english = "Enquiry Amendment", welsh = "Diwygiad ymholiad"),
    MainTrans("4920") -> Message(english = "First payment on account", welsh = "Taliad ar gyfrif cyntaf"),
    MainTrans("4930") -> Message(english = "Second payment on account", welsh = "Ail daliad ar gyfrif"),
    MainTrans("5190") -> Message(english = "Enquiry Amendment", welsh = "Diwygiad ymholiad"),
    MainTrans("4960") -> Message(english = "6 month late filing penalty", welsh = "Cosb am gyflwyno 6 mis yn hwyr"),
    MainTrans("4970") -> Message(english = "12 month late filing penalty", welsh = "Cosb am gyflwyno 12 mis yn hwyr"),
    MainTrans("5010") -> Message(english = "Partnership 6 months late filing penalty", welsh = "Cosb i bartneriaeth am gyflwyno 6 mis yn hwyr"),
    MainTrans("5020") -> Message(english = "Partnership 12 months late filing penalty", welsh = "Cosb i bartneriaeth am gyflwyno 12 mis yn hwyr"),
    MainTrans("6010") -> Message(english = "Late Payment interest", welsh = "Llog ar daliadau hwyr"),
    MainTrans("5110") -> Message(english = "30 days late payment penalty", welsh = "Cosb am dalu 30 diwrnod yn hwyr"),
    MainTrans("5120") -> Message(english = "6 months late payment penalty", welsh = "Cosb am dalu 6 mis yn hwyr"),
    MainTrans("5130") -> Message(english = "12 months late payment penalty", welsh = "Cosb am dalu 12 mis yn hwyr"),
    MainTrans("5080") -> Message(english = "Penalty", welsh = "Cosb"),
    MainTrans("5100") -> Message(english = "Amount no longer included in Tax Code", welsh = "Swm sydd heb ei gynnwys yn y Cod Treth mwyach"),
    MainTrans("5070") -> Message(english = "Repayment supplement", welsh = "Atodiad ad-daliad"),
    MainTrans("5140") -> Message(english = "First penalty for late tax return", welsh = "Cosb gyntaf ar gyfer Ffurflen Dreth hwyr"),
    MainTrans("4940") -> Message(english = "First penalty for late tax return", welsh = "Cosb gyntaf ar gyfer Ffurflen Dreth hwyr"),
    MainTrans("5150") -> Message(english = "Second penalty for late tax return", welsh = "Ail gosb ar gyfer Ffurflen Dreth hwy"),
    MainTrans("5160") -> Message(english = "First penalty for late partnership tax return", welsh = "Cosb gyntaf ar gyfer Ffurflen Dreth Partneriae hwyr"),
    MainTrans("4980") -> Message(english = "First penalty for late partnership tax return", welsh = "Cosb gyntaf ar gyfer Ffurflen Dreth Partneriae hwyr"),
    MainTrans("5170") -> Message(english = "Second penalty for late partnership tax return", welsh = "Ail gosb ar gyfer Ffurflen Dreth Partneriaeth hwr"),
    MainTrans("5200") -> Message(english = "Tax return amendment", welsh = "Diwygiad i'r Ffurflen Dreth"),
    MainTrans("5071") -> Message(english = "Repayment", welsh = "Ad-daliad"),
    MainTrans("5180") -> Message(english = "Enquiry amendment", welsh = "Diwygiad ymholiad"),
    MainTrans("5090") -> Message(english = "Amount no longer included in tax code", welsh = "Swm sydd heb ei gynnwys yn y cod treth mwyach"),
    MainTrans("5030") -> Message(english = "First surcharge for late payment", welsh = "Gordal cyntaf ar gyfer taliad hwyr"),
    MainTrans("5040") -> Message(english = "Second surcharge for late payment", welsh = "Ail ordal ar gyfer taliad hwyr"),
    MainTrans("5073") -> Message(english = "Transfer to OAS", welsh = "Trosglwyddo i OAS"),
    MainTrans("4000") -> Message(english = "HMRC adjustment", welsh = "Addasiad gan CThEF"),
    MainTrans("4001") -> Message(english = "HMRC adjustment", welsh = "Addasiad gan CThEF"),
    MainTrans("4002") -> Message(english = "HMRC adjustment", welsh = "Addasiad gan CThEF"),
    MainTrans("4003") -> Message(english = "HMRC adjustment", welsh = "Addasiad gan CThEF"),
    MainTrans("4026") -> Message(english = "ITSA Penalty Interest", welsh = "Llog ar gosb ITSA")
  )

}
