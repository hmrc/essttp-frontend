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

package testOnly

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Request, Session}
import testOnly.models.AuthToken
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoginService @Inject() (httpClient: HttpClient)(implicit ec: ExecutionContext) {

  //  def logIn(tu: TestUser)(implicit request: Request[_]): Future[Session] = for {
  //    authToken <- callAuthLoginApi(tu)
  //  } yield buildSession(at)
  //
  //  private def buildSession(authToken: AuthToken)(implicit request: Request[_]) =
  //    Session(Map(
  //      SessionKeys.sessionId -> s"session-$randomUUID",
  //      SessionKeys.authToken -> authToken.v,
  //      SessionKeys.lastRequestTimestamp -> clockProvider.getClock.millis().toString
  //    ))

}
