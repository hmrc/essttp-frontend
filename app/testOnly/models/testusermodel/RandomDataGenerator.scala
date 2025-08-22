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

package testOnly.models.testusermodel

import cats.syntax.eq.*
import essttp.rootmodel.epaye.{TaxOfficeNumber, TaxOfficeReference}
import essttp.rootmodel.{Email, EmpRef, Nino, SaUtr, Vrn}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Random

object RandomDataGenerator {

  def nextEpayeRefs()(using Random): (TaxOfficeNumber, TaxOfficeReference, EmpRef) = {
    val ton = nextTaxOfficeNumber()
    val tor = nextTaxOfficeReference()
    (ton, tor, EmpRef.makeEmpRef(ton, tor))
  }

  private def nextTaxOfficeNumber()(using Random): TaxOfficeNumber       = TaxOfficeNumber(nextNumber(3))
  private def nextTaxOfficeReference()(using Random): TaxOfficeReference = TaxOfficeReference(
    s"GZ${nextNumber(5)}"
  )

  def nextEmail()(using Random): String = s"${nextForename()}.${nextSurname()}@${nextDomain()}"

  /** This was copied from direct-debit-backend
    * @return
    *   hopefully a valid Vrn
    */
  def nextVrn()(using r: Random): Vrn = {
    // Generates a valid VRN with the option of specifying specific digits for the first seven digits of the VRN
    def generateVRN(
      v1: Option[Int],
      v2: Option[Int],
      v3: Option[Int],
      v4: Option[Int],
      v5: Option[Int],
      v6: Option[Int],
      v7: Option[Int]
    ): Vrn = {
      @tailrec def calculateResidual(i: Int): Int = if (i <= 0) -i else calculateResidual(i - 97)
      val digits   = List(v1, v2, v3, v4, v5, v6, v7).map(_.getOrElse(r.nextInt(9)))
      val sum      = digits.zipWithIndex.foldLeft(0) { case (acc, (d, w)) => acc + d * (8 - w) }
      val residual = {
        val r = calculateResidual(sum).toString
        if (r.length === 1) r.padTo(2, '0').reverse else r
      }
      Vrn(digits.mkString("") + residual)
    }

    val firstDigit: Option[Int]  = Some(0)
    val secondDigit              = Some(1)
    val thirdDigit: Option[Int]  = Some(2)
    val fourthDigit: Option[Int] = Some(3)
    val sixthDigit: Option[Int]  = Some(4)
    generateVRN(firstDigit, secondDigit, thirdDigit, fourthDigit, None, sixthDigit, None)
  }

  def nextSaUtr()(using r: Random): SaUtr =
    SaUtrGenerator.nextSaUtr

  def nextNino()(using Random): Nino =
    Nino(s"AA${nextNumber(6)}A")

  def nextAuthorityId(): AuthorityId = AuthorityId(s"authId-${UUID.randomUUID().toString}")

  def nextForename()(using Random): String = choose(
    "Sophia",
    "Emma",
    "Olivia",
    "Ava",
    "Mia",
    "Isabella",
    "Riley",
    "Aria",
    "Zoe",
    "Charlotte",
    "Lily",
    "Layla",
    "Amelia",
    "Emily",
    "Madelyn",
    "Aubrey",
    "Adalyn",
    "Madison",
    "Chloe",
    "Harper",
    "Abigail",
    "Aaliyah",
    "Avery",
    "Evelyn",
    "Kaylee",
    "Ella",
    "Ellie",
    "Scarlett",
    "Arianna",
    "Hailey",
    "Nora",
    "Addison",
    "Brooklyn",
    "Hannah",
    "Mila",
    "Leah",
    "Elizabeth",
    "Sarah",
    "Eliana",
    "Mackenzie",
    "Peyton",
    "Maria",
    "Grace",
    "Adeline",
    "Elena",
    "Anna",
    "Victoria",
    "Camilla",
    "Lillian",
    "Jackson",
    "Aiden",
    "Lucas",
    "Liam",
    "Noah",
    "Ethan",
    "Mason",
    "Caden",
    "Oliver",
    "Elijah",
    "Grayson",
    "Jacob",
    "Michael",
    "Benjamin",
    "Carter",
    "James",
    "Jayden",
    "Logan",
    "Alexander",
    "Caleb",
    "Ryan",
    "Luke",
    "Daniel",
    "Jack",
    "William",
    "Owen",
    "Gabriel",
    "Matthew",
    "Connor",
    "Jayce",
    "Isaac",
    "Sebastian",
    "Henry",
    "Muhammad",
    "Cameron",
    "Wyatt",
    "Dylan",
    "Nathan",
    "Nicholas",
    "Julian",
    "Eli",
    "Levi",
    "Isaiah",
    "Landon",
    "David",
    "Christian",
    "Andrew",
    "Brayden",
    "John",
    "Andy",
    "Kenny",
    "Steve"
  )

