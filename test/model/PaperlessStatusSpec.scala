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

import helpers.Resources
import model.Category.ActionRequired
import model.StatusName.EmailNotVerified
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsString, JsValue, Json }

class PaperlessStatusSpec extends PlaySpec {

  val paperlessStatusJson: JsValue = Resources.readJson("PaperlessStatusNotVerified.json")

  val paperlessStatusModel = StatusWithUrl(
    PaperlessStatus(
      EmailNotVerified,
      ActionRequired,
      "By post, until you verify your email address",
      "You have not verified your email address."
    ),
    Url(
      "http://localhost:9024/paperless/email-re-verify?returnUrl=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&" +
        "returnLinkText=VYBxyuFWQBQZAGpe5tSgmw%3D%3D&email=e9zVPuZh0zbnXYIdEw1gz%2FYGFxquqdJgiJJN8WJGzOQ%3D",
      "Fix this"
    )
  )

  "PaperlessStatus model" should {
    "Serialise into the correct PaperlessStatus json structure" in {
      Json.toJson(paperlessStatusModel) mustBe paperlessStatusJson
    }

    "Deserialise into a PaperlessStatus Model" in {
      paperlessStatusJson.as[StatusWithUrl] mustBe paperlessStatusModel
    }
  }

  "model.Category" should {
    "Serialise into the correct json structure" in {
      Category.values.map(name => Json.toJson(name)).toList mustBe List(
        JsString("ACTION_REQUIRED"),
//        JsString("OPTION_AVAILABLE"),
        JsString("INFO")
      )
    }
  }
}
