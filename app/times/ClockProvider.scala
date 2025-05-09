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

package times

import java.time._

class ClockProvider {

  /** Get's the clock instance, which can be overriden in session (for testing purposes)
    */
  given getClock: Clock = defaultClock

  def now(): LocalDateTime = LocalDateTime.now(getClock)
  def nowDate(): LocalDate = LocalDate.now(getClock)

  protected val defaultClock: Clock = Clock.systemDefaultZone.withZone(ZoneOffset.UTC)

}
