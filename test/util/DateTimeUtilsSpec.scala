/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package util

import utils.SpecBase

import java.time.Instant

class DateTimeUtilsSpec extends SpecBase {

  "now" should {
    "return the time in Instant" in {
      DateTimeUtils.now mustBe a[Instant]
    }
  }
}