  def nextSurname()(using Random): String = choose(
    "Williams",
    "Johnson",
    "Taylor",
    "Thomas",
    "Roberts",
    "Khan",
    "Lewis",
    "Jackson",
    "Clarke",
    "James",
    "Phillips",
    "Wilson",
    "Ali",
    "Mason",
    "Mitchell",
    "Rose",
    "Davis",
    "Davies",
    "Rodriguez",
    "Cox",
    "Alexander",
    "Morgan",
    "Moore",
    "Mills",
    "King",
    "Adams",
    "Garcia",
    "White",
    "Stone",
    "Edwards",
    "Watson",
    "Mallen",
    "Walker",
    "Austin",
    "Pearce",
    "Reid",
    "Simon",
    "Chung",
    "Vo"
  )

  def nextFullNameAndEmail()(using Random): (String, Email) = {
    val forename = nextForename()
    val surname  = nextSurname()
    val email    = Email(SensitiveString(s"$forename.$surname@${nextDomain()}"))
    val fullName = s"$forename $surname"
    (fullName, email)
  }

  def nextSortCode()(using Random): String = nextNumber(6)

  def nextAccountNumber()(using Random): String = nextNumber(8)

  def nextDomain()(using Random): String = choose(
    "google.test",
    "yahoo.test",
    "easy.email.test"
  )

  def nextCompanyName()(using Random): String = choose(
    "Panasoftix LTD",
    "Colm Cavanagh LTD",
    "Blom Digital LDT",
    "SAD Systems LTD",
    "Geoff Watson Limited",
    "D-Mon Tech",
    "Acme"
  )

  def nextBoolean()(using Random): Boolean = choose(true, false)

  def nextAlphanumeric(n: Int)(using r: Random): String = r.alphanumeric.take(n).mkString

  def nextHex(n: Int)(using r: Random): String =
    r.alphanumeric.filter(x => x.isDigit || "abcdefABCDEF".contains(x)).take(n).mkString

  def nextAlpha(n: Int)(using r: Random): String = r.alphanumeric.filter(_.isLetter).take(n).mkString

  /** Next n-digit number string. Values can start from '0'
    */
  def nextNumber(n: Int)(using r: Random): String = r.alphanumeric.filter(_.isDigit).take(n).mkString

  def nextIntBetween(i: Int, j: Int): Int = i + Random.nextInt(j - i + 1)

  def nextPositiveLong(n: Int)(using random: Random): Long = {
    val x = random.nextLong()
    val y = if (x < 0) -x else x
    y % n
  }

  def choose[T](s0: T, ss: T*)(using random: Random): T = {
    val choices = s0 :: ss.toList
    val choice  = random.nextInt(choices.size)
    choices.lift(choice).getOrElse(throw new IndexOutOfBoundsException)
  }

  def chooseSeq[T](seq: Seq[T])(using Random): T =
    seq.toList match {
      case Nil          => sys.error("expected at least one element to choose from but found none")
      case head :: Nil  => head
      case head :: tail => choose(head, tail*)
    }
}
