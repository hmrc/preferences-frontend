/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package views.sa.prefs.helpers

import java.time.LocalDate
import play.twirl.api.Html

import java.time.format.DateTimeFormatter

object DateFormat {

  private val longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def longDateFormat(date: Option[LocalDate]): Option[Html] = date.map(d => Html(longDateFormatter.format(d)))
}
