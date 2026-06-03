/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
