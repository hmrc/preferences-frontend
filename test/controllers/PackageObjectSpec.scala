/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package controllers

import utils.SpecBase

class PackageObjectSpec extends SpecBase {

  "EMPTY_STRING" should {
    "return correct value" in {
      EMPTY_STRING mustBe ""
    }
  }

  "REGIME_ITSA" should {
    "return correct value of itsa regime" in {
      REGIME_ITSA mustBe "itsa"
    }
  }
}
