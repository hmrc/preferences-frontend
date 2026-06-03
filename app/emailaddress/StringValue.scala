/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.emailaddress

object StringValue {
  implicit def stringValueToString(e: StringValue): String = e.value
}

trait StringValue {
  def value: String
  override def toString: String = value
}
