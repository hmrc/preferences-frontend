/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model

import play.api.libs.json.*

enum Language(val entryName: String) {
  case English extends Language("en")
  case Welsh extends Language("cy")
}

object Language {
  implicit val format: Format[Language] = new Format[Language] {
    def reads(json: JsValue): JsResult[Language] = json match {
      case JsString("cy") => JsSuccess(Welsh)
      case _              => JsSuccess(English)
    }

    def writes(language: Language): JsValue = JsString(language.entryName)
  }
}
