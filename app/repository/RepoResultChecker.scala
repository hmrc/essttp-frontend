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

package repository

import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ExecutionContext, Future}

object RepoResultChecker {
  implicit class WriteResultChecker(future: Future[WriteResult]) {
    def checkResult[T](ifOkay: => T)(implicit ec: ExecutionContext): Future[T] = future.map {
      case writeResult: WriteResult if isNotReallyOk(writeResult) => throw new RuntimeException(writeResult.toString)
      case _ => ifOkay
    }

    def checkResult(implicit ec: ExecutionContext): Future[Unit] = checkResult(())
  }

  private def isNotReallyOk(writeResult: WriteResult) = !writeResult.ok || writeResult.writeErrors.nonEmpty || writeResult.writeConcernError.isDefined

}
