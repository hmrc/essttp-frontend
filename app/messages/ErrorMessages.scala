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

object ErrorMessages {

  //based on bootstrap-frontend-play-28_2.12-5.17.0
  object NotFound {
    val title: Message = Message(
      english = "Page not found - 404",
      welsh   = "Heb ddod o hyd i’r dudalen – 404"
    )
    val heading: Message = Message(
      english = "This page can’t be found",
      welsh   = "Ni ellir dod o hyd i’r dudalen hon"
    )

    val message: Message = Message(
      english = "Please check that you have entered the correct web address.",
      welsh   = "Gwiriwch eich bod wedi nodi’r cyfeiriad gwe cywir."
    )
  }

  object Unahthorised {
    val `You do not have access to this service`: Message = Message(
      english = "You do not have access to this service",
      welsh   = "Nid oes gennych fynediad at y gwasanaeth hwn"
    )
  }

  object Gone {
    //TODO: run this message through content designer
    val `The page you are referring does not exist anymore`: Message = Message(
      english = "The page you are referring does not exist anymore"
    )
  }

  val `choose an option`: Message = Message(
    english = "Select whether to manage your accounts or track a VAT repayment",
    welsh   = "Dewiswch a ydych am reoli’ch cyfrifon neu olrhain ad-daliad TAW"
  )

  val `general error title`: Message = Message(
    english = "Sorry, there is a problem with the service",
    welsh   = "Mae’n ddrwg gennym – mae problem gyda’r gwasanaeth"
  )

  val `try again later`: Message = Message(
    english = "Try again later.",
    welsh   = "Rhowch gynnig arall arni yn nes ymlaen."
  )

  val `Error`: Message = Message(
    english = "Error"
  )

}
