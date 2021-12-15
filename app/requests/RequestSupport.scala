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

package requests

import langswitch.Language
import play.api.i18n._
import play.api.mvc.Request
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

object RequestSupport {
  def isLoggedIn(implicit request: Request[_]): Boolean = request.session.get(SessionKeys.authToken).isDefined

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HcProvider.headerCarrier

  /**
   * This is because we want to give responsibility of creation of [[HeaderCarrier]] to the platform code.
   * If they refactor how hc is created our code will pick it up automatically.
   */
  private object HcProvider extends FrontendHeaderCarrierProvider {
    def headerCarrier(implicit request: Request[_]): HeaderCarrier = hc(request)
  }

  implicit def language(implicit messages: Messages): Language = Language(messages.lang)
}
