/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package object controllers {
  val EMPTY_STRING = ""
  val REGIME_ITSA = "itsa"

  object PageIdentifier extends Enumeration {
    type PageIdentifier = Value
    val OptInConfirmation, OptOutConfirmation, OptInMail = Value
  }
}
