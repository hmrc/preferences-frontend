/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model
import play.api.libs.json.*

enum PageType {
  case IPage, ReOptInPage, AndroidOptInPage, AndroidReOptInPage, AndroidOptOutPage, AndroidReOptOutPage,
    IosOptInPage, IosReOptInPage, IosOptOutPage, IosReOptOutPage, CYSConfirmPage
}

object PageType {
  implicit val format: Format[PageType] = new Format[PageType] {
    def reads(json: JsValue): JsResult[PageType] = json match {
      case JsString("IPage")               => JsSuccess(IPage)
      case JsString("ReOptInPage")         => JsSuccess(ReOptInPage)
      case JsString("AndroidOptInPage")    => JsSuccess(AndroidOptInPage)
      case JsString("AndroidReOptInPage")  => JsSuccess(AndroidReOptInPage)
      case JsString("AndroidOptOutPage")   => JsSuccess(AndroidOptOutPage)
      case JsString("AndroidReOptOutPage") => JsSuccess(AndroidReOptOutPage)
      case JsString("IosOptInPage")        => JsSuccess(IosOptInPage)
      case JsString("IosReOptInPage")      => JsSuccess(IosReOptInPage)
      case JsString("IosOptOutPage")       => JsSuccess(IosOptOutPage)
      case JsString("IosReOptOutPage")     => JsSuccess(IosReOptOutPage)
      case JsString("CYSConfirmPage")      => JsSuccess(CYSConfirmPage)
      case _                               => JsError("Invalid Page Type")
    }

    def writes(pageType: PageType): JsValue = JsString(pageType.toString)
  }
}
