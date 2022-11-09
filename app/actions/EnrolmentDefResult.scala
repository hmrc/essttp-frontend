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

package actions

sealed trait EnrolmentDefResult[T]

object EnrolmentDefResult {

  final case class Success[T](t: T) extends EnrolmentDefResult[T]

  final case class Inactive[T]() extends EnrolmentDefResult[T]

  final case class EnrolmentNotFound[T]() extends EnrolmentDefResult[T]

  final case class IdentifierNotFound[T](enrolmentDefs: Set[EnrolmentDef]) extends EnrolmentDefResult[T]

  implicit class EnrolmentDefResultOps[T](private val e: EnrolmentDefResult[T]) extends AnyVal {

    def isSuccess: Boolean = e match {
      case Success(_)            => true
      case Inactive()            => false
      case EnrolmentNotFound()   => false
      case IdentifierNotFound(_) => false
    }

    def map[U](f: T => U): EnrolmentDefResult[U] = e match {
      case Success(t)            => Success(f(t))
      case Inactive()            => Inactive()
      case EnrolmentNotFound()   => EnrolmentNotFound()
      case IdentifierNotFound(d) => IdentifierNotFound(d)
    }

  }

}
