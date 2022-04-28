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

package error

import com.google.inject.Inject
import messages.ErrorMessages
import play.api.mvc.Results.{NotFound, Unauthorized}
import play.api.mvc.{Request, Result, Results}
import requests.RequestSupport

class ErrorResponses @Inject() (errorHandler: ErrorHandler, requestSupport: RequestSupport) {

  import requestSupport._

  def notFound(implicit request: Request[_]): Result = NotFound(
    errorHandler.standardErrorTemplate(
      ErrorMessages.NotFound.title.show,
      ErrorMessages.NotFound.heading.show,
      ErrorMessages.NotFound.message.show
    )
  )

  def unauthorised()(implicit request: Request[_]): Result = Unauthorized(
    errorHandler.standardErrorTemplate(
      ErrorMessages.Unahthorised.`You do not have access to this service`.show,
      ErrorMessages.Unahthorised.`You do not have access to this service`.show,
      ErrorMessages.Unahthorised.`You do not have access to this service`.show
    )
  )

  def gone()(implicit request: Request[_]): Result = Results.Gone(
    errorHandler.standardErrorTemplate(
      ErrorMessages.Gone.`The page you are referring does not exist anymore`.show,
      ErrorMessages.Gone.`The page you are referring does not exist anymore`.show,
      ErrorMessages.Gone.`The page you are referring does not exist anymore`.show
    )
  )

}
