/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package views

object ViewHelper {
  val ITSA = "itsa"

  val EMAIL_RE_VERIFY = "re-verify"
  val EMAIL_BOUNCE = "bounce"
  val RE_OPT_IN_MODIFY = "re-opt-in-modify"

  def serviceName(svcName: Option[String], regimeName: Option[String]): Option[String] =
    (svcName, regimeName) match {
      case (Some(svc), _) if svc == ITSA => Some("sa_printing_preference_svc_name_itsa")
      case (_, Some(rn)) if rn == ITSA   => Some("sa_printing_preference_svc_name_itsa")
      case (_, _)                        => svcName
    }
}
