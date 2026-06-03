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

package views

import controllers.internal.IPage7
import helpers.TestFixtures
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.SpecBase
import utils.TestData.TEST_EMAIL_VALUE
import views.html.account_details_printing_preference_confirm

class AccountDetailsPrintingPreferenceConfirmSpec extends SpecBase {

  "view" should {

    "render the content correctly" in {
      val view: account_details_printing_preference_confirm =
        app.injector.instanceOf[account_details_printing_preference_confirm]

      implicit val hostContext: HostContext = TestFixtures.sampleHostContext

      val viewHtml: Document = Jsoup.parse(view.apply(IPage7, Some(EmailAddress("test@gmail.com"))).body)

      viewHtml.getElementsByTag("h1").html() mustBe messages("sa_printing_preference_confirm.heading")

      val elements: Elements = viewHtml.getElementsByClass("govuk-body")

      elements.get(0).text() mustBe messages("sa_printing_preference_confirm.paragraph", "test@gmail.com")
      elements.get(1).text() mustBe messages("manage.paperless.check.settings.linkText")
    }
  }
}
