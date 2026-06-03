/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import play.api.libs.json.*

enum JourneyType {
  case SinglePage, MultiPage1, MultiPage2, EmailReVerify, Bounce
}

object JourneyType {
  implicit val format: Format[JourneyType] = new Format[JourneyType] {
    def reads(json: JsValue): JsResult[JourneyType] = json match {
      case JsString("SinglePage")    => JsSuccess(SinglePage)
      case JsString("MultiPage1")    => JsSuccess(MultiPage1)
      case JsString("MultiPage2")    => JsSuccess(MultiPage2)
      case JsString("EmailReVerify") => JsSuccess(EmailReVerify)
      case JsString("Bounce")        => JsSuccess(Bounce)
      case _                         => JsError("Invalid Journey Type")
    }
    def writes(journeyType: JourneyType): JsValue = JsString(journeyType.toString)
  }
}
