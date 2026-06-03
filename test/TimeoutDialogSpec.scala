/*
 * Copyright 2026 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import utils.SpecBase
import views.html.timeout_dialog

class TimeoutDialogSpec extends SpecBase {

  "view" should {
    "render the content correctly" in {
      val view: timeout_dialog = app.injector.instanceOf[timeout_dialog]

      val viewHtml: Document = Jsoup.parse(view.apply().body)

      val metaTagValue: Elements = viewHtml.getElementsByTag("meta")

      metaTagValue.attr("data-title") mustBe messages("timeout.title")
      metaTagValue.attr("data-keep-alive-url") mustBe "/session/keep-alive"
      metaTagValue.attr("data-sign-out-url") mustBe "/paperless/timeout"
      metaTagValue.attr("data-message") mustBe messages("timeout.message")
      metaTagValue.attr("data-keep-alive-button-text") mustBe messages("timeout.keep-alive-button")
    }
  }
}
