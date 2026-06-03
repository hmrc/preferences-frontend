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

package views.manage

import _root_.helpers.{ ConfigHelper, LanguageHelper, TestFixtures }
import connectors.EmailPreference
import controllers.auth.AuthenticatedRequest
import model.HostContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import utils.SpecBase
import utils.TestData.TEST_LINK_TEXT
import views.html.manage.*
import views.html.manage.digital_false_full

class DigitalFalseFullSpec extends SpecBase {

  "view" should {

    "render the contents correctly" in {
      implicit val hostContextForView: HostContext = hostContext()

      val viewDocument: Document = Jsoup.parse(app.injector.instanceOf[digital_false_full].apply(TEST_LINK_TEXT).body)

      viewDocument.getElementById("saCheckSettings").html() mustBe messages("manage.paperless.header")
      viewDocument.getElementsByClass("govuk-heading-m").get(0).html() mustBe messages(
        "manage.paperless.contact.header"
      )

      viewDocument.getElementsByClass("govuk-button").get(0).html() mustBe messages(
        s"manage.paperless.cys.button.$TEST_LINK_TEXT"
      )

      val linkElement: Elements = viewDocument.getElementsByTag("a")
      linkElement.get(0).html() mustBe messages("manage.paperless.change")
    }
  }
}
