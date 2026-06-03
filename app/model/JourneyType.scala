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
