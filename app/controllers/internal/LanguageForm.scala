/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import play.api.data.Forms._
import play.api.data._

object LanguageForm {
  def apply() =
    Form[Data](
      mapping(
        "language" -> optional(boolean)
      )(Data.apply)(d => Option(d.language))
    )

  case class Data(language: Option[Boolean])
}
