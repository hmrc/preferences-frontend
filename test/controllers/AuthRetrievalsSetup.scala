/*
 * Copyright 2025 HM Revenue & Customs
 *
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
