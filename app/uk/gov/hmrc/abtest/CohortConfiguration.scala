/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

import com.typesafe.config.ConfigFactory

import scala.util.Try

trait CohortConfiguration[C <: Cohort] {
  self: CohortValues[C] =>

  def isEnabled(cohort: C) =
    Try(ConfigFactory.load().getBoolean(s"abTesting.cohort.${cohort.toString}.enabled")).getOrElse(false)

}
