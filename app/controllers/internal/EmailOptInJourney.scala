/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

object EmailOptInJourney extends Enumeration {
  type Journey = Value
  val Interstitial, AccountDetails = Value
}
