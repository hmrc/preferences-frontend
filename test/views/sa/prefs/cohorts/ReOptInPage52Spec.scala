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

package views.sa.prefs.cohorts

import controllers.internal.EmailForm.Data
import controllers.internal.{ EmailForm, routes }
import helpers.TestFixtures
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.SpecBase
import views.html.sa.prefs.cohorts.reoptin_page52

class ReOptInPage52Spec extends SpecBase {

  "view" should {

    "render the contents correctly" when {
      "form has no error" in {
        val form = EmailForm()
        val submitCall = routes.ActivationController.preferences()

        val view: reoptin_page52 = app.injector.instanceOf[reoptin_page52]

        implicit val hostContext: HostContext = TestFixtures.sampleHostContext

        val viewHtml: Document = Jsoup.parse(view.apply(form, submitCall).body)

        viewHtml.getElementsByClass("govuk-heading-xl").get(0).text() mustBe messages("reoptin_page52.fg_page.title")
        viewHtml.getElementById("message-after-email-input-para-2").text() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_2"
        )

        viewHtml.getElementById("message-after-email-input-para-3").text() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_3"
        )

        viewHtml.getElementById("submitEmailButton").text() mustBe messages("reoptin_page52.submitButton")
      }

      "form has error" in {
        val form = EmailForm().withError("accept-tc", "error occurred")
        val submitCall = routes.ActivationController.preferences()

        val view: reoptin_page52 = app.injector.instanceOf[reoptin_page52]

        implicit val hostContext: HostContext = TestFixtures.sampleHostContext

        val viewHtml: Document = Jsoup.parse(view.apply(form, submitCall).body)

        viewHtml.getElementsByClass("govuk-heading-xl").get(0).text() mustBe messages("reoptin_page52.fg_page.title")
        viewHtml.getElementById("message-after-email-input-para-2").text() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_2"
        )

        viewHtml.getElementById("message-after-email-input-para-3").text() mustBe messages(
          "reoptin_page52.fg_page.yes_send_email_info_3"
        )

        viewHtml.getElementById("submitEmailButton").text() mustBe messages("reoptin_page52.submitButton")

        viewHtml.html() must include("error occurred")
      }
    }
  }
}
