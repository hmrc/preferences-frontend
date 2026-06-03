/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers.internal

import play.api.data.Form
import play.api.data.Forms._

object OptInOrOutForm {
  def apply() =
    Form[Data](
      mapping(
        "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined)
      )(Data.apply)(d => Some(d.optedIn))
    )

  case class Data(optedIn: Option[Boolean])
}

object ReOptInOrOutForm {
  def apply() =
    Form[Data](
      mapping(
        "opt-in" -> optional(boolean).verifying("sa_printing_preference.opt_in_choice_required", _.isDefined)
      )(Data.apply)(d => Some(d.optedIn))
    )

  case class Data(optedIn: Option[Boolean])
}
