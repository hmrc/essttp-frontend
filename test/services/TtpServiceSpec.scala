/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import essttp.rootmodel.{Nino, TaxRegime}
import essttp.rootmodel.ttp.eligibility.{IdType, IdValue, Identification}
import testsupport.ItSpec

class TtpServiceSpec extends ItSpec {

  val service: TtpService = app.injector.instanceOf[TtpService]

  "identificationCheck should" - {
    "not add an additional nino when one already exists" in {

      val identification = List(Identification(IdType("NINO"), IdValue("QQ123456A")))

      val additionalNino = Some(Nino("AB123456C"))

      val result = service.identificationCheck(TaxRegime.Sa, identification, additionalNino)

      result.map(_.idValue.value) shouldBe List("QQ123456A")
    }

    "add additional nino when none is present in Identification" in {
      val identification = List()

      val additionalNino = Some(Nino("AB123456C"))

      val result = service.identificationCheck(TaxRegime.Sa, identification, additionalNino)

      result.map(_.idValue.value) shouldBe List("AB123456C")
    }
  }

}
