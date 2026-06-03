/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.abtest

trait Cohort {
  def name: String

  override def toString = name
}
