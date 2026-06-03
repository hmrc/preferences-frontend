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

package controllers

import uk.gov.hmrc.auth.core.{ AffinityGroup, ConfidenceLevel }
import uk.gov.hmrc.auth.core.retrieve.{ LoginTimes, ~ }

import java.time.{ Instant, ZoneOffset, ZonedDateTime }
import scala.concurrent.Future

trait AuthRetrievalsSetup {
  type AuthRetrievals =
    LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel

  val currentLogin: Instant = ZonedDateTime.of(2015, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant
  val previousLogin: Instant = ZonedDateTime.of(2012, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant

  val retrievalResult: Future[LoginTimes ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ ConfidenceLevel] =
    Future.successful(
      new ~(
        new ~(
          new ~(
            new ~(
              LoginTimes(currentLogin, Some(previousLogin)),
              Option.empty[String]
            ),
            Some("1234567890")
          ),
          Some(AffinityGroup.Individual)
        ),
        ConfidenceLevel.L200
      )
    )
}
