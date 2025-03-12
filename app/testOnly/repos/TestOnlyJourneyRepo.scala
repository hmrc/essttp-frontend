/*
 * Copyright 2025 HM Revenue & Customs
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

package testOnly.repos

import com.mongodb.client.model.ReplaceOptions
import essttp.journey.model.JourneyId
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.SingleObservableFuture
import testOnly.models.TestOnlyJourney
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestOnlyJourneyRepo @Inject() (mongoComponent: MongoComponent)(using ExecutionContext)
    extends PlayMongoRepository[TestOnlyJourney](
      mongoComponent,
      "test-only-journey",
      TestOnlyJourney.format,
      TestOnlyJourneyRepo.indexes(30.minutes)
    ) {

  def insert(journey: TestOnlyJourney): Future[Unit] =
    collection
      .replaceOne(
        Filters.equal("journeyId", journey.journeyId.value),
        journey,
        new ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map { updateResult =>
        if (!updateResult.wasAcknowledged())
          sys.error(s"Error inserting journey with journey id ${journey.journeyId.value}")
        else ()
      }

  def get(journeyId: JourneyId): Future[Option[TestOnlyJourney]] =
    collection
      .find(
        Filters.equal("journeyId", journeyId.value)
      )
      .headOption()

}

object TestOnlyJourneyRepo {

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending("journeyId")
    ),
    IndexModel(
      keys = Indexes.ascending("updatedAt"),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("updateAtIdx")
    )
  )

}
