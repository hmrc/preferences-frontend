/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package util

import java.time.Instant

trait DateTimeUtils {
  def now: Instant = Instant.now()
}

object DateTimeUtils extends DateTimeUtils
