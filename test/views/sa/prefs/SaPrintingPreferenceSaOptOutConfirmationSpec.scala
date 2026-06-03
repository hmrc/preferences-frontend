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

package views.sa.prefs

import controllers.internal.routes
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.SpecBase
import views.html.sa.prefs.sa_printing_preference_sa_opt_out_confirmation

class SaPrintingPreferenceSaOptOutConfirmationSpec extends SpecBase {

  "view" should {
    "render the content correctly" when {
      "the form is ITSA" in {
        val view: sa_printing_preference_sa_opt_out_confirmation =
          app.injector.instanceOf[sa_printing_preference_sa_opt_out_confirmation]

        implicit val hostContext: HostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )

        val viewHtml: Document = Jsoup.parse(view.apply(true, routes.ActivationController.preferences()).body)

        viewHtml.getElementsByClass("govuk-heading-l").html() mustBe messages(
          "sa_printing_preference.sps.opt_out_by_post_confimation_title"
        )

        viewHtml.getElementsByClass("govuk-link").html() mustBe messages(
          "sa_printing_preference.sps.opt_out_by_post_confimation_link"
        )

        viewHtml.getElementById("submitEmailButton").html() mustBe messages("reoptin_page54.submitButton")
      }

      "the form is not ITSA" in {
        val view: sa_printing_preference_sa_opt_out_confirmation =
          app.injector.instanceOf[sa_printing_preference_sa_opt_out_confirmation]

        implicit val hostContext: HostContext = HostContext(
          returnUrl = "someReturnUrl",
          returnLinkText = "someReturnLinkText"
        )

        val viewHtml: Document = Jsoup.parse(view.apply(false, routes.ActivationController.preferences()).body)

        viewHtml.getElementsByClass("govuk-panel__title").html() mustBe messages(
          "sa_printing_preference.sps.opt_out_by_post_confimation_title"
        )

        viewHtml.getElementsByClass("govuk-link").html() mustBe messages(
          "sa_printing_preference.sps.opt_out_by_post_confimation_link"
        )

        viewHtml.getElementById("submitEmailButton").html() mustBe messages("reoptin_page54.submitButton")
      }
    }
  }
}
