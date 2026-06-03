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

import controllers.{ EMPTY_STRING, internal }
import controllers.internal.OptInEmailForm
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.{ Document, Element }
import play.api.data.Form
import utils.SpecBase
import views.html.sa.prefs.sa_printing_preference_optin_email

class SaPrintingPreferenceOptinEmailSpec extends SpecBase {

  "view" should {
    "render the contents correctly" when {
      "form has no error" in {
        val view: sa_printing_preference_optin_email = app.injector.instanceOf[sa_printing_preference_optin_email]

        implicit val defaultHostContext: HostContext = hostContext()

        val emailId = "test@test.com"
        val form: Form[String] = OptInEmailForm.apply().fill(emailId)
        val callInput = internal.paperless.routes.OptInController.submitOptInEmail(Some(false), defaultHostContext)

        implicit val viewHtml: Document = Jsoup.parse(view.apply(form, callInput).body)

        shouldContainCorrectQuestionLabel
        shouldContainInputText
        shouldContainCorrectHintText
        shouldContainCorrectSubmitButtonDetails
        shouldNotContainErrorMsg
      }

      "form has error" in {
        val view: sa_printing_preference_optin_email = app.injector.instanceOf[sa_printing_preference_optin_email]

        implicit val defaultHostContext: HostContext = hostContext()

        val form: Form[String] = OptInEmailForm.apply().withError("error.message", "error occurred")
        val callInput = internal.paperless.routes.OptInController.submitOptInEmail(Some(false), defaultHostContext)

        implicit val viewHtml: Document = Jsoup.parse(view.apply(form, callInput).body)

        shouldContainCorrectQuestionLabel
        shouldContainInputText
        shouldContainCorrectHintText
        shouldContainCorrectSubmitButtonDetails
        shouldContainErrorMsg
      }
    }
  }

  private def shouldContainCorrectQuestionLabel(implicit viewHtml: Document) = {
    val questionLabel: Element = viewHtml.getElementsByTag("h1").get(0)
    questionLabel.getElementsByClass("govuk-heading-l").text() mustBe messages(
      "sa_printing_preference.sps_opt_in_email"
    )
  }

  private def shouldContainInputText(implicit viewHtml: Document) =
    viewHtml.getElementById("sps-opt-in-email").attributes().get("type") mustBe messages(
      "sa_printing_preference.sps_opt_in_email_label"
    )

  private def shouldContainCorrectHintText(implicit viewHtml: Document) = {
    val hintText = viewHtml.getElementsByClass("govuk-hint")
    hintText.text() mustBe messages("sa_printing_preference.sps_opt_in_email_hint")
  }

  private def shouldContainCorrectSubmitButtonDetails(implicit viewHtml: Document) = {
    val submitButton = viewHtml.getElementById("submitEmailButton")
    submitButton.text() mustBe messages("sa_printing_preference.submitButton")
  }

  private def shouldContainErrorMsg(implicit viewHtml: Document) = {
    val errorSummaryMsgHeading: Element = viewHtml.getElementsByClass("govuk-error-summary__title").get(0)
    errorSummaryMsgHeading.text() mustBe messages("sa_printing_preference.sps_opt_in_choice.problem")

    val ulElement: Element = viewHtml.getElementsByClass("govuk-error-summary__list").get(0)
    ulElement.getElementsByTag("li").text() must be("error occurred")
  }

  private def shouldNotContainErrorMsg(implicit viewHtml: Document) =
    viewHtml.getElementsByClass("govuk-error-summary__title").size() must be(0)
}
