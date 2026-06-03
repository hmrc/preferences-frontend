/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

trait CohortValues[C <: Cohort] {
  def availableValues: List[C]

  def cohorts: Cohorts[C] = enabledCohorts

  protected lazy val enabledCohorts: Cohorts[C] = {
    val cohorts = availableValues.filter(isEnabled)
    Cohorts(
      cohorts.headOption.getOrElse(throw new IllegalArgumentException("No cohorts are enabled")),
      cohorts.tail*
    )
  }

  def isEnabled(cohort: C): Boolean
}
