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

package models.enumsforforms

import enumeratum.Enum
import essttp.rootmodel.bank.{TypeOfBankAccount, TypesOfBankAccount}

import scala.collection.immutable

sealed trait TypeOfAccountFormValue extends enumeratum.EnumEntry

object TypeOfAccountFormValue extends Enum[TypeOfAccountFormValue] {
  case object Business extends TypeOfAccountFormValue

  case object Personal extends TypeOfAccountFormValue

  override def values: immutable.IndexedSeq[TypeOfAccountFormValue] = findValues

  def typeOfBankAccountAsFormValue(typeOfAccount: TypeOfBankAccount): TypeOfAccountFormValue = typeOfAccount match {
    case TypesOfBankAccount.Business => Business
    case TypesOfBankAccount.Personal => Personal
  }

  def typeOfBankAccountFromFormValue(typeOfAccountFormValue: TypeOfAccountFormValue): TypeOfBankAccount = typeOfAccountFormValue match {
    case Business => TypesOfBankAccount.Business
    case Personal => TypesOfBankAccount.Personal
  }
}
